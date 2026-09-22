package fr.acano.workout.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import fr.acano.workout.MainActivity
import fr.acano.workout.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Porte le décompte pendant la récupération et les exercices chronométrés.
 *
 * Deux choix structurent ce service :
 *  1. le temps restant est toujours recalculé depuis une **échéance absolue**
 *     ([SystemClock.elapsedRealtime]), jamais accumulé : aucune dérive, et la reprise
 *     après une mise en arrière-plan est exacte ;
 *  2. c'est un **service de premier plan** : Android ne gèle pas le process, donc le
 *     chrono se termine et vibre même écran éteint.
 */
class RestTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob())
    private var tickJob: Job? = null

    private var kind: TimerKind = TimerKind.REST
    private var label: String = ""
    private var totalMs: Long = 0
    private var deadlineElapsed: Long = 0
    private var pausedRemainingMs: Long? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_ADD -> handleAdd(intent.getLongExtra(EXTRA_DURATION_MS, 30_000L))
            ACTION_STOP -> stopTimer()
            else -> stopTimer()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        kind = TimerKind.valueOf(intent.getStringExtra(EXTRA_KIND) ?: TimerKind.REST.name)
        label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        totalMs = intent.getLongExtra(EXTRA_DURATION_MS, 60_000L)
        deadlineElapsed = SystemClock.elapsedRealtime() + totalMs
        pausedRemainingMs = null

        startForegroundCompat(buildRunningNotification(totalMs))
        publish(totalMs, running = true, finished = false)
        startTicking()
    }

    private fun handlePause() {
        if (pausedRemainingMs != null) return
        val remaining = remainingMs()
        pausedRemainingMs = remaining
        tickJob?.cancel()
        publish(remaining, running = false, finished = false)
        updateNotification(remaining)
    }

    private fun handleResume() {
        val remaining = pausedRemainingMs ?: return
        deadlineElapsed = SystemClock.elapsedRealtime() + remaining
        pausedRemainingMs = null
        publish(remaining, running = true, finished = false)
        startTicking()
    }

    private fun handleAdd(extraMs: Long) {
        totalMs += extraMs
        val paused = pausedRemainingMs
        if (paused != null) {
            pausedRemainingMs = paused + extraMs
            publish(paused + extraMs, running = false, finished = false)
            updateNotification(paused + extraMs)
        } else {
            deadlineElapsed += extraMs
            publish(remainingMs(), running = true, finished = false)
            if (tickJob?.isActive != true) startTicking()
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            var lastShownSecond = -1
            while (isActive) {
                val remaining = remainingMs()
                if (remaining <= 0) {
                    onTimerFinished()
                    return@launch
                }
                publish(remaining, running = true, finished = false)
                val second = ((remaining + 999) / 1000).toInt()
                if (second != lastShownSecond) {
                    lastShownSecond = second
                    updateNotification(remaining)
                }
                delay(TICK_MS)
            }
        }
    }

    private fun onTimerFinished() {
        publish(0, running = false, finished = true)
        vibrate()
        notifyFinished()
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopTimer() {
        tickJob?.cancel()
        ActiveTimer.update(null)
        NotificationManagerCompat.from(this).cancel(DONE_NOTIFICATION_ID)
        stopForegroundCompat()
        stopSelf()
    }

    private fun remainingMs(): Long =
        pausedRemainingMs ?: (deadlineElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0)

    private fun publish(remaining: Long, running: Boolean, finished: Boolean) {
        ActiveTimer.update(
            TimerState(
                kind = kind,
                label = label,
                totalMs = totalMs,
                remainingMs = remaining,
                isRunning = running,
                isFinished = finished,
            ),
        )
    }

    // --- Notifications ---

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun buildRunningNotification(remaining: Long): Notification =
        NotificationCompat.Builder(this, TimerNotifications.RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(if (kind == TimerKind.REST) "Repos" else label)
            .setContentText(formatClock(remaining))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(contentIntent())
            .build()

    /** Sur Android 13+, notifier sans la permission lève une SecurityException. */
    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // La permission est vérifiée par canNotify(), que lint ne sait pas reconnaître comme un garde.
    @SuppressLint("MissingPermission")
    private fun updateNotification(remaining: Long) {
        if (!canNotify() || !NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        runCatching {
            NotificationManagerCompat.from(this)
                .notify(TimerNotifications.RUNNING_NOTIFICATION_ID, buildRunningNotification(remaining))
        }
    }

    @SuppressLint("MissingPermission")
    private fun notifyFinished() {
        val notification = NotificationCompat.Builder(this, TimerNotifications.DONE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(if (kind == TimerKind.REST) "Repos terminé" else "$label terminé")
            .setContentText(if (kind == TimerKind.REST) "Série suivante" else "Série validée")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        if (!canNotify()) return
        runCatching {
            NotificationManagerCompat.from(this).notify(DONE_NOTIFICATION_ID, notification)
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    TimerNotifications.RUNNING_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(TimerNotifications.RUNNING_NOTIFICATION_ID, notification)
            }
        }
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService<Vibrator>()
        } ?: return
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 250), -1),
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "fr.acano.workout.timer.START"
        private const val ACTION_PAUSE = "fr.acano.workout.timer.PAUSE"
        private const val ACTION_RESUME = "fr.acano.workout.timer.RESUME"
        private const val ACTION_ADD = "fr.acano.workout.timer.ADD"
        private const val ACTION_STOP = "fr.acano.workout.timer.STOP"

        private const val EXTRA_KIND = "kind"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_DURATION_MS = "durationMs"

        private const val DONE_NOTIFICATION_ID = 1002
        private const val TICK_MS = 200L

        fun start(context: Context, kind: TimerKind, label: String, durationMs: Long) {
            val intent = Intent(context, RestTimerService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_KIND, kind.name)
                .putExtra(EXTRA_LABEL, label)
                .putExtra(EXTRA_DURATION_MS, durationMs)
            context.startForegroundService(intent)
        }

        fun pause(context: Context) = send(context, ACTION_PAUSE)

        fun resume(context: Context) = send(context, ACTION_RESUME)

        fun addTime(context: Context, extraMs: Long = 30_000L) {
            context.startService(
                Intent(context, RestTimerService::class.java)
                    .setAction(ACTION_ADD)
                    .putExtra(EXTRA_DURATION_MS, extraMs),
            )
        }

        fun stop(context: Context) {
            ActiveTimer.update(null)
            send(context, ACTION_STOP)
        }

        private fun send(context: Context, action: String) {
            runCatching {
                context.startService(
                    Intent(context, RestTimerService::class.java).setAction(action),
                )
            }
        }
    }
}

/** « 01:23 » — format unique utilisé par la notification et par l'écran de séance. */
fun formatClock(millis: Long): String {
    val totalSeconds = ((millis + 999) / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

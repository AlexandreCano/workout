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
import android.view.View
import android.widget.RemoteViews
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
import java.util.concurrent.atomic.AtomicBoolean

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

    /**
     * Identifiant de la dernière commande reçue.
     *
     * Indispensable : `stopSelf()` sans identifiant arrête le service en jetant
     * toute commande arrivée entre-temps. Si un `ACTION_START` est ainsi perdu
     * après un `startForegroundService()`, le système tue l'application avec une
     * `RemoteServiceException` non rattrapable. `stopSelf(startId)` ne s'arrête
     * au contraire que si aucune commande plus récente n'est en attente.
     */
    private var latestStartId: Int = 0

    private var kind: TimerKind = TimerKind.REST
    private var label: String = ""
    private var totalMs: Long = 0
    private var deadlineElapsed: Long = 0
    private var pausedRemainingMs: Long? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning.set(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
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
        // Le « Repos terminé » du chrono précédent n'a plus de sens une fois le suivant lancé.
        NotificationManagerCompat.from(this).cancel(TimerNotifications.DONE_NOTIFICATION_ID)

        if (!startForegroundCompat(buildRunningNotification(totalMs))) {
            // Sans passage en premier plan, le contrat de startForegroundService
            // ne peut pas être tenu : on s'arrête nous-mêmes plutôt que de laisser
            // le système tuer l'application cinq secondes plus tard.
            stopTimer()
            return
        }
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
        updateNotification(remaining)
        startTicking()
    }

    /**
     * Ajoute [deltaMs] au temps restant, ou en retire s'il est négatif.
     *
     * On ne retire jamais plus que ce qui reste : le décalage réellement
     * appliqué est reporté sur la durée totale, qui reste ainsi celle du chrono
     * tel qu'il a été vécu (anneau de progression, durée d'effort enregistrée).
     * Arriver à zéro termine le chrono comme s'il était allé au bout.
     */
    private fun handleAdd(deltaMs: Long) {
        val before = remainingMs()
        val after = (before + deltaMs).coerceAtLeast(0)
        val applied = after - before
        totalMs = (totalMs + applied).coerceAtLeast(0)

        if (after == 0L) {
            tickJob?.cancel()
            pausedRemainingMs = null
            onTimerFinished()
            return
        }
        val paused = pausedRemainingMs
        if (paused != null) {
            pausedRemainingMs = after
            publish(after, running = false, finished = false)
            updateNotification(after)
        } else {
            deadlineElapsed += applied
            publish(after, running = true, finished = false)
            updateNotification(after)
            if (tickJob?.isActive != true) startTicking()
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val remaining = remainingMs()
                if (remaining <= 0) {
                    onTimerFinished()
                    return@launch
                }
                // Pas de mise à jour de la notification ici : le décompte y est
                // tenu par le système (voir buildRunningNotification).
                publish(remaining, running = true, finished = false)
                delay(TICK_MS)
            }
        }
    }

    private fun onTimerFinished() {
        publish(0, running = false, finished = true)
        vibrate()
        notifyFinished()
        stopForegroundCompat()
        stopSelf(latestStartId)
    }

    private fun stopTimer() {
        tickJob?.cancel()
        ActiveTimer.update(null)
        NotificationManagerCompat.from(this).cancel(TimerNotifications.DONE_NOTIFICATION_ID)
        stopForegroundCompat()
        stopSelf(latestStartId)
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

    /**
     * Notification du décompte, sur le modèle du minuteur de l'Horloge Pixel :
     *  - le temps restant **en grand** dans une mise en page personnalisée
     *    (le gabarit standard n'offre qu'une petite taille de texte). Le style
     *    « décoré » garde l'en-tête, l'icône et les boutons du système, donc
     *    l'apparence et le thème clair / sombre de la notification ;
     *  - en marche, un Chronometer que le système fait avancer seul, même si
     *    le processeur met l'application en veille ; on ne redessine la
     *    notification qu'aux changements d'état ;
     *  - publique : un chrono n'a rien de confidentiel, il s'affiche en entier
     *    sur l'écran verrouillé ;
     *  - Pause / Reprendre, pour ne pas avoir à déverrouiller.
     *
     * Contrepartie : une mise en page personnalisée exclut la notification des
     * Live Updates d'Android 16.
     */
    private fun buildRunningNotification(remaining: Long): Notification {
        val title = if (kind == TimerKind.REST) "Repos" else label
        val paused = pausedRemainingMs != null
        // En marche, le décompte se suffit à lui-même : pas de ligne secondaire.
        val subtitle = if (paused) "En pause" else null
        val collapsed = timerViews(R.layout.notification_timer_collapsed, title, subtitle, remaining, paused)
        val expanded = timerViews(R.layout.notification_timer_expanded, title, subtitle, remaining, paused)

        return NotificationCompat.Builder(this, TimerNotifications.RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            // Titre et texte restent renseignés : ils servent aux montres, à
            // l'accessibilité et à tout affichage qui ignore la vue personnalisée.
            .setContentTitle(title)
            .setContentText(if (paused) "En pause · ${formatClock(remaining)}" else null)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setShowWhen(false)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(contentIntent())
            .addAction(
                if (paused) {
                    NotificationCompat.Action(android.R.drawable.ic_media_play, "Reprendre", serviceIntent(ACTION_RESUME))
                } else {
                    NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", serviceIntent(ACTION_PAUSE))
                },
            )
            .build()
    }

    private fun timerViews(
        layout: Int,
        title: String,
        subtitle: String?,
        remaining: Long,
        paused: Boolean,
    ): RemoteViews = RemoteViews(packageName, layout).apply {
        setTextViewText(R.id.timer_title, title)
        if (subtitle == null) {
            setViewVisibility(R.id.timer_subtitle, View.GONE)
        } else {
            setViewVisibility(R.id.timer_subtitle, View.VISIBLE)
            setTextViewText(R.id.timer_subtitle, subtitle)
        }
        if (paused) {
            setViewVisibility(R.id.timer_chronometer, View.GONE)
            setViewVisibility(R.id.timer_paused_value, View.VISIBLE)
            setTextViewText(R.id.timer_paused_value, formatClock(remaining))
        } else {
            setViewVisibility(R.id.timer_paused_value, View.GONE)
            setViewVisibility(R.id.timer_chronometer, View.VISIBLE)
            // Compte à rebours : la base est l'échéance, sur la même horloge que le service.
            setChronometer(R.id.timer_chronometer, SystemClock.elapsedRealtime() + remaining, null, true)
            setChronometerCountDown(R.id.timer_chronometer, true)
        }
    }

    /** Les boutons de la notification renvoient simplement une commande au service. */
    private fun serviceIntent(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, RestTimerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        if (!canNotify()) return
        runCatching {
            NotificationManagerCompat.from(this).notify(TimerNotifications.DONE_NOTIFICATION_ID, notification)
        }
    }

    private fun startForegroundCompat(notification: Notification): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                TimerNotifications.RUNNING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(TimerNotifications.RUNNING_NOTIFICATION_ID, notification)
        }
    }.isSuccess

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
        isRunning.set(false)
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

        private const val TICK_MS = 200L

        /** Vrai entre `onCreate` et `onDestroy` du service. */
        private val isRunning = AtomicBoolean(false)

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

        /** Ajoute du temps au chrono en cours ; une valeur négative en retire. */
        fun addTime(context: Context, extraMs: Long = 30_000L) {
            context.startService(
                Intent(context, RestTimerService::class.java)
                    .setAction(ACTION_ADD)
                    .putExtra(EXTRA_DURATION_MS, extraMs),
            )
        }

        /**
         * Arrête le chronomètre. Si le service ne tourne pas, on se contente de
         * vider l'état : `startService()` le créerait uniquement pour le détruire,
         * et ce cycle création/destruction entrait en collision avec le
         * `startForegroundService()` qui suit immédiatement.
         */
        fun stop(context: Context) {
            ActiveTimer.update(null)
            if (isRunning.get()) send(context, ACTION_STOP)
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

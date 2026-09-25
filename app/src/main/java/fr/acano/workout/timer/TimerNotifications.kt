package fr.acano.workout.timer

import android.app.NotificationChannel
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import fr.acano.workout.R

object TimerNotifications {

    const val RUNNING_CHANNEL_ID = "timer_countdown"

    /**
     * Ancien canal du décompte, en importance faible : Android range ces
     * notifications parmi les « silencieuses », masquées par défaut sur l'écran
     * verrouillé. L'importance d'un canal ne pouvant plus changer une fois créé,
     * on le remplace par [RUNNING_CHANNEL_ID] et on supprime celui-ci.
     */
    private const val LEGACY_RUNNING_CHANNEL_ID = "timer_running"
    const val DONE_CHANNEL_ID = "timer_done"
    const val RUNNING_NOTIFICATION_ID = 1001
    const val DONE_NOTIFICATION_ID = 1002

    /**
     * Retire le « Repos terminé » : il sert à rappeler hors de l'application,
     * et n'a plus rien à dire une fois qu'on y est revenu.
     */
    fun dismissFinished(context: Context) {
        NotificationManagerCompat.from(context).cancel(DONE_NOTIFICATION_ID)
    }

    /**
     * Deux canaux distincts : le décompte doit rester silencieux, seule la fin
     * émet un son léger — et le mode silencieux du téléphone est respecté
     * automatiquement puisqu'on passe par le système de notifications.
     *
     * Le décompte est en importance normale, mais sans son ni vibration : c'est
     * ce qui le garde visible sur l'écran verrouillé sans jamais faire de bruit.
     */
    fun createChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        manager.deleteNotificationChannel(LEGACY_RUNNING_CHANNEL_ID)
        manager.createNotificationChannel(
            NotificationChannel(
                RUNNING_CHANNEL_ID,
                context.getString(R.string.channel_running_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_running_description)
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                DONE_CHANNEL_ID,
                context.getString(R.string.channel_done_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_done_description)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            },
        )
    }
}

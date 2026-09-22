package fr.acano.workout.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.content.getSystemService

object TimerNotifications {

    const val RUNNING_CHANNEL_ID = "timer_running"
    const val DONE_CHANNEL_ID = "timer_done"
    const val RUNNING_NOTIFICATION_ID = 1001

    /**
     * Deux canaux distincts : le décompte doit rester silencieux, seule la fin
     * émet un son léger — et le mode silencieux du téléphone est respecté
     * automatiquement puisqu'on passe par le système de notifications.
     */
    fun createChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                RUNNING_CHANNEL_ID,
                "Chronomètre en cours",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Décompte affiché pendant la récupération ou un exercice chronométré."
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                DONE_CHANNEL_ID,
                "Fin de chronomètre",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Signale la fin de la récupération ou de la série chronométrée."
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

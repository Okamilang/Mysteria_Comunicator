package com.okamilang.mysteria.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager

object Notifs {

    const val CHANNEL_DEPECHES = "depeches"
    const val CHANNEL_URGENT = "urgent"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (nm.getNotificationChannel(CHANNEL_DEPECHES) == null) {
            val channel = NotificationChannel(
                CHANNEL_DEPECHES,
                "Dépêches",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Réception de nouvelles dépêches d'agents"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 200, 250)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }
            nm.createNotificationChannel(channel)
        }

        if (nm.getNotificationChannel(CHANNEL_URGENT) == null) {
            // Canal haute importance pour les transmissions urgentes :
            // bypass Ne pas déranger, sonnerie de ringtone, vibration agressive,
            // affichage en plein écran (full-screen intent) quand l'écran est verrouillé.
            val channel = NotificationChannel(
                CHANNEL_URGENT,
                "Transmissions urgentes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes prioritaires nécessitant une réponse immédiate"
                setBypassDnd(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                )
            }
            nm.createNotificationChannel(channel)
        }
    }
}

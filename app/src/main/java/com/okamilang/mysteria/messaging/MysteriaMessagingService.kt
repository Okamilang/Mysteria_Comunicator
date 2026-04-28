package com.okamilang.mysteria.messaging

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.okamilang.mysteria.R
import com.okamilang.mysteria.urgent.UrgentTransmissionActivity

/**
 * Reçoit les FCM. Pour les dépêches urgentes on construit une notification
 * full-screen-intent qui demande au système Android de lancer notre écran
 * urgent par-dessus l'écran de verrouillage. C'est la seule méthode fiable
 * sur Android 10+, où startActivity() depuis un service en arrière-plan
 * est bloqué.
 */
class MysteriaMessagingService : FirebaseMessagingService() {

    private val tag = "Mysteria/FCMservice"

    override fun onNewToken(token: String) {
        Log.i(tag, "Nouveau jeton FCM")
        FcmTokens.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val type = data["type"].orEmpty()
        Log.i(tag, "Dépêche reçue (type=$type)")

        if (type == "urgent") {
            afficherTransmissionUrgente(data)
        }
        // Pour les dépêches normales, le bloc notification du payload FCM
        // est déjà affiché par le système — rien à faire ici.
    }

    private fun afficherTransmissionUrgente(data: Map<String, String>) {
        val fromCode = data["fromCode"] ?: "Agent inconnu"
        val fromUid = data["fromUid"].orEmpty()
        val body = data["body"].orEmpty()
        val messageId = data["messageId"].orEmpty()

        val urgentIntent = Intent(this, UrgentTransmissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(UrgentTransmissionActivity.EXTRA_FROM_CODE, fromCode)
            putExtra(UrgentTransmissionActivity.EXTRA_FROM_UID, fromUid)
            putExtra(UrgentTransmissionActivity.EXTRA_BODY, body)
            putExtra(UrgentTransmissionActivity.EXTRA_MESSAGE_ID, messageId)
        }
        val fullScreen = PendingIntent.getActivity(
            this,
            messageId.hashCode(),
            urgentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, Notifs.CHANNEL_URGENT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠ TRANSMISSION URGENTE — $fromCode")
            .setContentText(body.ifBlank { "tente de vous joindre par fréquence prioritaire" })
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    if (body.isBlank()) "$fromCode tente de vous joindre par fréquence prioritaire"
                    else "$fromCode :\n$body"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .build()

        // L'utilisation de notify() avec un setFullScreenIntent + canal
        // IMPORTANCE_HIGH déclenche le lancement automatique de l'activité
        // par le système (équivalent appel entrant). Si l'écran est déjà
        // déverrouillé, l'utilisateur voit une notification heads-up.
        runCatching {
            NotificationManagerCompat.from(this).notify(messageId.hashCode(), notif)
        }.onFailure { Log.w(tag, "Échec notify urgent", it) }
    }
}

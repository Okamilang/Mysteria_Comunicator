package com.okamilang.mysteria.messaging

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.okamilang.mysteria.urgent.UrgentTransmissionActivity

/**
 * Reçoit les FCM. Si le payload est marqué "urgent", on lance l'écran
 * de Transmission Urgente (full-screen, vibration soutenue). Sinon, le
 * système Android affiche directement la notification (via le bloc
 * notification du payload), donc on n'a rien à faire.
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
            val intent = Intent(this, UrgentTransmissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(UrgentTransmissionActivity.EXTRA_FROM_CODE, data["fromCode"])
                putExtra(UrgentTransmissionActivity.EXTRA_FROM_UID, data["fromUid"])
                putExtra(UrgentTransmissionActivity.EXTRA_BODY, data["body"])
                putExtra(UrgentTransmissionActivity.EXTRA_MESSAGE_ID, data["messageId"])
            }
            startActivity(intent)
        }
        // Pour le type "normal", la notification système est gérée par
        // le payload notification du FCM — rien à faire ici.
    }
}

package com.okamilang.mysteria.messaging

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Gestion du jeton FCM de l'agent : on l'enregistre dans son document
 * `agents/{uid}` (champ array `fcmTokens`) à chaque connexion et à
 * chaque rafraîchissement du jeton par Firebase.
 */
object FcmTokens {

    private const val TAG = "Mysteria/FCM"

    /** Récupère le jeton FCM courant et l'attache à l'agent connecté. */
    suspend fun ensureRegistered() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            attach(uid, token)
        }.onFailure { Log.w(TAG, "ensureRegistered échec", it) }
    }

    /** Variante synchrone pour le callback onNewToken du service. */
    fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().doc("agents/$uid")
            .update("fcmTokens", FieldValue.arrayUnion(token))
            .addOnFailureListener { Log.w(TAG, "onNewToken échec", it) }
    }

    private suspend fun attach(uid: String, token: String) {
        FirebaseFirestore.getInstance().doc("agents/$uid")
            .update("fcmTokens", FieldValue.arrayUnion(token))
            .await()
        Log.i(TAG, "Jeton FCM attaché à l'agent $uid")
    }

    /** Détache le jeton courant (à appeler avant signOut). */
    suspend fun unregister() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            FirebaseFirestore.getInstance().doc("agents/$uid")
                .update("fcmTokens", FieldValue.arrayRemove(token))
                .await()
        }.onFailure { Log.w(TAG, "unregister échec", it) }
    }
}

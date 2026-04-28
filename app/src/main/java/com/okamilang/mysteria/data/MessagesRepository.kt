package com.okamilang.mysteria.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object MessagesRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Envoie une dépêche au destinataire identifié par son nom de code.
     * Le destinataire doit exister dans la collection `agents`.
     */
    suspend fun sendMessage(toCodeName: String, body: String): Result<Unit> = runCatching {
        val me = auth.currentUser ?: error("Agent non identifié")
        require(body.isNotBlank()) { "Dépêche vide" }

        val cleanCode = toCodeName.trim()
        require(cleanCode.isNotEmpty()) { "Destinataire requis" }

        val target = db.collection("agents")
            .whereEqualTo("codeName", cleanCode)
            .limit(1)
            .get()
            .await()
        require(!target.isEmpty) { "Aucun agent ne porte ce nom de code" }
        val targetDoc = target.documents.first()
        val toUid = targetDoc.getString("uid") ?: targetDoc.id

        val myDoc = db.collection("agents").document(me.uid).get().await()
        val fromCode = myDoc.getString("codeName") ?: me.email.orEmpty()

        db.collection("messages").add(
            mapOf(
                "fromUid" to me.uid,
                "fromCode" to fromCode,
                "toUid" to toUid,
                "toCode" to cleanCode,
                "body" to body.trim(),
                "sentAt" to Timestamp.now(),
                "participants" to listOf(me.uid, toUid)
            )
        ).await()
    }

    /**
     * Flux des dépêches reçues par l'agent courant, plus récentes en tête.
     */
    fun observeInbox(): Flow<List<Message>> = callbackFlow {
        val me = auth.currentUser
        if (me == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = db.collection("messages")
            .whereEqualTo("toUid", me.uid)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().mapNotNull { d ->
                    Message(
                        id = d.id,
                        fromUid = d.getString("fromUid") ?: return@mapNotNull null,
                        fromCode = d.getString("fromCode") ?: "Agent inconnu",
                        toUid = d.getString("toUid") ?: return@mapNotNull null,
                        toCode = d.getString("toCode") ?: "",
                        body = d.getString("body") ?: "",
                        sentAt = d.getTimestamp("sentAt")?.toDate()?.time ?: 0L
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }
}

data class Message(
    val id: String,
    val fromUid: String,
    val fromCode: String,
    val toUid: String,
    val toCode: String,
    val body: String,
    val sentAt: Long
)

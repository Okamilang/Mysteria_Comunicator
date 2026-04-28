package com.okamilang.mysteria.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
     * Flux de toutes les dépêches où l'agent courant est impliqué (envoyées ou reçues).
     * Tri client pour éviter d'exiger un index composite.
     */
    fun observeAllMyMessages(): Flow<List<Message>> = callbackFlow {
        val me = auth.currentUser
        if (me == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = db.collection("messages")
            .whereArrayContains("participants", me.uid)
            .limit(500)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("Mysteria", "observeAllMyMessages error", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().mapNotNull(::mapDoc)
                    .sortedByDescending { it.sentAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /**
     * Flux des messages d'une conversation entre l'agent courant et `otherUid`,
     * du plus ancien au plus récent (ordre naturel de fil de discussion).
     */
    fun observeConversation(otherUid: String): Flow<List<Message>> = callbackFlow {
        val me = auth.currentUser
        if (me == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = db.collection("messages")
            .whereArrayContains("participants", me.uid)
            .limit(500)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("Mysteria", "observeConversation error", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty()
                    .mapNotNull(::mapDoc)
                    .filter { msg ->
                        val parts = msg.participants
                        parts.contains(me.uid) && parts.contains(otherUid)
                    }
                    .sortedBy { it.sentAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    private fun mapDoc(d: com.google.firebase.firestore.DocumentSnapshot): Message? {
        val from = d.getString("fromUid") ?: return null
        val to = d.getString("toUid") ?: return null
        @Suppress("UNCHECKED_CAST")
        val parts = (d.get("participants") as? List<String>) ?: listOf(from, to)
        return Message(
            id = d.id,
            fromUid = from,
            fromCode = d.getString("fromCode") ?: "Agent inconnu",
            toUid = to,
            toCode = d.getString("toCode") ?: "",
            body = d.getString("body") ?: "",
            sentAt = d.getTimestamp("sentAt")?.toDate()?.time ?: 0L,
            participants = parts
        )
    }
}

data class Message(
    val id: String,
    val fromUid: String,
    val fromCode: String,
    val toUid: String,
    val toCode: String,
    val body: String,
    val sentAt: Long,
    val participants: List<String> = emptyList()
)

/** Résumé d'une conversation pour l'écran Dépêches. */
data class ConversationSummary(
    val otherUid: String,
    val otherCode: String,
    val dernierMessage: String,
    val dernierEnvoiMs: Long,
    val dernierEstDeMoi: Boolean
)

/** Regroupe une liste de messages par interlocuteur (autre que `meUid`). */
fun groupConversations(messages: List<Message>, meUid: String): List<ConversationSummary> {
    if (meUid.isBlank()) return emptyList()
    return messages
        .groupBy { msg -> if (msg.fromUid == meUid) msg.toUid to msg.toCode else msg.fromUid to msg.fromCode }
        .map { (cle, list) ->
            val (otherUid, otherCode) = cle
            val dernier = list.maxBy { it.sentAt }
            ConversationSummary(
                otherUid = otherUid,
                otherCode = otherCode,
                dernierMessage = dernier.body,
                dernierEnvoiMs = dernier.sentAt,
                dernierEstDeMoi = dernier.fromUid == meUid
            )
        }
        .sortedByDescending { it.dernierEnvoiMs }
}

package com.okamilang.mysteria.data

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

object MessagesRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /** Envoie une dépêche texte. */
    suspend fun sendMessage(
        toCodeName: String,
        body: String,
        urgent: Boolean = false
    ): Result<Unit> = runCatching {
        require(body.isNotBlank()) { "Dépêche vide" }
        envoyer(toCodeName, body.trim(), urgent, null, null, null)
    }

    /** Envoie une plaque photographique (image), depuis une URI locale. */
    suspend fun sendImage(
        context: Context,
        toCodeName: String,
        imageUri: Uri,
        legende: String = "",
        urgent: Boolean = false
    ): Result<Unit> = runCatching {
        val me = auth.currentUser ?: error("Agent non identifié")
        val ext = guessExtension(context, imageUri, default = "jpg")
        val ref = storage.reference
            .child("messages/${me.uid}/${UUID.randomUUID()}.$ext")
        context.contentResolver.openInputStream(imageUri).use { stream ->
            requireNotNull(stream) { "Impossible de lire l'image sélectionnée" }
            ref.putStream(stream).await()
        }
        val url = ref.downloadUrl.await().toString()
        envoyer(toCodeName, legende.trim(), urgent, "image", url, null)
    }

    /** Envoie un cylindre phonographique (vocal). */
    suspend fun sendAudio(
        toCodeName: String,
        audioFile: File,
        durationMs: Long,
        urgent: Boolean = false
    ): Result<Unit> = runCatching {
        val me = auth.currentUser ?: error("Agent non identifié")
        require(audioFile.exists() && audioFile.length() > 0) { "Cylindre invalide" }
        val ref = storage.reference
            .child("messages/${me.uid}/${UUID.randomUUID()}.m4a")
        ref.putFile(Uri.fromFile(audioFile)).await()
        val url = ref.downloadUrl.await().toString()
        envoyer(toCodeName, "", urgent, "audio", url, durationMs)
    }

    private suspend fun envoyer(
        toCodeName: String,
        body: String,
        urgent: Boolean,
        attachmentType: String?,
        attachmentUrl: String?,
        audioDurationMs: Long?
    ) {
        val me = auth.currentUser ?: error("Agent non identifié")

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

        val payload = mutableMapOf<String, Any?>(
            "fromUid" to me.uid,
            "fromCode" to fromCode,
            "toUid" to toUid,
            "toCode" to cleanCode,
            "body" to body,
            "sentAt" to Timestamp.now(),
            "participants" to listOf(me.uid, toUid),
            "urgent" to urgent
        )
        if (attachmentType != null) {
            payload["attachmentType"] = attachmentType
            payload["attachmentUrl"] = attachmentUrl
            if (audioDurationMs != null) payload["audioDurationMs"] = audioDurationMs
        }

        db.collection("messages").add(payload).await()
    }

    private fun guessExtension(context: Context, uri: Uri, default: String): String {
        val mime = context.contentResolver.getType(uri).orEmpty()
        return when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            else -> default
        }
    }

    /** Toutes les dépêches où l'agent courant est impliqué (envoyées ou reçues). */
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

    /** Messages d'une conversation entre l'agent courant et `otherUid`. */
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
            participants = parts,
            attachmentType = d.getString("attachmentType"),
            attachmentUrl = d.getString("attachmentUrl"),
            audioDurationMs = d.getLong("audioDurationMs")
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
    val participants: List<String> = emptyList(),
    val attachmentType: String? = null,
    val attachmentUrl: String? = null,
    val audioDurationMs: Long? = null
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
            val preview = when (dernier.attachmentType) {
                "image" -> "📷 Plaque photographique"
                "audio" -> "🎙 Cylindre"
                else -> dernier.body
            }
            ConversationSummary(
                otherUid = otherUid,
                otherCode = otherCode,
                dernierMessage = preview,
                dernierEnvoiMs = dernier.sentAt,
                dernierEstDeMoi = dernier.fromUid == meUid
            )
        }
        .sortedByDescending { it.dernierEnvoiMs }
}

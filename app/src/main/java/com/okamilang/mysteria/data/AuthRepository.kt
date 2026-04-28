package com.okamilang.mysteria.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa -> trySend(fa.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        val res = auth.signInWithEmailAndPassword(email.trim(), password).await()
        res.user ?: error("Aucun agent renvoyé")
    }

    suspend fun signUp(email: String, password: String, codeName: String): Result<FirebaseUser> = runCatching {
        val cleanCode = codeName.trim()
        require(cleanCode.length in 2..40) { "Nom de code invalide" }

        val existing = db.collection("agents")
            .whereEqualTo("codeName", cleanCode)
            .limit(1)
            .get()
            .await()
        require(existing.isEmpty) { "Ce nom de code est déjà attribué à un autre agent" }

        val res = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = res.user ?: error("Création échouée")

        db.collection("agents").document(user.uid).set(
            mapOf(
                "uid" to user.uid,
                "codeName" to cleanCode,
                "email" to email.trim(),
                "createdAt" to com.google.firebase.Timestamp.now()
            )
        ).await()

        user
    }

    fun signOut() = auth.signOut()

    suspend fun currentAgent(): Agent? {
        val u = auth.currentUser ?: return null
        val doc = db.collection("agents").document(u.uid).get().await()
        if (!doc.exists()) return null
        return Agent(
            uid = u.uid,
            codeName = doc.getString("codeName") ?: "",
            email = doc.getString("email") ?: u.email.orEmpty()
        )
    }
}

data class Agent(
    val uid: String,
    val codeName: String,
    val email: String
)

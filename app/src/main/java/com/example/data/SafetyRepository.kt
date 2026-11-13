package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

const val COMMUNITY_TERMS_VERSION = "2026-11-13"

class SafetyRepository(private val db: FirebaseFirestore) {
    private val uid = requireNotNull(Firebase.auth.currentUser).uid
    private val user = db.collection("users").document(uid)
    private fun checkSession() = check(Firebase.auth.currentUser?.uid == uid) { "Sign in again." }

    fun observeBlocks() = user.collection("blocks").snapshots().map { it.documents.map { doc -> doc.id }.toSet() }
    suspend fun block(authorId: String) {
        checkSession()
        require(authorId != uid && authorId.isNotBlank())
        user.collection("blocks").document(authorId).set(mapOf("createdAt" to FieldValue.serverTimestamp())).await()
    }
    suspend fun unblock(authorId: String) {
        checkSession()
        user.collection("blocks").document(authorId).delete().await()
    }
    suspend fun acceptTerms() {
        checkSession()
        user.collection("preferences").document("community").set(mapOf(
            "termsVersion" to COMMUNITY_TERMS_VERSION, "acceptedAt" to FieldValue.serverTimestamp()
        )).await()
    }
    fun observeTerms() = user.collection("preferences").document("community").snapshots()
        .map { it.getString("termsVersion") == COMMUNITY_TERMS_VERSION }

    suspend fun report(kind: String, ideaId: String, commentId: String, reason: String, details: String) {
        checkSession()
        require(kind in listOf("idea", "comment", "ai"))
        require(reason in REPORT_REASONS && details.length <= 2000)
        user.collection("reports").document().set(mapOf(
            "kind" to kind, "ideaId" to ideaId, "commentId" to commentId,
            "reason" to reason, "details" to details.trim(), "createdAt" to FieldValue.serverTimestamp()
        )).await()
    }
}

val REPORT_REASONS = listOf("Unsafe or illegal", "Harassment or hate", "Sexual content", "Spam or misleading", "Other")

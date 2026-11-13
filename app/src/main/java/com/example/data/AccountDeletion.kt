package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/** Resumable cleanup. A server-enforced marker prevents writes from other devices. */
class AccountDeletion(private val auth: FirebaseAuth, private val db: FirebaseFirestore) {
    suspend fun isPending(cacheOnly: Boolean = false): Boolean {
        val uid = requireNotNull(auth.currentUser).uid
        return db.collection("accountDeletions").document(uid).get(if (cacheOnly) Source.CACHE else Source.SERVER).await().exists()
    }

    suspend fun deleteAccount() {
        val user = requireNotNull(auth.currentUser) { "Sign in again to delete your account." }
        val uid = user.uid
        fun checkSession() = check(auth.currentUser?.uid == uid) { "Your account changed. Sign in again." }
        checkSession()
        val marker = db.collection("accountDeletions").document(uid)
        // A transaction fails offline instead of queueing a deletion request on the device.
        db.runTransaction { tx ->
            if (!tx.get(marker).exists()) tx.set(marker, mapOf("startedAt" to FieldValue.serverTimestamp()))
        }.await()
        checkSession()

        // Includes comments on ideas that were made private or deleted by someone else.
        deletePages(db.collectionGroup("comments").whereEqualTo("userId", uid), ::checkSession)
        while (true) {
            checkSession()
            val ideas = db.collection("ideas").whereEqualTo("userId", uid).limit(100).get(Source.SERVER).await()
            if (ideas.isEmpty) break
            for (idea in ideas.documents) {
                deletePages(idea.reference.collection("comments"), ::checkSession)
                checkSession()
                idea.reference.delete().await()
            }
        }
        for (collection in listOf("blocks", "reports", "preferences")) {
            deletePages(db.collection("users").document(uid).collection(collection), ::checkSession)
        }
        checkSession()
        // Keep only the UID-keyed marker: existing ID tokens must never recreate deleted data.
        // It contains no email, name, idea text or report content.
        user.delete().await()
    }

    private suspend fun deletePages(query: Query, checkSession: () -> Unit) {
        while (true) {
            checkSession()
            val page = query.limit(100).get(Source.SERVER).await()
            if (page.isEmpty) return
            checkSession()
            db.batch().also { batch -> page.documents.forEach { batch.delete(it.reference) } }.commit().await()
        }
    }
}

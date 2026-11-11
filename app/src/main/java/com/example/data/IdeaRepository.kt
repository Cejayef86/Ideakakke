package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.Comment
import com.example.model.Idea
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "IdeaRepository"

class IdeaRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google to access ideas.")
    }

    /**
     * Observes ideas created by the authenticated user, ordered by creation time descending.
     */
    fun observeUserIdeas(): Flow<List<Idea>> = flow {
        val uid = requireUserId()
        val query = db.collection("ideas")
            .whereEqualTo("userId", uid)

        val flow = query.snapshots().map { snapshot ->
            snapshot.toObjects<Idea>().sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.e(TAG, "Error observing user ideas: ${firestoreError?.message ?: error.message}", error)
            throw error
        }
        emitAll(flow)
    }

    /**
     * Observes public/shared community ideas created by users for collaboration.
     */
    fun observeCommunityIdeas(): Flow<List<Idea>> = flow {
        val flow = db.collection("ideas")
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects<Idea>().sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.e(TAG, "Error observing community ideas: ${firestoreError?.message ?: error.message}", error)
                throw error
            }
        emitAll(flow)
    }

    /**
     * Observes a specific idea by document ID.
     */
    fun observeIdea(ideaId: String): Flow<Idea?> {
        return db.collection("ideas").document(ideaId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(Idea::class.java) else null
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.e(TAG, "Error observing idea $ideaId: ${error.message}", error)
                throw error
            }
    }

    suspend fun createIdea(
        title: String,
        description: String,
        category: String,
        tags: List<String>,
        priority: String,
        matrixQuadrant: String,
        isPublic: Boolean,
        authorName: String
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("ideas").document()
        val payload = hashMapOf<String, Any?>(
            "userId" to uid,
            "authorName" to authorName,
            "title" to title.trim(),
            "description" to description.trim(),
            "category" to category.trim(),
            "tags" to tags,
            "priority" to priority,
            "matrixQuadrant" to matrixQuadrant,
            "isPublic" to isPublic,
            "collaborators" to listOf(authorName.ifBlank { "Creator" }),
            "aiResearchSummary" to "",
            "aiActionPlan" to emptyList<String>(),
            "aiMarketplaceKeywords" to emptyList<String>(),
            "aiMarketplaceItems" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create idea: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateIdea(idea: Idea) {
        val uid = requireUserId()
        require(idea.id.isNotBlank()) { "Idea ID must not be blank" }
        val docRef = db.collection("ideas").document(idea.id)

        val updates = hashMapOf<String, Any?>(
            "title" to idea.title.trim(),
            "description" to idea.description.trim(),
            "category" to idea.category.trim(),
            "tags" to idea.tags,
            "priority" to idea.priority,
            "matrixQuadrant" to idea.matrixQuadrant,
            "isPublic" to idea.isPublic,
            "collaborators" to idea.collaborators,
            "aiResearchSummary" to idea.aiResearchSummary,
            "aiActionPlan" to idea.aiActionPlan,
            "aiMarketplaceKeywords" to idea.aiMarketplaceKeywords,
            "aiMarketplaceItems" to idea.aiMarketplaceItems,
            "authorName" to idea.authorName,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update idea ${idea.id}: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateAiResearch(
        ideaId: String,
        researchSummary: String,
        actionPlan: List<String>,
        marketplaceKeywords: List<String>,
        marketplaceItemsJson: String
    ) {
        val docRef = db.collection("ideas").document(ideaId)
        val updates = hashMapOf<String, Any?>(
            "aiResearchSummary" to researchSummary,
            "aiActionPlan" to actionPlan,
            "aiMarketplaceKeywords" to marketplaceKeywords,
            "aiMarketplaceItems" to marketplaceItemsJson,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update AI research for $ideaId: ${e.message}", e)
            throw e
        }
    }

    suspend fun addCollaborator(ideaId: String, collaboratorName: String) {
        val docRef = db.collection("ideas").document(ideaId)
        try {
            docRef.update(
                "collaborators", FieldValue.arrayUnion(collaboratorName.trim()),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add collaborator to $ideaId: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteIdea(ideaId: String) {
        val uid = requireUserId()
        val docRef = db.collection("ideas").document(ideaId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete idea $ideaId: ${e.message}", e)
            throw e
        }
    }

    fun observeComments(ideaId: String): Flow<List<Comment>> {
        return db.collection("ideas").document(ideaId).collection("comments")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects<Comment>().sortedBy { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.e(TAG, "Error observing comments for $ideaId: ${error.message}", error)
                throw error
            }
    }

    suspend fun addComment(ideaId: String, content: String, userName: String) {
        val uid = requireUserId()
        val commentRef = db.collection("ideas").document(ideaId).collection("comments").document()
        val payload = hashMapOf<String, Any?>(
            "ideaId" to ideaId,
            "userId" to uid,
            "userName" to userName.ifBlank { "Innovator" },
            "content" to content.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            commentRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add comment to $ideaId: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteComment(ideaId: String, commentId: String) {
        val docRef = db.collection("ideas").document(ideaId).collection("comments").document(commentId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete comment $commentId: ${e.message}", e)
            throw e
        }
    }
}

private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(flow: Flow<T>) {
    flow.collect { emit(it) }
}

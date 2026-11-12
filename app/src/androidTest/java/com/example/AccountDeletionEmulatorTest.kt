package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.AccountDeletion
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable accounts in local emulators only. No production account is used or deleted. */
@RunWith(AndroidJUnit4::class)
class AccountDeletionEmulatorTest {
    @Test(timeout = 120000) fun deletesPrivateAndOrphanCommentsBeforeAuthAndKeepsOtherAccounts() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("runDeletionEmulatorTests") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val options = FirebaseOptions.Builder().setProjectId("demo-ideaforge").setApplicationId("demo-app").setApiKey("demo-key").build()
        val apps = (1..2).map { FirebaseApp.initializeApp(context, options, "delete-emulator-$it-${System.nanoTime()}") }
        try {
            val auths = apps.map { FirebaseAuth.getInstance(it).apply { useEmulator("10.0.2.2", 9099) } }
            val databases = apps.map { FirebaseFirestore.getInstance(it).apply { useEmulator("10.0.2.2", 8085) } }
            val users = auths.map { requireNotNull(it.signInAnonymously().await().user) }
            val alice = databases[0]
            val bob = databases[1]
            val aliceId = users[0].uid
            val bobId = users[1].uid
            for (i in 0..1) databases[i].collection("users").document(users[i].uid).collection("preferences").document("community")
                .set(mapOf("termsVersion" to "2026-11-13", "acceptedAt" to FieldValue.serverTimestamp())).await()
            fun idea(uid: String) = mapOf("userId" to uid, "title" to "Disposable test", "isPublic" to true, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp())
            fun comment(uid: String, parent: String) = mapOf("userId" to uid, "ideaId" to parent, "content" to "Disposable", "createdAt" to FieldValue.serverTimestamp())
            val ownIdea = alice.collection("ideas").document()
            ownIdea.set(idea(aliceId)).await()
            val foreignComment = bob.collection("ideas").document(ownIdea.id).collection("comments").document()
            foreignComment.set(comment(bobId, ownIdea.id)).await()
            val privateParent = bob.collection("ideas").document()
            privateParent.set(idea(bobId)).await()
            val ownComment = alice.collection("ideas").document(privateParent.id).collection("comments").document()
            ownComment.set(comment(aliceId, privateParent.id)).await()
            privateParent.update(mapOf("isPublic" to false, "updatedAt" to FieldValue.serverTimestamp())).await()
            val orphanParent = bob.collection("ideas").document()
            orphanParent.set(idea(bobId)).await()
            val orphanComment = alice.collection("ideas").document(orphanParent.id).collection("comments").document()
            orphanComment.set(comment(aliceId, orphanParent.id)).await()
            orphanParent.delete().await()
            // Simulate a previous interrupted deletion, including the server-side write lock.
            alice.collection("accountDeletions").document(aliceId).set(mapOf("startedAt" to FieldValue.serverTimestamp())).await()
            val deletion = AccountDeletion(auths[0], alice)
            assertTrue(deletion.isPending())
            try {
                ownIdea.update(mapOf("title" to "Late write", "updatedAt" to FieldValue.serverTimestamp())).await()
                fail("Deletion marker did not block a late write")
            } catch (expected: FirebaseFirestoreException) { assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, expected.code) }
            deletion.deleteAccount()
            assertNull(auths[0].currentUser)
            assertEquals(bobId, auths[1].currentUser?.uid)
            assertTrue(privateParent.get(Source.SERVER).await().exists())
            // Bob can query his remaining private idea; Alice's account/data cleanup leaves it intact.
            assertEquals(1, bob.collection("ideas").whereEqualTo("userId", bobId).get(Source.SERVER).await().size())
            assertTrue(bob.collectionGroup("comments").whereEqualTo("userId", bobId).get(Source.SERVER).await().isEmpty)
        } finally { apps.forEach { it.delete() } }
    }
}

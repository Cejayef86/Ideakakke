package com.example

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ai.AiResearchService
import com.example.data.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in checks against an explicitly selected live project; skipped in ordinary CI. */
@RunWith(AndroidJUnit4::class)
class LiveFirebaseConnectionTest {
    @Before fun prepareLiveProject() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Live Firebase checks require explicit opt-in", args.getString("runLiveFirebaseTests") == "true")
        val expectedProject = requireNotNull(args.getString("firebaseProjectId"))
        assertEquals(expectedProject, FirebaseApp.getInstance().options.projectId)
        val debugToken = requireNotNull(args.getString("appCheckDebugToken")) {
            "A private App Check debug token must be supplied for live verification"
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        configureAppCheck(context, Intent().putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", debugToken))
    }

    @Test(timeout = 30000) fun unauthenticatedClientCannotReadLiveIdeas() = runBlocking {
        assertNull("Use an isolated test device without a signed-in user", Firebase.auth.currentUser)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        try {
            withTimeout(20000) { db.collection("ideas").limit(1).get(Source.SERVER).await() }
            fail("The live database allowed an unauthenticated ideas query")
        } catch (error: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, error.code)
        }
    }

    @Test(timeout = 90000) fun protectedAiReturnsStructuredResearch() = runBlocking {
        val result = AiResearchService().researchIdea(
            title = "Neighborhood book and seed swap",
            description = "A small community lending shelf and occasional swap event.",
            category = "Creative Projects",
            tags = listOf("#community")
        )
        assertTrue(result.summary.isNotBlank())
        assertTrue(result.actionPlan.isNotEmpty())
        assertTrue(result.marketplaceItems.isNotEmpty())
    }
}

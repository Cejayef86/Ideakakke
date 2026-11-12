package com.example

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Actual app UI with clearly synthetic, local-only content for store screenshots. */
class StoreScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun captureIdeasAndMatrixAndCheckAccountControls() = runBlocking<Unit> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("captureStoreScreenshots") == "true")
        check(FirebaseApp.getInstance().options.projectId?.startsWith("demo-") == true) { "Screenshots require the isolated demo Firebase fixture" }
        val db = FirebaseFirestore.getInstance()
        db.useEmulator("10.0.2.2", 8085)
        Firebase.auth.useEmulator("10.0.2.2", 9099)
        val uid = requireNotNull(Firebase.auth.signInAnonymously().await().user).uid
        db.collection("users").document(uid).collection("preferences").document("community").set(mapOf("termsVersion" to "2026-11-13", "acceptedAt" to FieldValue.serverTimestamp())).await()
        val samples = listOf(
            listOf("Neighbourhood book swap", "Start a small book-sharing shelf and a monthly community exchange.", "Creative Projects", "High", "do_first"),
            listOf("Balcony herb garden", "Plan a compact garden with basil, mint and parsley.", "Personal Goals", "Medium", "schedule"),
            listOf("Weekend furniture workshop", "Restore a secondhand chair and learn basic finishing skills.", "Art & Design", "Low", "delegate")
        )
        for ((title, description, category, priority, quadrant) in samples) db.collection("ideas").document().set(mapOf(
            "userId" to uid, "title" to title, "description" to description, "category" to category,
            "priority" to priority, "matrixQuadrant" to quadrant, "isPublic" to false,
            "authorName" to "Creator", "tags" to listOf("#project"), "collaborators" to listOf("Creator"),
            "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
        )).await()
        compose.waitUntil(30000) { compose.onAllNodes(androidx.compose.ui.test.hasText("Neighbourhood book swap")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("nav_my_ideas").assertIsDisplayed()
        capture("01-my-ideas.png")
        compose.onNodeWithTag("nav_matrix").performClick()
        compose.waitForIdle()
        capture("02-priority-matrix.png")
        compose.onNodeWithTag("nav_my_ideas").performClick()
        compose.onNodeWithTag("account_menu").performClick()
        compose.onNodeWithTag("delete_account").performClick()
        compose.onNodeWithText("Delete your IdeaForge account?").assertIsDisplayed()
        compose.onNodeWithText("Keep account").performClick()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrument = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrument.targetContext.getExternalFilesDir(null), "store-screenshots").apply { mkdirs() }
        val bitmap = requireNotNull(instrument.uiAutomation.takeScreenshot())
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}


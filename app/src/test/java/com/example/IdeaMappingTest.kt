package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.example.model.Idea
import com.google.firebase.firestore.util.CustomClassMapper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IdeaMappingTest {
    @Test fun `public and private flags survive Firestore mapping`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
            .setApplicationId("1:123456789012:android:test")
            .setApiKey("AIzaSyAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
            .setProjectId("demo-ideaforge").build(), "mapping-test")
        val ref = FirebaseFirestore.getInstance(app).document("ideas/one")
        for (public in listOf(true, false)) {
            val input = mapOf("userId" to "alice", "title" to "Idea", "isPublic" to public)
            val idea = CustomClassMapper.convertToCustomClass(input, Idea::class.java, ref)
            assertEquals(public, idea.isPublic)
            val encoded = CustomClassMapper.convertToPlainJavaTypes(idea) as Map<*, *>
            assertEquals(public, encoded["isPublic"])
            assertFalse(encoded.containsKey("public"))
        }
    }
}

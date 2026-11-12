package com.example.data

import com.example.model.Idea
import org.junit.Assert.*
import org.junit.Test

class IdeaUpdatesTest {
    @Test fun `saving stale editor leaves fresh research and membership intact`() {
        val stale = Idea(id="one", userId="alice", title="Edited title", isPublic=true)
        val server = mutableMapOf<String, Any?>(
            "aiResearchSummary" to "Fresh research", "aiActionPlan" to listOf("New step"),
            "aiMarketplaceKeywords" to listOf("New tool"), "aiMarketplaceItems" to "[]",
            "collaborators" to listOf("Alice", "Bob"), "userId" to "alice", "createdAt" to "original"
        )
        server.putAll(ideaDetailUpdates(stale))
        assertEquals("Edited title", server["title"])
        assertEquals(true, server["isPublic"])
        assertEquals("Fresh research", server["aiResearchSummary"])
        assertEquals(listOf("New step"), server["aiActionPlan"])
        assertEquals(listOf("New tool"), server["aiMarketplaceKeywords"])
        assertEquals("[]", server["aiMarketplaceItems"])
        assertEquals(listOf("Alice", "Bob"), server["collaborators"])
        assertEquals("alice", server["userId"])
        assertEquals("original", server["createdAt"])
    }
}

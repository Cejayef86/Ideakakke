package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

@IgnoreExtraProperties
data class Idea(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Business",
    val tags: List<String> = emptyList(),
    val priority: String = "Medium", // "High", "Medium", "Low"
    val matrixQuadrant: String = "schedule", // "do_first", "schedule", "delegate", "eliminate"
    val isPublic: Boolean = false,
    val collaborators: List<String> = emptyList(),
    val aiResearchSummary: String = "",
    val aiActionPlan: List<String> = emptyList(),
    val aiMarketplaceKeywords: List<String> = emptyList(),
    val aiMarketplaceItems: String = "", // JSON string containing MarketplaceItem list
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parseMarketplaceItems(): List<MarketplaceItem> {
        if (aiMarketplaceItems.isBlank()) return emptyList()
        return try {
            val list = mutableListOf<MarketplaceItem>()
            val jsonArray = JSONArray(aiMarketplaceItems)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MarketplaceItem(
                        name = obj.optString("name", "Item"),
                        whyNeeded = obj.optString("whyNeeded", ""),
                        estimatedPrice = obj.optString("estimatedPrice", "Price varies"),
                        inspectionTips = obj.optString("inspectionTips", ""),
                        searchQuery = obj.optString("searchQuery", obj.optString("name", ""))
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class MarketplaceItem(
    val name: String = "",
    val whyNeeded: String = "",
    val estimatedPrice: String = "",
    val inspectionTips: String = "",
    val searchQuery: String = ""
)

@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val ideaId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class PriorityLevel(val label: String, val colorHex: Long) {
    HIGH("High", 0xFFEF4444),
    MEDIUM("Medium", 0xFFF59E0B),
    LOW("Low", 0xFF10B981)
}

enum class EisenhowerQuadrant(
    val id: String,
    val label: String,
    val subtitle: String,
    val urgent: Boolean,
    val important: Boolean,
    val colorHex: Long
) {
    DO_FIRST("do_first", "Do First", "Urgent & Important", true, true, 0xFFEF4444),
    SCHEDULE("schedule", "Schedule", "Important, Not Urgent", false, true, 0xFF6366F1),
    DELEGATE("delegate", "Delegate", "Urgent, Not Important", true, false, 0xFFF59E0B),
    ELIMINATE("eliminate", "Backlog", "Not Urgent, Not Important", false, false, 0xFF64748B);

    companion object {
        fun fromId(id: String): EisenhowerQuadrant {
            return entries.find { it.id == id } ?: SCHEDULE
        }
    }
}

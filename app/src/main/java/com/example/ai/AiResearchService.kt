package com.example.ai

import android.util.Log
import com.example.model.MarketplaceItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.thinkingConfig
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "AiResearchService"

data class ResearchResult(
    val summary: String,
    val actionPlan: List<String>,
    val marketplaceKeywords: List<String>,
    val marketplaceItemsJson: String,
    val marketplaceItems: List<MarketplaceItem>
)

class AiResearchService {

    private val marketplaceItemSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Item or equipment name"),
            "whyNeeded" to Schema.string(description = "Why this item is needed for the idea"),
            "estimatedPrice" to Schema.string(description = "Typical secondhand price on Marketplace"),
            "inspectionTips" to Schema.string(description = "What to inspect before buying"),
            "searchQuery" to Schema.string(description = "Exact search term to find it on Facebook Marketplace")
        )
    )

    private val researchSchema = Schema.obj(
        properties = mapOf(
            "researchSummary" to Schema.string(description = "Comprehensive topic research, feasibility, target audience, and key opportunities"),
            "actionPlan" to Schema.array(items = Schema.string(), description = "Step-by-step milestones to achieve the goal"),
            "marketplaceKeywords" to Schema.array(items = Schema.string(), description = "Search query keywords for Facebook Marketplace"),
            "marketplaceItems" to Schema.array(items = marketplaceItemSchema, description = "Specific secondhand items to look for on Marketplace")
        )
    )

    private val generativeModel by lazy {
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                // Keep interactive idea research responsive within the request timeout.
                thinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.LOW }
                responseMimeType = "application/json"
                responseSchema = researchSchema
            }
        )
    }

    suspend fun researchIdea(
        title: String,
        description: String,
        category: String,
        tags: List<String>
    ): ResearchResult = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("You are an expert entrepreneurial researcher, product strategist, and procurement advisor.")
            appendLine("Conduct in-depth research and produce an actionable execution guide for the following user idea:")
            appendLine()
            appendLine("IDEA TITLE: $title")
            if (description.isNotBlank()) appendLine("DESCRIPTION: $description")
            if (category.isNotBlank()) appendLine("CATEGORY: $category")
            if (tags.isNotEmpty()) appendLine("TAGS: ${tags.joinToString(", ")}")
            appendLine()
            appendLine("REQUIREMENTS:")
            appendLine("1. researchSummary: Thorough, realistic research covering market viability, key challenges, target users, and competitive advantages (3-5 concise paragraphs).")
            appendLine("2. actionPlan: A 4-6 step chronological milestone roadmap with concrete action items to turn this idea into reality.")
            appendLine("3. marketplaceKeywords: 3-6 specific equipment, tools, furniture, hardware, or supplies that could be bought secondhand on Facebook Marketplace to save money.")
            appendLine("4. marketplaceItems: Curated list of 2-5 essential secondhand items to search for on Facebook Marketplace. For each, give the item name, why it's needed, expected secondhand price range, crucial physical inspection checks when buying in person, and the exact Facebook Marketplace search query.")
        }

        try {
            // Mandated: realistic 30-60s timeout for model inference
            val response = withTimeout(60_000L) {
                generativeModel.generateContent(prompt)
            }

            val jsonText = response.text
            if (jsonText.isNullOrBlank()) {
                throw IllegalStateException("Model returned an empty response.")
            }

            val root = JSONObject(jsonText)
            val summary = root.optString("researchSummary", "Research completed successfully.")

            val planArray = root.optJSONArray("actionPlan") ?: JSONArray()
            val plan = mutableListOf<String>()
            for (i in 0 until planArray.length()) {
                plan.add(planArray.getString(i))
            }

            val keywordsArray = root.optJSONArray("marketplaceKeywords") ?: JSONArray()
            val keywords = mutableListOf<String>()
            for (i in 0 until keywordsArray.length()) {
                keywords.add(keywordsArray.getString(i))
            }

            val itemsArray = root.optJSONArray("marketplaceItems") ?: JSONArray()
            val items = mutableListOf<MarketplaceItem>()
            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                items.add(
                    MarketplaceItem(
                        name = itemObj.optString("name", "Equipment"),
                        whyNeeded = itemObj.optString("whyNeeded", ""),
                        estimatedPrice = itemObj.optString("estimatedPrice", "Price varies"),
                        inspectionTips = itemObj.optString("inspectionTips", ""),
                        searchQuery = itemObj.optString("searchQuery", itemObj.optString("name", ""))
                    )
                )
            }

            ResearchResult(
                summary = summary,
                actionPlan = plan,
                marketplaceKeywords = keywords,
                marketplaceItemsJson = itemsArray.toString(),
                marketplaceItems = items
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI research timed out after 60s", e)
            throw IllegalStateException("Research timed out. Please try again with a connection.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI research generation failed", e)
            throw e
        }
    }
}

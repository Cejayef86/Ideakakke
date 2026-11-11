package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiResearchService
import com.example.data.IdeaRepository
import com.example.model.Comment
import com.example.model.EisenhowerQuadrant
import com.example.model.Idea
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "IdeaViewModel"

enum class ViewMode {
    LIST,
    MATRIX,
    COMMUNITY
}

class IdeaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IdeaRepository(application)
    private val aiService = AiResearchService()

    val currentUserId: String?
        get() = Firebase.auth.currentUser?.uid

    val currentUserName: String
        get() = Firebase.auth.currentUser?.displayName ?: "Creator"

    // Raw streams
    val userIdeas: StateFlow<List<Idea>> = repository.observeUserIdeas()
        .catch { e ->
            Log.e(TAG, "Error collecting user ideas", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val communityIdeas: StateFlow<List<Idea>> = repository.observeCommunityIdeas()
        .catch { e ->
            Log.e(TAG, "Error collecting community ideas", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Filter and display state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    private val _selectedPriority = MutableStateFlow<String?>("All")
    val selectedPriority = _selectedPriority.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode = _viewMode.asStateFlow()

    // Research status
    private val _researchingIdeaId = MutableStateFlow<String?>(null)
    val researchingIdeaId = _researchingIdeaId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    // Active Idea Detail state
    private val _selectedIdeaId = MutableStateFlow<String?>(null)
    val selectedIdeaId = _selectedIdeaId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeIdea: StateFlow<Idea?> = _selectedIdeaId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observeIdea(id)
    }.catch {
        emit(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeComments: StateFlow<List<Comment>> = _selectedIdeaId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeComments(id)
    }.catch {
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Filtered user ideas based on search, category, tag, and priority
    val filteredUserIdeas: StateFlow<List<Idea>> = combine(
        userIdeas,
        searchQuery,
        selectedCategory,
        selectedTag,
        selectedPriority
    ) { ideas, query, cat, tag, priority ->
        ideas.filter { idea ->
            val matchesQuery = query.isBlank() ||
                idea.title.contains(query, ignoreCase = true) ||
                idea.description.contains(query, ignoreCase = true) ||
                idea.category.contains(query, ignoreCase = true) ||
                idea.tags.any { it.contains(query, ignoreCase = true) } ||
                idea.aiResearchSummary.contains(query, ignoreCase = true)

            val matchesCategory = cat == null || cat == "All" || idea.category.equals(cat, ignoreCase = true)
            val matchesTag = tag == null || idea.tags.any { it.equals(tag, ignoreCase = true) }
            val matchesPriority = priority == null || priority == "All" || idea.priority.equals(priority, ignoreCase = true)

            matchesQuery && matchesCategory && matchesTag && matchesPriority
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // All distinct categories from user ideas
    val availableCategories: StateFlow<List<String>> = userIdeas.combine(communityIdeas) { my, pub ->
        val defaults = listOf("Business", "Creative Projects", "Personal Goals", "Tech & Hardware", "Art & Design")
        val custom = (my + pub).map { it.category }.filter { it.isNotBlank() }
        (defaults + custom).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), listOf("Business", "Creative Projects", "Personal Goals"))

    // All distinct tags from user ideas
    val availableTags: StateFlow<List<String>> = userIdeas.combine(communityIdeas) { my, pub ->
        (my + pub).flatMap { it.tags }.map { if (it.startsWith("#")) it else "#$it" }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun setSelectedPriority(priority: String?) {
        _selectedPriority.value = priority
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun selectIdea(ideaId: String?) {
        _selectedIdeaId.value = ideaId
    }

    fun clearFeedbackMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun createIdea(
        title: String,
        description: String,
        category: String,
        tags: List<String>,
        priority: String,
        matrixQuadrant: String,
        isPublic: Boolean,
        runAiResearchImmediately: Boolean
    ) {
        viewModelScope.launch {
            try {
                val formattedTags = tags.map { if (it.startsWith("#")) it else "#$it" }
                val newId = repository.createIdea(
                    title = title,
                    description = description,
                    category = category.ifBlank { "Business" },
                    tags = formattedTags,
                    priority = priority,
                    matrixQuadrant = matrixQuadrant,
                    isPublic = isPublic,
                    authorName = currentUserName
                )
                _successMessage.value = "Idea '$title' created!"

                if (runAiResearchImmediately) {
                    val createdIdea = Idea(
                        id = newId,
                        userId = currentUserId ?: "",
                        title = title,
                        description = description,
                        category = category,
                        tags = formattedTags
                    )
                    triggerAiResearch(createdIdea)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create idea", e)
                _errorMessage.value = "Could not save idea: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateIdeaDetails(idea: Idea) {
        viewModelScope.launch {
            try {
                val formattedTags = idea.tags.map { if (it.startsWith("#")) it else "#$it" }
                repository.updateIdea(idea.copy(tags = formattedTags))
                _successMessage.value = "Idea updated successfully!"
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update idea", e)
                _errorMessage.value = "Update failed: ${e.localizedMessage}"
            }
        }
    }

    fun updateIdeaQuadrant(idea: Idea, quadrant: EisenhowerQuadrant) {
        viewModelScope.launch {
            try {
                repository.updateIdea(idea.copy(matrixQuadrant = quadrant.id))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update quadrant", e)
            }
        }
    }

    fun updateIdeaPriority(idea: Idea, priority: String) {
        viewModelScope.launch {
            try {
                repository.updateIdea(idea.copy(priority = priority))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update priority", e)
            }
        }
    }

    fun deleteIdea(ideaId: String) {
        viewModelScope.launch {
            try {
                repository.deleteIdea(ideaId)
                if (_selectedIdeaId.value == ideaId) {
                    _selectedIdeaId.value = null
                }
                _successMessage.value = "Idea deleted"
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete idea", e)
                _errorMessage.value = "Delete failed: ${e.localizedMessage}"
            }
        }
    }

    fun triggerAiResearch(idea: Idea) {
        viewModelScope.launch {
            _researchingIdeaId.value = idea.id
            try {
                val result = aiService.researchIdea(
                    title = idea.title,
                    description = idea.description,
                    category = idea.category,
                    tags = idea.tags
                )

                repository.updateAiResearch(
                    ideaId = idea.id,
                    researchSummary = result.summary,
                    actionPlan = result.actionPlan,
                    marketplaceKeywords = result.marketplaceKeywords,
                    marketplaceItemsJson = result.marketplaceItemsJson
                )
                _successMessage.value = "AI Research & Facebook Marketplace search generated for '${idea.title}'!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI research failed", e)
                _errorMessage.value = "AI Research failed: ${e.localizedMessage ?: "Network or quota issue"}"
            } finally {
                _researchingIdeaId.value = null
            }
        }
    }

    fun addCollaborator(ideaId: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addCollaborator(ideaId, name)
                _successMessage.value = "Collaborator '$name' added to team!"
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add collaborator", e)
                _errorMessage.value = "Could not add collaborator"
            }
        }
    }

    fun addComment(ideaId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addComment(ideaId, content, currentUserName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add comment", e)
                _errorMessage.value = "Failed to post comment"
            }
        }
    }

    fun deleteComment(ideaId: String, commentId: String) {
        viewModelScope.launch {
            try {
                repository.deleteComment(ideaId, commentId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete comment", e)
            }
        }
    }
}

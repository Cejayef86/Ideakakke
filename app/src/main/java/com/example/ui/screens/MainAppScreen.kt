package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.model.Idea
import com.example.ui.IdeaViewModel
import com.example.ui.ViewMode
import com.example.ui.signOutUser
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: IdeaViewModel,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val userIdeas by viewModel.filteredUserIdeas.collectAsState()
    val allUserIdeas by viewModel.userIdeas.collectAsState()
    val communityIdeas by viewModel.communityIdeas.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val selectedPriority by viewModel.selectedPriority.collectAsState()

    val availableCategories by viewModel.availableCategories.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val researchingIdeaId by viewModel.researchingIdeaId.collectAsState()

    val activeIdea by viewModel.activeIdea.collectAsState()
    val activeComments by viewModel.activeComments.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val termsAccepted by viewModel.termsAccepted.collectAsState()
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    var researchTarget by remember { mutableStateOf<Idea?>(null) }
    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }
    var blockTarget by remember { mutableStateOf<String?>(null) }
    var showBlockedUsers by remember { mutableStateOf(false) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var ideaToEdit by remember { mutableStateOf<Idea?>(null) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirmIdeaId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedbackMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedbackMessages()
        }
    }

    // Detail Screen Display
    if (activeIdea != null) {
        IdeaDetailScreen(
            idea = activeIdea!!,
            currentUserId = viewModel.currentUserId,
            comments = activeComments,
            isResearching = researchingIdeaId == activeIdea!!.id,
            onBackClick = { viewModel.selectIdea(null) },
            onEditClick = {
                ideaToEdit = activeIdea
                showAddEditDialog = true
            },
            onDeleteClick = {
                showDeleteConfirmIdeaId = activeIdea!!.id
            },
            onTriggerResearch = {
                researchTarget = activeIdea
            },
            onAddCollaborator = { name ->
                viewModel.addCollaborator(activeIdea!!.id, name)
            },
            onAddComment = { text ->
                viewModel.addComment(activeIdea!!.id, text)
            },
            onDeleteComment = { commentId ->
                viewModel.deleteComment(activeIdea!!.id, commentId)
            },
            onReport = { kind, commentId -> reportTarget = ReportTarget(kind, activeIdea!!.id, commentId) },
            onBlock = { uid -> blockTarget = uid }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(IndigoPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "IdeaForge",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    },
                    actions = {
                        // User Profile Icon & Menu
                        Box {
                            IconButton(onClick = { showProfileMenu = true }, modifier = Modifier.testTag("account_menu")) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(IndigoPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = viewModel.currentUserName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = IndigoPrimary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showProfileMenu,
                                onDismissRequest = { showProfileMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Signed in as ${viewModel.currentUserName}") },
                                    enabled = false,
                                    onClick = {}
                                )
                                DropdownMenuItem(text = { Text("Privacy policy") }, onClick = { showProfileMenu = false; uri.openUri("$SUPPORT_SITE/privacy") })
                                DropdownMenuItem(text = { Text("Terms and community rules") }, onClick = { showProfileMenu = false; uri.openUri("$SUPPORT_SITE/terms") })
                                DropdownMenuItem(text = { Text("Blocked users") }, onClick = { showProfileMenu = false; showBlockedUsers = true })
                                DropdownMenuItem(text = { Text("Delete account", color = MaterialTheme.colorScheme.error) }, onClick = { showProfileMenu = false; onDeleteAccount() }, modifier = Modifier.testTag("delete_account"))
                                DropdownMenuItem(
                                    text = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Logout,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showProfileMenu = false
                                        showSignOutConfirm = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = viewMode == ViewMode.LIST,
                        onClick = { viewModel.setViewMode(ViewMode.LIST) },
                        icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "My Ideas") },
                        label = { Text("My Ideas") },
                        modifier = Modifier.testTag("nav_my_ideas")
                    )
                    NavigationBarItem(
                        selected = viewMode == ViewMode.MATRIX,
                        onClick = { viewModel.setViewMode(ViewMode.MATRIX) },
                        icon = { Icon(Icons.Default.ViewKanban, contentDescription = "Eisenhower Matrix") },
                        label = { Text("Matrix") },
                        modifier = Modifier.testTag("nav_matrix")
                    )
                    NavigationBarItem(
                        selected = viewMode == ViewMode.COMMUNITY,
                        onClick = { viewModel.setViewMode(ViewMode.COMMUNITY) },
                        icon = { Icon(Icons.Default.Public, contentDescription = "Community") },
                        label = { Text("Community") },
                        modifier = Modifier.testTag("nav_community")
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        ideaToEdit = null
                        showAddEditDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Create") },
                    text = { Text("New Idea") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_create_idea")
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (viewMode) {
                    ViewMode.LIST -> {
                        MyIdeasListView(
                            ideas = userIdeas,
                            totalIdeasCount = allUserIdeas.size,
                            searchQuery = searchQuery,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            categories = availableCategories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = { viewModel.setSelectedCategory(it) },
                            tags = availableTags,
                            selectedTag = selectedTag,
                            onSelectTag = { viewModel.setSelectedTag(it) },
                            selectedPriority = selectedPriority,
                            onSelectPriority = { viewModel.setSelectedPriority(it) },
                            researchingIdeaId = researchingIdeaId,
                            onIdeaClick = { idea -> viewModel.selectIdea(idea.id) },
                            onEditClick = { idea ->
                                ideaToEdit = idea
                                showAddEditDialog = true
                            },
                            onDeleteClick = { idea ->
                                showDeleteConfirmIdeaId = idea.id
                            },
                            onResearchClick = { idea -> researchTarget = idea },
                            onQuadrantChange = { idea, quad ->
                                viewModel.updateIdeaQuadrant(idea, quad)
                            }
                        )
                    }

                    ViewMode.MATRIX -> {
                        EisenhowerMatrixScreen(
                            ideas = allUserIdeas,
                            onIdeaClick = { idea -> viewModel.selectIdea(idea.id) },
                            onMoveQuadrant = { idea, quadrant ->
                                viewModel.updateIdeaQuadrant(idea, quadrant)
                            }
                        )
                    }

                    ViewMode.COMMUNITY -> {
                        CommunityIdeasScreen(
                            communityIdeas = communityIdeas,
                            onIdeaClick = { idea -> viewModel.selectIdea(idea.id) }
                        )
                    }
                }
            }
        }
    }

    if (!termsAccepted) CommunityTermsDialog(
        onAccept = viewModel::acceptCommunityTerms,
        onSignOut = { signOutUser(credentialManager, coroutineScope, onSignOut) },
        onDeleteAccount = onDeleteAccount
    )
    reportTarget?.let { target -> ReportContentDialog(target, { reportTarget = null }) { reason, details, done, failed ->
        viewModel.reportContent(target.kind, target.ideaId, target.commentId, reason, details, done, failed)
    } }
    blockTarget?.let { uid -> BlockUserDialog({ blockTarget = null }) { viewModel.blockUser(uid); blockTarget = null } }
    if (showBlockedUsers) BlockedUsersDialog(blockedUsers, { showBlockedUsers = false }, viewModel::unblockUser)

    researchTarget?.let { idea ->
        AlertDialog(onDismissRequest = { researchTarget = null }, title = { Text("Generate suggestions with Google AI?") },
            text = { Text("This sends this idea's title, description, category and tags to Google's Gemini service. On the current unpaid service, Google may use prompts and responses to improve its products, and human reviewers may process them. Don't send confidential, sensitive or personal information. AI can be inaccurate; verify its suggestions.") },
            confirmButton = { TextButton(onClick = { researchTarget = null; viewModel.triggerAiResearch(idea) }) { Text("Generate with Google AI") } },
            dismissButton = { TextButton(onClick = { researchTarget = null }) { Text("Cancel") } })
    }

    // Add / Edit Idea Dialog
    if (showAddEditDialog) {
        AddEditIdeaDialog(
            ideaToEdit = ideaToEdit,
            isSaving = isSaving,
            availableCategories = availableCategories,
            onDismiss = { if (!isSaving) showAddEditDialog = false },
            onSave = { title, desc, cat, tags, priority, matrix, isPublic, runAi ->
                if (ideaToEdit == null) {
                    viewModel.createIdea(title, desc, cat, tags, priority, matrix, isPublic, runAi) { showAddEditDialog = false }
                } else {
                    viewModel.updateIdeaDetails(
                        ideaToEdit!!.copy(
                            title = title,
                            description = desc,
                            category = cat,
                            tags = tags,
                            priority = priority,
                            matrixQuadrant = matrix,
                            isPublic = isPublic
                        ),
                        onSaved = { showAddEditDialog = false }
                    )
                }
            }
        )
    }

    // Sign Out Confirm Dialog
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out of IdeaForge?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        signOutUser(credentialManager, coroutineScope, onSignOut)
                    }
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Idea Confirm Dialog
    if (showDeleteConfirmIdeaId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmIdeaId = null },
            title = { Text("Delete Idea") },
            text = { Text("Are you sure you want to delete this idea? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idToDelete = showDeleteConfirmIdeaId
                        showDeleteConfirmIdeaId = null
                        idToDelete?.let { viewModel.deleteIdea(it) }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmIdeaId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MyIdeasListView(
    ideas: List<Idea>,
    totalIdeasCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    tags: List<String>,
    selectedTag: String?,
    onSelectTag: (String?) -> Unit,
    selectedPriority: String?,
    onSelectPriority: (String?) -> Unit,
    researchingIdeaId: String?,
    onIdeaClick: (Idea) -> Unit,
    onEditClick: (Idea) -> Unit,
    onDeleteClick: (Idea) -> Unit,
    onResearchClick: (Idea) -> Unit,
    onQuadrantChange: (Idea, com.example.model.EisenhowerQuadrant) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search TextField
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search title, notes, #tags, gear...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null || selectedCategory == "All",
                            onClick = { onSelectCategory("All") },
                            label = { Text("All Categories") }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                if (selectedCategory == cat) onSelectCategory("All") else onSelectCategory(cat)
                            },
                            label = { Text(cat) }
                        )
                    }
                }

                // Priority & Tag Filters Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Filters
                    listOf("All", "High", "Medium", "Low").forEach { p ->
                        val isSelected = (selectedPriority == null && p == "All") || selectedPriority == p
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (p == "All") onSelectPriority("All") else onSelectPriority(p)
                            },
                            label = { Text(if (p == "All") "Any Priority" else "$p Priority") }
                        )
                    }

                    // Tag Chips
                    tags.forEach { tag ->
                        val isSelected = selectedTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) onSelectTag(null) else onSelectTag(tag)
                            },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }

        // Idea Cards List
        if (ideas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (totalIdeasCount == 0) "No ideas created yet" else "No matching ideas",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (totalIdeasCount == 0)
                            "Tap '+ New Idea' to add your idea and let AI research equipment on Facebook Marketplace."
                        else
                            "Try clearing filters or search terms.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(ideas, key = { it.id }) { idea ->
                    IdeaCard(
                        idea = idea,
                        isOwner = true,
                        isResearching = researchingIdeaId == idea.id,
                        onCardClick = { onIdeaClick(idea) },
                        onEditClick = { onEditClick(idea) },
                        onDeleteClick = { onDeleteClick(idea) },
                        onResearchClick = { onResearchClick(idea) },
                        onTagClick = { tag -> onSelectTag(tag) },
                        onQuadrantChange = { quad -> onQuadrantChange(idea, quad) }
                    )
                }
            }
        }
    }
}

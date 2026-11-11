package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EisenhowerQuadrant
import com.example.model.Idea
import com.example.model.PriorityLevel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdeaCard(
    idea: Idea,
    isOwner: Boolean,
    isResearching: Boolean,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onResearchClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onQuadrantChange: (EisenhowerQuadrant) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showQuadrantMenu by remember { mutableStateOf(false) }

    val quadrant = EisenhowerQuadrant.fromId(idea.matrixQuadrant)
    val priorityColor = when (idea.priority.lowercase()) {
        "high" -> Color(0xFFEF4444)
        "low" -> Color(0xFF10B981)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("idea_card_${idea.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Category, Priority, Public Badge, and Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = idea.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Priority Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = idea.priority,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = priorityColor
                            )
                        }
                    }

                    if (idea.isPublic) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElectricCyan.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Public Idea",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Public",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                            }
                        }
                    }
                }

                if (isOwner) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Idea") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Change Matrix Quadrant") },
                                leadingIcon = { Icon(Icons.Default.ViewKanban, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showQuadrantMenu = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Externally") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    shareIdeaIntent(context, idea)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Idea", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                }
                            )
                        }

                        // Quadrant Submenu
                        DropdownMenu(
                            expanded = showQuadrantMenu,
                            onDismissRequest = { showQuadrantMenu = false }
                        ) {
                            EisenhowerQuadrant.entries.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text("${q.label} (${q.subtitle})") },
                                    onClick = {
                                        showQuadrantMenu = false
                                        onQuadrantChange(q)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = idea.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Description
            if (idea.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = idea.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tags Row
            if (idea.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    idea.tags.take(5).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onTagClick(tag) }
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Eisenhower Quadrant Bar
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(quadrant.colorHex).copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewKanban,
                        contentDescription = null,
                        tint = Color(quadrant.colorHex),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Matrix: ${quadrant.label} • ${quadrant.subtitle}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(quadrant.colorHex)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Research Status & Facebook Marketplace Quick Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (idea.aiResearchSummary.isNotBlank()) {
                    val marketplaceItems = idea.parseMarketplaceItems()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Researched (${idea.aiActionPlan.size} steps)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        if (marketplaceItems.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = IndigoPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    val firstItem = marketplaceItems.first()
                                    openFacebookMarketplace(context, firstItem.searchQuery)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${marketplaceItems.size} Gear Items",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = IndigoPrimary
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Search",
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (isResearching) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = IndigoPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Researching topic...",
                            style = MaterialTheme.typography.labelSmall,
                            color = IndigoPrimary
                        )
                    }
                } else if (isOwner) {
                    FilledTonalButton(
                        onClick = onResearchClick,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Research & Marketplace", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (idea.collaborators.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${idea.collaborators.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun openFacebookMarketplace(context: Context, query: String) {
    val encodedQuery = Uri.encode(query.ifBlank { "tools" })
    val marketplaceUrl = "https://www.facebook.com/marketplace/search/?query=$encodedQuery"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(marketplaceUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to browser
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(marketplaceUrl))
        context.startActivity(browserIntent)
    }
}

fun shareIdeaIntent(context: Context, idea: Idea) {
    val items = idea.parseMarketplaceItems()
    val shareContent = buildString {
        appendLine("💡 IDEA: ${idea.title}")
        appendLine("📂 Category: ${idea.category} | Priority: ${idea.priority}")
        if (idea.tags.isNotEmpty()) appendLine("🏷️ Tags: ${idea.tags.joinToString(" ")}")
        appendLine()
        if (idea.description.isNotBlank()) {
            appendLine("📝 Description:")
            appendLine(idea.description)
            appendLine()
        }
        if (idea.aiResearchSummary.isNotBlank()) {
            appendLine("🔬 AI Research & Feasibility:")
            appendLine(idea.aiResearchSummary)
            appendLine()
        }
        if (idea.aiActionPlan.isNotEmpty()) {
            appendLine("🚀 Action Plan & Milestones:")
            idea.aiActionPlan.forEachIndexed { idx, step ->
                appendLine("${idx + 1}. $step")
            }
            appendLine()
        }
        if (items.isNotEmpty()) {
            appendLine("🛒 Recommended Facebook Marketplace Gear:")
            items.forEach { item ->
                appendLine("• ${item.name} (${item.estimatedPrice}) - ${item.whyNeeded}")
                appendLine("  Search: https://www.facebook.com/marketplace/search/?query=${Uri.encode(item.searchQuery)}")
            }
            appendLine()
        }
        appendLine("— Shared from IdeaForge")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_SUBJECT, "Idea: ${idea.title}")
        putExtra(Intent.EXTRA_TEXT, shareContent)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Idea Plan")
    context.startActivity(shareIntent)
}

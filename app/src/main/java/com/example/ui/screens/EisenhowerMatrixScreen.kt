package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EisenhowerQuadrant
import com.example.model.Idea

@Composable
fun EisenhowerMatrixScreen(
    ideas: List<Idea>,
    onIdeaClick: (Idea) -> Unit,
    onMoveQuadrant: (Idea, EisenhowerQuadrant) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val quadrants = EisenhowerQuadrant.entries

    val currentQuadrant = quadrants[selectedTabIndex]
    val quadrantIdeas = ideas.filter { it.matrixQuadrant == currentQuadrant.id }

    Column(modifier = modifier.fillMaxSize()) {
        // Explanatory Banner
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ViewKanban,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Eisenhower Matrix Prioritization",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Organize goals by urgency and strategic importance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Scrollable Tabs for the 4 Quadrants with count badges
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color(currentQuadrant.colorHex)
                )
            }
        ) {
            quadrants.forEachIndexed { index, quadrant ->
                val count = ideas.count { it.matrixQuadrant == quadrant.id }
                val isSelected = selectedTabIndex == index
                val qColor = Color(quadrant.colorHex)

                Tab(
                    selected = isSelected,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = quadrant.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) qColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) qColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) qColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                )
            }
        }

        // Quadrant Description Header
        val quadrantColor = Color(currentQuadrant.colorHex)
        Surface(
            color = quadrantColor.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${currentQuadrant.label} • ${currentQuadrant.subtitle}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = quadrantColor
                    )
                    Text(
                        text = when (currentQuadrant) {
                            EisenhowerQuadrant.DO_FIRST -> "Critical & pressing — execute right away"
                            EisenhowerQuadrant.SCHEDULE -> "High long-term ROI — block dedicated time"
                            EisenhowerQuadrant.DELEGATE -> "Time-sensitive but low impact — automate or collaborate"
                            EisenhowerQuadrant.ELIMINATE -> "Backlog ideas — revisit or archive"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quadrant Ideas List
        if (quadrantIdeas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No ideas in ${currentQuadrant.label}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Create an idea or move one here from other quadrants",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quadrantIdeas, key = { it.id }) { idea ->
                    MatrixIdeaItem(
                        idea = idea,
                        onCardClick = { onIdeaClick(idea) },
                        onMove = { targetQuadrant -> onMoveQuadrant(idea, targetQuadrant) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixIdeaItem(
    idea: Idea,
    onCardClick: () -> Unit,
    onMove: (EisenhowerQuadrant) -> Unit
) {
    var showMoveMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = idea.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    val pColor = when (idea.priority.lowercase()) {
                        "high" -> Color(0xFFEF4444)
                        "low" -> Color(0xFF10B981)
                        else -> Color(0xFFF59E0B)
                    }
                    Text(
                        text = "• ${idea.priority} Priority",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = pColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = idea.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (idea.description.isNotBlank()) {
                    Text(
                        text = idea.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (idea.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = idea.tags.joinToString(" "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick Move Button
            Box {
                IconButton(onClick = { showMoveMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Move Quadrant",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = showMoveMenu,
                    onDismissRequest = { showMoveMenu = false }
                ) {
                    Text(
                        text = "Move to Quadrant:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    EisenhowerQuadrant.entries.filter { it.id != idea.matrixQuadrant }.forEach { q ->
                        DropdownMenuItem(
                            text = { Text("${q.label} (${q.subtitle})") },
                            onClick = {
                                showMoveMenu = false
                                onMove(q)
                            }
                        )
                    }
                }
            }
        }
    }
}

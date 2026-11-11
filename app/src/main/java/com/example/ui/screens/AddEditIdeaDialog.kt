package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.EisenhowerQuadrant
import com.example.model.Idea
import com.example.model.PriorityLevel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditIdeaDialog(
    ideaToEdit: Idea? = null,
    isSaving: Boolean = false,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: String,
        tags: List<String>,
        priority: String,
        matrixQuadrant: String,
        isPublic: Boolean,
        runAiResearch: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(ideaToEdit?.title ?: "") }
    var description by remember { mutableStateOf(ideaToEdit?.description ?: "") }
    var category by remember { mutableStateOf(ideaToEdit?.category ?: "Business") }
    var customCategory by remember { mutableStateOf("") }
    var isAddingCustomCategory by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val tags = remember {
        mutableStateListOf<String>().apply {
            if (ideaToEdit != null) addAll(ideaToEdit.tags)
        }
    }
    var tagInput by remember { mutableStateOf("") }

    var priority by remember { mutableStateOf(ideaToEdit?.priority ?: "Medium") }
    var matrixQuadrant by remember {
        mutableStateOf(ideaToEdit?.matrixQuadrant ?: EisenhowerQuadrant.SCHEDULE.id)
    }
    var isPublic by remember { mutableStateOf(ideaToEdit?.isPublic ?: false) }
    var runAiResearch by remember { mutableStateOf(ideaToEdit == null) }

    val popularTags = listOf("#tech", "#art", "#lowbudget", "#business", "#diy", "#ai", "#sustainability", "#mobile")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (ideaToEdit == null) "New Idea" else "Edit Idea",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(200) },
                        label = { Text("Idea Title *") },
                        placeholder = { Text("e.g. Automated Mushroom Grow Kit") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("idea_title_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Description Field
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(10000) },
                        label = { Text("Description & Notes") },
                        placeholder = { Text("Explain what you want to build, target audience, key components...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("idea_description_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selector
                    Column {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (isAddingCustomCategory) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customCategory,
                                    onValueChange = { customCategory = it.take(60) },
                                    label = { Text("Custom Category") },
                                    placeholder = { Text("e.g. Hardware Hack") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (customCategory.isNotBlank()) {
                                            category = customCategory.trim()
                                            isAddingCustomCategory = false
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Set")
                                }
                                TextButton(onClick = { isAddingCustomCategory = false }) {
                                    Text("Cancel")
                                }
                            }
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = categoryDropdownExpanded,
                                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false }
                                ) {
                                    availableCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                category = cat
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("+ Add Custom Category...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            categoryDropdownExpanded = false
                                            isAddingCustomCategory = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Tags Input & Chips
                    Column {
                        Text(
                            text = "Tags (e.g. #tech, #art, #lowbudget)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = tagInput,
                                onValueChange = { tagInput = it },
                                placeholder = { Text("Type tag and tap +") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("idea_tag_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val cleaned = tagInput.trim()
                                    if (cleaned.isNotBlank()) {
                                        val formatted = if (cleaned.startsWith("#")) cleaned else "#$cleaned"
                                        if (!tags.contains(formatted)) tags.add(formatted)
                                        tagInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("add_tag_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Tag")
                            }
                        }

                        // Added tags
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            IconButton(
                                                onClick = { tags.remove(tag) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove Tag",
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Popular Tag Suggestions
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Suggestions:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            popularTags.filter { !tags.contains(it) }.take(5).forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { tags.add(tag) }
                                ) {
                                    Text(
                                        text = "+ $tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Priority Level Selector
                    Column {
                        Text(
                            text = "Priority Level",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("High", "Medium", "Low").forEach { p ->
                                val selected = priority.equals(p, ignoreCase = true)
                                val pColor = when (p) {
                                    "High" -> Color(0xFFEF4444)
                                    "Low" -> Color(0xFF10B981)
                                    else -> Color(0xFFF59E0B)
                                }

                                FilterChip(
                                    selected = selected,
                                    onClick = { priority = p },
                                    label = { Text(p, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = pColor.copy(alpha = 0.2f),
                                        selectedLabelColor = pColor
                                    ),
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(pColor)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Eisenhower Matrix Quadrant Picker (2x2 Grid)
                    Column {
                        Text(
                            text = "Eisenhower Prioritization Matrix",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Classify by urgency and strategic importance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Row 1: Do First & Schedule
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuadrantButton(
                                    quadrant = EisenhowerQuadrant.DO_FIRST,
                                    isSelected = matrixQuadrant == EisenhowerQuadrant.DO_FIRST.id,
                                    onClick = { matrixQuadrant = EisenhowerQuadrant.DO_FIRST.id },
                                    modifier = Modifier.weight(1f)
                                )
                                QuadrantButton(
                                    quadrant = EisenhowerQuadrant.SCHEDULE,
                                    isSelected = matrixQuadrant == EisenhowerQuadrant.SCHEDULE.id,
                                    onClick = { matrixQuadrant = EisenhowerQuadrant.SCHEDULE.id },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Row 2: Delegate & Eliminate/Backlog
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuadrantButton(
                                    quadrant = EisenhowerQuadrant.DELEGATE,
                                    isSelected = matrixQuadrant == EisenhowerQuadrant.DELEGATE.id,
                                    onClick = { matrixQuadrant = EisenhowerQuadrant.DELEGATE.id },
                                    modifier = Modifier.weight(1f)
                                )
                                QuadrantButton(
                                    quadrant = EisenhowerQuadrant.ELIMINATE,
                                    isSelected = matrixQuadrant == EisenhowerQuadrant.ELIMINATE.id,
                                    onClick = { matrixQuadrant = EisenhowerQuadrant.ELIMINATE.id },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Public Community Sharing Switch
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Share with Community",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Allow other app creators to view, discuss, and offer feedback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isPublic,
                                onCheckedChange = { isPublic = it }
                            )
                        }
                    }

                    // Run AI Research Checkbox
                    if (ideaToEdit == null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { runAiResearch = !runAiResearch },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = runAiResearch,
                                onCheckedChange = { runAiResearch = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Auto-research & find Facebook Marketplace gear",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = IndigoPrimary
                                    )
                                }
                                Text(
                                    text = "Generates milestones and item searches upon saving",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    title.trim(),
                                    description.trim(),
                                    category.trim(),
                                    tags.toList(),
                                    priority,
                                    matrixQuadrant,
                                    isPublic,
                                    runAiResearch
                                )
                            }
                        },
                        enabled = !isSaving && title.trim().length in 1..200 && description.length <= 10000 && category.length <= 60 && tags.size <= 30,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_idea_button")
                    ) {
                        Text(if (isSaving) "Saving…" else if (ideaToEdit == null) "Create Idea" else "Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuadrantButton(
    quadrant: EisenhowerQuadrant,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qColor = Color(quadrant.colorHex)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) qColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, qColor) else null,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = quadrant.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) qColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = quadrant.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

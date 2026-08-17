package com.echomind.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echomind.app.data.memory.*
import com.echomind.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MemoryScreen(
    modifier: Modifier = Modifier,
    memoryViewModel: MemoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by memoryViewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editMemory by remember { mutableStateOf<Memory?>(null) }

    LaunchedEffect(Unit) {
        memoryViewModel.loadMemories()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("记忆", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Sync button
                if (uiState.syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = {
                        scope.launch {
                            memoryViewModel.syncFromNotes()
                        }
                    }) {
                        Icon(Icons.Default.Sync, contentDescription = "同步")
                    }
                }
                // Add button
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }
        }

        // Stats bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatChip("全部", uiState.totalCount.toString(), selected = uiState.activeFilter == null)
            StatChip("偏好", uiState.prefCount.toString(), selected = uiState.activeFilter == MemoryType.PREFERENCE.name)
            StatChip("事实", uiState.factCount.toString(), selected = uiState.activeFilter == MemoryType.FACT.name)
        }

        // Memory list
        when {
            uiState.loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.memories.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = TextMuted,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "还没有记忆，点击右上角 + 添加第一条",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextDim,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "记忆会自动从每日复盘中抽取，也可手动添加",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDim,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(uiState.memories, key = { it.id }) { memory ->
                        MemoryCard(
                            memory = memory,
                            onEdit = { editMemory = memory },
                            onDelete = {
                                scope.launch {
                                    memoryViewModel.deleteMemory(memory.id)
                                }
                            },
                        )
                    }
                }
            }
        }

        // Add dialog
        if (showAddDialog) {
            AddMemoryDialog(
                onDismiss = { showAddDialog = false },
                onSave = { content, category, type, importance, tags ->
                    scope.launch {
                        memoryViewModel.addMemory(content, category, type, importance, tags)
                        showAddDialog = false
                    }
                },
            )
        }

        // Edit dialog
        editMemory?.let { mem ->
            EditMemoryDialog(
                memory = mem,
                onDismiss = { editMemory = null },
                onSave = { updated ->
                    scope.launch {
                        memoryViewModel.updateMemory(updated)
                        editMemory = null
                    }
                },
            )
        }
    }
}

@Composable
private fun StatChip(label: String, count: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Primary else BgAccent)
            .clickable { /* filter toggle — expand as needed */ }
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$label $count",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else TextMuted,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun MemoryCard(
    memory: Memory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val cat = memory.categoryEnum
    val type = memory.typeEnum

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgAccent.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(cat?.emoji ?: "📌", fontSize = 18.sp)
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Tags
                if (memory.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        memory.tags.take(3).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                            )
                        }
                    }
                }

                // Content
                Text(
                    text = memory.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Meta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${cat?.label ?: "?"} · ${type?.label ?: "?"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDim,
                        )
                        if (memory.importance >= 4) {
                            Text(
                                text = "⭐".repeat(memory.importance - 2),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Text(
                        text = memory.source.replaceFirst("auto:", ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (content: String, category: String, type: String, importance: Int, tags: List<String>) -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MemoryCategory.USER_FACTS) }
    var selectedType by remember { mutableStateOf(MemoryType.FACT) }
    var importance by remember { mutableStateOf(3) }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(emptyList<String>()) }

    fun addTag() {
        val t = tagInput.trim()
        if (t.isNotBlank() && t !in tags) {
            tags = tags + t
            tagInput = ""
        }
    }

    fun removeTag(tag: String) {
        tags = tags.filter { it != tag }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加记忆") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("记忆内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )

                // Category & Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterDropdown(
                        label = "分类",
                        items = MemoryCategory.entries.map { it.label },
                        selected = selectedCategory.label,
                        onSelect = { idx -> selectedCategory = MemoryCategory.entries[idx] },
                        modifier = Modifier.weight(1f),
                    )
                    FilterDropdown(
                        label = "类型",
                        items = MemoryType.entries.map { it.label },
                        selected = selectedType.label,
                        onSelect = { idx -> selectedType = MemoryType.entries[idx] },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Importance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("重要性", style = MaterialTheme.typography.labelMedium, color = TextDim)
                    Slider(
                        value = importance.toFloat(),
                        onValueChange = { importance = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.weight(1f),
                    )
                    Text("$importance", style = MaterialTheme.typography.labelSmall)
                }

                // Tags
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("标签（回车添加）") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addTag() }),
                    singleLine = true,
                )
                if (tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Primary.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { removeTag(tag) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("#$tag", style = MaterialTheme.typography.labelSmall, color = Primary)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Close, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(content, selectedCategory.name, selectedType.name, importance, tags) },
                enabled = content.isNotBlank(),
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun EditMemoryDialog(
    memory: Memory,
    onDismiss: () -> Unit,
    onSave: (Memory) -> Unit,
) {
    var content by remember { mutableStateOf(memory.content) }
    var selectedCategory by remember { mutableStateOf(memory.categoryEnum ?: MemoryCategory.USER_FACTS) }
    var selectedType by remember { mutableStateOf(memory.typeEnum ?: MemoryType.FACT) }
    var importance by remember { mutableStateOf(memory.importance) }
    var tags by remember { mutableStateOf(memory.tags.toList()) }
    var tagInput by remember { mutableStateOf("") }

    fun addTag() {
        val t = tagInput.trim()
        if (t.isNotBlank() && t !in tags) {
            tags = tags + t
            tagInput = ""
        }
    }

    fun removeTag(tag: String) {
        tags = tags.filter { it != tag }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑记忆") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("记忆内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterDropdown(
                        label = "分类",
                        items = MemoryCategory.entries.map { it.label },
                        selected = selectedCategory.label,
                        onSelect = { idx -> selectedCategory = MemoryCategory.entries[idx] },
                        modifier = Modifier.weight(1f),
                    )
                    FilterDropdown(
                        label = "类型",
                        items = MemoryType.entries.map { it.label },
                        selected = selectedType.label,
                        onSelect = { idx -> selectedType = MemoryType.entries[idx] },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("重要性", style = MaterialTheme.typography.labelMedium, color = TextDim)
                    Slider(
                        value = importance.toFloat(),
                        onValueChange = { importance = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.weight(1f),
                    )
                    Text("$importance", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("标签（回车添加）") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addTag() }),
                    singleLine = true,
                )
                if (tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Primary.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { removeTag(tag) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("#$tag", style = MaterialTheme.typography.labelSmall, color = Primary)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Close, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(memory.copy(
                        content = content,
                        category = selectedCategory.name,
                        type = selectedType.name,
                        importance = importance,
                        tags = tags,
                    ))
                },
                enabled = content.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    items: List<String>,
    selected: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIdx by remember { mutableStateOf(items.indexOf(selected)) }

    Box(modifier = modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextDim)
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selected, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        if (expanded) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { expanded = false },
            ) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            selectedIdx = index
                            onSelect(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

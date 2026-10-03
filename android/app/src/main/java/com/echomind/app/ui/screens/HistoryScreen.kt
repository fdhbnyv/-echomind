package com.echomind.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.echomind.app.R
import com.echomind.app.data.model.StructuredNote
import com.echomind.app.data.repository.NoteRepository
import com.echomind.app.ui.theme.BgAccent
import com.echomind.app.ui.theme.BgMuted
import com.echomind.app.ui.theme.Border
import com.echomind.app.ui.theme.CaveatFontFamily
import com.echomind.app.ui.theme.Primary
import com.echomind.app.ui.theme.PrimaryLight
import com.echomind.app.ui.theme.Success
import com.echomind.app.ui.theme.TextDim
import com.echomind.app.ui.theme.TextMuted
import com.echomind.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch

// --- List item UI model (with id) ---
private data class HistoryListItem(
    val id: Long,
    val title: String,
    val date: String,
    val templateType: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { NoteRepository(context) }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var filterTemplate by remember { mutableStateOf("") }
    var selectedNoteId by remember { mutableStateOf<Long?>(null) }
    var selectedNote by remember { mutableStateOf<StructuredNote?>(null) }

    val entitiesFlow = remember(searchQuery, filterTemplate) {
        if (searchQuery.isBlank() && filterTemplate.isBlank()) repo.allEntities
        else if (filterTemplate.isNotBlank() && searchQuery.isNotBlank()) {
            // Note: allEntities doesn't support search/filter; use allNotes for count
            repo.allEntities
        }
        else if (filterTemplate.isNotBlank()) repo.allEntities
        else repo.allEntities
    }

    val entities by entitiesFlow.collectAsState(initial = emptyList())

    // Filter locally
    val filteredItems = remember(entities, searchQuery, filterTemplate) {
        entities
            .filter { e ->
                (filterTemplate.isBlank() || e.templateType == filterTemplate) &&
                (searchQuery.isBlank() ||
                 e.title.contains(searchQuery, ignoreCase = true) ||
                 e.summary.contains(searchQuery, ignoreCase = true) ||
                 e.tags.contains(searchQuery, ignoreCase = true))
            }
            .map { e ->
                HistoryListItem(
                    id = e.id,
                    title = e.title,
                    date = e.date,
                    templateType = e.templateType,
                )
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ── 1. Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "EchoMind",
                    fontFamily = CaveatFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 32.sp,
                )
                Text(
                    text = if (entities.isEmpty()) "外脑知识库" else "外脑知识库 · ${entities.size} 条记录",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_gear),
                    contentDescription = "设置",
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        AnimatedContent(
            targetState = selectedNote,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(tween(250)) { it / 4 } + fadeIn(tween(250)))
                        .togetherWith(slideOutHorizontally(tween(200)) { -it / 4 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(tween(250)) { -it / 4 } + fadeIn(tween(250)))
                        .togetherWith(slideOutHorizontally(tween(200)) { it / 4 } + fadeOut(tween(200)))
                }
            },
            label = "history_detail_transition",
            modifier = Modifier.fillMaxSize(),
        ) { activeNote ->
            if (activeNote != null) {
                // --- Detail view ---
                NoteDetailView(
                    note = activeNote,
                    onBack = {
                        selectedNote = null
                        selectedNoteId = null
                    },
                    onDelete = {
                        scope.launch {
                            val idToDelete = selectedNoteId
                            if (idToDelete != null) {
                                repo.deleteNoteById(idToDelete)
                                selectedNote = null
                                selectedNoteId = null
                            }
                        }
                    },
                )
            } else {
                // --- List view ---
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索记录...", style = MaterialTheme.typography.bodySmall, color = TextDim) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Border,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { }),
                        textStyle = MaterialTheme.typography.bodySmall,
                    )

                    // Template filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip("全部", filterTemplate == "") { filterTemplate = "" }
                        FilterChip("每日复盘", filterTemplate == "daily-review") { filterTemplate = "daily-review" }
                        FilterChip("碎片想法", filterTemplate == "quick-idea") { filterTemplate = "quick-idea" }
                        FilterChip("会议纪要", filterTemplate == "meeting-notes") { filterTemplate = "meeting-notes" }
                    }
                    Spacer(Modifier.height(4.dp))

                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank() || filterTemplate.isNotBlank())
                                    "没有找到匹配的记录" else "还没有记录，开始录音吧！",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextDim,
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
                            items(filteredItems, key = { it.id }) { item ->
                                SwipeRevealHistoryItem(
                                    item = item,
                                    onClick = {
                                        scope.launch {
                                            val note = repo.getNoteById(item.id)
                                            if (note != null) {
                                                selectedNote = note
                                                selectedNoteId = item.id
                                            }
                                        }
                                    },
                                    onDelete = {
                                        scope.launch {
                                            repo.deleteNoteById(item.id)
                                        }
                                    }
                                )
                                HorizontalDivider(color = Border, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeRevealHistoryItem(
    item: HistoryListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val density = LocalDensity.current
    val maxRevealPx = with(density) { 80.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
    ) {
        // 底层红色删除按钮区域（固定在右侧，不会被滑走）
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFFE53935))
                .clickable {
                    coroutineScope.launch {
                        offsetX.snapTo(0f)
                    }
                    onDelete()
                }
                .padding(end = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.width(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "删除",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // 表层内容卡片：左滑时位移最多 maxRevealPx，绝不会直接滑走误删
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(item.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(-maxRevealPx, 0f)
                                offsetX.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                // 拖动超过一半露出距离则吸附展开删除按钮；否则回弹归零
                                if (offsetX.value < -maxRevealPx / 2) {
                                    offsetX.animateTo(-maxRevealPx, tween(180))
                                } else {
                                    offsetX.animateTo(0f, tween(180))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, tween(180))
                            }
                        }
                    )
                }
        ) {
            HistoryItem(
                item = item,
                onClick = {
                    if (offsetX.value < -10f) {
                        // 处于露出状态时，点击卡片优先收回，不误进详情
                        coroutineScope.launch {
                            offsetX.animateTo(0f, tween(180))
                        }
                    } else {
                        onClick()
                    }
                }
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Primary else BgAccent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else TextMuted,
            fontWeight = if (selected) FontWeight.W500 else FontWeight.Normal,
        )
    }
}

@Composable
private fun HistoryItem(item: HistoryListItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = when (item.templateType) {
            "daily-review" -> "\uD83C\uDF19"
            "quick-idea" -> "\uD83D\uDCA1"
            "meeting-notes" -> "\uD83D\uDCCB"
            else -> "\uD83D\uDCDD"
        }
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(BgAccent),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = icon, fontSize = MaterialTheme.typography.titleMedium.fontSize)
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val badgeLabel = when (item.templateType) {
                    "daily-review" -> "\uD83C\uDF19 复盘"
                    "quick-idea" -> "\uD83D\uDCA1 想法"
                    "meeting-notes" -> "\uD83D\uDCCB 会议"
                    else -> "\uD83D\uDCDD 笔记"
                }
                Text(text = badgeLabel, style = MaterialTheme.typography.labelSmall, color = Primary)
                Text(text = " · ${item.date}", style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
        }
    }
}

// --- Note detail view ---

@Composable
private fun NoteDetailView(
    note: StructuredNote,
    onBack: () -> Unit,
    onDelete: () -> Unit = {},
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除这条笔记吗？删除后将无法恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        // Top Action Bar with Back & Delete button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.TextButton(onClick = onBack) {
                Text("← 返回", fontSize = 13.sp)
            }
            androidx.compose.material3.TextButton(
                onClick = { showDeleteConfirmDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除记录",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("删除记录", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Title
                val icon = when (note.templateType) {
                    "daily-review" -> "\uD83C\uDF19"
                    "quick-idea" -> "\uD83D\uDCA1"
                    "meeting-notes" -> "\uD83D\uDCCB"
                    else -> "\uD83D\uDCDD"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(note.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))

                // Summary
                Text(note.summary, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                Spacer(Modifier.height(8.dp))

                // Accomplishments
                if (note.accomplishments.isNotEmpty()) {
                    Text("\u2705 完成事项", style = MaterialTheme.typography.labelLarge, color = Success)
                    note.accomplishments.forEach { Text("  \u2022 $it", style = MaterialTheme.typography.bodySmall, color = TextMuted) }
                    Spacer(Modifier.height(6.dp))
                }

                // Action items
                if (note.actionItems.isNotEmpty()) {
                    Text("\uD83C\uDFAF 行动项", style = MaterialTheme.typography.labelLarge, color = Primary)
                    note.actionItems.forEach { Text("  \u2611 $it", style = MaterialTheme.typography.bodySmall, color = TextMuted) }
                    Spacer(Modifier.height(6.dp))
                }

                // Key points
                if (note.keyPoints.isNotEmpty()) {
                    Text("\uD83D\uDCCC 关键点", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    note.keyPoints.forEach { Text("  \u2022 $it", style = MaterialTheme.typography.bodySmall, color = TextMuted) }
                    Spacer(Modifier.height(6.dp))
                }

                // Challenges
                if (note.challenges.isNotEmpty()) {
                    Text("\u26A0\uFE0F 挑战", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                    note.challenges.forEach { Text("  \u2022 $it", style = MaterialTheme.typography.bodySmall, color = TextMuted) }
                    Spacer(Modifier.height(6.dp))
                }

                // Tags
                if (note.tags.isNotEmpty()) {
                    Text(
                        note.tags.joinToString("  #", prefix = "#"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                    )
                }

                // Raw transcription
                if (note.rawTranscription.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    Spacer(Modifier.height(8.dp))
                    Text("原始转写", style = MaterialTheme.typography.labelMedium, color = TextDim)
                    Text(note.rawTranscription, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }
    }
}

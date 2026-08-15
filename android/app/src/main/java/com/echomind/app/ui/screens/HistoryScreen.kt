package com.echomind.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echomind.app.data.model.StructuredNote
import com.echomind.app.data.repository.NoteRepository
import com.echomind.app.ui.theme.BgAccent
import com.echomind.app.ui.theme.BgMuted
import com.echomind.app.ui.theme.Border
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

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
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
        // Header
        Text(
            text = "EchoMind",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        )
        HorizontalDivider(color = Border, thickness = 0.5.dp)

        if (selectedNote != null) {
            // --- Detail view ---
            NoteDetailView(
                note = selectedNote!!,
                onBack = {
                    selectedNote = null
                    selectedNoteId = null
                },
            )
        } else {
            // --- List view ---
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索记录...", style = MaterialTheme.typography.bodySmall, color = TextDim) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .height(44.dp),
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
                        HistoryItem(
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
                        )
                        HorizontalDivider(color = Border, thickness = 0.5.dp)
                    }
                }
            }
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
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        // Back button
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text("\u2190 返回", fontSize = 13.sp)
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

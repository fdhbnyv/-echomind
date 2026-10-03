package com.echomind.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echomind.app.R
import com.echomind.app.audio.AudioRecorder
import com.echomind.app.data.model.RecordingState
import com.echomind.app.data.model.TemplateType
import com.echomind.app.ui.theme.BgAccent
import com.echomind.app.ui.theme.BgMuted
import com.echomind.app.ui.theme.Border
import com.echomind.app.ui.theme.CaveatFontFamily
import com.echomind.app.ui.theme.GlassCard
import com.echomind.app.ui.theme.GlassInputBox
import com.echomind.app.ui.theme.Primary
import com.echomind.app.ui.theme.PrimaryLight
import com.echomind.app.ui.theme.RecordingPulse
import com.echomind.app.ui.theme.Success
import com.echomind.app.ui.theme.TextDim
import com.echomind.app.ui.theme.TextMuted
import com.echomind.app.ui.theme.TextPrimary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
) {
    var inputText by remember { mutableStateOf("") }
    var permissionDenied by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val vmState by viewModel.uiState.collectAsStateWithLifecycle()
    val audioRecorder = remember { AudioRecorder(context) }
    val isRecording = vmState.recordingState == RecordingState.RECORDING
    val isBusy = vmState.recordingState == RecordingState.TRANSCRIBING ||
            vmState.recordingState == RecordingState.STRUCTURING

    LaunchedEffect(vmState.latestTranscribedText) {
        val newText = vmState.latestTranscribedText
        if (!newText.isNullOrBlank()) {
            if (inputText.isBlank()) {
                inputText = newText
            } else {
                inputText = "${inputText.trimEnd()}\n$newText"
            }
            viewModel.clearLatestTranscribedText()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            viewModel.startRecordingReal(audioRecorder)
        } else {
            permissionDenied = true
        }
    }

    val todayDateStr = remember {
        SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE).format(Date())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                    text = todayDateStr,
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

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ── 2. Template Selector Chips ──
            TemplateSelectorChips(
                selected = vmState.selectedTemplate,
                onSelect = { viewModel.selectTemplate(it) },
                enabled = !isBusy && !isRecording
            )

            Spacer(Modifier.height(12.dp))

            // ── 3. Frosted Text Input Card ──
            GlassInputBox(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { if (!isBusy) inputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(Primary),
                        enabled = !isBusy,
                        decorationBox = { inner ->
                            if (inputText.isEmpty()) {
                                Text(
                                    "把你想说或想写的话放在这里…\n• 支持随手打字输入\n• 也可随时点击下方说话\n• AI 将按选中模板自动结构化整理",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        color = TextDim
                                    )
                                )
                            }
                            inner()
                        },
                    )

                    // Processing indicator
                    if (isBusy) {
                        Spacer(Modifier.height(8.dp))
                        ProcessingIndicator(state = vmState.recordingState)
                    }

                    // Recording waveform & Live Streaming Transcription
                    if (isRecording) {
                        Spacer(Modifier.height(8.dp))
                        RecordingWaveform(
                            audioRecorder = audioRecorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                        )
                        if (vmState.transcription.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_mic_rounded),
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = vmState.transcription,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Primary,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Card Bottom Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_tag_label),
                                contentDescription = "标签",
                                tint = TextDim,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "结构化标签",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDim
                            )
                        }

                        Text(
                            text = "${inputText.length} 字",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDim
                        )
                    }
                }
            }

            if (permissionDenied) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "需要麦克风权限才能录音",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── 4. Compact Dual-Action Bar (Voice Capsule + AI Organize) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compact Voice Pill Button
                CompactRecordPillButton(
                    isRecording = isRecording,
                    isBusy = isBusy,
                    onClick = {
                        val ok = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (ok) {
                            if (vmState.recordingState == RecordingState.RECORDING) {
                                viewModel.stopRecordingReal(audioRecorder)
                            } else {
                                viewModel.startRecordingReal(audioRecorder)
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // AI Organize Button
                Button(
                    onClick = {
                        if (inputText.isNotBlank() && !isBusy) {
                            viewModel.processTextInput(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        disabledContainerColor = Primary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ai_sparkles),
                        contentDescription = "AI 整理",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "AI 整理",
                        style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    )
                }

                // Quick Save Draft Button (when text is present)
                if (inputText.isNotBlank() && !isBusy) {
                    IconButton(
                        onClick = {
                            viewModel.saveRawText(inputText)
                            inputText = ""
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Success.copy(alpha = 0.15f))
                    ) {
                        Text("存", color = Success, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 5. Recent Captures Capsule ──
            RecentCapturesCard(
                recentNotes = vmState.recentNotes,
                onViewAll = onNavigateToHistory
            )

            Spacer(Modifier.height(16.dp))

            // ── 6. Result Area (When completed) ──
            AnimatedVisibility(
                visible = vmState.recordingState == RecordingState.COMPLETED && vmState.structuredNote != null,
                enter = fadeIn(tween(400)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    initialOffsetY = { it / 3 }
                ),
                exit = fadeOut(tween(200)),
            ) {
                val note = vmState.structuredNote
                if (note != null) {
                    Column {
                        ResultCard(
                            note = note,
                            isEditing = vmState.isEditing,
                            onStartEdit = { viewModel.startEditing() },
                            onSave = { updated -> viewModel.saveEditedNote(updated) },
                            onCancel = { viewModel.cancelEditing() },
                        )
                        Spacer(Modifier.height(12.dp))
                        if (!vmState.isEditing) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.resetState() },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha = 0.85f))
                                ) {
                                    Text("再来一条")
                                }
                            }
                        }
                    }
                }
            }

            if (vmState.recordingState == RecordingState.ERROR) {
                vmState.errorMessage?.let { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            if (msg.contains("设置")) {
                                TextButton(
                                    onClick = onNavigateToSettings,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("去设置", color = Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Template Selector Chips
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TemplateSelectorChips(
    selected: TemplateType,
    onSelect: (TemplateType) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TemplateChipItem(
            label = "✨ 智能识别",
            iconRes = R.drawable.ic_ai_sparkles,
            isSelected = selected == TemplateType.AUTO,
            onClick = { if (enabled) onSelect(TemplateType.AUTO) },
        )
        TemplateChipItem(
            label = "碎片想法",
            iconRes = R.drawable.ic_tpl_idea,
            isSelected = selected == TemplateType.QUICK_IDEA,
            onClick = { if (enabled) onSelect(TemplateType.QUICK_IDEA) },
        )
        TemplateChipItem(
            label = "每日复盘",
            iconRes = R.drawable.ic_tpl_daily,
            isSelected = selected == TemplateType.DAILY_REVIEW,
            onClick = { if (enabled) onSelect(TemplateType.DAILY_REVIEW) },
        )
        TemplateChipItem(
            label = "会议纪要",
            iconRes = R.drawable.ic_tpl_meeting,
            isSelected = selected == TemplateType.MEETING_NOTES,
            onClick = { if (enabled) onSelect(TemplateType.MEETING_NOTES) },
        )
    }
}

@Composable
private fun TemplateChipItem(
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgAlpha = if (isSelected) 0.2f else 0.05f
    val borderColor = if (isSelected) Primary else Border
    val textColor = if (isSelected) Primary else TextMuted

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) Primary.copy(alpha = bgAlpha) else MaterialTheme.colorScheme.surface)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compact Record Pill Button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompactRecordPillButton(
    isRecording: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val bgColor = when {
        isRecording -> RecordingPulse.copy(alpha = pulseAlpha)
        isBusy -> MaterialTheme.colorScheme.surfaceVariant
        else -> Primary.copy(alpha = 0.15f)
    }

    val contentColor = when {
        isRecording -> Color.White
        isBusy -> TextMuted
        else -> Primary
    }

    Row(
        modifier = modifier
            .height(48.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (isRecording) RecordingPulse else Primary.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isBusy,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mic_rounded),
            contentDescription = "录音",
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = when {
                isRecording -> "点击结束录音"
                isBusy -> "正在整理中…"
                else -> "轻触开始倾诉"
            },
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recent Captures Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentCapturesCard(
    recentNotes: List<NoteListItem>,
    onViewAll: () -> Unit,
) {
    val latestNote = recentNotes.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "最近闪念",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            Text(
                "查看全部 >",
                style = MaterialTheme.typography.labelSmall,
                color = Primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onViewAll)
            )
        }

        Spacer(Modifier.height(8.dp))

        if (latestNote != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgAccent)
                    .clickable(onClick = onViewAll)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = latestNote.title.ifBlank { "无标题笔记" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = latestNote.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim
                    )
                }

                if (latestNote.synced) {
                    Spacer(Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Success.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notion_sync),
                            contentDescription = "已同步",
                            tint = Success,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "Notion已同步",
                            fontSize = 10.5.sp,
                            color = Success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            Text(
                "暂无记录，快去说一句或写下一段灵感吧 ✨",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Processing Indicator & Waveform
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProcessingIndicator(state: RecordingState) {
    val infiniteTransition = rememberInfiniteTransition(label = "processing_shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_pos",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Primary.copy(alpha = 0.5f),
            PrimaryLight,
            Primary.copy(alpha = 0.5f),
        ),
        start = Offset(shimmerTranslate, 0f),
        end = Offset(shimmerTranslate + 140f, 0f),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val label = when (state) {
            RecordingState.TRANSCRIBING -> "正在转写语音…"
            RecordingState.STRUCTURING -> "AI 正在结构化整理…"
            else -> "正在处理中…"
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.W600,
                brush = shimmerBrush,
            ),
        )
    }
}

@Composable
private fun RecordingWaveform(
    audioRecorder: AudioRecorder,
    modifier: Modifier = Modifier,
) {
    val bars = remember { mutableStateListOf(*Array(24) { 0.15f }) }
    val primaryColor = Primary

    LaunchedEffect(Unit) {
        while (true) {
            val amp = audioRecorder.getAmplitude()
            val norm = (amp / 32767f).coerceIn(0.08f, 1f)
            bars.removeAt(0)
            bars.add(norm)
            kotlinx.coroutines.delay(60)
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val barWidthPx = with(density) { 3.dp.toPx() }
    val minHPx = with(density) { 4.dp.toPx() }
    val cornerRadiusPx = with(density) { 2.dp.toPx() }

    Canvas(modifier = modifier) {
        val barCount = bars.size
        val spacing = (size.width - barCount * barWidthPx) / (barCount - 1).coerceAtLeast(1)
        val maxH = size.height

        for (i in 0 until barCount) {
            val x = i * (barWidthPx + spacing)
            val h = (bars[i] * maxH).coerceAtLeast(minHPx)
            val top = (maxH - h) / 2f
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(x, top),
                size = androidx.compose.ui.geometry.Size(barWidthPx, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ResultCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ResultCard(
    note: com.echomind.app.data.model.StructuredNote,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onSave: (com.echomind.app.data.model.StructuredNote) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (isEditing) {
            ResultCardEdit(note = note, onSave = onSave, onCancel = onCancel)
        } else {
            ResultCardView(note = note, onStartEdit = onStartEdit)
        }
    }
}

@Composable
private fun ResultCardView(
    note: com.echomind.app.data.model.StructuredNote,
    onStartEdit: () -> Unit,
) {
    val categoryLabel = when (note.templateType) {
        "daily-review" -> "🌙 每日复盘"
        "quick-idea" -> "💡 碎片想法"
        "meeting-notes" -> "🤝 会议纪要"
        else -> "📝 结构化笔记"
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Header with Category Badge & Date & Edit
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (note.date.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = note.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onStartEdit, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = TextDim,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Title
        Text(
            text = note.title.ifBlank { "整理结果" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(10.dp))

        // 1. One-Sentence TL;DR Banner
        if (note.summary.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary.copy(alpha = 0.08f))
                    .border(0.5.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "📌 核心提炼 (TL;DR)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = note.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 2. Action items (待办清单)
        if (note.actionItems.isNotEmpty()) {
            Text(
                text = "🎯 行动清单",
                style = MaterialTheme.typography.labelMedium,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            note.actionItems.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("☑ ", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = action,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // 3. Key points (核心要点)
        if (note.keyPoints.isNotEmpty()) {
            Text(
                text = "💡 核心要点",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            note.keyPoints.forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("• ", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = point.replace("**", ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // 4. Accomplishments (已完成)
        if (note.accomplishments.isNotEmpty()) {
            Text(
                text = "✅ 完成事项",
                style = MaterialTheme.typography.labelMedium,
                color = Success,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            note.accomplishments.forEach {
                Text(
                    text = "  ✓ $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // 5. Challenges (阻碍与挑战)
        if (note.challenges.isNotEmpty()) {
            Text(
                text = "⚠️ 待解阻碍",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            note.challenges.forEach {
                Text(
                    text = "  ! $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // 6. Tags & Mood
        if (note.tags.isNotEmpty() || (note.mood != null && note.mood.isNotBlank())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                note.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Primary.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (note.mood != null && note.mood.isNotBlank()) {
                    val moodEmoji = when (note.mood) {
                        "productive" -> "⚡ 状态充沛"
                        "happy" -> "😊 心情愉悦"
                        "calm" -> "🌿 平和沉静"
                        "tired" -> "🥱 略显疲惫"
                        "stressed" -> "🔥 压力较大"
                        else -> note.mood
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = moodEmoji,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDim,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCardEdit(
    note: com.echomind.app.data.model.StructuredNote,
    onSave: (com.echomind.app.data.model.StructuredNote) -> Unit,
    onCancel: () -> Unit,
) {
    var editTitle by remember { mutableStateOf(note.title) }
    var editSummary by remember { mutableStateOf(note.summary) }
    var editAccomplishments by remember { mutableStateOf(note.accomplishments.toMutableList()) }
    var editActionItems by remember { mutableStateOf(note.actionItems.toMutableList()) }
    var editTagsText by remember { mutableStateOf(note.tags.joinToString(", ")) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("编辑笔记", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "取消", tint = TextDim)
            }
        }
        Spacer(Modifier.height(10.dp))

        Text("标题", style = MaterialTheme.typography.labelSmall, color = TextDim)
        BasicTextField(
            value = editTitle,
            onValueChange = { editTitle = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W600, color = TextPrimary),
            cursorBrush = SolidColor(Primary),
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
        Spacer(Modifier.height(10.dp))

        Text("摘要", style = MaterialTheme.typography.labelSmall, color = TextDim)
        BasicTextField(
            value = editSummary,
            onValueChange = { editSummary = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(vertical = 4.dp),
            textStyle = TextStyle(fontSize = 14.sp, color = TextMuted),
            cursorBrush = SolidColor(Primary),
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = onCancel) { Text("取消") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val updatedTags = editTagsText.split(",", "，")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    onSave(
                        note.copy(
                            title = editTitle,
                            summary = editSummary,
                            accomplishments = editAccomplishments,
                            actionItems = editActionItems,
                            tags = updatedTags,
                            keyPoints = note.keyPoints,
                            challenges = note.challenges,
                            mood = note.mood,
                            date = note.date,
                            templateType = note.templateType
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("保存")
            }
        }
    }
}

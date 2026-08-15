package com.echomind.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echomind.app.audio.AudioRecorder
import com.echomind.app.data.model.RecordingState
import com.echomind.app.data.model.TemplateType
import com.echomind.app.ui.theme.GlassCard
import com.echomind.app.ui.theme.GlassInputBox
import com.echomind.app.ui.theme.BgAccent
import com.echomind.app.ui.theme.BgMuted
import com.echomind.app.ui.theme.Border
import com.echomind.app.ui.theme.Primary
import com.echomind.app.ui.theme.PrimaryLight
import com.echomind.app.ui.theme.RecordingPulse
import com.echomind.app.ui.theme.Success
import com.echomind.app.ui.theme.TextDim
import com.echomind.app.ui.theme.TextMuted
import com.echomind.app.ui.theme.TextPrimary
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen (main)
// ─────────────────────────────────────────────────────────────────────────────

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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) { permissionDenied = false; viewModel.startRecordingReal(audioRecorder) }
        else { permissionDenied = true }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(com.echomind.app.R.drawable.ic_logo),
                contentDescription = "声念",
                tint = Primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("声念",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "设置", tint = TextMuted)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Spacer(Modifier.height(8.dp))

            // === Input card ===
            val isBusy = vmState.recordingState == RecordingState.TRANSCRIBING ||
                         vmState.recordingState == RecordingState.STRUCTURING

            GlassInputBox(modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { if (!isBusy) inputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = TextPrimary),
                        cursorBrush = SolidColor(Primary),
                        enabled = !isBusy,
                        decorationBox = { inner ->
                            if (inputText.isEmpty())
                                Text(
                                    "把你想说的话写在这里…",
                                    style = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = TextDim)
                                )
                            inner()
                        },
                    )

                    // Processing indicator
                    if (isBusy) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val label = when (vmState.recordingState) {
                                RecordingState.TRANSCRIBING -> "正在转写…"
                                RecordingState.STRUCTURING -> "AI 结构化中…"
                                else -> "处理中…"
                            }
                            Text(label, style = MaterialTheme.typography.bodySmall, color = Primary)
                        }
                    }

                    // Recording waveform
                    if (isRecording) {
                        RecordingWaveform(
                            audioRecorder = audioRecorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .height(48.dp),
                        )
                    }

                    InputActions(
                        isRecording = isRecording,
                        len = inputText.length,
                        canSubmit = inputText.isNotBlank() && !isBusy,
                        isBusy = isBusy,
                        onVoiceClick = {
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
                        onSaveRaw = {
                            viewModel.saveRawText(inputText)
                            inputText = ""
                        },
                        onSubmit = {
                            viewModel.processTextInput(inputText)
                            inputText = ""
                        },
                    )
                }
            }

            if (permissionDenied) {
                Text(
                    "需要麦克风权限才能录音",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(10.dp))
            Spacer(Modifier.height(16.dp))

            // === Result area ===
            when (vmState.recordingState) {
                RecordingState.COMPLETED -> {
                    val note = vmState.structuredNote
                    if (note != null) {
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
                                    shape = RoundedCornerShape(8.dp),
                                ) { Text("再来一条") }
                            }
                        }
                    }
                }
                RecordingState.ERROR -> {
                    vmState.errorMessage?.let { msg ->
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(16.dp))

            // === Link to history ===
            Text(
                "查看全部记录 →",
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.W500,
                modifier = Modifier
                    .clickable(onClick = onNavigateToHistory)
                    .padding(vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TemplateSelector
// ─────────────────────────────────────────────────────────────────────────────

@Composable




private fun InputActions(
    isRecording: Boolean,
    len: Int,
    canSubmit: Boolean,
    isBusy: Boolean,
    onVoiceClick: () -> Unit,
    onSaveRaw: () -> Unit,
    onSubmit: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Recording button
        Box(
            modifier = Modifier
                .size(38.dp)
                .scale(if (isRecording) pulseScale else 1f)
                .then(
                    Modifier
                        .clip(CircleShape)
                        .background(
                            when {
                                isRecording -> RecordingPulse
                                isBusy -> Primary
                                else -> Color.Transparent
                            }
                        )
                        .border(1.5.dp, if (isRecording) RecordingPulse else Border, CircleShape)
                )
                .clickable(enabled = !isBusy, onClick = onVoiceClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    isRecording -> "\u25A0"
                    isBusy -> "..."
                    else -> "\uD83C\uDFA4"
                },
                fontSize = 15.sp,
                color = if (isRecording || isBusy) Color.White else TextMuted,
            )
        }

        Spacer(Modifier.weight(1f))
        Text("$len 字", style = MaterialTheme.typography.bodySmall, color = TextDim)
        Spacer(Modifier.width(8.dp))

        Button(
            onClick = onSaveRaw,
            enabled = canSubmit,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Success,
                disabledContainerColor = Success.copy(alpha = 0.4f),
            ),
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        ) {
            Text("保存", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W500))
        }

        Spacer(Modifier.width(8.dp))

        Button(
            onClick = onSubmit,
            enabled = canSubmit,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                disabledContainerColor = Primary.copy(alpha = 0.4f),
            ),
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        ) {
            Text("AI 整理", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W500))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ResultCard — structured note preview + editable edit mode
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultCard(
    note: com.echomind.app.data.model.StructuredNote,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onSave: (com.echomind.app.data.model.StructuredNote) -> Unit,
    onCancel: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
    ) {
        if (isEditing) {
            ResultCardEdit(note = note, onSave = onSave, onCancel = onCancel)
        } else {
            ResultCardView(note = note, onStartEdit = onStartEdit)
        }
    }
}

// ── Read-only view ──

@Composable
private fun ResultCardView(
    note: com.echomind.app.data.model.StructuredNote,
    onStartEdit: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Title row + edit button
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = when (note.templateType) {
                "daily-review" -> "\uD83C\uDF19"
                "quick-idea" -> "\uD83D\uDCA1"
                "meeting-notes" -> "\uD83D\uDCCB"
                else -> "\uD83D\uDCDD"
            }
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(note.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            // Edit button
            IconButton(onClick = onStartEdit, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = TextDim, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(8.dp))

        // Summary
        Text(note.summary, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Spacer(Modifier.height(8.dp))

        // Accomplishments
        if (note.accomplishments.isNotEmpty()) {
            Text("✅ 完成事项", style = MaterialTheme.typography.labelLarge, color = Success)
            note.accomplishments.forEach { Text("  \u2022 $it", style = MaterialTheme.typography.bodySmall, color = TextMuted) }
            Spacer(Modifier.height(6.dp))
        }

        // Action items
        if (note.actionItems.isNotEmpty()) {
            Text("\uD83C\uDFAF 行动项", style = MaterialTheme.typography.labelLarge, color = Primary)
            note.actionItems.forEach { Text("  \u2611 $it", style = MaterialTheme.typography.bodySmall, color = TextMuted) }
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

        // Add to calendar button
        if (note.actionItems.isNotEmpty()) {
            @Suppress("DEPRECATION")
            val ctx = androidx.compose.ui.platform.LocalContext.current
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val helper = com.echomind.app.service.CalendarHelper
                    for (item in note.actionItems) {
                        val deadlinePat = Regex("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})")
                        val m = deadlinePat.find(item)
                        val ms = m?.let { helper.parseDeadlineToMillis(it.value) }
                        helper.insertEvent(ctx, item.take(50), startTimeMs = ms)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success),
                modifier = Modifier.fillMaxWidth().height(36.dp),
            ) {
                Text("\uD83D\uDCC5 添加到日历", fontSize = 13.sp)
            }
        }
    }
}

// ── Editable view ──

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
        // Title bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = when (note.templateType) {
                "daily-review" -> "\uD83C\uDF19"
                "quick-idea" -> "\uD83D\uDCA1"
                "meeting-notes" -> "\uD83D\uDCCB"
                else -> "\uD83D\uDCDD"
            }
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text("编辑笔记", style = MaterialTheme.typography.titleSmall, color = TextDim)
        }
        Spacer(Modifier.height(10.dp))

        // Title
        EditableLabel("标题")
        BasicTextField(
            value = editTitle,
            onValueChange = { editTitle = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W600, color = TextPrimary),
            cursorBrush = SolidColor(Primary),
        )
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
        Spacer(Modifier.height(10.dp))

        // Summary
        EditableLabel("摘要")
        BasicTextField(
            value = editSummary,
            onValueChange = { editSummary = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 4.dp),
            textStyle = TextStyle(fontSize = 14.sp, color = TextMuted),
            cursorBrush = SolidColor(Primary),
        )
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
        Spacer(Modifier.height(10.dp))

        // Accomplishments
        EditableLabel("✅ 完成事项")
        EditableStringList(
            items = editAccomplishments,
            onItemsChange = { editAccomplishments = it },
            placeholder = "新增完成事项…",
        )
        Spacer(Modifier.height(6.dp))

        // Action items
        EditableLabel("\uD83C\uDFAF 行动项")
        EditableStringList(
            items = editActionItems,
            onItemsChange = { editActionItems = it },
            placeholder = "新增行动项…",
        )
        Spacer(Modifier.height(6.dp))

        // Tags
        EditableLabel("标签（逗号分隔）")
        BasicTextField(
            value = editTagsText,
            onValueChange = { editTagsText = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            textStyle = TextStyle(fontSize = 13.sp, color = Primary),
            cursorBrush = SolidColor(Primary),
        )
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
        Spacer(Modifier.height(14.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
            ) { Text("取消") }
            Button(
                onClick = {
                    val updated = note.copy(
                        title = editTitle.trim(),
                        summary = editSummary.trim(),
                        accomplishments = editAccomplishments.filter { it.isNotBlank() },
                        actionItems = editActionItems.filter { it.isNotBlank() },
                        tags = editTagsText.split(",", "，").map { it.trim() }.filter { it.isNotBlank() },
                    )
                    onSave(updated)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) { Text("保存") }
        }
    }
}

// ── Editable list component ──

@Composable
private fun EditableStringList(
    items: MutableList<String>,
    onItemsChange: (MutableList<String>) -> Unit,
    placeholder: String,
) {
    Column {
        items.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
                BasicTextField(
                    value = item,
                    onValueChange = { newVal ->
                        val copy = items.toMutableList()
                        copy[index] = newVal
                        onItemsChange(copy)
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 13.sp, color = TextMuted),
                    cursorBrush = SolidColor(Primary),
                )
                IconButton(
                    onClick = {
                        val copy = items.toMutableList()
                        copy.removeAt(index)
                        onItemsChange(copy)
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                }
            }
        }
        // Add item button
        TextButton(
            onClick = {
                val copy = items.toMutableList()
                copy.add("")
                onItemsChange(copy)
            },
            modifier = Modifier.height(28.dp),
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = Primary)
            Spacer(Modifier.width(4.dp))
            Text("+ $placeholder", fontSize = 12.sp, color = Primary)
        }
    }
}

// ── Small label ──

@Composable
private fun EditableLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.W600,
        color = TextDim,
        modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp),
    )
}

// ── 录音水波纹动画 ──

private const val WAVE_SAMPLE_POINTS = 48
private const val WAVE_POLL_MS = 60L
private const val AMPLITUDE_MAX_LOG = 8f

@Composable
private fun RecordingWaveform(
    audioRecorder: AudioRecorder,
    modifier: Modifier = Modifier,
) {
    val samples = remember { mutableStateListOf<Float>() }

    LaunchedEffect(Unit) {
        while (true) {
            val raw = audioRecorder.getAmplitude().coerceAtLeast(1)
            val normalized = (kotlin.math.ln(raw.toDouble()) / AMPLITUDE_MAX_LOG).toFloat().coerceIn(0f, 1f)
            samples.add(normalized)
            if (samples.size > WAVE_SAMPLE_POINTS) {
                samples.removeAt(0)
            }
            kotlinx.coroutines.delay(WAVE_POLL_MS)
        }
    }

    Canvas(modifier = modifier) {
        if (samples.size < 2) return@Canvas
        val w = size.width
        val h = size.height

        // 两层波纹叠加，产生水感
        drawWaterRipple(samples, w, h, alpha = 0.25f, yOffset = -2.dp.toPx())
        drawWaterRipple(samples, w, h, alpha = 0.60f, yOffset = 0f)
    }
}

private fun DrawScope.drawWaterRipple(
    samples: MutableList<Float>,
    w: Float,
    h: Float,
    alpha: Float,
    yOffset: Float,
) {
    val stepX = w / (WAVE_SAMPLE_POINTS - 1)
    val baseY = h * 0.85f + yOffset // 基准线在下方，波形从底部涌起

    val fill = Path()
    val line = Path()

    // 首点
    val firstNorm = samples.first().coerceAtLeast(0.05f)
    var prevY = baseY - firstNorm * h * 0.7f
    fill.moveTo(0f, prevY)
    line.moveTo(0f, prevY)

    // 经过每个采样点的三次贝塞尔曲线
    for (i in 1 until samples.size) {
        val x = i * stepX
        val norm = samples[i].coerceAtLeast(0.05f)
        val y = baseY - norm * h * 0.7f
        val prevX = (i - 1) * stepX
        val cp1x = prevX + stepX * 0.4f
        val cp2x = x - stepX * 0.4f

        fill.cubicTo(cp1x, prevY, cp2x, y, x, y)
        line.cubicTo(cp1x, prevY, cp2x, y, x, y)
        prevY = y
    }

    // 填充路径封底
    val lastX = (samples.size - 1) * stepX
    fill.lineTo(lastX, h)
    fill.lineTo(0f, h)
    fill.close()

    // 渐变填充
    drawPath(
        fill,
        brush = Brush.verticalGradient(
            colors = listOf(
                Primary.copy(alpha = alpha),
                Primary.copy(alpha = alpha * 0.3f),
                Color.Transparent,
            ),
            startY = 0f,
            endY = h,
        ),
    )

    // 曲线描边
    drawPath(line, color = Primary.copy(alpha = (alpha * 1.5f).coerceAtMost(1f)), style = Stroke(width = 1.5.dp.toPx()))
}

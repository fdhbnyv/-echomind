package com.echomind.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echomind.app.ui.components.EchoMindLogoAnimation
import com.echomind.app.ui.theme.Border
import com.echomind.app.ui.theme.Primary
import com.echomind.app.ui.theme.Success
import com.echomind.app.ui.theme.TextDim
import com.echomind.app.ui.theme.TextMuted
import com.echomind.app.ui.theme.TextPrimary

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary,
        unfocusedBorderColor = Border,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (onBack != null) 6.dp else 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        HorizontalDivider(color = Border, thickness = 0.5.dp)
        Spacer(Modifier.height(8.dp))

        // === AI 服务配置 (OpenAI 兼容协议) ===
        var apiKeyVisible by remember { mutableStateOf(false) }

        Group("AI 服务设置 (OpenAI 协议)") {
            Column(modifier = Modifier.padding(14.dp)) {
                // 快捷预设 Chips
                Text(
                    text = "常用服务预设",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AiPresetChip(
                        label = "DeepSeek V4.1",
                        isSelected = uiState.openAiBaseUrl.contains("deepseek.com") && uiState.openAiModel == "deepseek-flash"
                    ) {
                        viewModel.applyPreset("https://api.deepseek.com/v1", "deepseek-flash")
                    }
                    AiPresetChip(
                        label = "DeepSeek Pro",
                        isSelected = uiState.openAiBaseUrl.contains("deepseek.com") && uiState.openAiModel == "deepseek-v4-pro"
                    ) {
                        viewModel.applyPreset("https://api.deepseek.com/v1", "deepseek-v4-pro")
                    }
                    AiPresetChip(
                        label = "OpenAI",
                        isSelected = uiState.openAiBaseUrl == "https://api.openai.com/v1"
                    ) {
                        viewModel.applyPreset("https://api.openai.com/v1", "gpt-4o-mini")
                    }
                    AiPresetChip(
                        label = "阿里百炼",
                        isSelected = uiState.openAiBaseUrl.contains("dashscope")
                    ) {
                        viewModel.applyPreset("https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus")
                    }
                    AiPresetChip(
                        label = "硅基流动",
                        isSelected = uiState.openAiBaseUrl.contains("siliconflow")
                    ) {
                        viewModel.applyPreset("https://api.siliconflow.cn/v1", "deepseek-ai/DeepSeek-V3")
                    }
                    AiPresetChip(
                        label = "OpenRouter",
                        isSelected = uiState.openAiBaseUrl.contains("openrouter")
                    ) {
                        viewModel.applyPreset("https://openrouter.ai/api/v1", "openai/gpt-4o-mini")
                    }
                    AiPresetChip(
                        label = "本地 Ollama",
                        isSelected = uiState.openAiBaseUrl.contains("11434")
                    ) {
                        viewModel.applyPreset("http://10.0.2.2:11434/v1", "qwen2.5:7b")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // API 基础地址
                OutlinedTextField(
                    value = uiState.openAiBaseUrl,
                    onValueChange = { viewModel.updateOpenAiBaseUrl(it) },
                    label = { Text("API 接口地址 (Base URL)") },
                    placeholder = { Text("https://api.openai.com/v1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                )

                Spacer(Modifier.height(10.dp))

                // API Key
                OutlinedTextField(
                    value = uiState.openAiApiKey,
                    onValueChange = { viewModel.updateOpenAiApiKey(it) },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "切换可见性",
                                tint = TextDim,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                )

                Spacer(Modifier.height(12.dp))

                // 测试连接与拉取模型按钮
                Button(
                    onClick = { viewModel.testConnectionAndFetchModels() },
                    enabled = !uiState.isTestingAi && uiState.openAiBaseUrl.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    if (uiState.isTestingAi) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("正在测试连接并拉取模型…", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("测试连接并拉取模型", fontWeight = FontWeight.Medium)
                    }
                }

                // 联通状态及拉取结果反馈
                if (uiState.testAiMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    val isSuccess = uiState.testAiSuccess == true
                    Surface(
                        color = if (isSuccess) Success.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = uiState.testAiMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) Success else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 当前模型下拉与自定义输入
                ModelSelectorField(
                    selectedModel = uiState.openAiModel,
                    availableModels = uiState.availableModels,
                    onModelSelected = { viewModel.updateOpenAiModel(it) },
                    fieldColors = fieldColors,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // === Notion 知识库 ===
        Group("Notion 知识库") {
            OutlinedTextField(
                value = uiState.notionApiKey,
                onValueChange = { viewModel.updateNotionKey(it) },
                label = { Text("Notion API Key") },
                placeholder = { Text("secret_...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors,
            )
            OutlinedTextField(
                value = uiState.notionDatabaseId,
                onValueChange = { viewModel.updateNotionDb(it) },
                label = { Text("Notion Database ID") },
                placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors,
            )
        }

        Spacer(Modifier.height(4.dp))

        // === 云端同步（Supabase） ===
        Group("云端同步") {
            RowItem("启用云端同步", uiState.cloudSyncEnabled) { viewModel.updateCloudSync(it) }
            HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(start = 14.dp))
            OutlinedTextField(
                value = uiState.supabaseUrl,
                onValueChange = { viewModel.updateSupabaseUrl(it) },
                label = { Text("Supabase Project URL") },
                placeholder = { Text("https://xxxx.supabase.co") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = uiState.supabaseAnonKey,
                onValueChange = { viewModel.updateSupabaseKey(it) },
                label = { Text("Supabase anon key") },
                placeholder = { Text("eyJhbGciOi...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "建表：在 Supabase SQL Editor 执行项目内 docs/supabase-schema.sql，然后填入 Project URL 与 anon key",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { viewModel.syncNow() },
                enabled = uiState.cloudSyncConfigured && uiState.cloudSyncStatus != "syncing",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text(
                    when (uiState.cloudSyncStatus) {
                        "syncing" -> "同步中…"
                        "done" -> "✓ 同步完成"
                        "error" -> "同步失败，请检查网络与配置"
                        else -> "立即同步"
                    },
                    fontWeight = FontWeight.W500,
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(4.dp))

        // === 偏好 ===
        Group("偏好") {
            RowItem("自动同步到 Notion", uiState.autoSync) { viewModel.updateAutoSync(it) }
            RowItem("暗色主题", uiState.isDarkMode ?: false) { viewModel.updateDarkMode(if (it) true else null) }
        }

        Spacer(Modifier.height(4.dp))

        // === 排版 ===
        Group("排版") {
            RowItem("自动添加标题", uiState.autoTitle) { viewModel.updateAutoTitle(it) }
            RowItem("自动识别列表", uiState.autoList) { viewModel.updateAutoList(it) }
            RowItem("添加标签", uiState.autoTags, last = true) { viewModel.updateAutoTags(it) }
        }

        Spacer(Modifier.height(4.dp))

        // === 主题风格文件夹 ===
        ThemeFolderGroup(
            selectedThemeId = uiState.selectedTheme,
            onSelectTheme = { viewModel.updateTheme(it) },
        )

        Spacer(Modifier.height(4.dp))

        // === 语音识别与录音 ===
        Group("语音识别与录音") {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "识别引擎",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 引擎选择 Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.echomind.app.data.model.AsrEngineMode.entries.forEach { mode ->
                        val isSelected = uiState.asrMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Primary else Primary.copy(alpha = 0.08f))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Primary else Border.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.updateAsrMode(mode) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = uiState.asrMode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim,
                    fontSize = 12.sp,
                )

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Border.copy(alpha = 0.6f), thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                // 本地模型状态卡片
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (uiState.isLocalAsrReady) Success.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "端侧离线语音模型",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = uiState.localAsrModelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDim,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = if (uiState.isLocalAsrReady) "✓ 已就绪 (离线)" else "未检测到模型",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isLocalAsrReady) Success else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = Border, thickness = 0.5.dp)
            RowItem("录音时实时预览文字", uiState.livePreviewEnabled, last = false) {
                viewModel.updateLivePreviewEnabled(it)
            }
            RowItem("静音自动结束", uiState.silentStop, last = true) {
                viewModel.updateSilentStop(it)
            }
        }

        Spacer(Modifier.height(4.dp))

        // === 数据 ===
        Group("数据") {
            DataRow("语音引擎", when (uiState.asrMode) {
                com.echomind.app.data.model.AsrEngineMode.LOCAL -> "✓ 本地 Zipformer (离线)"
                com.echomind.app.data.model.AsrEngineMode.CLOUD -> "云端 Whisper"
                com.echomind.app.data.model.AsrEngineMode.AUTO -> "智能 (本地优先)"
            })
            DataRow("AI 接口", if (uiState.openAiConfigured) "✓ ${uiState.openAiModel}" else "未配置")
            DataRow("Notion API", if (uiState.notionApiKey.isNotBlank()) "✓ 已配置" else "未配置")
            DataRow("云端同步", when {
                uiState.cloudSyncConfigured -> "✓ 已开启"
                uiState.cloudSyncEnabled -> "配置不完整"
                else -> "未开启"
            })
            DataRow("本地记录数", "${uiState.recordCount} 条", last = true)
        }

        Spacer(Modifier.height(16.dp))

        // 保存按钮
        Button(
            onClick = { viewModel.save() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
        ) {
            Text(
                if (uiState.saved) "✓ 已保存" else "保存设置",
                fontWeight = FontWeight.W500,
            )
        }

        if (uiState.saved) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "所有设置已保存到本地",
                style = MaterialTheme.typography.bodySmall,
                color = Success,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // 清除本地数据
        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
            ),
        ) {
            Text("🗑 清除所有本地数据", fontWeight = FontWeight.W500)
        }

        Spacer(Modifier.height(28.dp))

        // 关于声念 (Logo 动画展示)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EchoMindLogoAnimation(size = 140.dp)
            Text(
                text = "声念 EchoMind",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "将碎片思维，一语成章。 (v1.0.0)",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    // --- 删除确认弹窗 ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认清除所有数据？") },
            text = {
                Text("这将删除所有本地记录，包括已生成的结构化笔记和设置信息。此操作不可撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showDeleteDialog = false
                    },
                ) { Text("确认清除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }
}

// ===== 组件 =====

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.W600,
            color = TextDim,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun RowItem(
    label: String,
    checked: Boolean,
    last: Boolean = false,
    onToggle: (Boolean) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!checked) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = androidx.compose.ui.graphics.Color.White,
                    uncheckedTrackColor = Border,
                ),
            )
        }
        if (!last) {
            HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(start = 14.dp))
        }
    }
}

@Composable
private fun DataRow(label: String, value: String, last: Boolean = false, accent: Boolean = false) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = if (accent) Success else TextMuted,
            )
        }
        if (!last) {
            HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(start = 14.dp))
        }
    }
}

@Composable
private fun ThemeFolderGroup(
    selectedThemeId: String,
    onSelectTheme: (String) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val themes = com.echomind.app.ui.theme.AppTheme.entries.toList()
    val currentTheme = themes.find { it.id == selectedThemeId } ?: themes.first()
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "themeFolderArrow"
    )

    Group("主题风格") {
        Column {
            // Folder Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Theme icon with soft accent container
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "主题风格",
                        tint = Primary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "主题风格",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = if (isExpanded) "点击收起" else "点击展开切换主题",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                    )
                }

                // Current theme pill badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${currentTheme.icon} ${currentTheme.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(arrowRotation)
                )
            }

            // Expanded content with animation
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(
                        color = Border,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    themes.forEachIndexed { index, theme ->
                        val isSelected = selectedThemeId == theme.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                                .clickable {
                                    onSelectTheme(theme.id)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(theme.icon, fontSize = 16.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = theme.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Primary else TextPrimary,
                                )
                                Text(
                                    text = theme.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextDim,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "已选择",
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (index < themes.lastIndex) {
                            HorizontalDivider(
                                color = Border.copy(alpha = 0.5f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 38.dp, end = 14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Primary else Primary.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = if (isSelected) Primary else Border.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White else Primary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun ModelSelectorField(
    selectedModel: String,
    availableModels: List<String>,
    onModelSelected: (String) -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "当前使用模型",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim,
            )
            if (availableModels.isNotEmpty()) {
                Text(
                    text = "已拉取 ${availableModels.size} 个模型 (点击右侧图标可选择)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedModel,
                onValueChange = onModelSelected,
                label = { Text("模型名称 (可直接编辑或下拉选择)") },
                placeholder = { Text("如 gpt-4o, deepseek-chat...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors,
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "选择模型",
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .heightIn(max = 280.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (availableModels.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "暂无模型列表，请先点击「测试连接并拉取模型」",
                                color = TextDim,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        onClick = { expanded = false }
                    )
                } else {
                    availableModels.forEach { modelName ->
                        val isSelected = modelName == selectedModel
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = modelName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onModelSelected(modelName)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}



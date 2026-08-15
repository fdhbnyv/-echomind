package com.echomind.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echomind.app.ui.theme.Border
import com.echomind.app.ui.theme.Primary
import com.echomind.app.ui.theme.Success
import com.echomind.app.ui.theme.TextDim
import com.echomind.app.ui.theme.TextMuted

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
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
        Text(
            text = "EchoMind",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        )
        HorizontalDivider(color = Border, thickness = 0.5.dp)
        Spacer(Modifier.height(8.dp))

        // === 账户 ===
        Group("账户") {
            DataRow("AI 服务", "✅ 千问已内置", accent = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.notionApiKey,
                onValueChange = { viewModel.updateNotionKey(it) },
                label = { Text("Notion API Key") },
                placeholder = { Text("secret_...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.notionDatabaseId,
                onValueChange = { viewModel.updateNotionDb(it) },
                label = { Text("Notion Database ID") },
                placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors,
            )
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

        // === 主题风格 ===
        Group("主题风格") {
            val themes = com.echomind.app.ui.theme.AppTheme.entries.toList()
            themes.forEach { theme ->
                val isSelected = uiState.selectedTheme == theme.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateTheme(theme.id) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(theme.icon, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(theme.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text(theme.description, style = MaterialTheme.typography.bodySmall, color = TextDim)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Primary)
                    }
                }
                if (theme != themes.last()) {
                    HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(start = 14.dp))
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // === 录音 ===
        Group("录音") {
            RowItem("静音自动结束", uiState.silentStop, last = true) { viewModel.updateSilentStop(it) }
        }

        Spacer(Modifier.height(4.dp))

        // === 数据 ===
        Group("数据") {
            DataRow("Notion API", if (uiState.notionApiKey.isNotBlank()) "✓ 已配置" else "未配置")
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

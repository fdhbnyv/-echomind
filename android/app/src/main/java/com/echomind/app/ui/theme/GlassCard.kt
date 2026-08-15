package com.echomind.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃卡片 — EchoMind 全局玻璃 UI 组件。
 *
 * 特性：
 * - 半透明玻璃表面（配合 Theme 的 surface 颜色）
 * - 极薄边框 + 柔和阴影
 * - 右上角可选微光反射效果
 * - 圆角统一 16dp
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
        ),
    ) {
        // 右上角微光反射
        Box(modifier = Modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.Transparent,
                ),
                radius = 1.2f,
            )
        )) {
            content()
        }
    }
}

/**
 * 玻璃输入框 — 带玻璃边框的输入区域
 */
@Composable
fun GlassInputBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.75f),
                    )
                ),
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = shape,
            )
    ) {
        content()
    }
}

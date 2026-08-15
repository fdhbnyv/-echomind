package com.echomind.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

/**
 * 音频波形动画 — 仿 Siri 式声波动画条。
 *
 * 显示 7 根动态竖条，根据 amplitude 值跳动。
 * 每条有独立的相位偏移产生"波浪"传递感。
 *
 * @param amplitude 0.0–1.0 当前音量幅度（可接入实际麦克风数据）
 * @param barCount 竖条数量
 * @param colorStart 渐变起始色
 * @param colorEnd 渐变结束色
 */
@Composable
fun WaveformBar(
    modifier: Modifier = Modifier,
    amplitude: Float = 0.5f,
    barCount: Int = 7,
    colorStart: Color = Color(0xFF2563EB),
    colorEnd: Color = Color(0xFFA855F7),
    barWidth: Dp = 4.dp,
    maxHeight: Dp = 48.dp,
    roundedCap: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "waveform")

    // 每条柱独立的相位偏移 (0..360)
    val phases = remember(barCount) {
        FloatArray(barCount) { i -> i.toFloat() / barCount * 360f }
    }

    // 整体的波动速度
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    // 次级波动 (叠加层，形成更复杂的波形)
    val subPhase by transition.animateFloat(
        initialValue = 180f,
        targetValue = 540f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "subPhase",
    )

    val density = LocalDensity.current
    val barWidthPx = with(density) { barWidth.toPx() }
    val maxHeightPx = with(density) { maxHeight.toPx() }

    Box(
        modifier = modifier.fillMaxWidth().height(maxHeight + 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(maxHeight + 16.dp),
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f

            // 柱间距
            val totalBarWidth = barWidthPx * barCount
            val gap = (canvasWidth - totalBarWidth) / (barCount + 1)
            val startX = gap

            for (i in 0 until barCount) {
                val waveRad = Math.toRadians((wavePhase + phases[i]).toDouble())
                val subRad = Math.toRadians((subPhase + phases[i] * 0.7f).toDouble())

                // 混合主波 + 次波 + 随机微抖动 (amplitude 控制整体强度)
                val waveVal = sin(waveRad).toFloat() * 0.6f +
                        sin(subRad).toFloat() * 0.3f +
                        i * 0.02f

                // 居中偏移 + 振幅缩放
                val normalized = (waveVal * 0.5f + 0.5f) * amplitude.coerceIn(0.05f, 1f)
                val barHeight = (normalized * maxHeightPx * 0.9f + 4f)
                    .coerceIn(2f, maxHeightPx)

                val x = startX + i * (barWidthPx + gap)

                // 渐变填充
                val t = i.toFloat() / (barCount - 1).coerceAtLeast(1)
                val barColor = lerp(colorStart, colorEnd, t)

                // 圆角矩形
                val cornerR = if (roundedCap) barWidthPx / 2f else 0f
                drawRoundRect(
                    color = barColor.copy(alpha = 0.85f),
                    topLeft = Offset(x, centerY - barHeight / 2f),
                    size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR, cornerR),
                )

                // 发光光晕 (每条的半透明背景)
                if (amplitude > 0.3f) {
                    val glowHeight = barHeight * 1.3f
                    drawRoundRect(
                        color = barColor.copy(alpha = 0.12f * amplitude),
                        topLeft = Offset(x - barWidthPx * 0.3f, centerY - glowHeight / 2f),
                        size = androidx.compose.ui.geometry.Size(
                            barWidthPx * 1.6f,
                            glowHeight,
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR, cornerR),
                    )
                }
            }

            // 底部连接线 (让柱子看起来是一体的)
            val linePath = Path()
            val lineY = centerY + maxHeightPx / 2f + with(density) { 4.dp.toPx() }
            linePath.moveTo(startX, lineY)
            for (i in 0 until barCount) {
                val x = startX + i * (barWidthPx + gap) + barWidthPx / 2f
                linePath.lineTo(x, lineY)
            }
            drawPath(
                path = linePath,
                color = colorStart.copy(alpha = 0.15f),
                style = Stroke(width = with(density) { 1.dp.toPx() }),
            )
        }
    }
}

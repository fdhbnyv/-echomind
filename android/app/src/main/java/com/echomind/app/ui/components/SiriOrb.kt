package com.echomind.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Siri 式发光球体 — EchoMind 语音交互核心视觉组件。
 *
 * 多层 Canvas 渲染：
 * - 环境光晕 (ambient glow) — 大范围柔光
 * - 外光晕 (outer aura) — 动态彩色渐变
 * - 主体球 — 径向渐变 + 色彩流动
 * - 核心高光 — 偏置亮斑
 * - 镜面高光 — 小点高光
 *
 * 状态变化：
 * - idle: 缓慢呼吸、色彩柔移
 * - listening: 快速脉动、色彩活跃
 * - processing: 快速微闪、旋转感
 */
enum class OrbState { IDLE, LISTENING, PROCESSING }

/**
 * 球体色彩调色板 — 声波蓝→紫→粉 渐变
 */
private val OrbPalette = listOf(
    Color(0xFF2563EB), // 主蓝
    Color(0xFF4F46E5), // 靛蓝
    Color(0xFF7C3AED), // 紫
    Color(0xFFA855F7), // 浅紫
    Color(0xFFEC4899), // 粉
    Color(0xFF06B6D4), // 青
)

/**
 * 核心高光色 — 暖白
 */
private val CoreHighlight = Color(0xCCFFFFFF)
private val Specular = Color(0x66FFFFFF)

/**
 * 状态对应的动画参数
 */
private data class OrbParams(
    val breathePeriodMs: Int,
    val breatheMin: Float,
    val breatheMax: Float,
    val colorShiftSpeed: Float,
    val coreBrightness: Float,   // 0-1
    val auraAlpha: Float,
)

private val idleParams = OrbParams(
    breathePeriodMs = 3200,
    breatheMin = 0.88f,
    breatheMax = 1.02f,
    colorShiftSpeed = 0.15f,
    coreBrightness = 0.6f,
    auraAlpha = 0.25f,
)

private val listeningParams = OrbParams(
    breathePeriodMs = 1400,
    breatheMin = 0.92f,
    breatheMax = 1.06f,
    colorShiftSpeed = 0.4f,
    coreBrightness = 0.9f,
    auraAlpha = 0.45f,
)

private val processingParams = OrbParams(
    breathePeriodMs = 800,
    breatheMin = 0.94f,
    breatheMax = 1.03f,
    colorShiftSpeed = 0.6f,
    coreBrightness = 0.75f,
    auraAlpha = 0.35f,
)

@Composable
fun SiriOrb(
    modifier: Modifier = Modifier,
    state: OrbState = OrbState.IDLE,
    sizeDp: Float = 200f,
) {
    val params = when (state) {
        OrbState.IDLE -> idleParams
        OrbState.LISTENING -> listeningParams
        OrbState.PROCESSING -> processingParams
    }

    val transition = rememberInfiniteTransition(label = "orb")

    // 呼吸动画
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(params.breathePeriodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breathe",
    )

    // 色彩偏移 — 两个独立的相位偏移用于 RGB 通道混合
    val colorPhase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((params.breathePeriodMs * 2.5).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "color1",
    )
    val colorPhase2 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween((params.breathePeriodMs * 3.2).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "color2",
    )

    // 梯度中心轨道偏移
    val orbitX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((params.breathePeriodMs * 4).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbitX",
    )
    val orbitY by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween((params.breathePeriodMs * 4.7).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbitY",
    )

    // 核心脉动 — 独立于呼吸，用于"心跳"感
    val corePulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((params.breathePeriodMs * 0.6).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "corePulse",
    )

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val cx = size.width / 2
            val cy = size.height / 2
            val maxR = size.width / 2

            // 呼吸缩放
            val breatheRad = Math.toRadians(breathe.toDouble())
            val scale = params.breatheMin +
                    (params.breatheMax - params.breatheMin) *
                    (sin(breatheRad).toFloat() * 0.5f + 0.5f)

            val orbRadius = maxR * scale * 0.85f

            // 色彩插值 — 从调色板采样两个主色
            val c1 = samplePalette(OrbPalette, colorPhase1 * params.colorShiftSpeed)
            val c2 = samplePalette(OrbPalette, colorPhase1 * params.colorShiftSpeed + 0.3f)
            val c3 = samplePalette(OrbPalette, colorPhase2 * params.colorShiftSpeed)

            // 梯度中心轨道 (小幅度偏移，产生液体流动感)
            val orbitAngleX = Math.toRadians((orbitX * 360f).toDouble())
            val orbitAngleY = Math.toRadians((orbitY * 360f).toDouble())
            val gradOffsetX = (cos(orbitAngleX) * orbRadius * 0.15f).toFloat()
            val gradOffsetY = (sin(orbitAngleY) * orbRadius * 0.12f).toFloat()

            // ============ 层次 1: 环境光晕 ============
            val ambientRadius = orbRadius * 3.5f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        c1.copy(alpha = 0.06f * params.auraAlpha * 3f),
                        c1.copy(alpha = 0.02f * params.auraAlpha),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = ambientRadius,
                ),
                radius = ambientRadius,
                center = Offset(cx, cy),
            )

            // ============ 层次 2: 外光晕 ============
            val auraRadius = orbRadius * 1.6f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        c1.copy(alpha = 0.12f * params.auraAlpha),
                        c2.copy(alpha = 0.05f * params.auraAlpha),
                        Color.Transparent,
                    ),
                    center = Offset(cx + gradOffsetX * 0.5f, cy + gradOffsetY * 0.5f),
                    radius = auraRadius,
                ),
                radius = auraRadius,
                center = Offset(cx, cy),
            )

            // ============ 层次 3: 主体光晕层 ============
            val glowRadius = orbRadius * 1.2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        c1.copy(alpha = 0.2f),
                        c2.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = Offset(cx + gradOffsetX * 0.3f, cy + gradOffsetY * 0.3f),
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = Offset(cx, cy),
            )

            // ============ 层次 4: 主体球 ============
            val coreBright = params.coreBrightness * (0.85f + 0.15f * corePulse)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        lerp(Color.White, c1, 1f - coreBright * 0.5f).copy(alpha = 0.95f),
                        lerp(c1, c2, 0.5f).copy(alpha = 0.9f),
                        c3.copy(alpha = 0.85f),
                        c2.copy(alpha = 0.7f),
                        c1.copy(alpha = 0.3f),
                    ),
                    center = Offset(
                        cx + gradOffsetX + orbRadius * 0.08f,
                        cy + gradOffsetY + orbRadius * 0.08f,
                    ),
                    radius = orbRadius,
                ),
                radius = orbRadius,
                center = Offset(cx, cy),
            )

            // ============ 层次 5: 核心高光 ============
            val highlightRadius = orbRadius * 0.45f
            val highlightCx = cx - orbRadius * 0.18f + gradOffsetX * 0.3f
            val highlightCy = cy - orbRadius * 0.22f + gradOffsetY * 0.3f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CoreHighlight.copy(alpha = 0.7f * coreBright),
                        CoreHighlight.copy(alpha = 0.15f * coreBright),
                        Color.Transparent,
                    ),
                    center = Offset(highlightCx, highlightCy),
                    radius = highlightRadius,
                ),
                radius = highlightRadius,
                center = Offset(highlightCx, highlightCy),
            )

            // ============ 层次 6: 镜面高光 ============
            val specRadius = orbRadius * 0.12f
            val specCx = cx - orbRadius * 0.25f
            val specCy = cy - orbRadius * 0.3f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Specular.copy(alpha = 0.6f * coreBright),
                        Color.Transparent,
                    ),
                    center = Offset(specCx - 1f, specCy - 1f),
                    radius = specRadius,
                ),
                radius = specRadius,
                center = Offset(specCx, specCy),
            )

            // ============ 层次 7: 次高光 (底部反射) ============
            val rimRadius = orbRadius * 0.25f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        c2.copy(alpha = 0.15f * coreBright),
                        Color.Transparent,
                    ),
                    center = Offset(cx - orbRadius * 0.05f, cy + orbRadius * 0.5f),
                    radius = rimRadius,
                ),
                radius = rimRadius * 1.5f,
                center = Offset(cx + orbRadius * 0.3f, cy + orbRadius * 0.55f),
            )
        }
    }
}

/**
 * 从调色板循环采样颜色，phase ∈ [0, n) 循环索引。
 */
private fun samplePalette(palette: List<Color>, phase: Float): Color {
    val n = palette.size
    val p = phase % n
    val idx = p.toInt().coerceIn(0, n - 2)
    val frac = (p - idx).coerceIn(0f, 1f)
    return lerp(palette[idx], palette[(idx + 1).coerceAtMost(n - 1)], frac)
}

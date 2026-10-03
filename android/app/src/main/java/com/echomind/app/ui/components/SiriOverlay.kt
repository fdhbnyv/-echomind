package com.echomind.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.echomind.app.ui.theme.BorderLight
import com.echomind.app.ui.theme.DarkBg
import com.echomind.app.ui.theme.DarkBorder
import com.echomind.app.ui.theme.TextDim
import com.echomind.app.ui.theme.TextMuted
import com.echomind.app.ui.theme.TextOnGlass
import com.echomind.app.ui.theme.TextPrimary

/**
 * Siri 全屏覆盖层 — EchoMind 语音交互主界面。
 *
 * 组合 SiriOrb + WaveformBar + 毛玻璃背景 + 状态文字 + 操作栏。
 * 适配亮色/暗色主题下的液态玻璃效果。
 *
 * @param isDark 是否暗色模式
 * @param onDismiss 关闭回调
 */
@Composable
fun SiriOverlay(
    isDark: Boolean = false,
    onDismiss: () -> Unit = {},
) {
    // 状态机: idle → recording → processing → result
    var phase by remember { mutableStateOf(SiriPhase.IDLE) }
    var transcription by remember { mutableStateOf("") }
    var amplitude by remember { mutableStateOf(0.2f) }

    val context = LocalContext.current
    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) phase = SiriPhase.RECORDING
    }

    // 背景色
    val bgColor = if (isDark) DarkBg.copy(alpha = 0.55f) else Color(0xFFF2F5FB).copy(alpha = 0.55f)
    val glassBorderColor = if (isDark) DarkBorder else BorderLight
    val textColor = if (isDark) Color(0xFFF1F5F9) else TextPrimary
    val dimTextColor = if (isDark) TextDim else TextMuted

    // === 毛玻璃背景 (API 31+ 用 blur，低版本降级) ===
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .let { modifier ->
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    modifier.blur(20.dp)
                } else modifier
            },
        contentAlignment = Alignment.Center,
    ) {
        // 可点击背景区域关闭
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (phase == SiriPhase.IDLE) onDismiss()
                },
        )

        // Apple Intelligence 屏幕边缘光晕流光
        AppleIntelligenceBorderGlow(
            amplitude = amplitude,
            isListening = phase == SiriPhase.RECORDING,
        )

        // 主内容 — 从底部滑入
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(500),
            ) + fadeIn(animationSpec = tween(400)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300),
            ) + fadeOut(animationSpec = tween(300)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // === Siri Orb ===
                SiriOrb(
                    state = when (phase) {
                        SiriPhase.IDLE -> OrbState.IDLE
                        SiriPhase.RECORDING -> OrbState.LISTENING
                        SiriPhase.PROCESSING -> OrbState.PROCESSING
                    },
                    sizeDp = 180f,
                    amplitude = amplitude,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // === 状态文字 ===
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    label = "phaseText",
                ) { currentPhase ->
                    Text(
                        text = when (currentPhase) {
                            SiriPhase.IDLE -> "轻触开始录音"
                            SiriPhase.RECORDING -> "正在聆听…"
                            SiriPhase.PROCESSING -> "AI 整理中…"
                        },
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W400,
                        letterSpacing = 0.3.sp,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // === 波形 (录音时显示) ===
                AnimatedVisibility(
                    visible = phase == SiriPhase.RECORDING,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(200)),
                ) {
                    WaveformBar(
                        amplitude = amplitude,
                        barCount = 9,
                        colorStart = Color(0xFF2563EB),
                        colorEnd = Color(0xFFA855F7),
                        barWidth = 4.dp,
                        maxHeight = 40.dp,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // === 转写文字 ===
                if (transcription.isNotEmpty()) {
                    Text(
                        text = transcription,
                        color = dimTextColor,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(horizontal = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // === 底部操作栏 ===
                BottomBar(
                    phase = phase,
                    isDark = isDark,
                    onStart = {
                        if (hasPermission) {
                            phase = SiriPhase.RECORDING
                            // 模拟振幅变化 (后续替换为真实麦克风数据)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStop = {
                        phase = SiriPhase.PROCESSING
                        // 模拟处理
                        transcription = "今天下午三点跟老王开会，讨论Q3产品路线图，顺便确认周五的发布计划。"
                    },
                    onDismiss = onDismiss,
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Siri 交互阶段
 */
enum class SiriPhase { IDLE, RECORDING, PROCESSING }

/**
 * 底部操作栏 — 含主操作按钮 + 关闭
 */
@Composable
private fun BottomBar(
    phase: SiriPhase,
    isDark: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    val primaryColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val btnShape = RoundedCornerShape(28.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 关闭/取消按钮
        Button(
            onClick = onDismiss,
            shape = btnShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark)
                    Color.White.copy(alpha = 0.08f)
                else
                    Color.White.copy(alpha = 0.5f),
                contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF475569),
            ),
            modifier = Modifier.width(100.dp).height(48.dp),
        ) {
            Text(
                text = when (phase) {
                    SiriPhase.IDLE -> "关闭"
                    SiriPhase.RECORDING -> "取消"
                    SiriPhase.PROCESSING -> "取消"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.W500,
            )
        }

        // 主操作按钮 (录音/停止)
        val mainButtonAction: () -> Unit = when (phase) {
            SiriPhase.IDLE -> onStart
            SiriPhase.RECORDING -> onStop
            SiriPhase.PROCESSING -> fun() {}
        }
        Button(
            onClick = mainButtonAction,
            enabled = phase != SiriPhase.PROCESSING,
            shape = btnShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = when (phase) {
                    SiriPhase.IDLE -> primaryColor.copy(alpha = 0.85f)
                    SiriPhase.RECORDING -> Color(0xFFEF4444).copy(alpha = 0.85f)
                    SiriPhase.PROCESSING -> primaryColor.copy(alpha = 0.5f)
                },
                contentColor = Color.White,
                disabledContainerColor = primaryColor.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.6f),
            ),
            modifier = Modifier.width(120.dp).height(52.dp),
        ) {
            Text(
                text = when (phase) {
                    SiriPhase.IDLE -> "开始录音"
                    SiriPhase.RECORDING -> "完成"
                    SiriPhase.PROCESSING -> "处理中…"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
            )
        }
    }
}

/**
 * Apple Intelligence 边缘流光边框
 */
@Composable
private fun AppleIntelligenceBorderGlow(
    modifier: Modifier = Modifier,
    amplitude: Float = 0f,
    isListening: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "edge_glow")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 3500 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "edge_phase",
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isListening) (0.55f + amplitude * 0.45f).coerceAtMost(0.9f) else 0.3f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "glow_alpha",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val edgeDepth = (22.dp.toPx() * (1f + amplitude * 0.4f))

        // 顶边柔光
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF7C3AED).copy(alpha = glowAlpha * 0.7f), Color.Transparent),
                startY = 0f,
                endY = edgeDepth * 2f,
            ),
            size = Size(w, edgeDepth * 2f)
        )
        // 底边柔光
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFF2563EB).copy(alpha = glowAlpha * 0.7f)),
                startY = h - edgeDepth * 2f,
                endY = h,
            ),
            topLeft = Offset(0f, h - edgeDepth * 2f),
            size = Size(w, edgeDepth * 2f)
        )
        // 左边柔光
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFEC4899).copy(alpha = glowAlpha * 0.6f), Color.Transparent),
                startX = 0f,
                endX = edgeDepth * 1.5f,
            ),
            size = Size(edgeDepth * 1.5f, h)
        )
        // 右边柔光
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0xFF06B6D4).copy(alpha = glowAlpha * 0.6f)),
                startX = w - edgeDepth * 1.5f,
                endX = w,
            ),
            topLeft = Offset(w - edgeDepth * 1.5f, 0f),
            size = Size(edgeDepth * 1.5f, h)
        )
    }
}

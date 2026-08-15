package com.echomind.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// 液态玻璃 (Liquid Glass) 色彩系统
// ═══════════════════════════════════════════════════════════════

// 基底
val Background = Color(0xFFF2F5FB)           // 背景：极浅蓝灰
val BackgroundGradientEnd = Color(0xFFE8EEF6) // 渐变终点
val SurfaceGlass = Color(0xE6FFFFFF)          // 玻璃表面：白 90% 透明度
val SurfaceGlassDark = Color(0xCCFFFFFF)      // 玻璃表面：白 80% 透明度

// 边框与阴影
val BorderLight = Color(0x4Dffffff)           // 玻璃边框：白 30%
val BorderStroke = Color(0x1A475569)          // 描边：极淡灰
val ShadowColor = Color(0x1A2563EB)           // 阴影：主色 10%

// 文字
val TextPrimary = Color(0xFF0F172A)
val TextMuted = Color(0xFF64748B)
val TextDim = Color(0xFF94A3B8)
val TextOnGlass = Color(0xFF1E293B)           // 玻璃上的文字

// 主色
val Primary = Color(0xFF2563EB)
val PrimaryHover = Color(0xFF1D4ED8)
val PrimaryLight = Color(0x1A2563EB)          // 主色 10% 透明度
val PrimaryGlass = Color(0x1A2563EB)          // 主色玻璃态

// 功能色
val Success = Color(0xFF10B981)               // 翠绿
val SuccessLight = Color(0x1A10B981)
val RecordingPulse = Color(0xFFEF4444)        // 录音红
val RecordingPulseLight = Color(0x1AEF4444)

// 暗色
val DarkBg = Color(0xFF0F172A)
val DarkSurface = Color(0xE61E293B)
val DarkBorder = Color(0x33FFFFFF)

// 兼容别名（旧组件引用）
val BgAccent = Color(0x1A2563EB)
val BgMuted = Color(0xFFF2F5FB)
val Border = Color(0x1A475569)

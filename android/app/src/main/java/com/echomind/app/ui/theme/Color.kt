package com.echomind.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// 主题响应式色彩系统
//
// 所有颜色均为 @Composable 属性，读取 ThemeManager 当前主题（含系统深浅色），
// 主题切换后会自动触发重组。旧组件中的静态色引用无需改动即可跟随主题。
// ═══════════════════════════════════════════════════════════════

/** 当前主题配色（跟随系统深浅色） */
@Composable
private fun currentThemeColors(): ThemeColors =
    if (isSystemInDarkTheme()) ThemeManager.darkColors() else ThemeManager.lightColors()

// 基底
val Background: Color @Composable get() = currentThemeColors().bg
val BackgroundGradientEnd: Color @Composable get() = currentThemeColors().bgGradientEnd
val SurfaceGlass: Color @Composable get() = currentThemeColors().surface
val SurfaceGlassDark: Color @Composable get() = currentThemeColors().surfaceVariant

// 边框与阴影
val BorderLight: Color @Composable get() = currentThemeColors().borderLight
val BorderStroke: Color @Composable get() = currentThemeColors().border
val ShadowColor: Color @Composable get() = currentThemeColors().primaryLight

// 文字
val TextPrimary: Color @Composable get() = currentThemeColors().textPrimary
val TextMuted: Color @Composable get() = currentThemeColors().textMuted
val TextDim: Color @Composable get() = currentThemeColors().textDim
val TextOnGlass: Color @Composable get() = currentThemeColors().textPrimary

// 主色
val Primary: Color @Composable get() = currentThemeColors().primary
val PrimaryHover: Color @Composable get() = currentThemeColors().primary
val PrimaryLight: Color @Composable get() = currentThemeColors().primaryLight
val PrimaryGlass: Color @Composable get() = currentThemeColors().primaryLight

// 功能色
val Success: Color @Composable get() = currentThemeColors().success
val SuccessLight: Color @Composable get() = currentThemeColors().success.copy(alpha = 0.1f)
val RecordingPulse: Color @Composable get() = currentThemeColors().error
val RecordingPulseLight: Color @Composable get() = currentThemeColors().error.copy(alpha = 0.1f)

// 暗色兼容别名（旧组件引用，均跟随主题）
val DarkBg: Color @Composable get() = currentThemeColors().bg
val DarkSurface: Color @Composable get() = currentThemeColors().surface
val DarkBorder: Color @Composable get() = currentThemeColors().border

// 兼容别名（旧组件引用，均跟随主题）
val BgAccent: Color @Composable get() = currentThemeColors().primaryLight
val BgMuted: Color @Composable get() = currentThemeColors().surfaceVariant
val Border: Color @Composable get() = currentThemeColors().border

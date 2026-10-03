package com.echomind.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.echomind.app.R

val CaveatFontFamily = FontFamily(
    Font(R.font.caveat_regular, FontWeight.Normal),
    Font(R.font.caveat_bold, FontWeight.Bold),
)


private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W600,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W500,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

/** 衬线标题字体（纸语等温和场景） */
private val SerifTitleTypography = AppTypography.run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = FontFamily.Serif),
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.Serif),
        titleLarge = titleLarge.copy(fontFamily = FontFamily.Serif),
        titleMedium = titleMedium.copy(fontFamily = FontFamily.Serif),
        titleSmall = titleSmall.copy(fontFamily = FontFamily.Serif),
    )
}

/** 超粗标题字重（墨线等张扬场景） */
private val BoldTitleTypography = AppTypography.run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Black),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Black),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Black),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Black),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
fun EchoMindTheme(
    darkTheme: Boolean = ThemeManager.isDarkMode ?: isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val theme = ThemeManager.currentTheme
    val effectiveDark = ThemeManager.isDarkMode ?: darkTheme
    val tc = if (effectiveDark) theme.dark else theme.light
    val colorScheme = if (effectiveDark) {
        darkColorScheme(
            primary = tc.primary, onPrimary = Color.White, primaryContainer = tc.primaryLight,
            onPrimaryContainer = tc.primary, secondary = tc.textMuted, onSecondary = tc.bg,
            background = tc.bg, onBackground = tc.textPrimary, surface = tc.surface,
            onSurface = tc.textPrimary, surfaceVariant = tc.surfaceVariant,
            onSurfaceVariant = tc.textMuted, outline = tc.border, outlineVariant = tc.borderLight,
            error = tc.error, onError = Color.White,
        )
    } else {
        lightColorScheme(
            primary = tc.primary, onPrimary = Color.White, primaryContainer = tc.primaryLight,
            onPrimaryContainer = tc.primary, secondary = tc.textMuted, onSecondary = tc.bg,
            background = tc.bg, onBackground = tc.textPrimary, surface = tc.surface,
            onSurface = tc.textPrimary, surfaceVariant = tc.surfaceVariant,
            onSurfaceVariant = tc.textMuted, outline = tc.border, outlineVariant = tc.borderLight,
            error = tc.error, onError = Color.White,
        )
    }
    val typography = when (theme.typography) {
        ThemeTypography.SERIF -> SerifTitleTypography
        ThemeTypography.BOLD -> BoldTitleTypography
        ThemeTypography.SANS -> AppTypography
    }
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}

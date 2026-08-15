package com.echomind.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val GlassLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = Primary,
    secondary = TextMuted,
    onSecondary = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceGlass,
    onSurface = TextOnGlass,
    surfaceVariant = SurfaceGlassDark,
    onSurfaceVariant = TextMuted,
    outline = BorderStroke,
    outlineVariant = BorderStroke,
    error = RecordingPulse,
    onError = Color.White,
    errorContainer = RecordingPulseLight,
    onErrorContainer = RecordingPulse,
)

private val GlassDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = DarkBg,
    primaryContainer = Color(0x3360A5FA),
    onPrimaryContainer = Color(0xFF93C5FD),
    secondary = Color(0xFF94A3B8),
    onSecondary = DarkBg,
    background = DarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xCC1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = Color(0xFFF87171),
    onError = DarkBg,
    errorContainer = Color(0x33F87171),
    onErrorContainer = Color(0xFFF87171),
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

@Composable
fun EchoMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val theme = ThemeManager.currentTheme
    val tc = if (darkTheme) theme.dark else theme.light
    val colorScheme = if (darkTheme) {
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
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}

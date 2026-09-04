package com.echomind.app.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================
// Theme typography style selector
// ==============================

/** 主题对应的标题字体风格 */
enum class ThemeTypography {
    /** 无衬线（默认） */
    SANS,

    /** 衬线标题（纸语等温和场景） */
    SERIF,

    /** 超粗字重（墨线等张扬场景） */
    BOLD,
}

// ==============================
// Theme style selector
// ==============================

enum class AppTheme(
    val id: String,
    val displayName: String,
    val icon: String,
    val description: String,
    val typography: ThemeTypography,
    val light: ThemeColors,
    val dark: ThemeColors,
) {
    SOUND_GLOW(
        id = "shengguang",
        displayName = "声光",
        icon = "🌌",
        description = "深空玻璃 · 电光声核，旗舰暗色质感",
        typography = ThemeTypography.SANS,
        light = lightSoundGlow,
        dark = darkSoundGlow,
    ),
    PAPER_POEM(
        id = "zhiyu",
        displayName = "纸语",
        icon = "📜",
        description = "暖白纸感 · 墨色声纹环，衬线安静复盘",
        typography = ThemeTypography.SERIF,
        light = lightPaperPoem,
        dark = darkPaperPoem,
    ),
    INK_LINE(
        id = "moxian",
        displayName = "墨线",
        icon = "🖋",
        description = "高对比描边 · 漫画张力，红黑撞色",
        typography = ThemeTypography.BOLD,
        light = lightInkLine,
        dark = darkInkLine,
    ),
    MANGA(
        id = "manga",
        displayName = "漫画風",
        icon = "🎭",
        description = "高对比、纯黑轮廓、暴走气氛",
        typography = ThemeTypography.SANS,
        light = lightManga,
        dark = darkManga,
    ),
    LIQUID_GLASS(
        id = "glass",
        displayName = "液态玻璃",
        icon = "💧",
        description = "半透明玻璃质感，温柔光影",
        typography = ThemeTypography.SANS,
        light = lightGlass,
        dark = darkGlass,
    ),
    MINIMAL_WHITE(
        id = "minimal_white",
        displayName = "简约白",
        icon = "⬜",
        description = "白净纯粹，极致简约",
        typography = ThemeTypography.SANS,
        light = lightMinimalWhite,
        dark = darkMinimalWhite,
    ),
    MINIMAL_BLACK(
        id = "minimal_black",
        displayName = "简约黑",
        icon = "⚫",
        description = "深黑底色，高对比字体",
        typography = ThemeTypography.SANS,
        light = lightMinimalBlack,
        dark = darkMinimalBlack,
    ),
    PAPER(
        id = "paper",
        displayName = "纸质风",
        icon = "📄",
        description = "暖白纸张底色，温柔记事本感",
        typography = ThemeTypography.SANS,
        light = lightPaper,
        dark = darkPaper,
    ),
    EMERALD(
        id = "emerald",
        displayName = "翠绿风",
        icon = "🌿",
        description = "翡翠绿主色，清新自然",
        typography = ThemeTypography.SANS,
        light = lightEmerald,
        dark = darkEmerald,
    ),
}

// ==============================
// Theme color values data class
// ==============================

data class ThemeColors(
    val name: String = "",
    val bg: Color,
    val bgGradientEnd: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderLight: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val textDim: Color,
    val primary: Color,
    val primaryLight: Color,
    val success: Color,
    val error: Color,
    val accent: Color,
)

// ==============================
// Color definitions per theme
// ==============================

// ----- 声光 (Sound Glow) · 设计稿方向 A：深空玻璃 + 电光声核 -----
val lightSoundGlow = ThemeColors(
    name = "声光 亮",
    bg = Color(0xFFEFF3FA),
    bgGradientEnd = Color(0xFFE4EBF6),
    surface = Color(0xFFF8FAFE),
    surfaceVariant = Color(0xFFEDF2FA),
    border = Color(0xFFD6E0EF),
    borderLight = Color(0xFFE7EDF7),
    textPrimary = Color(0xFF0F172A),
    textMuted = Color(0xFF5A6B8C),
    textDim = Color(0xFF94A3B8),
    primary = Color(0xFF3B6FE0),
    primaryLight = Color(0x1A3B6FE0),
    success = Color(0xFF2FA36B),
    error = Color(0xFFE05252),
    accent = Color(0xFF7C6FE0),
)
val darkSoundGlow = ThemeColors(
    name = "声光 暗",
    bg = Color(0xFF0B1120),
    bgGradientEnd = Color(0xFF15223E),
    surface = Color(0xFF16213A),
    surfaceVariant = Color(0xFF1C2842),
    border = Color(0xFF2B3B5F),
    borderLight = Color(0xFF22304F),
    textPrimary = Color(0xFFEDF2FC),
    textMuted = Color(0xFF9AA8C0),
    textDim = Color(0xFF66748E),
    primary = Color(0xFF5E8BFF),
    primaryLight = Color(0x335E8BFF),
    success = Color(0xFF3ECF8E),
    error = Color(0xFFFF6B6B),
    accent = Color(0xFF8AB4FF),
)

// ----- 纸语 (Paper Poem) · 设计稿方向 B：暖白纸感 + 墨色声纹环 -----
val lightPaperPoem = ThemeColors(
    name = "纸语 亮",
    bg = Color(0xFFF7F1E4),
    bgGradientEnd = Color(0xFFEFE5D0),
    surface = Color(0xFFFFFDF6),
    surfaceVariant = Color(0xFFFBF4E6),
    border = Color(0xFFE5D8BF),
    borderLight = Color(0xFFEDE3CE),
    textPrimary = Color(0xFF3A3126),
    textMuted = Color(0xFF85775F),
    textDim = Color(0xFFAB9E86),
    primary = Color(0xFFA9763C),
    primaryLight = Color(0x1AA9763C),
    success = Color(0xFF5F8A5B),
    error = Color(0xFFC25450),
    accent = Color(0xFFC9A26B),
)
val darkPaperPoem = ThemeColors(
    name = "纸语 暗",
    bg = Color(0xFF241D13),
    bgGradientEnd = Color(0xFF1B1610),
    surface = Color(0xFF322A1D),
    surfaceVariant = Color(0xFF3C3323),
    border = Color(0xFF5A4E38),
    borderLight = Color(0xFF4A4030),
    textPrimary = Color(0xFFF0E8D8),
    textMuted = Color(0xFFB8AD9E),
    textDim = Color(0xFF8B7D6B),
    primary = Color(0xFFD4B896),
    primaryLight = Color(0x33D4B896),
    success = Color(0xFF8BC34A),
    error = Color(0xFFEF9A9A),
    accent = Color(0xFFD4A76A),
)

// ----- 墨线 (Ink Line) · 设计稿方向 C：高对比描边 + 漫画张力 -----
val lightInkLine = ThemeColors(
    name = "墨线 亮",
    bg = Color(0xFFF7F4EC),
    bgGradientEnd = Color(0xFFEFEAE0),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0EBE1),
    border = Color(0xFF141414),
    borderLight = Color(0xFF9A958A),
    textPrimary = Color(0xFF141414),
    textMuted = Color(0xFF5A564E),
    textDim = Color(0xFF8F8A7E),
    primary = Color(0xFFE83A2E),
    primaryLight = Color(0x16E83A2E),
    success = Color(0xFF2E9E3F),
    error = Color(0xFFE83A2E),
    accent = Color(0xFFD9A400),
)
val darkInkLine = ThemeColors(
    name = "墨线 暗",
    bg = Color(0xFF141414),
    bgGradientEnd = Color(0xFF1C1C1C),
    surface = Color(0xFF202020),
    surfaceVariant = Color(0xFF262626),
    border = Color(0xFF000000),
    borderLight = Color(0xFF3A3A3A),
    textPrimary = Color(0xFFF4F1E8),
    textMuted = Color(0xFFB9B4A6),
    textDim = Color(0xFF8A8578),
    primary = Color(0xFFFF4B3E),
    primaryLight = Color(0x26FF4B3E),
    success = Color(0xFF4CC45E),
    error = Color(0xFFFF4B3E),
    accent = Color(0xFFFFD84D),
)

// ----- Manga (Comic) -----
val lightManga = ThemeColors(
    bg = Color(0xFFFFFEF5),
    bgGradientEnd = Color(0xFFF5F0E8),
    surface = Color(0xFFFFFEF5),
    surfaceVariant = Color(0xFFF0E8D8),
    border = Color(0xFF1A1A1A),
    borderLight = Color(0xFF333333),
    textPrimary = Color(0xFF1A1A1A),
    textMuted = Color(0xFF555555),
    textDim = Color(0xFF888888),
    primary = Color(0xFFCC0000),
    primaryLight = Color(0x1ACC0000),
    success = Color(0xFF008800),
    error = Color(0xFFCC0000),
    accent = Color(0xFFFFD700),
)
val darkManga = ThemeColors(
    bg = Color(0xFF1A1A1A),
    bgGradientEnd = Color(0xFF111111),
    surface = Color(0xFF2A2A2A),
    surfaceVariant = Color(0xFF333333),
    border = Color(0xFF444444),
    borderLight = Color(0xFF555555),
    textPrimary = Color(0xFFFFFEF5),
    textMuted = Color(0xFFCCCCCC),
    textDim = Color(0xFF888888),
    primary = Color(0xFFFF4444),
    primaryLight = Color(0x33FF4444),
    success = Color(0xFF44CC44),
    error = Color(0xFFFF4444),
    accent = Color(0xFFFFD700),
)

// ----- Glass (Liquid Glass) -----
val lightGlass = ThemeColors(
    bg = Color(0xFFF2F5FB),
    bgGradientEnd = Color(0xFFE8EEF6),
    surface = Color(0xE6FFFFFF),
    surfaceVariant = Color(0xCCFFFFFF),
    border = Color(0x1A475569),
    borderLight = Color(0x4DFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textMuted = Color(0xFF64748B),
    textDim = Color(0xFF94A3B8),
    primary = Color(0xFF2563EB),
    primaryLight = Color(0x1A2563EB),
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    accent = Color(0xFF6366F1),
)
val darkGlass = ThemeColors(
    bg = Color(0xFF0F172A),
    bgGradientEnd = Color(0xFF0B1120),
    surface = Color(0xE61E293B),
    surfaceVariant = Color(0xCC1E293B),
    border = Color(0x33FFFFFF),
    borderLight = Color(0x20FFFFFF),
    textPrimary = Color(0xFFF1F5F9),
    textMuted = Color(0xFF94A3B8),
    textDim = Color(0xFF64748B),
    primary = Color(0xFF60A5FA),
    primaryLight = Color(0x3360A5FA),
    success = Color(0xFF34D399),
    error = Color(0xFFF87171),
    accent = Color(0xFFA78BFA),
)

// ----- Minimal White -----
val lightMinimalWhite = ThemeColors(
    bg = Color(0xFFFEFEFE),
    bgGradientEnd = Color(0xFFF8F8F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF5F5F5),
    border = Color(0xFFE0E0E0),
    borderLight = Color(0xFFEEEEEE),
    textPrimary = Color(0xFF1C1C1E),
    textMuted = Color(0xFF6C6C70),
    textDim = Color(0xFFAEAEB2),
    primary = Color(0xFF007AFF),
    primaryLight = Color(0x1A007AFF),
    success = Color(0xFF34C759),
    error = Color(0xFFFF3B30),
    accent = Color(0xFFAF52DE),
)
val darkMinimalWhite = lightMinimalWhite  // same, minimal white is light only

// ----- Minimal Black -----
val lightMinimalBlack = ThemeColors(
    bg = Color(0xFF000000),
    bgGradientEnd = Color(0xFF0A0A0A),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    border = Color(0xFF38383A),
    borderLight = Color(0xFF48484A),
    textPrimary = Color(0xFFFFFFFF),
    textMuted = Color(0xFFAEAEB2),
    textDim = Color(0xFF636366),
    primary = Color(0xFF0A84FF),
    primaryLight = Color(0x330A84FF),
    success = Color(0xFF30D158),
    error = Color(0xFFFF453A),
    accent = Color(0xFFBF5AF2),
)
val darkMinimalBlack = lightMinimalBlack  // same, minimal black is dark only

// ----- Paper -----
val lightPaper = ThemeColors(
    bg = Color(0xFFFCF8F0),
    bgGradientEnd = Color(0xFFF5F0E6),
    surface = Color(0xFFFFFCF5),
    surfaceVariant = Color(0xFFF8F3EA),
    border = Color(0xFFD4C9B8),
    borderLight = Color(0xFFE8DFD0),
    textPrimary = Color(0xFF3B3226),
    textMuted = Color(0xFF8B7D6B),
    textDim = Color(0xFFB8AD9E),
    primary = Color(0xFF8B6F47),
    primaryLight = Color(0x1A8B6F47),
    success = Color(0xFF5B8C5A),
    error = Color(0xFFC44E4E),
    accent = Color(0xFFC49B6C),
)
val darkPaper = ThemeColors(
    bg = Color(0xFF2C2418),
    bgGradientEnd = Color(0xFF221B12),
    surface = Color(0xFF3B3226),
    surfaceVariant = Color(0xFF4A4032),
    border = Color(0xFF5C5040),
    borderLight = Color(0xFF6B5F4E),
    textPrimary = Color(0xFFF0E8D8),
    textMuted = Color(0xFFB8AD9E),
    textDim = Color(0xFF8B7D6B),
    primary = Color(0xFFD4B896),
    primaryLight = Color(0x33D4B896),
    success = Color(0xFF8BC34A),
    error = Color(0xFFEF9A9A),
    accent = Color(0xFFD4A76A),
)

// ----- Emerald -----
val lightEmerald = ThemeColors(
    bg = Color(0xFFF0FDF4),
    bgGradientEnd = Color(0xFFE6F7EC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0FDF4),
    border = Color(0xFFA7F3D0),
    borderLight = Color(0xFFD1FAE5),
    textPrimary = Color(0xFF064E3B),
    textMuted = Color(0xFF6B7280),
    textDim = Color(0xFF9CA3AF),
    primary = Color(0xFF059669),
    primaryLight = Color(0x1A059669),
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    accent = Color(0xFF34D399),
)
val darkEmerald = ThemeColors(
    bg = Color(0xFF022C22),
    bgGradientEnd = Color(0xFF011A14),
    surface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFF065F46),
    border = Color(0xFF047857),
    borderLight = Color(0xFF059669),
    textPrimary = Color(0xFFECFDF5),
    textMuted = Color(0xFFA7F3D0),
    textDim = Color(0xFF6EE7B7),
    primary = Color(0xFF34D399),
    primaryLight = Color(0x3334D399),
    success = Color(0xFF10B981),
    error = Color(0xFFF87171),
    accent = Color(0xFF6EE7B7),
)

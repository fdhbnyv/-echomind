import os

path = "D:\\EchoMind\\android\\app\\src\\main\\java\\com\\echomind\\app\\ui\\theme\\ThemeDefs.kt"
with open(path, "r", encoding="utf-8") as f:
    t = f.read()

# Use actual emoji chars (Python handles them natively in utf-8)
MANGA_ICON = "\U0001F3AD"

manga_block = f'''
    /** \u6f2b\u753b\u98a8 (Manga/Comic) */
    MANGA(
        id = "manga",
        displayName = "\u6f2b\u753b\u98a8",
        icon = "{MANGA_ICON}",
        description = "\u9ad8\u5bf9\u6bd4\u3001\u7eaf\u9ed1\u8f6e\u5ed3\u3001\u66b4\u8d70\u6c14\u6c1b",
        light = lightManga,
        dark = darkManga,
    ),

    /** \u6db2\u6001\u73bb\u7483 (Liquid Glass) - \u9ed8\u8ba4 */
    LIQUID_GLASS(
'''

t = t.replace("    LIQUID_GLASS(", manga_block)

color_block = """
// ===== \u6f2b\u753b\u98a8 (Manga/Comic) =====
val lightManga = ThemeColors(
    name = "\u6f2b\u753b\u98a8",
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
    name = "\u6f2b\u753b\u98a8 \u6697\u8272",
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

"""

t = t.replace("val lightGlass", color_block + "val lightGlass")

with open(path, "w", encoding="utf-8") as f:
    f.write(t)

# Verify
with open(path, "r", encoding="utf-8") as f:
    t2 = f.read()
print(f"Size: {len(t2)} bytes")
for e in ["MANGA", "lightManga", "darkManga"]:
    print(f"  {e}: {'OK' if e in t2 else 'MISSING'}")

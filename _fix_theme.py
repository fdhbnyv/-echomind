import os

path = "D:\\EchoMind\\android\\app\\src\\main\\java\\com\\echomind\\app\\ui\\theme\\ThemeDefs.kt"
with open(path, "r", encoding="utf-8") as f:
    t = f.read()

# Check what's missing
entries_found = []
for e in ["MANGA", "LIQUID_GLASS", "MINIMAL_WHITE", "MINIMAL_BLACK", "PAPER", "EMERALD"]:
    entries_found.append((e, e in t))
    
for e, found in entries_found:
    print(f"{e}: {'OK' if found else 'MISSING'}")

# Add MANGA enum entry before LIQUID_GLASS
manga_enum = '''
    /** \u6f2b\u753b\u98a8 (Comic/Manga) */
    MANGA(
        id = "manga",
        displayName = "\u6f2b\u753b\u98a8",
        icon = "\uD83C\uDFAD",
        description = "\u9ad8\u5bf9\u6bd4\u5904\u7406\uff0c\u7eaf\u9ed1\u8f6e\u5ed3\uff0c\u66b4\u8d70\u6c14\u6c1b",
        light = lightManga,
        dark = darkManga,
    ),

    /** \u6db2\u6001\u73bb\u7483 (Liquid Glass) - \u9ed8\u8ba4 */
    LIQUID_GLASS('''

if "LIQUID_GLASS(" in t and "MANGA(" not in t:
    t = t.replace("    LIQUID_GLASS(", manga_enum)
    print("Added MANGA enum entry")
    
    # Now add color definitions
    # Find where lightEmerald is and add manga colors after
    color_defs = '''

// ══════════════ \u6f2b\u753b\u98a8 (Manga/Comic) ══════════════
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
'''
    # Insert before lightGlass
    if "val lightGlass" in t:
        t = t.replace("val lightGlass", color_defs + "val lightGlass")
        print("Added manga color definitions")
    else:
        # Try other insertion point
        print("Could not find insertion point")

with open(path, "w", encoding="utf-8") as f:
    f.write(t)

# Verify
with open(path, "r", encoding="utf-8") as f:
    t2 = f.read()
print(f"Size: {len(t2)} bytes")
for e in ["MANGA", "lightManga", "darkManga"]:
    print(f"  {e}: {'OK' if e in t2 else 'MISSING'}")

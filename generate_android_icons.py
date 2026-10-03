import math
import os
from PIL import Image, ImageDraw

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
RES_DIR = os.path.join(BASE_DIR, "android", "app", "src", "main", "res")

# 1. 绘制高精度单图标 (无外层文字，纯 Icon Mark，适合 App Icon)
def render_app_icon(size=1024, is_round=False):
    scale = 4
    ss_size = size * scale
    img = Image.new("RGBA", (ss_size, ss_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    s = ss_size / 1024.0

    # 背景
    if is_round:
        draw.ellipse([0, 0, ss_size, ss_size], fill=(255, 255, 255, 255))
    else:
        # 极简浅微圆角白底（或留透明由系统裁剪）
        draw.rectangle([0, 0, ss_size, ss_size], fill=(255, 255, 255, 255))

    c_teal = (20, 184, 166, 255)       # 雾蓝绿 #14B8A6
    c_teal_dark = (13, 148, 136, 255)  # 沉稳深雾蓝绿 #0D9488
    c_slate = (30, 41, 59, 255)        # 深蓝灰 #1E293B
    c_slate_sub = (148, 163, 184, 255) # 浅蓝灰 #94A3B8

    # 笔记本页面轮廓（居中）
    cx, cy = 512 * s, 512 * s
    p_w, p_h = 560 * s, 680 * s
    p_x1, p_y1 = cx - p_w / 2, cy - p_h / 2
    p_x2, p_y2 = cx + p_w / 2, cy + p_h / 2
    p_r = 72 * s

    draw.rounded_rectangle([p_x1, p_y1, p_x2, p_y2], radius=p_r, outline=c_slate, width=int(18 * s))

    # 回声与星芒中心
    mark_cy = cy - 40 * s

    # 弧线 1（外层回声曲线）
    r1 = 175 * s
    draw.arc([cx - r1, mark_cy - r1, cx + r1, mark_cy + r1], start=125, end=235, fill=c_teal, width=int(18 * s))
    draw.arc([cx - r1, mark_cy - r1, cx + r1, mark_cy + r1], start=-55, end=55, fill=c_teal, width=int(18 * s))

    # 弧线 2（内层回声曲线）
    r2 = 112 * s
    draw.arc([cx - r2, mark_cy - r2, cx + r2, mark_cy + r2], start=120, end=240, fill=c_teal_dark, width=int(18 * s))
    draw.arc([cx - r2, mark_cy - r2, cx + r2, mark_cy + r2], start=-60, end=60, fill=c_teal_dark, width=int(18 * s))

    # 核心：大号突出灵感星芒
    star_r = 54 * s
    inner_factor = 0.38
    pts = [
        (cx, mark_cy - star_r),
        (cx + star_r * inner_factor, mark_cy - star_r * inner_factor),
        (cx + star_r, mark_cy),
        (cx + star_r * inner_factor, mark_cy + star_r * inner_factor),
        (cx, mark_cy + star_r),
        (cx - star_r * inner_factor, mark_cy + star_r * inner_factor),
        (cx - star_r, mark_cy),
        (cx - star_r * inner_factor, mark_cy - star_r * inner_factor)
    ]
    draw.polygon(pts, fill=c_slate)

    # 笔记本底部双横线
    line1_y = cy + 185 * s
    line2_y = cy + 235 * s
    draw.rounded_rectangle([cx - 180 * s, line1_y - 8*s, cx + 180 * s, line1_y + 8*s], radius=8*s, fill=c_teal)
    draw.rounded_rectangle([cx - 180 * s, line2_y - 8*s, cx + 50 * s, line2_y + 8*s], radius=8*s, fill=c_slate_sub)

    final = img.resize((size, size), Image.Resampling.LANCZOS)
    return final

def generate_all_mipmaps():
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }

    for folder, size in densities.items():
        folder_path = os.path.join(RES_DIR, folder)
        os.makedirs(folder_path, exist_ok=True)

        # 1. ic_launcher.png (方角/自适应备用)
        square_icon = render_app_icon(size, is_round=False)
        square_icon.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")

        # 2. ic_launcher_round.png (圆形图标)
        round_icon = render_app_icon(size, is_round=True)
        round_icon.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")

        print(f"Generated {folder}: {size}x{size}")

    # 生成 512x512 高清应用商店/设计预览图
    ui_design_dir = os.path.join(BASE_DIR, "ui-design")
    hd_icon = render_app_icon(512, is_round=False)
    hd_icon.save(os.path.join(ui_design_dir, "app_icon_512.png"), "PNG")
    print("Saved ui-design/app_icon_512.png")

def update_vector_drawable():
    # 1. 更新 ic_launcher_background.xml
    bg_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M0,0h108v108h-108z" />
</vector>
"""
    with open(os.path.join(RES_DIR, "drawable", "ic_launcher_background.xml"), "w", encoding="utf-8") as f:
        f.write(bg_xml)

    # 2. 更新 ic_launcher_foreground.xml (自适应前景色，居中在 108x108 视口内部的 72x72 安全区)
    fg_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    
    <!-- Group centered in 108x108 viewport -->
    <group
        android:pivotX="54"
        android:pivotY="54">
        
        <!-- Notebook Page Outline (#1E293B) -->
        <path
            android:fillColor="#00000000"
            android:strokeColor="#1E293B"
            android:strokeWidth="2.2"
            android:strokeLineCap="round"
            android:pathData="M37,24 h34 c4.4,0 8,3.6 8,8 v44 c0,4.4 -3.6,8 -8,8 h-34 c-4.4,0 -8,-3.6 -8,-8 v-44 c0,-4.4 3.6,-8 8,-8 z" />
            
        <!-- Outer Echo Arcs (#14B8A6) -->
        <path
            android:fillColor="#00000000"
            android:strokeColor="#14B8A6"
            android:strokeWidth="2.2"
            android:strokeLineCap="round"
            android:pathData="M43,40 C39.5,45.5 39.5,54.5 43,60" />
        <path
            android:fillColor="#00000000"
            android:strokeColor="#14B8A6"
            android:strokeWidth="2.2"
            android:strokeLineCap="round"
            android:pathData="M65,40 C68.5,45.5 68.5,54.5 65,60" />

        <!-- Inner Echo Arcs (#0D9488) -->
        <path
            android:fillColor="#00000000"
            android:strokeColor="#0D9488"
            android:strokeWidth="2.0"
            android:strokeLineCap="round"
            android:pathData="M47.5,44.5 C45.5,47.8 45.5,52.2 47.5,55.5" />
        <path
            android:fillColor="#00000000"
            android:strokeColor="#0D9488"
            android:strokeWidth="2.0"
            android:strokeLineCap="round"
            android:pathData="M60.5,44.5 C62.5,47.8 62.5,52.2 60.5,55.5" />

        <!-- Prominent Central Thought Star (#1E293B) -->
        <path
            android:fillColor="#1E293B"
            android:pathData="M54,44 L56.2,48.8 L61,50 L56.2,51.2 L54,56 L51.8,51.2 L47,50 L51.8,48.8 Z" />

        <!-- Record Lines -->
        <path
            android:fillColor="#00000000"
            android:strokeColor="#14B8A6"
            android:strokeWidth="1.8"
            android:strokeLineCap="round"
            android:pathData="M37,68 L71,68" />
        <path
            android:fillColor="#00000000"
            android:strokeColor="#94A3B8"
            android:strokeWidth="1.8"
            android:strokeLineCap="round"
            android:pathData="M37,73 L55,73" />
    </group>
</vector>
"""
    with open(os.path.join(RES_DIR, "drawable", "ic_launcher_foreground.xml"), "w", encoding="utf-8") as f:
        f.write(fg_xml)
    print("Updated vector drawables.")

if __name__ == "__main__":
    generate_all_mipmaps()
    update_vector_drawable()

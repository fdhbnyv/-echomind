import math
import os
from PIL import Image, ImageDraw, ImageFont

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
FONT_CAL_SANS = os.path.join(BASE_DIR, "android", "app", "src", "main", "assets", "fonts", "Cal Sans.ttf")
FONT_YAHEI = "C:/Windows/Fonts/msyh.ttc"

def get_font(path, size):
    if os.path.exists(path):
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            pass
    return ImageFont.load_default()

def create_ss_canvas(size=1024, scale=4, bg_color=(255, 255, 255, 255)):
    ss_size = size * scale
    img = Image.new("RGBA", (ss_size, ss_size), bg_color)
    draw = ImageDraw.Draw(img)
    return img, draw, scale

def save_res(img, path, target_size=1024):
    final = img.resize((target_size, target_size), Image.Resampling.LANCZOS)
    full_path = os.path.join(BASE_DIR, path) if not os.path.isabs(path) else path
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    final.save(full_path, "PNG")
    print(f"Saved: {full_path}")

# =========================================================================
# 方案 B 强化版：放大更突出的灵感星芒
# =========================================================================
def render_custom_logo_variant_b(path="ui-design/echomind_logo_spec_b.png", star_scale=1.75):
    img, draw, s = create_ss_canvas(1024, 4, (255, 255, 255, 255))
    
    c_teal = (20, 184, 166, 255)       # 雾蓝绿 #14B8A6
    c_teal_dark = (13, 148, 136, 255)  # 沉稳深雾蓝绿 #0D9488
    c_slate = (30, 41, 59, 255)        # 深蓝灰 #1E293B
    c_slate_sub = (148, 163, 184, 255) # 浅蓝灰 #94A3B8
    
    # 1. 笔记本形态 (圆角微矩形，深蓝灰线条)
    p_x1, p_y1 = 280 * s, 150 * s
    p_x2, p_y2 = 744 * s, 760 * s
    p_r = 52 * s
    
    # 笔记本外框（粗细适中、非常克制）
    draw.rounded_rectangle([p_x1, p_y1, p_x2, p_y2], radius=p_r, outline=c_slate, width=int(14 * s))
    
    # 2. 中心聚焦位置
    cx, cy = 512 * s, 435 * s
    
    # 弧线 1（外层回声曲线）
    r1 = 150 * s
    draw.arc([cx - r1, cy - r1, cx + r1, cy + r1], start=125, end=235, fill=c_teal, width=int(15 * s))
    draw.arc([cx - r1, cy - r1, cx + r1, cy + r1], start=-55, end=55, fill=c_teal, width=int(15 * s))
    
    # 弧线 2（内层回声曲线）
    r2 = 98 * s
    draw.arc([cx - r2, cy - r2, cx + r2, cy + r2], start=120, end=240, fill=c_teal_dark, width=int(15 * s))
    draw.arc([cx - r2, cy - r2, cx + r2, cy + r2], start=-60, end=60, fill=c_teal_dark, width=int(15 * s))
    
    # 3. 核心：大号清晰突出的灵感星芒 (4角星芒，具有视觉中心张力)
    star_r = 46 * s
    inner_factor = 0.38
    pts = [
        (cx, cy - star_r),
        (cx + star_r * inner_factor, cy - star_r * inner_factor),
        (cx + star_r, cy),
        (cx + star_r * inner_factor, cy + star_r * inner_factor),
        (cx, cy + star_r),
        (cx - star_r * inner_factor, cy + star_r * inner_factor),
        (cx - star_r, cy),
        (cx - star_r * inner_factor, cy - star_r * inner_factor)
    ]
    draw.polygon(pts, fill=c_slate)
    
    # 4. 笔记本下部的文字记录横行（双行沉淀）
    draw.rounded_rectangle([356 * s, 625 * s, 668 * s, 639 * s], radius=7 * s, fill=c_teal)
    draw.rounded_rectangle([356 * s, 672 * s, 536 * s, 686 * s], radius=7 * s, fill=c_slate_sub)

    # 5. 品牌字标 "EchoMind"
    font_logo = get_font(FONT_CAL_SANS, int(58 * s))
    draw.text((370 * s, 815 * s), "EchoMind", font=font_logo, fill=c_slate)

    save_res(img, path)

# 导出更新后的 SVG 矢量图
def export_spec_b_svg(path="ui-design/echomind_logo_spec_b.svg"):
    full_path = os.path.join(BASE_DIR, path) if not os.path.isabs(path) else path
    svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <!-- Background -->
  <rect width="512" height="512" fill="#FFFFFF"/>
  
  <!-- Notebook Page Outline -->
  <rect x="140" y="75" width="232" height="305" rx="26" fill="none" stroke="#1E293B" stroke-width="7" stroke-linecap="round"/>
  
  <!-- Outer Echo Curves (Misty Teal #14B8A6) -->
  <path d="M 181 155 A 75 75 0 0 0 181 280" fill="none" stroke="#14B8A6" stroke-width="7.5" stroke-linecap="round"/>
  <path d="M 331 155 A 75 75 0 0 1 331 280" fill="none" stroke="#14B8A6" stroke-width="7.5" stroke-linecap="round"/>

  <!-- Inner Echo Curves (Deep Misty Teal #0D9488) -->
  <path d="M 207 175 A 49 49 0 0 0 207 260" fill="none" stroke="#0D9488" stroke-width="7.5" stroke-linecap="round"/>
  <path d="M 305 175 A 49 49 0 0 1 305 260" fill="none" stroke="#0D9488" stroke-width="7.5" stroke-linecap="round"/>

  <!-- Prominent Central Thought Star (#1E293B) -->
  <path d="M 256 194 L 264.8 212.8 L 279 217.5 L 264.8 222.2 L 256 241 L 247.2 222.2 L 233 217.5 L 247.2 212.8 Z" fill="#1E293B"/>

  <!-- Ruled Record Lines -->
  <rect x="178" y="312" width="156" height="7" rx="3.5" fill="#14B8A6"/>
  <rect x="178" y="336" width="90" height="7" rx="3.5" fill="#94A3B8"/>

  <!-- Wordmark "EchoMind" -->
  <text x="256" y="440" font-family="'Cal Sans', 'Segoe UI', system-ui, sans-serif" font-size="32" font-weight="700" fill="#1E293B" text-anchor="middle" letter-spacing="1.2">EchoMind</text>
</svg>
"""
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(svg)
    print(f"Saved SVG: {full_path}")

if __name__ == "__main__":
    render_custom_logo_variant_b("ui-design/echomind_logo_spec_b.png", star_scale=1.75)
    export_spec_b_svg()

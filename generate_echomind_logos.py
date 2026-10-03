import math
import os
from PIL import Image, ImageDraw, ImageFont

FONT_CAL_SANS = "android/app/src/main/assets/fonts/Cal Sans.ttf"
FONT_YAHEI_BOLD = "C:/Windows/Fonts/msyhbd.ttc"
FONT_YAHEI = "C:/Windows/Fonts/msyh.ttc"

def get_latin_font(size):
    if os.path.exists(FONT_CAL_SANS):
        try:
            return ImageFont.truetype(FONT_CAL_SANS, size)
        except Exception:
            pass
    return ImageFont.load_default()

def get_chinese_font(size, bold=False):
    path = FONT_YAHEI_BOLD if bold else FONT_YAHEI
    if os.path.exists(path):
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            pass
    return ImageFont.load_default()

def create_canvas(w=1600, h=1000, scale=3, bg=(10, 15, 29)):
    sw, sh = w * scale, h * scale
    img = Image.new("RGBA", (sw, sh), bg + (255,))
    draw = ImageDraw.Draw(img)
    return img, draw, scale

def save_scaled(img, path, target_w=1600, target_h=1000):
    final = img.resize((target_w, target_h), Image.Resampling.LANCZOS)
    final.save(path, "PNG")
    print(f"Saved: {path}")

# -------------------------------------------------------------
# Logo 1: E-Wave Monogram (E声波字母标 + EchoMind)
# -------------------------------------------------------------
def draw_logo_mark_1(draw, cx, cy, scale, s_factor=1.0):
    s = scale * s_factor
    bar_h = 24 * s
    gap = 42 * s
    r = 12 * s
    
    # 竖梁 (左侧立柱，带渐变紫)
    draw.rounded_rectangle([cx - 100*s, cy - gap - bar_h, cx - 64*s, cy + gap + bar_h], radius=r, fill=(99, 102, 241, 255))

    # 顶杠 (短波进长波)
    draw.rounded_rectangle([cx - 100*s, cy - gap - bar_h, cx + 80*s, cy - gap], radius=r, fill=(99, 102, 241, 255))

    # 中杠 (带声波频率突起的波形柱)
    draw.rounded_rectangle([cx - 100*s, cy - bar_h//2, cx + 45*s, cy + bar_h//2], radius=r, fill=(56, 189, 248, 255))
    
    # 底杠 (延展波)
    draw.rounded_rectangle([cx - 100*s, cy + gap, cx + 105*s, cy + gap + bar_h], radius=r, fill=(16, 185, 129, 255))
    
    # 声波向右扩散的小脉冲点群
    pulses = [
        (cx + 120*s, cy - gap - bar_h//2, 10*s, (99, 102, 241, 255)),
        (cx + 152*s, cy - gap - bar_h//2, 7*s, (99, 102, 241, 180)),
        (cx + 85*s, cy, 12*s, (56, 189, 248, 255)),
        (cx + 122*s, cy, 8*s, (56, 189, 248, 180)),
        (cx + 145*s, cy + gap + bar_h//2, 10*s, (16, 185, 129, 255)),
    ]
    for px, py, pr, pcol in pulses:
        draw.ellipse([px - pr, py - pr, px + pr, py + pr], fill=pcol)

# -------------------------------------------------------------
# Logo 2: Echo Ripple Mind (回响涟漪大脑 + 极简圆弧)
# -------------------------------------------------------------
def draw_logo_mark_2(draw, cx, cy, scale, s_factor=1.0):
    s = scale * s_factor
    
    # 核心心智圆点
    draw.ellipse([cx - 20*s, cy - 20*s, cx + 20*s, cy + 20*s], fill=(16, 185, 129, 255))
    
    # 3 重声波涟漪波纹
    rings = [
        (54*s, 16*s, (16, 185, 129, 255)),
        (96*s, 14*s, (52, 211, 153, 220)),
        (138*s, 12*s, (110, 231, 183, 160))
    ]
    
    for radius, stroke_w, col in rings:
        # 左弧
        draw.arc([cx - radius, cy - radius, cx + radius, cy + radius], start=120, end=240, fill=col, width=int(stroke_w))
        # 右弧
        draw.arc([cx - radius, cy - radius, cx + radius, cy + radius], start=-60, end=60, fill=col, width=int(stroke_w))
        
    # 顶部灵感微光
    draw.rounded_rectangle([cx - 7*s, cy - 145*s, cx + 7*s, cy - 110*s], radius=7*s, fill=(245, 158, 11, 255))

# -------------------------------------------------------------
# Logo 3: Infinity Voice Flow (左声右记 · 无限声念流)
# -------------------------------------------------------------
def draw_logo_mark_3(draw, cx, cy, scale, s_factor=1.0):
    s = scale * s_factor
    
    left_cx = cx - 60*s
    right_cx = cx + 60*s
    
    # 左侧声波柱
    bars = [26, 52, 85, 120, 85, 52, 26]
    for i, h in enumerate(bars):
        bx = left_cx - 52*s + i * 17*s
        draw.rounded_rectangle([bx - 6*s, cy - h*s//2, bx + 6*s, cy + h*s//2], radius=6*s, fill=(244, 63, 94, 255))
        
    # 右侧结构化笔记框
    draw.rounded_rectangle([right_cx - 40*s, cy - 60*s, right_cx + 48*s, cy + 60*s], radius=18*s, outline=(139, 92, 246, 255), width=int(14*s))
    
    # 右框内部 2 条整齐的记录横线
    draw.rounded_rectangle([right_cx - 18*s, cy - 20*s, right_cx + 26*s, cy - 8*s], radius=6*s, fill=(168, 85, 247, 255))
    draw.rounded_rectangle([right_cx - 18*s, cy + 8*s, right_cx + 16*s, cy + 20*s], radius=6*s, fill=(192, 132, 252, 255))

# -------------------------------------------------------------
# Logo 4: Spark Waveform (灵感音律星标)
# -------------------------------------------------------------
def draw_logo_mark_4(draw, cx, cy, scale, s_factor=1.0):
    s = scale * s_factor
    
    # 中央星芒
    star_r = 50 * s
    star_points = [
        (cx, cy - star_r),
        (cx + star_r * 0.35, cy - star_r * 0.35),
        (cx + star_r, cy),
        (cx + star_r * 0.35, cy + star_r * 0.35),
        (cx, cy + star_r),
        (cx - star_r * 0.35, cy + star_r * 0.35),
        (cx - star_r, cy),
        (cx - star_r * 0.35, cy - star_r * 0.35),
    ]
    draw.polygon(star_points, fill=(245, 158, 11, 255))
    
    # 左右延伸的等化器声波线条
    left_bars = [22, 42, 70, 100, 55]
    for i, h in enumerate(reversed(left_bars)):
        bx = cx - 72*s - i * 20*s
        draw.rounded_rectangle([bx - 6*s, cy - h*s//2, bx + 6*s, cy + h*s//2], radius=6*s, fill=(59, 130, 246, 255))
        
    right_bars = [55, 100, 70, 42, 22]
    for i, h in enumerate(right_bars):
        bx = cx + 72*s + i * 20*s
        draw.rounded_rectangle([bx - 6*s, cy - h*s//2, bx + 6*s, cy + h*s//2], radius=6*s, fill=(59, 130, 246, 255))

# -------------------------------------------------------------
# 生成全景 2x2 Logo 品牌提案看板 (Brand Identity Board)
# -------------------------------------------------------------
def render_brand_showcase(filename="ui-design/echomind_logo_showcase.png"):
    w, h = 1800, 1200
    scale = 3
    img, draw, s = create_canvas(w, h, scale, bg=(10, 15, 29))
    
    # 顶部大标题
    font_head_en = get_latin_font(int(50 * s))
    font_sub_cn = get_chinese_font(int(22 * s))
    
    draw.text((80 * s, 55 * s), "EchoMind", font=font_head_en, fill=(255, 255, 255))
    draw.text((360 * s, 70 * s), "Brand Identity Design System", font=get_latin_font(int(30 * s)), fill=(99, 102, 241))
    draw.text((80 * s, 125 * s), "声念 · 将碎片思维，一语成章 — 4 款极简矢量品牌 Logo 提案", font=font_sub_cn, fill=(148, 163, 184))
    
    # 4 个格子的坐标
    grid = [
        (80*s, 190*s, 860*s, 640*s, 1, "01. E-Wave Monogram", "声波字母标", "以声波与律动构筑品牌首字母 'E'，极简科技现代"),
        (940*s, 190*s, 1720*s, 640*s, 2, "02. Echo Ripple Mind", "声波心智涟漪", "同心声纹环抱心智核心，优雅人本温度"),
        (80*s, 680*s, 860*s, 1130*s, 3, "03. Infinity Flow", "无限声念流", "左声右记，声波与笔记形成无限循环流"),
        (940*s, 680*s, 1720*s, 1130*s, 4, "04. Spark Waveform", "灵感音律星标", "AI 灵感星芒与均衡器声谱，突出智能速记"),
    ]
    
    font_card_title = get_latin_font(int(26 * s))
    font_logo_name = get_latin_font(int(62 * s))
    font_card_cn = get_chinese_font(int(20 * s), bold=True)
    font_card_desc = get_chinese_font(int(18 * s))
    
    for x1, y1, x2, y2, idx, en_title, cn_tag, desc in grid:
        # 卡片底色
        draw.rounded_rectangle([x1, y1, x2, y2], radius=28*s, fill=(18, 26, 47, 255), outline=(30, 41, 59, 255), width=int(2*s))
        
        # 居中偏左绘制 Mark
        card_cx = (x1 + 180*s)
        card_cy = ((y1 + y2) // 2)
        
        if idx == 1:
            draw_logo_mark_1(draw, card_cx, card_cy, s, s_factor=0.9)
        elif idx == 2:
            draw_logo_mark_2(draw, card_cx, card_cy, s, s_factor=0.9)
        elif idx == 3:
            draw_logo_mark_3(draw, card_cx, card_cy, s, s_factor=0.9)
        elif idx == 4:
            draw_logo_mark_4(draw, card_cx, card_cy, s, s_factor=0.9)
            
        # 文字排版
        tx = x1 + 330*s
        draw.text((tx, y1 + 75*s), en_title, font=font_card_title, fill=(148, 163, 184))
        draw.text((tx, y1 + 120*s), "EchoMind", font=font_logo_name, fill=(255, 255, 255))
        draw.text((tx, y1 + 205*s), f"声念 · {cn_tag}", font=font_card_cn, fill=(16, 185, 129))
        draw.text((tx, y1 + 245*s), desc, font=font_card_desc, fill=(148, 163, 184))
        
    save_scaled(img, filename, w, h)

if __name__ == "__main__":
    render_brand_showcase()

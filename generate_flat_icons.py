import math
from PIL import Image, ImageDraw, ImageFont

def create_supersampled_canvas(size=1024, scale=4):
    ss_size = size * scale
    img = Image.new("RGBA", (ss_size, ss_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    return img, draw, scale

def save_supersampled(img, path, target_size=1024):
    final = img.resize((target_size, target_size), Image.Resampling.LANCZOS)
    final.save(path, "PNG")
    print(f"Saved: {path}")

def draw_squircle_bg(draw, size, scale, bg_color, corner_radius=220):
    r = corner_radius * scale
    margin = 32 * scale
    draw.rounded_rectangle(
        [margin, margin, (size - 32) * scale, (size - 32) * scale],
        radius=r,
        fill=bg_color
    )

# ==========================================
# 方案 1: 【声波行笺】Soundwave to Ruled Note Lines
# ==========================================
def render_flat_icon_1(path="ui-design/icon_flat_1.png"):
    img, draw, s = create_supersampled_canvas()
    
    # 1. 背景：极深蓝黑 / Slate 950
    draw_squircle_bg(draw, 1024, s, (15, 23, 42, 255), corner_radius=220)
    
    # 2. 居中的记录卡片 (白底，带微圆角与右下折角/极简几何)
    card_x1, card_y1 = 260 * s, 210 * s
    card_x2, card_y2 = 764 * s, 814 * s
    draw.rounded_rectangle([card_x1, card_y1, card_x2, card_y2], radius=44 * s, fill=(255, 255, 255, 255))
    
    # 3. 顶部第一行：声波记录状态（录音指示红/翠绿圆点 + 动感声波）
    dot_cx, dot_cy = 340 * s, 320 * s
    draw.ellipse([dot_cx - 16*s, dot_cy - 16*s, dot_cx + 16*s, dot_cy + 16*s], fill=(16, 185, 129, 255))
    
    # 声波柱（从高低起伏的声波过渡到记录行）
    wave_x = 380 * s
    heights = [18, 36, 58, 80, 52, 92, 64, 40, 24, 16]
    for i, h in enumerate(heights):
        x = wave_x + i * 28 * s
        y_top = dot_cy - (h * s // 2)
        y_bot = dot_cy + (h * s // 2)
        draw.rounded_rectangle([x - 5*s, y_top, x + 5*s, y_bot], radius=5*s, fill=(16, 185, 129, 255))
    
    # 连接尾部的平滑文本线
    draw.rounded_rectangle([(wave_x + 10 * 28 * s), dot_cy - 6*s, (card_x2 - 70*s), dot_cy + 6*s], radius=6*s, fill=(16, 185, 129, 255))

    # 4. 下方 3 行整齐的结构化记录行 (灰度排版线条)
    lines_data = [
        (430 * s, [(340*s, 44*s, (99, 102, 241, 255)), (404*s, 290*s, (203, 213, 225, 255))]),  # 标签块 + 长线
        (540 * s, [(340*s, 44*s, (14, 165, 233, 255)), (404*s, 240*s, (203, 213, 225, 255))]),  # 标签块 + 中长线
        (650 * s, [(340*s, 44*s, (245, 158, 11, 255)), (404*s, 180*s, (203, 213, 225, 255))])   # 标签块 + 短线
    ]
    
    for y_center, items in lines_data:
        for x_start, width, color in items:
            draw.rounded_rectangle([x_start, y_center - 10*s, x_start + width, y_center + 10*s], radius=10*s, fill=color)

    save_supersampled(img, path)

# ==========================================
# 方案 2: 【极简手账与连续声波笔触】Minimalist Journal & Audio Pen
# ==========================================
def render_flat_icon_2(path="ui-design/icon_flat_2.png"):
    img, draw, s = create_supersampled_canvas()
    
    # 1. 背景：纯粹暖黑 / Zinc 900
    draw_squircle_bg(draw, 1024, s, (24, 24, 27, 255), corner_radius=220)
    
    # 2. 极简展开笔记本轮廓 (双页设计)
    center_x = 512 * s
    left_page = [220 * s, 260 * s, 488 * s, 764 * s]
    right_page = [536 * s, 260 * s, 804 * s, 764 * s]
    
    draw.rounded_rectangle(left_page, radius=32 * s, fill=(244, 244, 245, 255))
    draw.rounded_rectangle(right_page, radius=32 * s, fill=(244, 244, 245, 255))
    
    # 书脊中间的优雅装订点
    for y in [340 * s, 440 * s, 540 * s, 640 * s]:
        draw.rounded_rectangle([502 * s, y - 10*s, 522 * s, y + 10*s], radius=10*s, fill=(113, 113, 122, 255))
        
    # 左页：整齐的排版记录线
    for y in [360 * s, 450 * s, 540 * s, 630 * s]:
        w = 190 * s if y != 630 * s else 120 * s
        draw.rounded_rectangle([264 * s, y - 7*s, 264 * s + w, y + 7*s], radius=7*s, fill=(212, 212, 216, 255))
        
    # 右页：一条亮橙色/珊瑚红 (Tangerine Coral) 连续声波笔触，象征即刻记录
    stroke_color = (249, 115, 22, 255)
    
    # 声波波形在右页中央
    wave_r_x = 576 * s
    r_heights = [20, 50, 90, 130, 80, 110, 60, 30]
    mid_y = 480 * s
    for i, h in enumerate(r_heights):
        x = wave_r_x + i * 24 * s
        draw.rounded_rectangle([x - 5*s, mid_y - h*s//2, x + 5*s, mid_y + h*s//2], radius=5*s, fill=stroke_color)
        
    # 右页底部的记录勾选框与完成线
    draw.rounded_rectangle([576 * s, 630 * s - 7*s, 576 * s + 150*s, 630 * s + 7*s], radius=7*s, fill=(161, 161, 170, 255))

    save_supersampled(img, path)

# ==========================================
# 方案 3: 【语音气泡与结构清单】Voice Bubble to Structured Checklist
# ==========================================
def render_flat_icon_3(path="ui-design/icon_flat_3.png"):
    img, draw, s = create_supersampled_canvas()
    
    # 1. 背景：极简深青蓝 (Deep Indigo Blue #0B1120)
    draw_squircle_bg(draw, 1024, s, (11, 17, 32, 255), corner_radius=220)
    
    # 2. 大号极简对话/记录气泡卡片
    b_x1, b_y1 = 220 * s, 230 * s
    b_x2, b_y2 = 804 * s, 750 * s
    draw.rounded_rectangle([b_x1, b_y1, b_x2, b_y2], radius=52 * s, fill=(255, 255, 255, 255))
    
    # 气泡下巴小三角 (平滑极简圆润尾部)
    triangle = [(310 * s, 746 * s), (310 * s, 820 * s), (380 * s, 746 * s)]
    draw.polygon(triangle, fill=(255, 255, 255, 255))
    
    # 3. 气泡内部：第一行是声音录入图标（圆点 + 声波）
    mic_x, mic_y = 310 * s, 330 * s
    draw.rounded_rectangle([mic_x, mic_y - 12*s, mic_x + 24*s, mic_y + 12*s], radius=12*s, fill=(59, 130, 246, 255))
    
    wave_bars = [16, 32, 54, 38, 64, 46, 28, 14]
    for i, h in enumerate(wave_bars):
        x = (mic_x + 50*s) + i * 20 * s
        draw.rounded_rectangle([x - 4*s, mic_y - h*s//2, x + 4*s, mid_y_temp := mic_y + h*s//2], radius=4*s, fill=(59, 130, 246, 255))
        
    # 分割线
    draw.rounded_rectangle([310 * s, 400 * s, 714 * s, 404 * s], radius=2*s, fill=(241, 245, 249, 255))
    
    # 4. 下方：结构化清单条目 (带复选框圆点和双色条目)
    tasks = [
        (480 * s, (99, 102, 241, 255), 330 * s),  # 任务1
        (560 * s, (16, 185, 129, 255), 260 * s),  # 任务2
        (640 * s, (245, 158, 11, 255), 190 * s),  # 任务3
    ]
    for y_t, color, w in tasks:
        # 勾选框小圆点
        draw.ellipse([310*s, y_t - 12*s, 334*s, y_t + 12*s], fill=color)
        # 对应文本行
        draw.rounded_rectangle([360 * s, y_t - 9*s, 360 * s + w, y_t + 9*s], radius=9*s, fill=(71, 85, 105, 255))

    save_supersampled(img, path)

# ==========================================
# 方案 4: 【经典便签与声纹印记】Acoustic Note & Fold Metaphor
# ==========================================
def render_flat_icon_4(path="ui-design/icon_flat_4.png"):
    img, draw, s = create_supersampled_canvas()
    
    # 1. 背景：极简森林墨绿 (Emerald Black #022C22)
    draw_squircle_bg(draw, 1024, s, (2, 44, 34, 255), corner_radius=220)
    
    # 2. 扁平文档卡片 (优雅极简微黄米白 / 清爽薄荷白)
    d_x1, d_y1 = 260 * s, 200 * s
    d_x2, d_y2 = 764 * s, 824 * s
    draw.rounded_rectangle([d_x1, d_y1, d_x2, d_y2], radius=44 * s, fill=(248, 250, 252, 255))
    
    # 3. 顶部右上角极简折角效果 (纯扁平色块划分)
    fold_size = 110 * s
    # 覆盖右上折角
    draw.polygon([(d_x2 - fold_size, d_y1), (d_x2, d_y1 + fold_size), (d_x2 - fold_size, d_y1 + fold_size)], fill=(203, 213, 225, 255))
    draw.polygon([(d_x2 - fold_size, d_y1), (d_x2, d_y1 + fold_size), (d_x2, d_y1)], fill=(2, 44, 34, 255)) # 切空背景
    
    # 4. 居中核心：醒目的翡翠绿极简声纹脉冲（代表随声记录）
    center_y = 440 * s
    center_x = 512 * s
    bars = [30, 60, 110, 180, 260, 170, 230, 120, 65, 35]
    total_w = len(bars) * 36 * s
    start_x = center_x - total_w // 2 + 18*s
    
    for i, h in enumerate(bars):
        bx = start_x + i * 36 * s
        draw.rounded_rectangle([bx - 8*s, center_y - h*s//2, bx + 8*s, center_y + h*s//2], radius=8*s, fill=(16, 185, 129, 255))
        
    # 5. 底部两条整洁的横线，象征沉淀下来的文字记录
    draw.rounded_rectangle([330 * s, 660 * s - 8*s, 694 * s, 660 * s + 8*s], radius=8*s, fill=(148, 163, 184, 255))
    draw.rounded_rectangle([330 * s, 720 * s - 8*s, 560 * s, 720 * s + 8*s], radius=8*s, fill=(203, 213, 225, 255))

    save_supersampled(img, path)

if __name__ == "__main__":
    render_flat_icon_1()
    render_flat_icon_2()
    render_flat_icon_3()
    render_flat_icon_4()

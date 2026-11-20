#!/usr/bin/env python3
"""Generate the 64x64 pixel-art Keep But Penalty 2.0.0 badge icon (scaled 8x to 512x512)."""
from pathlib import Path
import math
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SIZE = 64
SCALE = 8  # 64 * 8 = 512x512

# Palette (RGBA)
TRANSPARENT = (0, 0, 0, 0)
OUTLINE = (13, 17, 26, 255)
BG_DARK = (19, 24, 38, 255)
BG_MID = (28, 36, 56, 255)
BG_LIGHT = (39, 50, 76, 255)
FRAME_DARK = (46, 58, 86, 255)
FRAME_GOLD = (212, 162, 74, 255)
FRAME_GOLD_HI = (247, 212, 122, 255)
FRAME_GOLD_DK = (138, 92, 26, 255)

# Steel chestplate
STEEL_HI = (232, 240, 250, 255)
STEEL_LIGHT = (182, 198, 218, 255)
STEEL_MID = (128, 146, 170, 255)
STEEL_DARK = (78, 92, 114, 255)
STEEL_SHADOW = (44, 54, 72, 255)

# Battle-damage crack
CRACK_CORE = (20, 22, 32, 255)
CRACK_EDGE = (244, 248, 255, 255)
CRACK_GLINT = (232, 120, 62, 255)

# Golden ward chain / halo
GOLD_CORE = (255, 228, 132, 255)
GOLD_MID = (235, 176, 56, 255)
GOLD_DARK = (158, 102, 22, 255)
GOLD_GLOW = (235, 176, 56, 85)

# Cyan-green XP orbs
XP_CORE = (224, 255, 236, 255)
XP_BRIGHT = (92, 252, 168, 255)
XP_CYAN = (34, 214, 176, 255)
XP_DEEP = (18, 134, 118, 255)
XP_GLOW = (72, 240, 176, 80)


def blend(dst, src):
    sa = src[3] / 255.0
    da = dst[3] / 255.0
    oa = sa + da * (1.0 - sa)
    if oa <= 0:
        return TRANSPARENT
    r = int((src[0] * sa + dst[0] * da * (1.0 - sa)) / oa)
    g = int((src[1] * sa + dst[1] * da * (1.0 - sa)) / oa)
    b = int((src[2] * sa + dst[2] * da * (1.0 - sa)) / oa)
    return (r, g, b, int(oa * 255))


def put(px, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        if color[3] == 255:
            px[x, y] = color
        else:
            px[x, y] = blend(px[x, y], color)


def in_rounded_rect(x, y, x0, y0, x1, y1, r):
    cx = min(max(x, x0 + r), x1 - r)
    cy = min(max(y, y0 + r), y1 - r)
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def draw_badge_background(px):
    for y in range(SIZE):
        for x in range(SIZE):
            if not in_rounded_rect(x, y, 2, 2, 61, 61, 10):
                continue
            if not in_rounded_rect(x, y, 4, 4, 59, 59, 8):
                # Outer dark outline + bevel rim
                if not in_rounded_rect(x, y, 3, 3, 60, 60, 9):
                    put(px, x, y, OUTLINE)
                else:
                    put(px, x, y, FRAME_GOLD if (x + y) < 62 else FRAME_GOLD_DK)
                continue
            if not in_rounded_rect(x, y, 5, 5, 58, 58, 7):
                put(px, x, y, FRAME_DARK)
                continue
            # Subtle radial background glow
            dx = x - 31.5
            dy = y - 31.5
            dist = math.hypot(dx, dy)
            if dist < 17:
                put(px, x, y, BG_LIGHT)
            elif dist < 27:
                put(px, x, y, BG_MID)
            else:
                put(px, x, y, BG_DARK)

    # Corner gold accent studs
    for sx, sy in [(8, 8), (55, 8), (8, 55), (55, 55)]:
        put(px, sx, sy, FRAME_GOLD_HI)
        put(px, sx + (1 if sx < 32 else -1), sy, FRAME_GOLD)
        put(px, sx, sy + (1 if sy < 32 else -1), FRAME_GOLD)


def draw_golden_ward_ring(px):
    cx, cy = 31.5, 32.5
    # Soft warm ward halo behind ring
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            if 18.2 <= d <= 24.4:
                put(px, x, y, GOLD_GLOW)

    # Continuous braided golden ward ring (radius ~21.3)
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            if 19.6 <= d <= 23.0:
                angle = math.atan2(y - cy, x - cx)
                seg = int(round((angle + math.pi) * 24 / (2 * math.pi)))
                if d < 20.3 or d > 22.3:
                    put(px, x, y, GOLD_DARK)
                elif seg % 2 == 0:
                    put(px, x, y, GOLD_CORE if y < cy + 4 else GOLD_MID)
                else:
                    put(px, x, y, GOLD_MID if y < cy + 4 else GOLD_DARK)
            elif 18.8 <= d < 19.6 or 23.0 < d <= 23.8:
                put(px, x, y, OUTLINE)

    # Cardinal ward clasps
    for ax, ay in [(31, 11), (32, 11), (31, 53), (32, 53), (10, 32), (53, 32)]:
        put(px, ax, ay, GOLD_CORE)


def chestplate_mask(x, y):
    """Return True if (x, y) is inside the bold Minecraft-inspired steel chestplate silhouette."""
    # Y range: 15..49, centered around x=31.5 (31 and 32 are center columns)
    if y < 15 or y > 49:
        return False
    # Shoulder pauldrons (y 15..28)
    if 15 <= y <= 28:
        if 14 <= x <= 49:
            # Collar U-notch in center top
            if 15 <= y <= 19 and 26 <= x <= 37:
                return False
            if y == 20 and 27 <= x <= 36:
                return False
            if y == 21 and 29 <= x <= 34:
                return False
            # Rounded top outer shoulder corners
            if y == 15 and (x < 18 or x > 45):
                return False
            if y == 16 and (x < 16 or x > 47):
                return False
            if y == 17 and (x < 15 or x > 48):
                return False
            # Armhole taper at bottom of sleeves
            if y >= 25 and (x < 15 or x > 48):
                return False
            if y == 28 and (x < 17 or x > 46):
                return False
            return True
        return False

    # Torso (y 29..49)
    if 29 <= y <= 49:
        # Slight waist taper
        half_w = 11 if y < 38 else (10 if y < 44 else 11)
        if y == 48:
            half_w = 10
        if y == 49:
            half_w = 8
        return abs(x - 31.5) <= half_w
    return False


def draw_chestplate(px):
    # First pass: dark outline around silhouette
    for y in range(13, 52):
        for x in range(12, 52):
            if chestplate_mask(x, y):
                continue
            for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                if chestplate_mask(x + dx, y + dy):
                    put(px, x, y, OUTLINE)
                    break

    # Second pass: shaded steel fill
    for y in range(15, 50):
        for x in range(14, 50):
            if not chestplate_mask(x, y):
                continue
            # Check inner border bevel
            top_edge = not chestplate_mask(x, y - 1)
            left_edge = not chestplate_mask(x - 1, y)
            bottom_edge = not chestplate_mask(x, y + 1)
            right_edge = not chestplate_mask(x + 1, y)

            if bottom_edge or right_edge:
                color = STEEL_SHADOW
            elif top_edge or left_edge:
                color = STEEL_HI
            else:
                # Specular chest curvature
                if 22 <= y <= 34 and 23 <= x <= 33:
                    color = STEEL_HI if (x + y) < 51 else STEEL_LIGHT
                elif x < 31:
                    color = STEEL_LIGHT if y < 38 else STEEL_MID
                else:
                    color = STEEL_MID if y < 38 else STEEL_DARK

            # Central chest ridge & abdominal plate lines
            if x == 31 and 23 <= y <= 46:
                color = STEEL_HI if y < 35 else STEEL_LIGHT
            elif x == 32 and 23 <= y <= 46:
                color = STEEL_DARK
            if y in (36, 42) and 23 <= x <= 40:
                color = STEEL_SHADOW
            if y in (37, 43) and 23 <= x <= 40:
                color = STEEL_LIGHT

            # Pauldron golden trim rivets
            if y == 27 and x in (17, 18, 45, 46):
                color = FRAME_GOLD
            if y == 26 and x in (17, 46):
                color = FRAME_GOLD_HI

            put(px, x, y, color)

    # Third pass: crisp diagonal battle-damage crack across the breastplate
    crack_path = [
        (39, 23), (38, 24), (37, 25), (36, 26), (35, 26),
        (34, 27), (33, 28), (33, 29), (32, 30), (31, 31),
        (30, 31), (29, 32), (28, 33), (28, 34), (27, 35),
        (26, 36), (25, 37), (24, 38), (24, 39),
    ]
    branch_path = [
        (33, 29), (34, 30), (35, 31), (36, 31), (37, 32),
    ]
    all_crack = set(crack_path + branch_path)

    for cx, cy in all_crack:
        if chestplate_mask(cx, cy):
            put(px, cx, cy, CRACK_CORE)
            # Highlight upper-left chipped metal edge
            if chestplate_mask(cx - 1, cy - 1) and (cx - 1, cy - 1) not in all_crack:
                put(px, cx - 1, cy - 1, CRACK_EDGE)
            # Subtle warm glint on lower-right fracture lip
            if chestplate_mask(cx + 1, cy) and (cx + 1, cy) not in all_crack:
                put(px, cx + 1, cy, CRACK_GLINT if (cx + cy) % 3 == 0 else STEEL_SHADOW)


def draw_xp_orb(px, cx, cy, radius):
    # Soft glow
    for dy in range(-radius - 2, radius + 3):
        for dx in range(-radius - 2, radius + 3):
            d = math.hypot(dx, dy)
            if radius < d <= radius + 1.8:
                put(px, cx + dx, cy + dy, XP_GLOW)

    # Pixel orb body + dark outline
    for dy in range(-radius - 1, radius + 2):
        for dx in range(-radius - 1, radius + 2):
            d = math.hypot(dx, dy)
            if d <= radius + 0.15:
                if d <= radius * 0.42:
                    put(px, cx + dx, cy + dy, XP_CORE)
                elif dx + dy < 0:
                    put(px, cx + dx, cy + dy, XP_BRIGHT)
                elif d <= radius * 0.82:
                    put(px, cx + dx, cy + dy, XP_CYAN)
                else:
                    put(px, cx + dx, cy + dy, XP_DEEP)
            elif d <= radius + 1.05:
                put(px, cx + dx, cy + dy, OUTLINE)


def draw_xp_orbs(px):
    # Floating cyan-green XP orbs drifting away from the cracked chestplate
    draw_xp_orb(px, 50, 15, 4)
    draw_xp_orb(px, 43, 11, 2)
    draw_xp_orb(px, 53, 25, 2)
    draw_xp_orb(px, 14, 48, 3)
    draw_xp_orb(px, 21, 52, 2)


def main():
    img = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)
    px = img.load()

    draw_badge_background(px)
    draw_golden_ward_ring(px)
    draw_chestplate(px)
    draw_xp_orbs(px)

    scaled = img.resize((SIZE * SCALE, SIZE * SCALE), Image.Resampling.NEAREST)
    targets = [
        ROOT / "common/src/main/resources/icon.png",
        ROOT / "docs/artwork/icon.png",
    ]
    for path in targets:
        path.parent.mkdir(parents=True, exist_ok=True)
        scaled.save(path, format="PNG")
        print(f"Wrote {path} ({scaled.size[0]}x{scaled.size[1]})")


if __name__ == "__main__":
    main()

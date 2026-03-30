"""
Generates valid ic_launcher.png and ic_launcher_round.png for all mipmap densities.
Design: dark navy background (#0F172A), cyan-to-indigo gradient "XL" monogram.
No external dependencies beyond the stdlib struct/zlib — pure PNG writer.
"""

import struct, zlib, math, os

# ── Density map: folder → pixel size ─────────────────────────────────────────
DENSITIES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

RES_DIR = os.path.join(os.path.dirname(__file__), "app", "src", "main", "res")

# ── Colours ───────────────────────────────────────────────────────────────────
BG_COLOUR    = (15,  23,  42)          # #0F172A  deep navy
CYAN_COLOUR  = (34,  211, 238)         # #22D3EE  cyan accent
INDIGO_COLOUR= (99,  102, 241)         # #6366F1  indigo accent


# ── Minimal pure-Python PNG encoder ──────────────────────────────────────────
def _png_chunk(chunk_type: bytes, data: bytes) -> bytes:
    c = chunk_type + data
    return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)

def encode_png(pixels: list[list[tuple[int,int,int]]], size: int) -> bytes:
    """Encode an RGB pixel grid as a valid PNG bytestring."""
    raw = b""
    for row in pixels:
        raw += b"\x00"                                   # filter type None
        for r, g, b in row:
            raw += bytes([r, g, b])
    compressed = zlib.compress(raw, 9)
    return (
        b"\x89PNG\r\n\x1a\n"
        + _png_chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 2, 0, 0, 0))
        + _png_chunk(b"IDAT", compressed)
        + _png_chunk(b"IEND", b"")
    )


# ── Drawing helpers ───────────────────────────────────────────────────────────
def lerp_colour(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def blend(bg, fg, alpha: float):
    """Alpha-composite fg over bg (alpha 0-1)."""
    return tuple(int(bg[i] * (1 - alpha) + fg[i] * alpha) for i in range(3))

def draw_icon(size: int, round_shape: bool) -> list[list[tuple[int,int,int]]]:
    pixels = [[BG_COLOUR] * size for _ in range(size)]
    cx = cy = size / 2
    r  = size / 2

    for y in range(size):
        for x in range(size):
            # ── Shape mask (square for normal, circle for round) ──────────────
            if round_shape:
                dx, dy = x - cx + 0.5, y - cy + 0.5
                dist   = math.sqrt(dx*dx + dy*dy)
                if dist > r:
                    continue                              # outside circle
            # ── Background gradient (top-left cyan → bottom-right indigo) ────
            t      = ((x + y) / (2 * (size - 1)))
            bg_col = lerp_colour(CYAN_COLOUR, INDIGO_COLOUR, t)
            # Darken slightly for depth
            bg_col = lerp_colour(BG_COLOUR, bg_col, 0.85)
            pixels[y][x] = bg_col

    # ── Draw "₹" symbol centred in the icon ──────────────────────────────────
    # We rasterise a simple stylised rupee sign as thick lines/arcs.
    # Scale factor relative to 48 px base
    s  = size / 48
    lw = max(2, int(3 * s))   # line width

    def draw_rect(x0, y0, x1, y1, col):
        for yy in range(int(y0), int(y1)+1):
            for xx in range(int(x0), int(x1)+1):
                if 0 <= yy < size and 0 <= xx < size:
                    pixels[yy][xx] = col

    def draw_hline(y_pos, x_start, x_end, col):
        draw_rect(x_start, y_pos - lw//2, x_end, y_pos + lw//2, col)

    def draw_vline(x_pos, y_start, y_end, col):
        draw_rect(x_pos - lw//2, y_start, x_pos + lw//2, y_end, col)

    def draw_arc(cx_, cy_, rad, t_start, t_end, col, steps=60):
        for i in range(steps):
            t  = t_start + (t_end - t_start) * i / steps
            ax = cx_ + rad * math.cos(t)
            ay = cy_ + rad * math.sin(t)
            draw_rect(ax - lw//2, ay - lw//2, ax + lw//2, ay + lw//2, col)

    # Rupee symbol dimensions scaled to icon size
    margin  = size * 0.22
    left    = margin
    right   = size - margin
    top     = size * 0.20
    bottom  = size * 0.82
    mid_x   = size * 0.36
    sym_col = (255, 255, 255)   # white glyph

    # Vertical stem
    draw_vline(left + lw, top, bottom, sym_col)
    # Top horizontal bar
    draw_hline(top + lw, left, right, sym_col)
    # Middle horizontal bar
    draw_hline(size * 0.44, left, right, sym_col)
    # Top arc (rounded top-right of the ₹ bump)
    arc_cx = (left + right) / 2
    arc_cy = top + (size * 0.44 - top) / 2
    arc_r  = (right - left) / 2 - lw
    draw_arc(arc_cx, arc_cy, arc_r, -math.pi / 2, math.pi / 2, sym_col)
    # Diagonal descender
    x_start = right
    y_start = size * 0.44
    x_end   = left + lw * 2
    y_end   = bottom
    steps   = max(30, size)
    for i in range(steps):
        tt = i / steps
        lx = x_start + (x_end - x_start) * tt
        ly = y_start + (y_end - y_start) * tt
        draw_rect(lx - lw//2, ly - lw//2, lx + lw//2, ly + lw//2, sym_col)

    return pixels


# ── Generate all icons ────────────────────────────────────────────────────────
generated = []
for folder, size in DENSITIES.items():
    out_dir = os.path.join(RES_DIR, folder)
    os.makedirs(out_dir, exist_ok=True)

    for fname, is_round in [("ic_launcher.png", False), ("ic_launcher_round.png", True)]:
        pixels = draw_icon(size, is_round)
        png    = encode_png(pixels, size)
        path   = os.path.join(out_dir, fname)
        with open(path, "wb") as f:
            f.write(png)
        generated.append(f"  {folder}/{fname}  ({size}x{size}, {len(png)} bytes)")

print("Generated icons:")
for g in generated:
    print(g)
print("\nDone.")


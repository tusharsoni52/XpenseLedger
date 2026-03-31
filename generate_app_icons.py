"""
XpenseLedger icon generator — v3
- Adaptive layers: 1024×1024 (drawn at 2048 and downsampled 2× for anti-aliasing)
- Wallet fills the adaptive safe zone (centre 66% of canvas)
- Legacy density PNGs: proper sizes, no blur
"""

from PIL import Image, ImageDraw, ImageFilter
import os, math

RES = os.path.join(os.path.dirname(__file__), "app", "src", "main", "res")

# ── Palette ───────────────────────────────────────────────────────────────────
BG_TOP  = (13,  17,  23,  255)
BG_MID  = (14,  32,  48,  255)
BG_BOT  = (26,  31,  58,  255)
TEAL    = (0,  212, 160, 255)
INDIGO  = (123,104, 238, 255)
WHITE   = (255,255,255, 255)
SLOT    = (20,  20,  30,  90)

def lerp(a,b,t): return a+(b-a)*t
def lerpC(c0,c1,t):
    return tuple(int(lerp(c0[i],c1[i],t)) for i in range(4))

# ── Gradient helpers ──────────────────────────────────────────────────────────
def draw_diagonal_gradient(img, c0, c1, c2):
    """3-stop diagonal top-left→bottom-right gradient."""
    w, h = img.size
    pix = img.load()
    for y in range(h):
        for x in range(w):
            t = (x/w + y/h) / 2.0
            pix[x,y] = lerpC(c0,c1,min(t*2,1)) if t<0.5 else lerpC(c1,c2,min((t-0.5)*2,1))

def draw_radial_glow(img, cx_f, cy_f, r_f, color, max_a):
    w,h = img.size
    cx,cy,r = int(cx_f*w), int(cy_f*h), int(r_f*w)
    overlay = Image.new("RGBA",(w,h),(0,0,0,0))
    d = ImageDraw.Draw(overlay)
    for s in range(r,0,-3):
        a = int(max_a*(1-s/r)**1.6)
        d.ellipse([cx-s,cy-s,cx+s,cy+s], fill=color[:3]+(a,))
    img.alpha_composite(overlay)

def draw_horiz_gradient_rect(img, x0,y0,x1,y1, c0,c1, mask=None):
    """Horizontal gradient rectangle, optionally masked."""
    w = x1-x0; h = y1-y0
    band = Image.new("RGBA",(w,h),(0,0,0,0))
    for x in range(w):
        col = lerpC(c0,c1, x/max(w-1,1))
        ImageDraw.Draw(band).line([(x,0),(x,h)], fill=col)
    if mask:
        img.paste(band,(x0,y0),mask=mask)
    else:
        img.alpha_composite(band, (x0,y0))

# ── Background layer ──────────────────────────────────────────────────────────
def make_background(size):
    img = Image.new("RGBA",(size,size))
    draw_diagonal_gradient(img, BG_TOP, BG_MID, BG_BOT)
    draw_radial_glow(img, 0.15,0.15, 0.55, TEAL,   60)
    draw_radial_glow(img, 0.85,0.85, 0.48, INDIGO, 45)
    return img

# ── Foreground layer ──────────────────────────────────────────────────────────
# Adaptive safe zone = centre 66.7% of canvas (72/108 dp ratio).
# We draw the wallet to fill that zone with comfortable padding inside it.
def make_foreground(size):
    # Draw at 2× then downsample for clean anti-aliasing
    S = size * 2
    img = Image.new("RGBA",(S,S),(0,0,0,0))
    draw = ImageDraw.Draw(img)

    # Safe zone in the 2× canvas
    # Adaptive icon: safe zone = inner 72/108 = 66.7% → margins = 16.7% each side
    margin = int(S * 0.10)          # give a little more breathing room (10%)
    wm = margin + int(S*0.04)       # wallet extra inset

    # Wallet bounding box — fills most of the safe zone
    wx0 = wm
    wy0 = wm + int(S*0.06)
    wx1 = S - wm
    wy1 = S - wm - int(S*0.04)
    rad = int((wx1-wx0) * 0.12)     # rounded corner radius

    # ── White card body ────────────────────────────────────────────────────
    draw.rounded_rectangle([wx0,wy0,wx1,wy1], radius=rad, fill=WHITE)

    # ── Teal→Indigo header strip (top ~22% of card) ────────────────────────
    strip_h = int((wy1-wy0)*0.24)
    strip_y1 = wy0 + strip_h

    # Build strip gradient image
    sw, sh = wx1-wx0, strip_h
    strip = Image.new("RGBA",(sw,sh),(0,0,0,0))
    for x in range(sw):
        col = lerpC(TEAL, INDIGO, x/max(sw-1,1))
        ImageDraw.Draw(strip).line([(x,0),(x,sh)], fill=col)

    # Clip strip to rounded top of card
    card_mask = Image.new("L",(wx1-wx0, wy1-wy0),0)
    ImageDraw.Draw(card_mask).rounded_rectangle([0,0,wx1-wx0,wy1-wy0], radius=rad, fill=255)
    strip_mask = card_mask.crop((0,0,sw,sh))
    img.paste(strip,(wx0,wy0), mask=strip_mask)

    # ── Slot / card lines ──────────────────────────────────────────────────
    lw = max(3, int(S*0.010))
    mid_y = wy0 + int((wy1-wy0)*0.60)
    lo_y  = wy0 + int((wy1-wy0)*0.76)
    draw.line([(wx0+int((wx1-wx0)*0.12), mid_y),
               (wx0+int((wx1-wx0)*0.72), mid_y)], fill=SLOT, width=lw)
    draw.line([(wx0+int((wx1-wx0)*0.12), lo_y),
               (wx0+int((wx1-wx0)*0.50), lo_y)], fill=SLOT, width=lw)

    # ── Teal coin (bottom-right quadrant of card) ──────────────────────────
    coin_cx = wx0 + int((wx1-wx0)*0.80)
    coin_cy = wy0 + int((wy1-wy0)*0.68)
    coin_r  = int((wx1-wx0)*0.11)
    draw.ellipse([coin_cx-coin_r, coin_cy-coin_r,
                  coin_cx+coin_r, coin_cy+coin_r], fill=TEAL)
    inner_r = int(coin_r*0.55)
    draw.ellipse([coin_cx-inner_r, coin_cy-inner_r,
                  coin_cx+inner_r, coin_cy+coin_r], fill=(255,255,255,210))

    # ── Growth arrow (teal→indigo shaft + head) ───────────────────────────
    # Sits in the bottom-left area of the card, pointing up-right
    alw = max(4, int(S*0.018))
    px0 = wx0 + int((wx1-wx0)*0.14)
    py0 = wy0 + int((wy1-wy0)*0.88)
    px1 = wx0 + int((wx1-wx0)*0.55)
    py1 = wy0 + int((wy1-wy0)*0.42)

    arrow = Image.new("RGBA",(S,S),(0,0,0,0))
    adraw = ImageDraw.Draw(arrow)
    steps = 30
    for i in range(steps):
        t0,t1 = i/steps,(i+1)/steps
        x0=int(lerp(px0,px1,t0)); y0=int(lerp(py0,py1,t0))
        x1=int(lerp(px0,px1,t1)); y1=int(lerp(py0,py1,t1))
        adraw.line([(x0,y0),(x1,y1)], fill=lerpC(TEAL,INDIGO,(t0+t1)/2), width=alw)

    # Arrowhead
    hs = int(S*0.055)
    ang = math.atan2(py0-py1, px1-px0)   # arrow angle
    def tip_pt(a, dist):
        return (int(px1 + dist*math.cos(a)), int(py1 - dist*math.sin(a)))
    head = [
        (px1, py1),
        tip_pt(ang - math.radians(145), hs),
        tip_pt(ang - math.radians(210), hs),
    ]
    adraw.polygon(head, fill=INDIGO)
    img.alpha_composite(arrow)

    # Downsample 2× for anti-aliasing
    return img.resize((size,size), Image.LANCZOS)

# ── Composites ────────────────────────────────────────────────────────────────
def make_full(size):
    bg = make_background(size)
    bg.alpha_composite(make_foreground(size))
    return bg

def make_round(size):
    icon = make_full(size)
    mask = Image.new("L",(size,size),0)
    ImageDraw.Draw(mask).ellipse([0,0,size-1,size-1], fill=255)
    out = Image.new("RGBA",(size,size),(0,0,0,0))
    out.paste(icon, mask=mask)
    return out

def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path,"PNG")
    print(f"  ✓  {os.path.relpath(path)}  {img.size[0]}×{img.size[1]}")

if __name__ == "__main__":
    print("\nGenerating XpenseLedger icons…\n")

    legacy = {
        "mipmap-mdpi":48, "mipmap-hdpi":72, "mipmap-xhdpi":96,
        "mipmap-xxhdpi":144, "mipmap-xxxhdpi":192,
    }
    for folder, size in legacy.items():
        save(make_full(size),  os.path.join(RES, folder, "ic_launcher.png"))
        save(make_round(size), os.path.join(RES, folder, "ic_launcher_round.png"))

    # Adaptive icon PNG layers — 1024px (drawn at 2048 then downsampled)
    A = 1024
    save(make_background(A),
         os.path.join(RES,"mipmap-anydpi-v26","ic_launcher_bg_layer.png"))
    save(make_foreground(A),
         os.path.join(RES,"mipmap-anydpi-v26","ic_launcher_fg_layer.png"))

    print("\nDone! Uninstall old APK then install new build.")

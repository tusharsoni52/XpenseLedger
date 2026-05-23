"""
Generate XpenseLedger launcher PNGs from app-icon-01.png.

Source image : app/src/main/res/app-icon-01.png  (1254×1254 RGB)

Adaptive icon spec (API 26+)
─────────────────────────────
  Full canvas      : 108dp  →  432px at xxxhdpi (4×)
  Safe zone        : 72dp   →  288px at xxxhdpi (central 66.7%)
  Required padding : 18dp each side  →  72px at xxxhdpi

  The foreground PNG must be 432×432 with the artwork scaled to fit
  inside the central 288×288 safe zone (72px transparent padding on all
  four sides).  Without this padding Android crops ~16.7% from every
  edge and the icon appears zoomed in.

Legacy mipmap PNGs (pre-API-26)
────────────────────────────────
  Standard launcher icon sizes with squircle / circle mask applied.
  The FULL image is used here (no padding needed — no adaptive crop).

PNG sizes
─────────
  mdpi    :  48 × 48
  hdpi    :  72 × 72
  xhdpi   :  96 × 96
  xxhdpi  : 144 × 144
  xxxhdpi : 192 × 192
"""

import os
from PIL import Image, ImageDraw

RES        = "app/src/main/res"
SOURCE_PNG = os.path.join(RES, "app-icon-01.png")

# Adaptive icon canvas / safe-zone fractions
# safe_zone_fraction = 72/108 = 0.6667
SAFE_ZONE_FRACTION = 72 / 108          # artwork occupies this fraction of canvas
# Foreground PNG size at xxxhdpi = 108dp × 4 = 432px
FOREGROUND_SIZE = 432

SIZES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}


def make_squircle_mask(size: int, radius_frac: float = 0.22) -> Image.Image:
    r = int(size * radius_frac)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=255)
    return mask


def make_circle_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    return mask


def apply_mask(img: Image.Image, mask: Image.Image) -> Image.Image:
    out = img.copy().convert("RGBA")
    out.putalpha(mask)
    return out


def make_foreground_png(src: Image.Image, canvas_size: int) -> Image.Image:
    """
    Create the adaptive icon foreground PNG.

    The artwork is scaled to fit the safe zone (66.7% of canvas),
    centred on a fully-transparent canvas.  This ensures Android's
    launcher crop never cuts into the artwork.
    """
    safe_px = int(canvas_size * SAFE_ZONE_FRACTION)
    padding = (canvas_size - safe_px) // 2

    # Scale artwork to safe zone size
    artwork = src.convert("RGBA").resize((safe_px, safe_px), Image.LANCZOS)

    # Place on transparent canvas with padding on all sides
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    canvas.paste(artwork, (padding, padding))
    return canvas


def main():
    base = os.path.dirname(os.path.abspath(__file__))
    src_path = os.path.join(base, SOURCE_PNG)
    src = Image.open(src_path).convert("RGB")
    print(f"Source: {src.size[0]}×{src.size[1]}  →  {src_path}")

    # ── 1. Adaptive icon foreground PNG (432×432, safe-zone padded) ──────────
    fg_png = make_foreground_png(src, FOREGROUND_SIZE)
    fg_path = os.path.join(base, RES, "drawable", "ic_launcher_foreground_img.png")
    fg_png.save(fg_path)
    safe_px = int(FOREGROUND_SIZE * SAFE_ZONE_FRACTION)
    padding = (FOREGROUND_SIZE - safe_px) // 2
    print(f"\n  Foreground PNG  {FOREGROUND_SIZE}×{FOREGROUND_SIZE}")
    print(f"    artwork area: {safe_px}×{safe_px}  (padding: {padding}px each side)")
    print(f"    saved → {fg_path}")

    # ── 2. Legacy mipmap PNGs (squircle + circle, no padding needed) ─────────
    print()
    for folder, size in SIZES.items():
        out_dir = os.path.join(base, RES, folder)
        os.makedirs(out_dir, exist_ok=True)

        resized = src.convert("RGBA").resize((size, size), Image.LANCZOS)

        apply_mask(resized, make_squircle_mask(size)).save(
            os.path.join(out_dir, "ic_launcher.png")
        )
        apply_mask(resized, make_circle_mask(size)).save(
            os.path.join(out_dir, "ic_launcher_round.png")
        )
        print(f"  ✓  {folder}  →  {size}×{size}")

    print("\nDone.")


if __name__ == "__main__":
    main()



"""Generate Money Journal launcher icons (premium dark + green rupee badge)."""
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")
RES = os.path.join(ROOT, "app/src/main/res")
FONT = os.path.expanduser("~/tools/PJS-700.ttf")

BG_TOP = (18, 27, 38)
BG_BOTTOM = (8, 12, 17)
GREEN_TOP = (56, 244, 170)
GREEN_BOTTOM = (10, 169, 116)


def vertical_gradient(size, top, bottom):
    img = Image.new("RGB", (1, size[1]))
    px = img.load()
    for y in range(size[1]):
        t = y / max(1, size[1] - 1)
        px[0, y] = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
    return img.resize(size, Image.BILINEAR)


def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return m


def glyph_layer(size, glyph_size, grad_box, glow=True):
    """Transparent layer with a centered green rupee glyph + soft glow."""
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    font = ImageFont.truetype(FONT, glyph_size)
    mask = Image.new("L", size, 0)
    d = ImageDraw.Draw(mask)
    bbox = d.textbbox((0, 0), "\u20b9", font=font)
    w, h = bbox[2] - bbox[0], bbox[3] - bbox[1]
    pos = ((size[0] - w) / 2 - bbox[0], (size[1] - h) / 2 - bbox[1] - size[1] * 0.01)
    d.text(pos, "\u20b9", font=font, fill=255)

    grad = vertical_gradient(size, GREEN_TOP, GREEN_BOTTOM).convert("RGBA")
    grad.putalpha(mask)
    if glow:
        glow_mask = mask.filter(ImageFilter.GaussianBlur(size[0] * 0.035))
        glow_img = Image.new("RGBA", size, GREEN_TOP + (0,))
        glow_img.putalpha(glow_mask.point(lambda v: int(v * 0.55)))
        layer = Image.alpha_composite(layer, glow_img)
    layer = Image.alpha_composite(layer, grad)
    return layer


def legacy_icon(px):
    size = px * 4  # supersample for crisper edges
    bg = vertical_gradient((size, size), BG_TOP, BG_BOTTOM).convert("RGBA")

    # subtle green aura top-right
    aura = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ad = ImageDraw.Draw(aura)
    r = int(size * 0.62)
    ad.ellipse([size - r, -r // 2, size + r // 2, r], fill=(34, 227, 154, 46))
    aura = aura.filter(ImageFilter.GaussianBlur(size * 0.10))
    bg = Image.alpha_composite(bg, aura)

    # fine inner border
    border = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(border).rounded_rectangle(
        [size * 0.012, size * 0.012, size * 0.988, size * 0.988],
        radius=size * 0.225, outline=(46, 62, 80, 190), width=max(2, int(size * 0.006)))
    bg = Image.alpha_composite(bg, border)

    badge = int(size * 0.74)
    offset = (size - badge) // 2
    ring = Image.new("RGBA", (badge, badge), (0, 0, 0, 0))
    rd = ImageDraw.Draw(ring)
    rd.ellipse([badge * 0.03, badge * 0.03, badge * 0.97, badge * 0.97],
               outline=(34, 227, 154, 58), width=max(2, int(size * 0.007)))
    bg.alpha_composite(ring, (offset, offset))

    glyph = glyph_layer((badge, badge), int(badge * 1.02), None)
    bg.alpha_composite(glyph, (offset, offset))

    bg = bg.resize((px, px), Image.LANCZOS)
    mask = rounded_mask((px, px), int(px * 0.225))
    out = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    out.paste(bg, (0, 0), mask)
    return out


def adaptive_foreground(px):
    size = 1024
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    badge = int(size * 0.58)  # sits inside the 66/108 safe zone
    off = (size - badge) // 2
    circle = Image.new("RGBA", (badge, badge), (0, 0, 0, 0))
    cd = ImageDraw.Draw(circle)
    cd.ellipse([0, 0, badge - 1, badge - 1], fill=(15, 24, 33, 255))
    # ring
    cd.ellipse([badge * 0.012, badge * 0.012, badge * 0.988, badge * 0.988],
               outline=(34, 227, 154, 72), width=int(badge * 0.014))
    layer.alpha_composite(circle, (off, off))

    glyph = glyph_layer((badge, badge), int(badge * 1.0), None)
    layer.alpha_composite(glyph, (off, off))
    return layer.resize((px, px), Image.LANCZOS)


def splash_logo(px):
    # 2/3 of canvas content, like adaptive foreground, on transparency
    size = 1024
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    badge = int(size * 0.66)
    off = (size - badge) // 2
    circle = Image.new("RGBA", (badge, badge), (0, 0, 0, 0))
    cd = ImageDraw.Draw(circle)
    cd.ellipse([0, 0, badge - 1, badge - 1], fill=(15, 24, 33, 255))
    cd.ellipse([badge * 0.01, badge * 0.01, badge * 0.99, badge * 0.99],
               outline=(34, 227, 154, 80), width=int(badge * 0.016))
    layer.alpha_composite(circle, (off, off))
    layer.alpha_composite(glyph_layer((badge, badge), int(badge * 1.0), None), (off, off))
    return layer.resize((px, px), Image.LANCZOS)


os.makedirs(RES, exist_ok=True)
densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for density, px in densities.items():
    icon = legacy_icon(px)
    icon.save(os.path.join(RES, f"mipmap-{density}/ic_launcher.png"))
    round_icon = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    mask = Image.new("L", (px * 4, px * 4), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, px * 4 - 1, px * 4 - 1], fill=255)
    round_icon.paste(legacy_icon(px * 4).resize((px, px), Image.LANCZOS), (0, 0),
                     mask.resize((px, px), Image.LANCZOS))
    round_icon.save(os.path.join(RES, f"mipmap-{density}/ic_launcher_round.png"))

adaptive_foreground(432).save(os.path.join(RES, "mipmap-xxxhdpi/ic_launcher_foreground.png"))
splash_logo(384).save(os.path.join(RES, "drawable-xxxhdpi/ic_splash_logo.png"))
print("icons written")

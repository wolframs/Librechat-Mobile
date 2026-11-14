#!/usr/bin/env python3
"""Render the fork's launcher icon (the dyad sigil) into the app's resources.

The sigil is one drawing in three palettes ("alloys"). Its geometry and the palettes live here;
`fork/icons/launcher/<alloy>.svg` is the whole icon for looking at, written by this script.

    fork/scripts/gen-launcher-icon.py              write the SVGs and the resources of every alloy
    fork/scripts/gen-launcher-icon.py cold-lab     only that alloy

Per alloy it writes, under app/src/main/res: the foreground layer as PNG in five densities
(mipmap-*/fork_launcher_<alloy>_foreground.png), the background as a vector gradient
(drawable/fork_launcher_<alloy>_background.xml) and the adaptive icon
(mipmap-anydpi-v26/fork_launcher_<alloy>.xml). The monochrome layer is shared
(mipmap-*/fork_launcher_monochrome.png). Needs rsvg-convert. The foreground is a bitmap because the
rings are woven with SVG masks, which a VectorDrawable cannot express.
"""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SVG_DIR = ROOT / "fork" / "icons" / "launcher"
RES = ROOT / "app" / "src" / "main" / "res"

# background centre and edge; ring A (and the arrow) from light to dark; ring B (and the arc) likewise
ALLOYS = {
    "night-glass": ("#2a2f6b", "#0b0d26", "#f5f7fc", "#a3adc6", "#d6deff", "#7783b5"),
    "ember-alchemy": ("#4c1e3e", "#1a0815", "#f2ad6c", "#b5603a", "#fbe6a6", "#cf9c48"),
    "cold-lab": ("#1b1b22", "#050507", "#f4f4f7", "#b4b4c0", "#7ff7ff", "#17b3d4"),
}
DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
STROKE = 4.5
GAP = STROKE + 2.6  # the width cut out of one ring where the other passes over it

GLYPH = """<defs>
<linearGradient id="ga" gradientUnits="userSpaceOnUse" x1="28" y1="26" x2="80" y2="84">
<stop offset="0" stop-color="{a0}"/><stop offset="1" stop-color="{a1}"/></linearGradient>
<linearGradient id="gb" gradientUnits="userSpaceOnUse" x1="28" y1="26" x2="80" y2="84">
<stop offset="0" stop-color="{b0}"/><stop offset="1" stop-color="{b1}"/></linearGradient>
<clipPath id="cTop"><circle cx="54" cy="43" r="6.5"/></clipPath>
<clipPath id="cBot"><circle cx="54" cy="67" r="6.5"/></clipPath>
<mask id="mB" maskUnits="userSpaceOnUse" x="0" y="0" width="108" height="108">
<rect width="108" height="108" fill="#ffffff"/>
<g clip-path="url(#cTop)"><circle cx="45" cy="55" r="15" fill="none" stroke="#000000" stroke-width="{gap}"/></g>
<rect x="44" y="52.1" width="8" height="5.8" fill="#000000"/>
</mask>
<mask id="mA" maskUnits="userSpaceOnUse" x="0" y="0" width="108" height="108">
<rect width="108" height="108" fill="#ffffff"/>
<g clip-path="url(#cBot)"><circle cx="63" cy="55" r="15" fill="none" stroke="#000000" stroke-width="{gap}"/></g>
<rect x="56" y="52.1" width="8" height="5.8" fill="#000000"/>
</mask>
</defs>
<g fill="none" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round"
   transform="translate(54 54) scale(0.98) translate(-54 -49.5)">
<g mask="url(#mA)" stroke="url(#ga)"><circle cx="45" cy="55" r="15"/></g>
<g mask="url(#mB)" stroke="url(#gb)"><circle cx="63" cy="55" r="15"/></g>
<path d="M47 29 A7 7 0 0 0 61 29" stroke="url(#gb)"/>
<path d="M35 55 L42 51 V59 Z" stroke="url(#ga)" stroke-width="2.6"/>
<path d="M42 55 H72 M68 51 V59" stroke="url(#ga)" stroke-width="3.1"/>
</g>"""

BACKGROUND = """<defs><radialGradient id="bg" cx="0.38" cy="0.3" r="0.8">
<stop offset="0" stop-color="{bg0}"/><stop offset="1" stop-color="{bg1}"/></radialGradient></defs>
<rect width="108" height="108" fill="url(#bg)"/>"""

BACKGROUND_XML = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by fork/scripts/gen-launcher-icon.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="41.04"
                android:centerY="32.4"
                android:gradientRadius="86.4"
                android:startColor="{bg0}"
                android:endColor="{bg1}" />
        </aapt:attr>
    </path>
</vector>
"""

ADAPTIVE_XML = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by fork/scripts/gen-launcher-icon.py. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/fork_launcher_{name}_background"/>
    <foreground android:drawable="@mipmap/fork_launcher_{name}_foreground"/>
    <monochrome android:drawable="@mipmap/fork_launcher_monochrome"/>
</adaptive-icon>
"""


def svg(body: str) -> str:
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">\n{body}\n</svg>\n'


def glyph(a0: str, a1: str, b0: str, b1: str) -> str:
    return GLYPH.format(a0=a0, a1=a1, b0=b0, b1=b1, sw=STROKE, gap=GAP)


def render(source: str, target: Path, size: int) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["rsvg-convert", "-w", str(size), "-h", str(size), "-o", str(target)],
        input=source.encode(), check=True,
    )


def write(target: Path, text: str) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text)


def main() -> int:
    names = sys.argv[1:] or list(ALLOYS)
    unknown = [n for n in names if n not in ALLOYS]
    if unknown:
        print(f"unknown alloy: {', '.join(unknown)}; known: {', '.join(ALLOYS)}", file=sys.stderr)
        return 2
    SVG_DIR.mkdir(parents=True, exist_ok=True)
    mono = svg(glyph("#ffffff", "#ffffff", "#ffffff", "#ffffff"))
    for density, size in DENSITIES.items():
        render(mono, RES / f"mipmap-{density}" / "fork_launcher_monochrome.png", size)
    for name in names:
        bg0, bg1, a0, a1, b0, b1 = ALLOYS[name]
        res_name = name.replace("-", "_")
        foreground = svg(glyph(a0, a1, b0, b1))
        (SVG_DIR / f"{name}.svg").write_text(svg(BACKGROUND.format(bg0=bg0, bg1=bg1) + "\n" + glyph(a0, a1, b0, b1)))
        for density, size in DENSITIES.items():
            render(foreground, RES / f"mipmap-{density}" / f"fork_launcher_{res_name}_foreground.png", size)
        write(RES / "drawable" / f"fork_launcher_{res_name}_background.xml", BACKGROUND_XML.format(bg0=bg0, bg1=bg1))
        write(RES / "mipmap-anydpi-v26" / f"fork_launcher_{res_name}.xml", ADAPTIVE_XML.format(name=res_name))
        print(f"{name}: icon written")
    return 0


if __name__ == "__main__":
    sys.exit(main())

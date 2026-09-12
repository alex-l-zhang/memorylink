#!/usr/bin/env python3
"""把 data.ts 里的窗口锚点画到场景照片上，用于人工校准。

窗灯是否落在真正的窗户上，直接影响"有点真实"这件事，所以锚点是照着照片量的。
改完 data.ts 的锚点后跑一次这个脚本，看输出图里的方块是否压在窗户/门口上。

用法：
    python3 prototype/tools/scene_check.py [输出目录]
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src" / "data.ts"
PUBLIC = ROOT / "public"
FIELDS = ("lights", "lamp", "candle")


def parse_scenes(text: str) -> list[dict]:
    block = text[text.index("export const SCENES: Scene[] = [") : text.index("/** 家人的照片")]
    scenes = []
    for chunk in re.split(r"\n  \{\n", block)[1:]:
        def find(name: str) -> str | None:
            m = re.search(rf"{name}: '([^']+)'", chunk)
            return m.group(1) if m else None

        scene = {"id": find("id"), "photo": find("photo"), "interior": find("interior")}
        for field in FIELDS:
            m = re.search(rf"{field}: \[(.*?)\n    \]", chunk, re.S)
            if m:
                scene[field] = [
                    dict(zip("xywh", map(float, n)))
                    for n in re.findall(r"x: ([\d.]+), y: ([\d.]+), w: ([\d.]+), h: ([\d.]+)", m.group(1))
                ]
                continue
            m = re.search(rf"{field}: \{{ x: ([\d.]+), y: ([\d.]+)(?:, w: ([\d.]+), h: ([\d.]+))? \}}", chunk)
            if m:
                x, y, w, h = m.groups()
                scene[field] = {"x": float(x), "y": float(y), "w": float(w or 2), "h": float(h or 2)}
        scenes.append(scene)
    return scenes


def draw(scene: dict, out_dir: Path) -> Path:
    image = Image.open(PUBLIC / scene["photo"].lstrip("/")).convert("RGB")
    width, height = image.size
    canvas = Image.new("RGB", (width * 2, height * 2))
    canvas.paste(image.resize((width * 2, height * 2), Image.LANCZOS), (0, 0))
    dr = ImageDraw.Draw(canvas, "RGBA")
    for i in range(1, 20):
        x, y = i * width * 2 // 20, i * height * 2 // 20
        dr.line([(x, 0), (x, height * 2)], fill=(0, 255, 255, 90))
        dr.line([(0, y), (width * 2, y)], fill=(0, 255, 255, 90))
    scale = width * 2 / 100

    def box(spot: dict, color=(255, 210, 120, 235)) -> None:
        dr.rectangle(
            [spot["x"] * scale, spot["y"] * scale, (spot["x"] + spot["w"]) * scale, (spot["y"] + spot["h"]) * scale],
            outline=color,
            width=3,
        )

    for spot in scene.get("lights", []):
        box(spot)
    if isinstance(scene.get("lamp"), dict):
        box(scene["lamp"], (255, 120, 120, 235))
    if isinstance(scene.get("candle"), dict):
        c = scene["candle"]
        dr.ellipse([c["x"] * scale - 8, c["y"] * scale - 8, c["x"] * scale + 8, c["y"] * scale + 8], outline=(120, 255, 160, 235), width=3)

    out = out_dir / f"{scene['id']}.jpg"
    canvas.save(out, quality=88)
    return out


def main() -> int:
    out_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/tmp/memorylink-scene-check")
    out_dir.mkdir(parents=True, exist_ok=True)
    scenes = parse_scenes(DATA.read_text())
    for scene in scenes:
        print(draw(scene, out_dir))
    print(f"\n共 {len(scenes)} 个场景。黄框=窗灯，红框=门口灯，绿点=长明灯。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

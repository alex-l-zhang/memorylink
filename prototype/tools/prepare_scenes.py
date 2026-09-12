#!/usr/bin/env python3
"""把一张场景照片裁成原型用的 1600×900（16:9）。

原型里的坐标系是"照片百分比"：data.ts 中的 lights / lamp / candle 锚点都按百分比记录，
所以只要底图统一裁成 16:9，"按比例铺满画面"就等于"不裁切"，锚点不需要换算。

用法：
    python3 prototype/tools/prepare_scenes.py 源图.jpg prototype/public/scenes/lake-study.jpg
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

WIDTH, HEIGHT = 1600, 900
RATIO = WIDTH / HEIGHT


def crop_16_9(path: Path) -> Image.Image:
    image = Image.open(path).convert("RGB")
    width, height = image.size
    if width / height > RATIO:
        new_width = int(height * RATIO)
        left = (width - new_width) // 2
        image = image.crop((left, 0, left + new_width, height))
    else:
        new_height = int(width / RATIO)
        top = (height - new_height) // 2
        image = image.crop((0, top, width, top + new_height))
    return image.resize((WIDTH, HEIGHT), Image.LANCZOS)


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    src, dst = Path(sys.argv[1]), Path(sys.argv[2])
    dst.parent.mkdir(parents=True, exist_ok=True)
    crop_16_9(src).save(dst, "JPEG", quality=88, optimize=True)
    print(f"{src} -> {dst} ({dst.stat().st_size // 1024} KB)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

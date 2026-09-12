#!/usr/bin/env python3
"""场景照片客观打分：把《场景素材生成描述》里的验收清单变成可跑的指标。

用来回答一个具体问题：一张候选图"看起来差"，到底差在哪、差多少。
它不判断美丑，只量能被量化的部分（影调、质感、冷暖、镜面度、天空是否发白）。
构图类指标（房子占比、窗户是否清晰、门在哪）仍需人工或 scene_check.py 目视确认。
阈值是按"屋外场景照片"标定的，拿它量室内的 `in-*.jpg` 会大面积报不合格，属正常。

用法：
    python3 prototype/tools/scene_grade.py 图1.jpg [图2.png ...]
    python3 prototype/tools/scene_grade.py --baseline prototype/public/scenes/lake-study.jpg 候选.png
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
from PIL import Image

# 阈值取自"当前首页图"（lake-study.jpg，1600×900）的实测值，向下留一档余量。
# 注意：局部细节的能量与分辨率相关，两张不同尺寸的图不能直接比——脚本会把所有图
# 统一缩到 1600 宽再算这一项（其余指标与分辨率无关，按原图算）。
NORM_WIDTH = 1600
REF = {
    "local_detail": 13.0,   # 首页图 16.3（统一到 1600 宽后）
    "clip_low": 0.5,        # 首页图 0.03%
    "dark_lum": 15.0,       # 最暗 20% 像素的平均亮度；首页图 25.1
    "dark_detail": 8.0,     # 最暗 20% 像素里的纹理能量；首页图 10.1
    "blue_ratio": 0.65,     # 偏蓝像素的 (B-G)/(B-R)；首页图 0.36（青蓝），越高越偏紫
    "clip_high": 0.5,       # 首页图 0.01%
    "sky_sat": 0.12,        # 首页图 0.16
    "mirror": 0.55,         # 越低越好（越高=水面越像镜子）；首页图 0.27
}


def box_blur(x: np.ndarray, k: int = 5) -> np.ndarray:
    pad = k // 2
    xp = np.pad(x, pad, mode="edge")
    c = np.cumsum(np.cumsum(xp, 0), 1)
    c = np.pad(c, ((1, 0), (1, 0)))
    h, w = x.shape
    return (c[k:k + h, k:k + w] - c[0:h, k:k + w] - c[k:k + h, 0:w] + c[0:h, 0:w]) / (k * k)


def mirror_score(lum: np.ndarray) -> float:
    """水面镜面度：在画面中部找一条最对称的水平线，取上下翻转的相关系数。"""
    h, w = lum.shape
    band = 120
    best = -1.0
    lo, hi = int(h * 0.30), int(h * 0.62)
    for y in range(lo, hi, 8):
        top = lum[y - band:y, :]
        bot = lum[y:y + band, :][::-1, :]
        if top.shape != bot.shape:
            continue
        a = top.ravel() - top.mean()
        b = bot.ravel() - bot.mean()
        denom = np.sqrt((a * a).sum() * (b * b).sum())
        if denom <= 1e-6:
            continue
        best = max(best, float((a * b).sum() / denom))
    return best


def grade(path: Path) -> dict:
    im = Image.open(path).convert("RGB")
    w, h = im.size
    a = np.asarray(im).astype(np.float32) / 255.0
    lum = 0.2126 * a[..., 0] + 0.7152 * a[..., 1] + 0.0722 * a[..., 2]
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    sat = a.max(-1) - a.min(-1)

    sky = slice(0, h // 7)
    warm = (r - b > 0.06) & (lum > 0.35)

    # 局部细节：统一缩到 1600 宽再量，避免"大图看起来细节多"的假象。
    if w != NORM_WIDTH:
        small = im.resize((NORM_WIDTH, max(1, round(h * NORM_WIDTH / w))), Image.LANCZOS)
        sa = np.asarray(small).astype(np.float32) / 255.0
        slum = 0.2126 * sa[..., 0] + 0.7152 * sa[..., 1] + 0.0722 * sa[..., 2]
    else:
        slum = lum
    sharp = slum - box_blur(slum, 5)
    local_detail = sharp.std() * 255

    # 暗部：整张图偏暗会让"死黑占比"虚高，所以另算"最暗 20% 区域还有多少亮度和纹理"。
    dark = slum <= np.percentile(slum, 20)
    dark_lum = slum[dark].mean() * 255
    dark_detail = sharp[dark].std() * 255

    # 冷色色相：自然的暮色阴影是青蓝（G 接近 B），紫蓝是模型自己造出来的颜色。
    bg = b - g
    blue = (b - r) >= np.percentile(b - r, 90)
    blue_ratio = bg[blue].mean() / max(1e-6, (b - r)[blue].mean())

    return {
        "file": path.name,
        "size": f"{w}×{h}",
        "ratio": w / h,
        "long_side": max(w, h),
        "lum_mean": lum.mean() * 255,
        "lum_std": lum.std() * 255,
        "clip_low": (lum < 0.02).mean() * 100,
        "clip_high": (lum > 0.98).mean() * 100,
        "p1": np.percentile(lum, 1) * 255,
        "p99": np.percentile(lum, 99) * 255,
        "local_detail": local_detail,
        "dark_lum": dark_lum,
        "dark_detail": dark_detail,
        "blue_ratio": blue_ratio,
        "detail_scale": "1600 宽" if w != NORM_WIDTH else "原生",
        "sky_sat": sat[sky].mean(),
        "sky_blown": (lum[sky] > 0.90).mean() * 100,
        "warm_frac": warm.mean() * 100,
        "rb_top": (r[sky] - b[sky]).mean(),
        "rb_mid": (r[h // 4:h // 2] - b[h // 4:h // 2]).mean(),
        "rb_bot": (r[h // 2:] - b[h // 2:]).mean(),
        "mirror": mirror_score(lum),
    }


def verdicts(m: dict) -> list[tuple[str, str, str]]:
    ok, warn, bad = "通过", "注意", "不合格"
    out = []
    if abs(m["ratio"] - 16 / 9) > 0.02:
        out.append(("画面比例", bad, f"{m['ratio']:.2f}:1，要求 16:9（{m['size']}）"))
    else:
        out.append(("画面比例", ok, f"16:9，{m['size']}"))

    if m["size"] == "1600×900":
        out.append(("分辨率", ok, "1600×900（已是产品成品尺寸）"))
    elif m["long_side"] < 1920:
        out.append(("分辨率", warn, f"长边 {m['long_side']}，出图建议 ≥1920"))
    else:
        out.append(("分辨率", ok, f"长边 {m['long_side']}"))

    d = m["local_detail"]
    if d < REF["local_detail"]:
        out.append(("质感层次", bad if d < REF["local_detail"] * 0.85 else warn,
                    f"局部细节 {d:.1f}（首页图 16.3，均已统一到 1600 宽），偏低 → 画面发糊/塑料感"))
    else:
        out.append(("质感层次", ok, f"局部细节 {d:.1f}（首页图 16.3，均已统一到 1600 宽）"))

    if m["clip_low"] > REF["clip_low"]:
        out.append(("暗部", bad, f"{m['clip_low']:.2f}% 像素压成死黑（首页图 0.03%）"))
    else:
        out.append(("暗部", ok, f"死黑 {m['clip_low']:.2f}%"))

    if m["dark_lum"] < REF["dark_lum"]:
        out.append(("暗部层次", bad,
                    f"最暗 20% 区域平均亮度只有 {m['dark_lum']:.1f}/255（首页图 25.1），"
                    f"纹理能量 {m['dark_detail']:.1f}（首页图 10.1）→ 暗部是剪影、没有信息"))
    else:
        out.append(("暗部层次", ok,
                    f"最暗 20% 区域平均亮度 {m['dark_lum']:.1f}/255，纹理能量 {m['dark_detail']:.1f}"))

    br_ratio = m["blue_ratio"]
    if br_ratio > REF["blue_ratio"]:
        out.append(("冷色色相", warn,
                    f"(B-G)/(B-R)={br_ratio:.2f}（首页图 0.36），冷色偏紫蓝；"
                    f"自然的暮色阴影应是青蓝（G 接近 B）"))
    else:
        out.append(("冷色色相", ok, f"(B-G)/(B-R)={br_ratio:.2f}（首页图 0.36），青蓝"))

    if m["p99"] > 240:
        out.append(("高光", warn, f"亮度 99 分位 {m['p99']:.0f}/255，高光接近溢出的区域偏多"))
    else:
        out.append(("高光", ok, f"亮度 99 分位 {m['p99']:.0f}/255"))

    if m["sky_sat"] < REF["sky_sat"]:
        out.append(("天空色彩", warn if m["sky_sat"] > 0.09 else bad,
                    f"天空区饱和度 {m['sky_sat']:.3f}（首页图 0.160），发白/发灰"))
    else:
        out.append(("天空色彩", ok, f"天空区饱和度 {m['sky_sat']:.3f}"))

    rbs = [m["rb_top"], m["rb_mid"], m["rb_bot"]]
    if all(x > 0.02 for x in rbs):
        out.append(("冷暖关系", warn,
                    f"全画面偏暖（R-B {rbs[0]:+.3f}/{rbs[1]:+.3f}/{rbs[2]:+.3f}），缺少冷色环境与暖光的对撞"))
    elif all(x < -0.02 for x in rbs) and m["warm_frac"] < 3:
        out.append(("冷暖关系", warn, "全画面偏冷，画面里几乎没有暖光落点"))
    else:
        out.append(("冷暖关系", ok,
                    f"冷底 + 暖光点（R-B {rbs[0]:+.3f}/{rbs[1]:+.3f}/{rbs[2]:+.3f}，暖光面积 {m['warm_frac']:.1f}%）"))

    if m["mirror"] > REF["mirror"]:
        out.append(("水面", warn, f"镜面度 {m['mirror']:.2f}（越接近 1 越像镜子），真实湖面应有细碎波纹"))
    else:
        out.append(("水面", ok, f"镜面度 {m['mirror']:.2f}"))
    return out


def main(argv: list[str]) -> int:
    paths = [Path(p) for p in argv[1:]]
    if not paths:
        print(__doc__)
        return 1
    for p in paths:
        if not p.exists():
            print(f"找不到文件：{p}")
            continue
        m = grade(p)
        print(f"\n=== {m['file']} · {m['size']} ===")
        print(f"亮度均值 {m['lum_mean']:.1f} | 亮度标准差 {m['lum_std']:.1f} | "
              f"亮度 p1 {m['p1']:.0f} / p99 {m['p99']:.0f}")
        for name, level, detail in verdicts(m):
            print(f"  [{level}] {name}：{detail}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))

#!/usr/bin/env python3
"""
从《大众家常菜大全-开源版.pdf》导出每道菜的成品图，并生成 cover_image 回填 SQL。

用法：
    python scripts/extract_recipe_images.py

输出：
    docs/recipes/images/recipe_001.jpg ... recipe_579.jpg
    docs/sql/05_recipe_cover_images.sql
"""

import argparse
import os
import re
import sys
from pathlib import Path

from PIL import Image
from pypdf import PdfReader


REPO_ROOT = Path(__file__).resolve().parent.parent


def find_recipe_pages(reader: PdfReader):
    """定位每道菜的起始页：页首形如 `1. 菜名` 且页面包含成品图。"""
    recipes = []
    title_re = re.compile(r"^(\d+)\.\s+(.+)$")
    for page_no, page in enumerate(reader.pages, start=1):
        if not page.images:
            continue
        text = page.extract_text() or ""
        first_line = next((ln.strip() for ln in text.splitlines() if ln.strip()), "")
        m = title_re.match(first_line)
        if not m:
            continue
        recipes.append(
            {
                "seq": int(m.group(1)),
                "name": m.group(2).strip(),
                "page": page_no,
                "image": page.images[0],
            }
        )
    recipes.sort(key=lambda r: r["seq"])
    return recipes


def extract_images(recipes, out_dir: Path):
    out_dir.mkdir(parents=True, exist_ok=True)
    saved = []
    for r in recipes:
        img = r["image"].image
        if img.mode != "RGB":
            img = img.convert("RGB")
        path = out_dir / f"recipe_{r['seq']:03d}.jpg"
        img.save(path, "JPEG", quality=90)
        saved.append((r["seq"], path.name))
    return saved


def generate_sql(recipes, out_path: Path):
    lines = [
        "-- ========================================================",
        "-- 菜谱封面图回填（由 docs/recipes/大众家常菜大全-开源版.pdf 导出）",
        "-- 按菜名更新 cover_image，图片由后端 /static/recipes/** 提供",
        "-- ========================================================",
        "USE jtcsm;",
        "",
    ]
    for r in recipes:
        name = r["name"].replace("'", "''")
        lines.append(
            f"UPDATE recipe SET cover_image = '/static/recipes/recipe_{r['seq']:03d}.jpg' "
            f"WHERE name = '{name}';"
        )
    lines.append("")
    out_path.write_text("\n".join(lines), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description="导出 PDF 中的菜谱成品图")
    parser.add_argument(
        "--pdf",
        default=REPO_ROOT / "docs" / "recipes" / "大众家常菜大全-开源版.pdf",
        help="大众家常菜大全 PDF 路径",
    )
    parser.add_argument(
        "--images-dir",
        default=REPO_ROOT / "docs" / "recipes" / "images",
        help="图片输出目录",
    )
    parser.add_argument(
        "--sql",
        default=REPO_ROOT / "docs" / "sql" / "05_recipe_cover_images.sql",
        help="cover_image 回填 SQL 输出路径",
    )
    args = parser.parse_args()

    pdf_path = Path(args.pdf)
    if not pdf_path.exists():
        print(f"PDF 不存在: {pdf_path}", file=sys.stderr)
        sys.exit(1)

    reader = PdfReader(str(pdf_path))
    recipes = find_recipe_pages(reader)
    if not recipes:
        print("未识别到任何菜谱页", file=sys.stderr)
        sys.exit(1)

    seqs = [r["seq"] for r in recipes]
    if seqs != list(range(1, len(recipes) + 1)):
        print("菜谱序号不连续，请人工检查 PDF", file=sys.stderr)
        sys.exit(1)

    saved = extract_images(recipes, Path(args.images_dir))
    generate_sql(recipes, Path(args.sql))

    print(f"共导出 {len(saved)} 张成品图 -> {args.images_dir}")
    print(f"生成 SQL -> {args.sql}")


if __name__ == "__main__":
    main()

# 大众菜谱 PDF 知识库

本项目使用的菜谱 PDF 从 GitHub 公开仓库整理生成，每道菜包含成品图、食材、备料和详细步骤。

## 文件清单

| 文件 | 菜谱数 | 页数 | 大小 | 内容 |
| --- | ---: | ---: | ---: | --- |
| 大众家常菜大全-开源版.pdf | 579 道 | 1047 | 42 MB | 每道菜含成品图、食材、备料、步骤、技巧、选购建议 |

## 数据来源

- GitHub 仓库：[tuozhekongqi/A-Bowl-of-Home](https://github.com/tuozhekongqi/A-Bowl-of-Home)
- 原仓库共 593 道家常菜，其中 14 道源数据没有成品图，未纳入本 PDF；本 PDF 保留的 579 道全部带图。
- 原仓库为公开 GitHub 仓库，但未声明正式开源许可证（无 LICENSE 文件）。用于内部学习与开发验证没有问题；若对外发布或商用，请先与作者确认授权。

## 使用说明

- PDF 每道菜从新页开始，包含菜品图与完整做法文字，可直接用于阅读或转图片素材。
- 如需接入 RAG 文本检索，本 PDF 已含文字层，可直接提取文本；菜品图也可独立导出用于小程序展示。

## 菜谱图片导出

`scripts/extract_recipe_images.py` 会从 PDF 中导出 579 张成品图到 `docs/recipes/images/recipe_001.jpg` ... `recipe_579.jpg`，并生成 `docs/sql/05_recipe_cover_images.sql` 按菜名回填 `recipe.cover_image`。

```bash
python scripts/extract_recipe_images.py
mysql -u root -p jtcsm < docs/sql/05_recipe_cover_images.sql
```

图片由后端通过 `/static/recipes/**` 提供，小程序端会将相对路径拼上 API 基础地址后展示。

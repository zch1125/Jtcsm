# -*- coding: utf-8 -*-
"""分析 douguo.com 详情页 HTML 结构"""
import sys, io, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

with open('D:\\Project\\Jtcsm\\scripts\\test_detail.html', 'r', encoding='utf-8') as f:
    html = f.read()

# 材料表格
m = re.search(r'<div class="metarial">.*?</div>\s*</div>\s*</div>', html, re.DOTALL)
if m:
    print("=== 材料表格 ===")
    print(m.group()[:2000])

# 每一行食材
print("\n=== 食材行 ===")
for m in re.finditer(r'<tr>.*?</tr>', html, re.DOTALL):
    tr = m.group()
    if 'lirre' in tr or 'lir' in tr:
        print(tr[:200])

# 步骤
print("\n=== 步骤 ===")
for m in re.finditer(r'<div class="step">.*?</div>\s*</div>', html, re.DOTALL):
    print(m.group()[:400])
    print("---")

# 封面
print("\n=== 封面图 ===")
m = re.search(r'class="menucover"', html)
if m:
    start = max(0, m.start() - 300)
    print(html[start:m.end()+50])

# h1
m = re.search(r'<h1[^>]*>(.*?)</h1>', html)
if m:
    print(f"\n=== 标题 ===\n{m.group(1)}")

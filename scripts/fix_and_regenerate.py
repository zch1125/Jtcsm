import sys, io, re, pymysql
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
conn = pymysql.connect(host="localhost", port=3306, user="root", password="Pass1234", database="jtcsm", charset="utf8mb4")
cur = conn.cursor()

# 修复步骤前导数字
cur.execute("SELECT id, content FROM recipe_step")
rows = cur.fetchall()
fixed = 0
for rid, content in rows:
    # 去掉前导数字: "1丝瓜..." -> "丝瓜...", "10. xxx" -> "xxx"
    cleaned = re.sub(r"^\d+[.、．]?\s*", "", content).strip()
    if cleaned != content:
        cur.execute("UPDATE recipe_step SET content = %s WHERE id = %s", (cleaned, rid))
        fixed += 1
conn.commit()
print("步骤修复完成: %d 条" % fixed)

# 验证
cur.execute("SELECT id, content FROM recipe_step WHERE content REGEXP %s", ("^[0-9]+",))
rem = cur.fetchall()
print("仍有前导数字: %d 条" % len(rem))
if rem:
    for r in rem[:5]:
        print("  [%d] %s" % (r[0], r[1][:30]))

# 重新生成 MD 文档
cur.execute("SELECT id, name, cover_image, description, cuisine, difficulty, cook_method, cook_time, calories, is_vip_only FROM recipe WHERE status = 1 ORDER BY cuisine, name")
recipes = cur.fetchall()

md_lines = []
md_lines.append("# 食谱知识库\n")
md_lines.append("> 共 %d 道菜谱，覆盖家常菜、川菜、粤菜、湘菜等各类菜系\n" % len(recipes))
md_lines.append("---\n")

for r in recipes:
    rid, name, cover, desc, cuisine, difficulty, cook_method, cook_time, calories, is_vip = r
    md_lines.append("## %s\n" % name)
    md_lines.append("| 属性 | 值 |")
    md_lines.append("|------|-----|")
    md_lines.append("| **菜系** | %s |" % (cuisine or "未分类"))
    md_lines.append("| **难度** | %s |" % (difficulty or "未标注"))
    if cook_method:
        md_lines.append("| **烹饪方式** | %s |" % cook_method)
    if cook_time:
        md_lines.append("| **烹饪时间** | %d 分钟 |" % cook_time)
    if calories:
        md_lines.append("| **热量** | %d 千卡 |" % calories)
    md_lines.append("")
    if desc:
        md_lines.append("### 简介\n")
        md_lines.append("%s\n" % desc)
    
    # 食材
    cur.execute("SELECT name, amount FROM recipe_ingredient WHERE recipe_id = %s ORDER BY sort_order", (rid,))
    ingredients = cur.fetchall()
    if ingredients:
        md_lines.append("### 食材\n")
        md_lines.append("| 食材 | 用量 |")
        md_lines.append("|------|------|")
        for ing_name, ing_amount in ingredients:
            md_lines.append("| %s | %s |" % (ing_name, ing_amount or "-"))
        md_lines.append("")
    
    # 步骤（已修复前导数字）
    cur.execute("SELECT step_no, content, duration FROM recipe_step WHERE recipe_id = %s ORDER BY step_no", (rid,))
    steps = cur.fetchall()
    if steps:
        md_lines.append("### 步骤\n")
        for sn, content, duration in steps:
            dur_str = " (%d 分钟)" % duration if duration else ""
            # content 已经过前导数字清理
            md_lines.append("%d. %s%s" % (sn, content, dur_str))
        md_lines.append("")
    
    md_lines.append("---\n")

cur.close()
conn.close()

output = "\n".join(md_lines)
with open("D:\\Project\\Jtcsm\\docs\\recipes_knowledge_base.md", "w", encoding="utf-8") as f:
    f.write(output)

print("MD 文档已重新生成: docs/recipes_knowledge_base.md (%d 道菜谱, %d 字)" % (len(recipes), len(output)))

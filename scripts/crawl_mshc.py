# -*- coding: utf-8 -*-
"""爬取 meishichina.com (美食天下) 食谱"""
import sys, io, re, time
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
import urllib3
from bs4 import BeautifulSoup
import pymysql

urllib3.disable_warnings()
http = urllib3.PoolManager(cert_reqs="CERT_NONE", assert_hostname=False)
HEADERS = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"}
MAX_RECIPES = 50
DELAY = 2.0
EXISTING_NAMES = set()
DB = {"host": "localhost", "port": 3306, "user": "root", "password": "Pass1234", "database": "jtcsm", "charset": "utf8mb4"}

def get_db():
    return pymysql.connect(**DB)

def load_existing():
    global EXISTING_NAMES
    conn = get_db(); cur = conn.cursor()
    cur.execute("SELECT name FROM recipe WHERE status = 1")
    for row in cur.fetchall():
        EXISTING_NAMES.add(row[0].strip())
    cur.close(); conn.close()
    print("数据库已有 %d 道菜谱" % len(EXISTING_NAMES))

def fetch(url):
    for att in range(3):
        try:
            r = http.request("GET", url, headers=HEADERS, timeout=15)
            if r.status == 200:
                return r.data
            time.sleep(5)
        except:
            time.sleep(5)
    return None

def collect_ids():
    ids = set()
    print("=== 收集菜谱ID ===")
    for src in ["https://www.meishichina.com",
                "https://home.meishichina.com/recipe/recai/",
                "https://home.meishichina.com/recipe/liangcai/",
                "https://home.meishichina.com/recipe/tang/",
                "https://home.meishichina.com/recipe/zhushi/",
                "https://home.meishichina.com/recipe-list-view-elite.html"]:
        html = fetch(src)
        if html:
            soup = BeautifulSoup(html, "html.parser")
            before = len(ids)
            for a in soup.find_all("a", href=True):
                m = re.search(r"recipe-(\d+)\.html", a["href"])
                if m:
                    ids.add(int(m.group(1)))
            print("  %s: +%d" % (src.split("/")[-1] or "home", len(ids)-before))
        time.sleep(1)
    print("总共 %d 个菜谱ID" % len(ids))
    return sorted(ids)

def parse_detail(recipe_id):
    url = "https://home.meishichina.com/recipe-%d.html" % recipe_id
    html = fetch(url)
    if not html:
        return None
    soup = BeautifulSoup(html, "html.parser")
    title_el = soup.select_one("h1.title")
    if not title_el:
        return None
    name = title_el.text.strip()
    if not name or name == "菜谱" or name in EXISTING_NAMES:
        return None
    desc = ""
    msg_el = soup.select_one(".recipemessage")
    if msg_el:
        desc = msg_el.text.strip().strip("\u201c").strip("\u201d").strip()[:300]
    cover = ""
    for img in soup.find_all("img"):
        src = img.get("src", "")
        if src and "http" in src and "step" not in src.lower():
            if not cover:
                cover = src
    cook_method = ""
    cook_time = 30
    difficulty = "普通"
    info_el = soup.select_one(".recipeCategory_sub_R.mt30.clear")
    if info_el:
        text = info_el.get_text("\n", strip=True)
        parts = text.split("\n")
        for i, p in enumerate(parts):
            if p == "工艺" and i+1 < len(parts):
                cook_method = parts[i+1]
            elif p == "耗时" and i+1 < len(parts):
                nums = re.findall(r"\d+", parts[i+1])
                if nums:
                    cook_time = int(nums[0])
            elif p == "难度" and i+1 < len(parts):
                d = parts[i+1]
                if "简" in d or "初" in d:
                    difficulty = "简单"
                elif "高" in d or "难" in d:
                    difficulty = "困难"
                else:
                    difficulty = "普通"
    ingredients = []
    for ing_div in soup.select(".recipeCategory_sub_R.clear"):
        a_el = ing_div.find("a")
        name_text = a_el.text.strip() if a_el else ""
        if name_text and name_text not in ("主料", "辅料", "调料", "配料", "食材"):
            amount_text = ""
            for child in ing_div.children:
                if child.name is None and child.strip():
                    amount_text = child.strip()
                    break
            spans = ing_div.find_all("span")
            if not amount_text and len(spans) > 1:
                amount_text = spans[-1].text.strip()
            if not amount_text:
                raw_parts = ing_div.get_text("\n", strip=True).split("\n")
                if len(raw_parts) > 1:
                    amount_text = raw_parts[-1].strip()
            ingredients.append((name_text, amount_text))
    steps = []
    for i, se in enumerate(soup.select(".recipeStep_word"), 1):
        text = se.get_text(strip=True)
        if text:
            steps.append({"stepNo": i, "content": text, "duration": 0})
    if not ingredients and not steps:
        return None
    return {"name": name, "cover": cover, "description": desc,
            "cuisine": "家常菜", "difficulty": difficulty,
            "cook_method": cook_method, "cook_time": cook_time,
            "calories": 0, "is_vip_only": False,
            "ingredients": ingredients, "steps": steps}

def insert_recipe(conn, recipe):
    cur = conn.cursor()
    try:
        cur.execute(
            "INSERT INTO recipe (name, cover_image, description, cuisine, difficulty, cook_method, cook_time, calories, is_vip_only, status, source) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, 1, 'crawl_mshc')",
            (recipe["name"], recipe["cover"], recipe["description"],
             recipe["cuisine"], recipe["difficulty"], recipe["cook_method"],
             recipe["cook_time"], recipe["calories"], 0))
        rid = cur.lastrowid
        for i, (n, a) in enumerate(recipe["ingredients"]):
            cur.execute("INSERT IGNORE INTO ingredient (name, category) VALUES (%s, '爬取')", (n,))
            cur.execute("SELECT id FROM ingredient WHERE name = %s", (n,))
            row = cur.fetchone()
            iid = row[0] if row else None
            cur.execute("INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES (%s,%s,%s,%s,%s)", (rid, iid, n, a, i+1))
        for s in recipe["steps"]:
            cur.execute("INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES (%s,%s,%s,%s)", (rid, s["stepNo"], s["content"], s["duration"]))
        conn.commit()
        EXISTING_NAMES.add(recipe["name"])
        return rid
    except Exception as e:
        conn.rollback()
        print("    入库失败: %s" % e)
        return None
    finally:
        cur.close()

def main():
    load_existing()
    all_ids = collect_ids()
    if not all_ids:
        print("无菜谱ID"); return
    print("=== 开始爬取 ===")
    conn = get_db()
    inserted = 0; crawled = 0
    for rid in all_ids:
        if inserted >= MAX_RECIPES:
            break
        crawled += 1
        print("[%d] %d..." % (crawled, rid), end=" ")
        r = parse_detail(rid)
        if not r:
            print("跳过")
            time.sleep(1)
            continue
        print("「%s」(%d种食材, %d步)" % (r["name"], len(r["ingredients"]), len(r["steps"])))
        nid = insert_recipe(conn, r)
        if nid:
            inserted += 1
            print("  -> 入库 (id=%d)" % nid)
        else:
            print("  -> 失败")
        time.sleep(DELAY)
    conn.close()
    print("完成: 尝试%d, 新入库%d" % (crawled, inserted))

if __name__ == "__main__":
    main()

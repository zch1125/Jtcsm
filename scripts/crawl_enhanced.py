"""
增强爬虫：从 meishichina.com 和 xiachufang.com 爬取更多食谱
"""
import sys, io, re, time, json, os
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
import urllib3
from bs4 import BeautifulSoup
import pymysql

urllib3.disable_warnings()
http = urllib3.PoolManager(cert_reqs="CERT_NONE", assert_hostname=False,
                           headers={"Content-Type": "application/json"})
HEADERS = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"}
DELAY = 2.0
EXISTING_NAMES = set()
TARGET_NEW = 200  # 目标新增食谱数
DB = {"host": "localhost", "port": 3306, "user": "root", "password": "Pass1234", "database": "jtcsm", "charset": "utf8mb4"}

def log(msg):
    print(msg)

def get_db():
    return pymysql.connect(**DB)

# ========== 采集 ID ==========
def collect_mshc_ids():
    """从 meishichina.com 收集菜谱ID"""
    ids = set()
    sources = [
        "https://www.meishichina.com",
        "https://home.meishichina.com/recipe.html",
        "https://home.meishichina.com/recipe-list-view-elite.html",
        "https://home.meishichina.com/show-top-type-recipe-order-pop.html",
    ]
    cats = ["recai", "liangcai", "tang", "zhushi", "hongbei", "xiaochi", "zaocan",
            "xiaji", "gaoyanzhi", "jiangpaoyancai", "zizhishicai"]
    sources += ["https://home.meishichina.com/recipe/%s/" % c for c in cats]

    for url in sources:
        try:
            r = http.request("GET", url, headers=HEADERS, timeout=15)
            soup = BeautifulSoup(r.data, "html.parser")
            for a in soup.find_all("a", href=True):
                m = re.search(r"recipe-(\d+)\.html", a["href"])
                if m:
                    ids.add(int(m.group(1)))
            time.sleep(0.5)
        except:
            pass
    log("  meishichina.com: 收集到 %d 个 ID" % len(ids))
    return ids

def collect_xcf_ids():
    """从 xiachufang.com 收集菜谱ID（探索页分页）"""
    ids = set()
    for page in range(1, 6):
        url = "https://www.xiachufang.com/explore/?page=%d" % page
        try:
            r = http.request("GET", url, headers=HEADERS, timeout=15)
            soup = BeautifulSoup(r.data, "html.parser")
            for a in soup.find_all("a", href=True):
                m = re.search(r"/recipe/(\d+)/", a["href"])
                if m:
                    ids.add(int(m.group(1)))
            log("  xiachufang.com page %d: %d 个" % (page, len(ids)))
            time.sleep(1)
        except Exception as e:
            log("  xiachufang page %d error: %s" % (page, e))
    log("  xiachufang.com: 收集到 %d 个 ID" % len(ids))
    return ids

# ========== 解析 meishichina.com ==========
def parse_mshc(recipe_id):
    """解析 meishichina.com 菜谱"""
    url = "https://home.meishichina.com/recipe-%d.html" % recipe_id
    r = http.request("GET", url, headers=HEADERS, timeout=15)
    if r.status != 200:
        return None
    soup = BeautifulSoup(r.data, "html.parser")
    title_el = soup.select_one("h1.title")
    if not title_el:
        return None
    name = title_el.text.strip()
    if not name or name == "菜谱" or name in EXISTING_NAMES:
        return None
    desc, cover = "", ""
    msg_el = soup.select_one(".recipemessage")
    if msg_el:
        desc = msg_el.text.strip().strip("\u201c").strip("\u201d").strip()[:300]
    for img in soup.find_all("img"):
        src = img.get("src", "")
        if src and "http" in src and "step" not in src.lower() and not cover:
            cover = src
    cook_method, cook_time, difficulty = "", 30, "普通"
    info_el = soup.select_one(".recipeCategory_sub_R.mt30.clear")
    if info_el:
        parts = info_el.get_text("\n", strip=True).split("\n")
        for i, p in enumerate(parts):
            if p == "工艺" and i+1 < len(parts): cook_method = parts[i+1]
            elif p == "耗时" and i+1 < len(parts):
                nums = re.findall(r"\d+", parts[i+1])
                if nums: cook_time = int(nums[0])
            elif p == "难度" and i+1 < len(parts):
                d = parts[i+1]
                difficulty = "简单" if ("简" in d or "初" in d) else ("困难" if ("高" in d or "难" in d) else "普通")
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
            if not amount_text:
                spans = ing_div.find_all("span")
                if len(spans) > 1:
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
            "source": "crawl_mshc", "ingredients": ingredients, "steps": steps}

# ========== 解析 xiachufang.com ==========
def parse_xcf(recipe_id):
    """解析 xiachufang.com 菜谱（JSON-LD格式）"""
    url = "https://www.xiachufang.com/recipe/%d/" % recipe_id
    try:
        r = http.request("GET", url, headers=HEADERS, timeout=15)
        if r.status != 200:
            return None
        html_text = r.data.decode("utf-8")
    except:
        return None

    soup = BeautifulSoup(html_text, "html.parser")
    
    # 提取 JSON-LD
    json_ld = None
    for script in soup.find_all("script", type="application/ld+json"):
        try:
            data = json.loads(script.string)
            if isinstance(data, dict) and data.get("@type") == "Recipe":
                json_ld = data
                break
        except:
            pass
    
    if not json_ld:
        return None
    
    name = json_ld.get("name", "")
    if not name or name in EXISTING_NAMES:
        return None
    
    desc = json_ld.get("description", "")[:300]
    
    # 食材
    ingredients = []
    for ing in json_ld.get("recipeIngredient", []):
        if ing:
            # 处理 "五花肉 500克" -> ("五花肉", "500克")
            ing = ing.strip()
            m = re.match(r"(\D+?)([\d.]+.*)", ing)
            if m:
                ingredients.append((m.group(1).strip(), m.group(2).strip()))
            else:
                ingredients.append((ing, ""))
    
    # 步骤
    steps = []
    instrs = json_ld.get("recipeInstructions", [])
    if isinstance(instrs, list):
        for i, step in enumerate(instrs, 1):
            text = ""
            if isinstance(step, dict):
                text = step.get("text", "")
            elif isinstance(step, str):
                text = step
            if text:
                steps.append({"stepNo": i, "content": text.strip(), "duration": 0})
    elif isinstance(instrs, str):
        steps.append({"stepNo": 1, "content": instrs.strip(), "duration": 0})
    
    if not ingredients and not steps:
        return None
    
    # 封面图
    cover = json_ld.get("image", "") or ""
    if isinstance(cover, dict):
        cover = cover.get("url", "")
    elif isinstance(cover, list):
        cover = cover[0] if cover else ""
    
    # 烹饪时间
    total_time = json_ld.get("totalTime", "")
    cook_time = 30
    if total_time:
        nums = re.findall(r"\d+", total_time)
        if nums:
            cook_time = int(nums[0])
    
    difficulty = json_ld.get("difficulty", "普通") or "普通"
    if "简单" in difficulty: difficulty = "简单"
    
    return {"name": name, "cover": cover, "description": desc,
            "cuisine": "家常菜", "difficulty": difficulty,
            "cook_method": "", "cook_time": cook_time,
            "source": "crawl_xcf", "ingredients": ingredients, "steps": steps}

# ========== 入库 ==========
def insert_recipe(conn, recipe):
    cur = conn.cursor()
    try:
        cur.execute(
            "INSERT INTO recipe (name, cover_image, description, cuisine, difficulty, cook_method, cook_time, calories, is_vip_only, status, source) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, 1, %s)",
            (recipe["name"], recipe["cover"], recipe["description"],
             recipe["cuisine"], recipe["difficulty"], recipe["cook_method"],
             recipe["cook_time"], 0, 0, recipe["source"]))
        rid = cur.lastrowid
        for i, (n, a) in enumerate(recipe["ingredients"]):
            cur.execute("INSERT IGNORE INTO ingredient (name, category) VALUES (%s, '爬取')", (n,))
            cur.execute("SELECT id FROM ingredient WHERE name = %s", (n,))
            row = cur.fetchone()
            iid = row[0] if row else None
            cur.execute("INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES (%s,%s,%s,%s,%s)",
                       (rid, iid, n, a, i+1))
        for s in recipe["steps"]:
            cur.execute("INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES (%s,%s,%s,%s)",
                       (rid, s["stepNo"], s["content"], s["duration"]))
        conn.commit()
        EXISTING_NAMES.add(recipe["name"])
        return rid
    except Exception as e:
        conn.rollback()
        log("    入库失败: %s" % e)
        return None
    finally:
        cur.close()

# ========== 主流程 ==========
def main():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT name FROM recipe WHERE status = 1")
    for row in cur.fetchall():
        EXISTING_NAMES.add(row[0].strip())
    cur.execute("SELECT COUNT(*) FROM recipe")
    cur_cnt = cur.fetchone()[0]
    cur.close()
    log("=== 现有食谱: %d 道 ===\n" % cur_cnt)

    # 收集ID
    log("=== 收集菜谱ID ===")
    mshc_ids = collect_mshc_ids()
    time.sleep(1)
    xcf_ids = collect_xcf_ids()
    
    all_ids = list(mshc_ids) + list(xcf_ids)
    all_ids = sorted(set(all_ids))
    log("\n总共 %d 个唯一菜谱ID（meishichina: %d, xiachufang: %d）" % (
        len(all_ids), len(mshc_ids), len(xcf_ids)))

    # 爬取
    log("\n=== 开始爬取（目标 %d 道新菜谱）===" % TARGET_NEW)
    inserted, crawled = 0, 0
    conn = get_db()

    for rid in all_ids:
        if inserted >= TARGET_NEW:
            break
        crawled += 1
        msg = "[%d] %d..." % (crawled, rid)
        print(msg, end=" ", flush=True)

        recipe = None
        if rid in mshc_ids:
            recipe = parse_mshc(rid)
        if not recipe and rid in xcf_ids:
            recipe = parse_xcf(rid)
        if not recipe:
            log("跳过")
            time.sleep(1)
            continue

        log("「%s」(%d种, %d步)" % (recipe["name"], len(recipe["ingredients"]), len(recipe["steps"])))
        nid = insert_recipe(conn, recipe)
        if nid:
            inserted += 1
            log("  -> id=%d" % nid)
        else:
            log("  -> 失败")
        time.sleep(DELAY)

    conn.close()
    
    # 统计最终结果
    conn = get_db(); cur = conn.cursor()
    cur.execute("SELECT COUNT(*) FROM recipe")
    total = cur.fetchone()[0]
    cur.execute("SELECT source, COUNT(*) FROM recipe GROUP BY source")
    log("\n=== 最终统计 ===")
    for row in cur.fetchall():
        log("  %s: %d" % row)
    log("  总计: %d 道菜谱" % total)
    log("  新增: %d 道" % (total - cur_cnt))
    cur.close(); conn.close()

if __name__ == "__main__":
    main()

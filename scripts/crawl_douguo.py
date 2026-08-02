# -*- coding: utf-8 -*-
"""
douguo.com 食谱爬虫
爬取豆果美食的菜谱，存入 MySQL，完成后触发 ES 同步
"""
import sys, io, re, time, hashlib
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

import requests
from bs4 import BeautifulSoup
import pymysql

requests.packages.urllib3.disable_warnings()

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
}

# MySQL 连接（从后端配置读取）
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': 'Pass1234',
    'database': 'jtcsm',
    'charset': 'utf8mb4',
}

# 爬取配置
MAX_RECIPES = 30        # 最多爬取数量
DELAY_SECS = 1.0        # 请求间隔（秒）

# 已存在的菜谱名缓存（避免重复）
EXISTING_NAMES = set()


def get_db():
    """获取数据库连接"""
    return pymysql.connect(**DB_CONFIG)


def load_existing_names():
    """加载已有菜谱名"""
    global EXISTING_NAMES
    try:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT name FROM recipe WHERE status = 1")
        for row in cur.fetchall():
            EXISTING_NAMES.add(row[0].strip())
        cur.close()
        conn.close()
        print(f"数据库已有 {len(EXISTING_NAMES)} 道菜谱")
    except Exception as e:
        print(f"加载已有菜谱失败: {e}")


def extract_recipe_ids_from_homepage():
    """从首页获取菜谱ID列表"""
    ids = set()
    try:
        r = requests.get('https://www.douguo.com', timeout=15, verify=False, headers=HEADERS)
        soup = BeautifulSoup(r.text, 'html.parser')
        for a in soup.select('a[href*="/cookbook/"]'):
            href = a.get('href', '')
            m = re.search(r'/cookbook/(\d+)', href)
            if m:
                ids.add(m.group(1))
    except Exception as e:
        print(f"首页获取菜谱ID失败: {e}")
    return list(ids)


def extract_recipe_ids_from_category():
    """从分类页获取更多菜谱ID"""
    ids = set()
    categories = ['/caipu/家常菜', '/zuixin', '/jingxuan']
    for cat in categories:
        try:
            url = f'https://www.douguo.com{cat}'
            r = requests.get(url, timeout=15, verify=False, headers=HEADERS)
            soup = BeautifulSoup(r.text, 'html.parser')
            for a in soup.select('a[href*="/cookbook/"]'):
                href = a.get('href', '')
                m = re.search(r'/cookbook/(\d+)', href)
                if m:
                    ids.add(m.group(1))
            time.sleep(0.5)
        except Exception as e:
            print(f"分类页 {cat} 失败: {e}")
    return list(ids)


def parse_detail(recipe_id):
    """解析单个菜谱详情页"""
    url = f'https://www.douguo.com/cookbook/{recipe_id}.html'
    r = requests.get(url, timeout=15, verify=False, headers=HEADERS)
    if r.status_code != 200:
        print(f"  [{recipe_id}] HTTP {r.status_code}")
        return None

    soup = BeautifulSoup(r.text, 'html.parser')

    # 标题
    h1 = soup.select_one('h1')
    if not h1:
        return None
    name = h1.text.strip()
    if not name or name in EXISTING_NAMES:
        return None

    # 封面图
    cover = ''
    img = soup.select_one('img.menucover')
    if img:
        cover = img.get('src', '')

    # 简介
    description = ''
    meta_desc = soup.select_one('meta[name="description"]')
    if meta_desc:
        desc_content = meta_desc.get('content', '')
        # 取第一句作为简介
        description = desc_content.split('。')[0] if '。' in desc_content else desc_content[:200]

    # 菜系和难度（从标签推断）
    cuisine = '家常菜'
    difficulty = '普通'
    cook_method = ''

    # 食材
    ingredients = []
    table = soup.select_one('table.retamr')
    if table:
        for td in table.select('td'):
            name_el = td.select_one('span.scname')
            amount_el = td.select_one('span.scnum')
            if name_el:
                ing_name = name_el.text.strip()
                ing_amount = amount_el.text.strip() if amount_el else ''
                if ing_name:
                    ingredients.append((ing_name, ing_amount))

    # 烹饪时间（从步骤推断总的）
    total_duration = 0

    # 步骤
    steps = []
    step_divs = soup.select('div.stepcont')
    for i, step_div in enumerate(step_divs, 1):
        # 步骤文本
        txt_el = step_div.select_one('div.steptxt, div.stepinfo, p')
        step_text = ''
        if txt_el:
            step_text = txt_el.text.strip()
        if not step_text:
            # fallback: 提取所有非空文本
            step_text = step_div.get_text(strip=True)
            # 去掉图片链接等噪声
            step_text = re.sub(r'http\S+', '', step_text).strip()

        if step_text:
            steps.append({'stepNo': i, 'content': step_text, 'duration': 0})

    # 如果步骤太少，尝试用其他方式提取
    if len(steps) < 2:
        alt_steps = soup.select('div.step')
        texts = []
        for s in alt_steps:
            t = s.get_text(strip=True)
            if t and len(t) > 5 and '做法' not in t:
                texts.append(t)
        if len(texts) > len(steps):
            steps = [{'stepNo': i+1, 'content': t, 'duration': 0} for i, t in enumerate(texts)]

    if not ingredients and not steps:
        return None

    # 推断烹饪时长
    cook_time_hint = 0
    for s in steps:
        content = s['content']
        nums = re.findall(r'(\d+)[分分钟钟]', content)
        for n in nums:
            cook_time_hint += int(n)
    total_duration = max(15, min(180, cook_time_hint if cook_time_hint > 0 else 30))

    return {
        'name': name,
        'cover': cover,
        'description': description[:300],
        'cuisine': cuisine,
        'difficulty': difficulty,
        'cook_method': cook_method,
        'cook_time': total_duration,
        'calories': 0,
        'is_vip_only': False,
        'ingredients': ingredients,
        'steps': steps,
    }


def insert_recipe(conn, recipe):
    """将菜谱插入 MySQL"""
    cur = conn.cursor()
    try:
        # 插入 recipe 表
        sql = """INSERT INTO recipe 
            (name, cover_image, description, cuisine, difficulty, cook_method, 
             cook_time, calories, is_vip_only, status, source)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, 1, 'crawl')"""
        cur.execute(sql, (
            recipe['name'], recipe['cover'], recipe['description'],
            recipe['cuisine'], recipe['difficulty'], recipe['cook_method'],
            recipe['cook_time'], recipe['calories'], 1 if recipe['is_vip_only'] else 0
        ))
        recipe_id = cur.lastrowid

        # 插入食材
        for i, (ing_name, ing_amount) in enumerate(recipe['ingredients']):
            # 确保食材字典表有这条
            try:
                cur.execute("INSERT IGNORE INTO ingredient (name, category) VALUES (%s, '爬取')", (ing_name,))
            except:
                pass
            # 获取 ingredient_id
            cur.execute("SELECT id FROM ingredient WHERE name = %s", (ing_name,))
            row = cur.fetchone()
            ing_id = row[0] if row else None

            cur.execute(
                "INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES (%s, %s, %s, %s, %s)",
                (recipe_id, ing_id, ing_name, ing_amount, i + 1)
            )

        # 插入步骤
        for step in recipe['steps']:
            cur.execute(
                "INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES (%s, %s, %s, %s)",
                (recipe_id, step['stepNo'], step['content'], step['duration'])
            )

        conn.commit()
        EXISTING_NAMES.add(recipe['name'])
        return recipe_id

    except Exception as e:
        conn.rollback()
        print(f"    入库失败: {e}")
        return None
    finally:
        cur.close()


def main():
    load_existing_names()

    # 1. 收集菜谱ID
    print("\n=== 从首页收集菜谱ID ===")
    ids = extract_recipe_ids_from_homepage()
    print(f"首页找到 {len(ids)} 个菜谱ID")

    if len(ids) < MAX_RECIPES:
        print("\n=== 从分类页收集更多菜谱ID ===")
        more_ids = extract_recipe_ids_from_category()
        ids.extend(more_ids)
        ids = list(set(ids))
        print(f"总共 {len(ids)} 个唯一菜谱ID")

    if not ids:
        print("未找到菜谱ID，退出")
        return

    # 2. 逐个爬取详情
    print(f"\n=== 开始爬取（最多 {MAX_RECIPES} 道新菜谱）===")
    conn = get_db()
    inserted_count = 0
    crawled_count = 0

    for rid in ids:
        if inserted_count >= MAX_RECIPES:
            break
        crawled_count += 1

        print(f"\n[{crawled_count}] 爬取菜谱 {rid}...", end=' ')
        recipe = parse_detail(rid)
        if not recipe:
            print("跳过")
            time.sleep(DELAY_SECS)
            continue

        print(f"「{recipe['name']}」({len(recipe['ingredients'])}种食材, {len(recipe['steps'])}步)")
        rid_new = insert_recipe(conn, recipe)
        if rid_new:
            inserted_count += 1
            print(f"  -> 入库成功 (id={rid_new})")
        else:
            print("  -> 入库失败")

        time.sleep(DELAY_SECS)

    conn.close()

    print(f"\n=== 爬取完成 ===")
    print(f"尝试爬取: {crawled_count}")
    print(f"成功入库: {inserted_count}")


if __name__ == '__main__':
    main()

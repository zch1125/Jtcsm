# -*- coding: utf-8 -*-
"""Python ES 同步脚本：将 MySQL 菜谱数据同步到 Elasticsearch（备用方案）"""
import sys, io, re, time, json
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

import urllib3, pymysql

urllib3.disable_warnings()

# ========== 配置 ==========
ES_HOST = "http://localhost:9200"
MYSQL = {"host": "localhost", "port": 3306, "user": "root",
         "password": "Pass1234", "database": "jtcsm", "charset": "utf8mb4"}
INDEX_NAME = "jtcsm_recipe"
BATCH_SIZE = 20
VECTOR_DIM = 1024

http = urllib3.PoolManager(cert_reqs="CERT_NONE", assert_hostname=False,
                           headers={"Content-Type": "application/json"})


def es_request(method, path, body=None):
    url = ES_HOST + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body else None
    r = http.request(method, url, body=data, timeout=15)
    return r.status, json.loads(r.data.decode("utf-8")) if r.data else {}


def init_index():
    """创建 ES 索引"""
    exists, _ = es_request("HEAD", "/%s" % INDEX_NAME)
    if exists == 200:
        print("索引 %s 已存在" % INDEX_NAME)
        return True

    mapping = {
        "mappings": {
            "properties": {
                "recipeId": {"type": "long"},
                "name": {"type": "text", "analyzer": "standard"},
                "description": {"type": "text", "analyzer": "standard"},
                "cuisine": {"type": "keyword"},
                "difficulty": {"type": "keyword"},
                "cookMethod": {"type": "keyword"},
                "cookTime": {"type": "integer"},
                "calories": {"type": "integer"},
                "coverImage": {"type": "keyword"},
                "isVipOnly": {"type": "boolean"},
                "viewCount": {"type": "integer"},
                "favoriteCount": {"type": "integer"},
                "ingredientsText": {"type": "text", "analyzer": "standard"},
                "stepsText": {"type": "text", "analyzer": "standard"},
                "contentVector": {"type": "dense_vector", "dims": VECTOR_DIM, "similarity": "cosine"},
                "nameVector": {"type": "dense_vector", "dims": VECTOR_DIM, "similarity": "cosine"},
            }
        }
    }
    status, resp = es_request("PUT", "/%s" % INDEX_NAME, mapping)
    if status == 200:
        print("索引 %s 创建成功" % INDEX_NAME)
        return True
    else:
        print("索引创建失败: %s" % resp)
        return False


def get_recipes():
    """从 MySQL 获取所有菜谱"""
    conn = pymysql.connect(**MYSQL)
    cur = conn.cursor(pymysql.cursors.DictCursor)
    cur.execute("SELECT * FROM recipe WHERE status = 1 ORDER BY id")
    recipes = cur.fetchall()

    for r in recipes:
        cur.execute(
            "SELECT name, amount FROM recipe_ingredient WHERE recipe_id = %s ORDER BY sort_order",
            (r["id"],))
        r["ingredients"] = cur.fetchall()
        cur.execute(
            "SELECT step_no, content, duration FROM recipe_step WHERE recipe_id = %s ORDER BY step_no",
            (r["id"],))
        r["steps"] = cur.fetchall()

    cur.close()
    conn.close()
    return recipes


def get_embedding(text):
    """简易 Embedding（调用 text-embedding-v3 或使用零向量占位）"""
    # 生产环境中应调用 Embedding API，这里返回零向量
    return [0.0] * VECTOR_DIM


def sync_to_es(recipes):
    """同步菜谱到 ES"""
    total = len(recipes)
    print("开始同步 %d 条菜谱到 ES..." % total)

    for i in range(0, total, BATCH_SIZE):
        batch = recipes[i:i + BATCH_SIZE]
        lines = []
        for r in batch:
            ing_text = " ".join([ig["name"] + (" " + ig["amount"] if ig["amount"] else "")
                                 for ig in r["ingredients"]])
            step_text = " ".join([s["content"] for s in r["steps"]])
            content_for_embed = "%s %s %s %s" % (r["name"], r.get("description", ""), ing_text, step_text)

            doc = {
                "recipeId": r["id"],
                "name": r["name"],
                "description": r.get("description", "") or "",
                "cuisine": r.get("cuisine", "") or "",
                "difficulty": r.get("difficulty", "") or "",
                "cookMethod": r.get("cook_method", "") or "",
                "cookTime": r.get("cook_time") or 0,
                "calories": r.get("calories") or 0,
                "coverImage": r.get("cover_image", "") or "",
                "isVipOnly": bool(r.get("is_vip_only")),
                "viewCount": r.get("view_count") or 0,
                "favoriteCount": r.get("favorite_count") or 0,
                "ingredientsText": ing_text,
                "stepsText": step_text,
                "contentVector": get_embedding(content_for_embed),
                "nameVector": get_embedding(r["name"]),
            }

            # ES bulk API: action line
            action = json.dumps({"index": {"_index": INDEX_NAME, "_id": str(r["id"])}}, ensure_ascii=False)
            lines.append(action)
            lines.append(json.dumps(doc, ensure_ascii=False))

        if lines:
            bulk_body = "\n".join(lines) + "\n"
            url = ES_HOST + "/_bulk"
            r = http.request("POST", url, body=bulk_body.encode("utf-8"),
                             headers={"Content-Type": "application/x-ndjson"}, timeout=30)
            if r.status == 200:
                resp = json.loads(r.data.decode("utf-8"))
                if resp.get("errors"):
                    print("  批量 [%d-%d] 存在错误" % (i + 1, min(i + BATCH_SIZE, total)))
                else:
                    print("  批量 [%d-%d] 成功" % (i + 1, min(i + BATCH_SIZE, total)))
            else:
                print("  批量 [%d-%d] 失败: HTTP %d" % (i + 1, min(i + BATCH_SIZE, total), r.status))

        time.sleep(0.5)

    print("同步完成!")


def main():
    print("=== 食谱 ES 同步工具 ===\n")

    # 1. 创建索引
    if not init_index():
        return

    # 2. 获取数据
    recipes = get_recipes()
    print("MySQL 读取到 %d 条菜谱\n" % len(recipes))

    # 3. 同步到 ES
    sync_to_es(recipes)


if __name__ == "__main__":
    main()

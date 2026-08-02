# -*- coding: utf-8 -*-
"""
将 recipes_knowledge_base.md 分块索引到 Elasticsearch
每道菜谱作为一条独立的 ES 文档，包含完整的 Markdown 内容
"""
import sys, io, re, json, time
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

import urllib3
urllib3.disable_warnings()

# ============ 配置 ============
ES_HOST = "http://localhost:9200"
INDEX_NAME = "jtcsm_knowledge_md"
MD_FILE = "D:\\Project\\Jtcsm\\docs\\recipes_knowledge_base.md"
BATCH_SIZE = 10

http = urllib3.PoolManager(cert_reqs="CERT_NONE", assert_hostname=False,
                           headers={"Content-Type": "application/json"})


def es_req(method, path, body=None):
    url = ES_HOST + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body else None
    r = http.request(method, url, body=data, timeout=15)
    return r.status, json.loads(r.data.decode("utf-8")) if r.data else {}


def create_index():
    """创建 Markdown 知识库索引"""
    exists, _ = es_req("HEAD", "/%s" % INDEX_NAME)
    if exists == 200:
        print("索引 %s 已存在，跳过创建" % INDEX_NAME)
        return True

    mapping = {
        "settings": {
            "index": {
                "number_of_shards": 1,
                "number_of_replicas": 0,
                "analysis": {
                    "analyzer": {
                        "smart_analyzer": {
                            "type": "standard"
                        }
                    }
                }
            }
        },
        "mappings": {
            "properties": {
                "recipe_name": {"type": "keyword"},
                "content": {
                    "type": "text",
                    "analyzer": "standard",
                    "fields": {
                        "keyword": {"type": "keyword", "ignore_above": 256}
                    }
                },
                "content_vector": {
                    "type": "dense_vector",
                    "dims": 1024,
                    "similarity": "cosine",
                    "index": True
                }
            }
        }
    }

    status, resp = es_req("PUT", "/%s" % INDEX_NAME, mapping)
    if status == 200:
        print("索引 %s 创建成功 (dims=1024)" % INDEX_NAME)
        return True
    else:
        print("索引创建失败: %s" % resp)
        return False


def parse_markdown():
    """解析 Markdown 文件，分割成每一道菜谱"""
    with open(MD_FILE, "r", encoding="utf-8") as f:
        content = f.read()

    # 按 ## 标题分割
    sections = re.split(r"\n(?=## )", content)
    recipes = []

    for sec in sections:
        sec = sec.strip()
        if not sec or sec.startswith("# ") or sec.startswith(">") or sec == "---":
            continue

        # 提取菜名：## 菜名
        m = re.match(r"## (.+)", sec)
        if not m:
            continue
        recipe_name = m.group(1).strip()

        recipes.append({
            "recipe_name": recipe_name,
            "content": sec,
        })

    return recipes


def get_embedding(text, api_key=None):
    """调用 text-embedding-v3 生成向量；API 不可用时返回零向量"""
    if api_key:
        try:
            payload = {
                "model": "text-embedding-v3",
                "input": text[:2000]  # 截断过长文本
            }
            r = http.request("POST", "https://api.deepseek.com/v1/embeddings",
                             body=json.dumps(payload).encode("utf-8"),
                             headers={
                                 "Content-Type": "application/json",
                                 "Authorization": "Bearer %s" % api_key
                             }, timeout=15)
            if r.status == 200:
                resp = json.loads(r.data.decode("utf-8"))
                return resp["data"][0]["embedding"]
        except:
            pass

    # 兜底：零向量
    return [0.0] * 1024


def index_recipes(recipes, api_key=None):
    """批量索引菜谱到 ES"""
    total = len(recipes)
    print("开始索引 %d 条菜谱到 %s..." % (total, INDEX_NAME))
    indexed = 0

    for i in range(0, total, BATCH_SIZE):
        batch = recipes[i:i + BATCH_SIZE]
        lines = []

        for r in batch:
            vec = get_embedding(r["recipe_name"] + " " + r["content"][:200], api_key)
            doc = {
                "recipe_name": r["recipe_name"],
                "content": r["content"],
                "content_vector": vec,
            }
            action = json.dumps({"index": {"_index": INDEX_NAME}}, ensure_ascii=False)
            lines.append(action)
            lines.append(json.dumps(doc, ensure_ascii=False))

        if lines:
            bulk_body = "\n".join(lines) + "\n"
            url = ES_HOST + "/_bulk"
            r = http.request("POST", url, body=bulk_body.encode("utf-8"),
                             headers={"Content-Type": "application/x-ndjson"}, timeout=60)
            if r.status == 200:
                resp = json.loads(r.data.decode("utf-8"))
                batch_ok = sum(1 for item in resp.get("items", [])
                               if "index" in item and item["index"].get("status") == 201)
                indexed += batch_ok
                print("  批次 [%d-%d]: %d 条成功" % (i + 1, min(i + BATCH_SIZE, total), batch_ok))
            else:
                print("  批次 [%d-%d] 失败: HTTP %d" % (i + 1, min(i + BATCH_SIZE, total), r.status))

        time.sleep(0.3)

    print("索引完成! 共 %d 条" % indexed)
    return indexed


def verify_index():
    """验证索引内容"""
    status, resp = es_req("POST", "/%s/_count" % INDEX_NAME)
    if status == 200:
        count = resp.get("count", 0)
        print("知识库文档总数: %d" % count)

        # 展示样本
        status, resp = es_req("POST", "/%s/_search" % INDEX_NAME,
                              {"query": {"match_all": {}}, "size": 3})
        if status == 200:
            hits = resp.get("hits", {}).get("hits", [])
            for h in hits:
                src = h["_source"]
                print("  样本: [%s] (%.2f字)" % (src["recipe_name"][:30], len(src["content"])))

    # 查询测试
    print("\n搜索测试: 番茄")
    status, resp = es_req("POST", "/%s/_search" % INDEX_NAME, {
        "query": {"match": {"content": "番茄"}},
        "size": 3
    })
    if status == 200:
        hits = resp.get("hits", {}).get("hits", [])
        print("  命中 %d 条" % len(hits))
        for h in hits:
            print("  - %s (score=%.3f)" % (h["_source"]["recipe_name"][:30], h["_score"]))


def main():
    import os
    api_key = os.environ.get("DEEPSEEK_API_KEY", "")

    print("=== Markdown 知识库 → ES 索引工具 ===\n")

    # 1. 连接 ES
    try:
        status, resp = es_req("GET", "/")
        if status != 200:
            print("ES 连接失败，请先启动 ES (docker-compose up -d)")
            return
        print("ES 已连接: v%s" % resp.get("version", {}).get("number", "?"))
    except:
        print("ES 不可用，请先启动 ES")
        return

    # 2. 创建索引
    if not create_index():
        return

    # 3. 解析 Markdown
    recipes = parse_markdown()
    print("解析到 %d 道菜谱\n" % len(recipes))

    # 4. 索引到 ES
    indexed = index_recipes(recipes, api_key if api_key else None)

    # 5. 验证
    if indexed > 0:
        verify_index()


if __name__ == "__main__":
    main()

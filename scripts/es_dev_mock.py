import sys, io, json, http.server, re, os
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
import pymysql

# 从 MySQL 加载所有菜谱
conn = pymysql.connect(host="localhost",port=3306,user="root",password="Pass1234",database="jtcsm",charset="utf8mb4")
cur = conn.cursor()
recipes = {}
cur.execute("SELECT id, name, description, cuisine, difficulty, cook_method, cook_time FROM recipe WHERE status=1")
for r in cur.fetchall():
    rid, name, desc, cuisine, diff, method, ctime = r
    ing_text = ""
    cur2 = conn.cursor()
    cur2.execute("SELECT name, amount FROM recipe_ingredient WHERE recipe_id=%s ORDER BY sort_order", (rid,))
    ings = [i[0]+(" "+i[1] if i[1] else "") for i in cur2.fetchall()]
    cur2.close()
    ing_text = " ".join(ings)
    step_text = ""
    cur3 = conn.cursor()
    cur3.execute("SELECT content FROM recipe_step WHERE recipe_id=%s ORDER BY step_no", (rid,))
    steps = [s[0] for s in cur3.fetchall()]
    cur3.close()
    step_text = " ".join(steps)
    
    doc = {"recipe_name": name, "content": f"# {name}\n\n{desc or ''}\n\n## 食材\n{ing_text}\n\n## 做法\n{step_text}", "name": name, "description": desc or "", "cuisine": cuisine or "", "difficulty": diff or "", "cook_method": method or "", "cook_time": ctime or 0, "ingredients_text": ing_text, "steps_text": step_text}
    recipes[str(rid)] = doc
cur.close(); conn.close()
print("Loaded %d recipes" % len(recipes))

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path in ("/", ""):
            return self.json({"name":"jtcsm-dev","cluster_name":"jtcsm-dev","version":{"number":"8.14.3"}})
        if self.path == "/_cluster/health":
            return self.json({"cluster_name":"jtcsm-dev","status":"green","number_of_nodes":1})
        if self.path.startswith("/_cat/indices"):
            return self.json([{"health":"green","index":"jtcsm_knowledge_md","docs.count":str(len(recipes)),"store.size":"1.5mb"},{"health":"green","index":"jtcsm_recipe","docs.count":str(len(recipes)),"store.size":"1.2mb"}])
        self.json({"error":"not_found"}, 404)

    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers.get("Content-Length",0)))) if self.headers.get("Content-Length") else {}
        if "_search" in self.path:
            query = ""
            q = body.get("query",{})
            if "match" in q:
                for v in q["match"].values():
                    query = v.get("query","") if isinstance(v,dict) else str(v)
            elif "bool" in q:
                for s in q["bool"].get("should",[]):
                    if "match" in s:
                        for v in s["match"].values():
                            query += " " + (v.get("query","") if isinstance(v,dict) else str(v))
            elif "match_all" in q:
                query = "*"

            hits = []
            for rid, doc in recipes.items():
                score = 1.0
                if query and query != "*":
                    score = 0
                    ql = query.lower()
                    if ql in doc["recipe_name"].lower(): score += 5
                    if doc["recipe_name"].lower().startswith(ql): score += 3
                    if ql in doc["ingredients_text"].lower(): score += 3
                    if ql in doc["content"].lower(): score += 1
                    if score == 0: continue
                src = {k:v for k,v in doc.items() if not k.endswith("Vector")}
                hits.append({"_index":"jtcsm_knowledge_md","_id":rid,"_score":score,"_source":src})

            hits.sort(key=lambda h:-h["_score"])
            size = body.get("size",10)
            hits = hits[body.get("from",0):body.get("from",0)+size]
            return self.json({"took":1,"hits":{"total":{"value":len(hits)},"max_score":hits[0]["_score"] if hits else 0,"hits":hits}})
        
        self.json({"acknowledged":True})

    def do_PUT(self):
        self.json({"acknowledged":True})

    def do_HEAD(self):
        self.send_response(200); self.end_headers()

    def json(self, data, code=200):
        self.send_response(code)
        self.send_header("Content-Type","application/json")
        self.end_headers()
        self.wfile.write(json.dumps(data,ensure_ascii=False).encode("utf-8"))
    
    def log_message(self, *a): pass

http.server.HTTPServer(("0.0.0.0", 9200), Handler).serve_forever()

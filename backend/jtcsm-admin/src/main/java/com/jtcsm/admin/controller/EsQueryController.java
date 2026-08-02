package com.jtcsm.admin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jtcsm.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ES 数据查询控制器 —— 搜索/浏览向量库中的文档
 */
@RestController
@RequestMapping("/api/admin/es")
public class EsQueryController {

    private static final Logger log = LoggerFactory.getLogger(EsQueryController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private String getEsBaseUrl() {
        String host = System.getenv().getOrDefault("ES_HOST", "localhost");
        String port = System.getenv().getOrDefault("ES_PORT", "9200");
        return "http://" + host + ":" + port;
    }

    /**
     * 搜索 ES 索引
     * POST /api/admin/es/search
     * Body: { "index": "jtcsm_knowledge_md", "query": "番茄鸡蛋", "size": 10, "from": 0 }
     */
    @PostMapping("/search")
    public Result<Map<String, Object>> search(@RequestBody Map<String, Object> params) {
        String index = (String) params.getOrDefault("index", "");
        String query = (String) params.getOrDefault("query", "");
        int size = params.get("size") instanceof Number ? ((Number) params.get("size")).intValue() : 10;
        int from = params.get("from") instanceof Number ? ((Number) params.get("from")).intValue() : 0;
        boolean raw = params.get("raw") instanceof Boolean && (Boolean) params.get("raw");

        if (query == null || query.trim().isEmpty()) {
            return Result.ok(Map.of("total", 0, "hits", List.of(), "took", 0));
        }

        try {
            // 构建 ES 查询
            Map<String, Object> esQuery = buildEsQuery(query, size, from, raw);
            String body = mapper.writeValueAsString(esQuery);

            // 调用 ES REST API
            String url = getEsBaseUrl() + "/" + (index.isEmpty() ? "_all" : index) + "/_search";
            String response = httpPost(url, body);

            if (response == null) {
                return Result.error("ES 查询失败：无响应");
            }

            Map<String, Object> esResp = mapper.readValue(response, new TypeReference<Map<String, Object>>() {});

            // 简化响应
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("took", esResp.getOrDefault("took", 0));
            result.put("timedOut", esResp.getOrDefault("timed_out", false));

            Object hitsObj = esResp.get("hits");
            if (hitsObj instanceof Map) {
                Map<String, Object> hitsMap = (Map<String, Object>) hitsObj;
                result.put("total", extractTotal(hitsMap));
                result.put("maxScore", hitsMap.getOrDefault("max_score", 0));

                List<Map<String, Object>> hits = new ArrayList<>();
                Object rawHits = hitsMap.get("hits");
                if (rawHits instanceof List) {
                    for (Object h : (List<Object>) rawHits) {
                        if (h instanceof Map) {
                            Map<String, Object> hit = (Map<String, Object>) h;
                            Map<String, Object> simplified = new LinkedHashMap<>();
                            simplified.put("_index", hit.getOrDefault("_index", ""));
                            simplified.put("_id", hit.getOrDefault("_id", ""));
                            simplified.put("_score", hit.getOrDefault("_score", 0));
                            Object source = hit.get("_source");
                            if (source instanceof Map) {
                                // 简化 source，移除长文本和高维向量
                                Map<String, Object> src = (Map<String, Object>) source;
                                Map<String, Object> cleanSrc = new LinkedHashMap<>();
                                for (Map.Entry<String, Object> e : src.entrySet()) {
                                    Object val = e.getValue();
                                    if (val instanceof List && ((List<?>) val).size() > 100) {
                                        cleanSrc.put(e.getKey(), "[向量 " + ((List<?>) val).size() + " 维]");
                                    } else if (val instanceof String && ((String) val).length() > 200) {
                                        cleanSrc.put(e.getKey(), ((String) val).substring(0, 200) + "...");
                                    } else {
                                        cleanSrc.put(e.getKey(), val);
                                    }
                                }
                                simplified.put("_source", cleanSrc);
                            }
                            // highlight
                            Object hl = hit.get("highlight");
                            if (hl != null) simplified.put("highlight", hl);
                            hits.add(simplified);
                        }
                    }
                }
                result.put("hits", hits);
            }

            return Result.ok(result);

        } catch (Exception e) {
            log.error("ES 查询失败", e);
            return Result.error("ES 查询失败: " + e.getMessage());
        }
    }

    /**
     * 浏览索引的全部文档
     * GET /api/admin/es/browse?index=jtcsm_knowledge_md&size=10&from=0
     */
    @GetMapping("/browse")
    public Result<Map<String, Object>> browse(
            @RequestParam(defaultValue = "jtcsm_knowledge_md") String index,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int from) {
        return search(Map.of("index", index, "query", "*", "size", size, "from", from, "raw", true));
    }

    /** 构建 ES 查询 DSL */
    private Map<String, Object> buildEsQuery(String query, int size, int from, boolean raw) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", size);
        body.put("from", from);

        if ("*".equals(query.trim())) {
            body.put("query", Map.of("match_all", Map.of()));
        } else {
            // multi-match: 搜索多个字段
            body.put("query", Map.of(
                    "bool", Map.of(
                            "should", List.of(
                                    Map.of("match", Map.of("content", Map.of("query", query, "boost", 2.0))),
                                    Map.of("match", Map.of("recipe_name", Map.of("query", query, "boost", 3.0))),
                                    Map.of("match", Map.of("name", Map.of("query", query, "boost", 3.0)))
                            )
                    )
            ));
            // 高亮
            body.put("highlight", Map.of(
                    "fields", Map.of(
                            "content", Map.of("fragment_size", 100, "number_of_fragments", 3),
                            "name", Map.of(),
                            "recipe_name", Map.of()
                    ),
                    "pre_tags", List.of("<em style='color:#e74c3c;font-style:normal;'>"),
                    "post_tags", List.of("</em>")
            ));
        }

        // 排除向量字段
        body.put("_source", Map.of("excludes", List.of("content_vector", "nameVector")));

        return body;
    }

    /** 从 ES hits 中提取 total */
    private long extractTotal(Map<String, Object> hitsMap) {
        Object totalObj = hitsMap.get("total");
        if (totalObj instanceof Map) {
            Object value = ((Map<String, Object>) totalObj).get("value");
            if (value instanceof Number) return ((Number) value).longValue();
        }
        if (totalObj instanceof Number) return ((Number) totalObj).longValue();
        return 0;
    }

    private String httpPost(String urlStr, String body) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            int code = conn.getResponseCode();
            if (code == 200) {
                byte[] data = conn.getInputStream().readAllBytes();
                return new String(data, StandardCharsets.UTF_8);
            }
            log.warn("ES POST {} -> HTTP {}", urlStr, code);
        } catch (Exception e) {
            log.warn("ES POST {} failed: {}", urlStr, e.getMessage());
        }
        return null;
    }
}

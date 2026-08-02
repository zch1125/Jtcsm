import re

with open("D:\\Project\\Jtcsm\\backend\\jtcsm-admin\\src\\main\\java\\com\\jtcsm\\admin\\service\\SystemMonitorService.java", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add imports
content = content.replace(
    "import java.util.ArrayList;",
    "import org.springframework.web.client.RestTemplate;\nimport java.util.ArrayList;\nimport java.util.Map;\nimport java.util.HashMap;\nimport com.fasterxml.jackson.databind.ObjectMapper;\nimport com.fasterxml.jackson.core.type.TypeReference;"
)

# 2. Remove unused RestTemplate.Builder
content = content.replace(
    "    private StringRedisTemplate redis;",
    "    private StringRedisTemplate redis;\n\n    private final ObjectMapper esMapper = new ObjectMapper();"
)

# 3. Add ES call in collect()
content = content.replace(
    'try { vo.setRedis(collectRedisInfo()); } catch (Exception e) { log.warn("采集 Redis 信息失败", e); }\n        return vo;',
    'try { vo.setRedis(collectRedisInfo()); } catch (Exception e) { log.warn("采集 Redis 信息失败", e); }\n        try { vo.setEs(collectEsInfo()); } catch (Exception e) { log.warn("采集 ES 信息失败", e); }\n        return vo;'
)

# 4. Add collectEsInfo method before utilities
old_util = "    // ==================== 工具方法 ===================="
new_es = """
    /** ES 状态（通过 REST API 采集） */
    private EsInfo collectEsInfo() {
        EsInfo info = new EsInfo();
        String esHost = System.getenv().getOrDefault("ES_HOST", "localhost");
        String esPort = System.getenv().getOrDefault("ES_PORT", "9200");
        String baseUrl = "http://" + esHost + ":" + esPort;

        try {
            // 1. 获取集群信息
            String response = httpGet(baseUrl + "/");
            if (response != null) {
                Map<String, Object> root = esMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                info.setConnected(true);
                Object versionObj = root.get("version");
                if (versionObj instanceof Map) {
                    info.setVersion((String) ((Map) versionObj).getOrDefault("number", "-"));
                }
                info.setClusterName((String) root.getOrDefault("cluster_name", "-"));
                info.setNodeName((String) root.getOrDefault("name", "-"));
            }

            // 2. 集群健康
            String healthResp = httpGet(baseUrl + "/_cluster/health");
            if (healthResp != null) {
                Map<String, Object> health = esMapper.readValue(healthResp, new TypeReference<Map<String, Object>>() {});
                info.setStatus((String) health.getOrDefault("status", "unknown"));
            }

            // 3. 索引信息
            String indicesResp = httpGet(baseUrl + "/_cat/indices?format=json");
            if (indicesResp != null) {
                java.util.List<Map<String, Object>> indices = esMapper.readValue(indicesResp, new TypeReference<java.util.List<Map<String, Object>>>() {});
                java.util.List<EsIndexInfo> indexInfos = new ArrayList<>();
                long totalDocs = 0;
                long totalStore = 0;

                for (Map<String, Object> idx : indices) {
                    String idxName = (String) idx.getOrDefault("index", "");
                    if (idxName.isEmpty()) continue;

                    EsIndexInfo idxInfo = new EsIndexInfo();
                    idxInfo.setName(idxName);
                    idxInfo.setHealth((String) idx.getOrDefault("health", "?"));
                    idxInfo.setDocCount(parseLong(idx.get("docs.count")));
                    idxInfo.setStoreSizeBytes(parseStoreSize((String) idx.getOrDefault("store.size", "0")));

                    indexInfos.add(idxInfo);
                    totalDocs += idxInfo.getDocCount();
                    totalStore += idxInfo.getStoreSizeBytes();
                }

                info.setIndexCount(indexInfos.size());
                info.setTotalDocCount(totalDocs);
                info.setTotalStoreSizeBytes(totalStore);
                info.setIndices(indexInfos);
            }
        } catch (Exception e) {
            info.setConnected(false);
            info.setStatus(e.getMessage());
            log.warn("ES REST API 调用失败: {}", e.getMessage());
        }

        return info;
    }

    private String httpGet(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code == 200) {
                byte[] data = conn.getInputStream().readAllBytes();
                return new String(data, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("HTTP GET {} failed: {}", url, e.getMessage());
        }
        return null;
    }

    private long parseLong(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0; }
    }

    private long parseStoreSize(String size) {
        if (size == null || size.isEmpty()) return 0;
        try {
            size = size.trim().toUpperCase();
            if (size.endsWith("TB")) return (long)(Double.parseDouble(size.replace("TB","").trim()) * 1024L * 1024 * 1024 * 1024);
            if (size.endsWith("GB")) return (long)(Double.parseDouble(size.replace("GB","").trim()) * 1024L * 1024 * 1024);
            if (size.endsWith("MB")) return (long)(Double.parseDouble(size.replace("MB","").trim()) * 1024L * 1024);
            if (size.endsWith("KB")) return (long)(Double.parseDouble(size.replace("KB","").trim()) * 1024L);
            if (size.endsWith("B")) return Long.parseLong(size.replace("B","").trim());
            return Long.parseLong(size);
        } catch (Exception e) { return 0; }
    }

""" + old_util

content = content.replace(old_util, new_es)

with open("D:\\Project\\Jtcsm\\backend\\jtcsm-admin\\src\\main\\java\\com\\jtcsm\\admin\\service\\SystemMonitorService.java", "w", encoding="utf-8") as f:
    f.write(content)
print("Service updated successfully")

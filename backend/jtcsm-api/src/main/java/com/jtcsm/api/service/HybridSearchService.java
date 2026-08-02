package com.jtcsm.api.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务
 * <p>同时执行 BM25 关键词检索和 dense_vector 语义检索，
 * 使用 RRF（Reciprocal Rank Fusion）融合排序。</p>
 */
@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);

    /** ES 索引名称 */
    private static final String INDEX = "jtcsm_recipe";

    /** RRF 常数 k */
    private static final int RRF_K = 60;

    /** BM25 检索返回数量 */
    private static final int BM25_SIZE = 30;

    /** 向量检索返回数量 */
    private static final int VECTOR_SIZE = 30;

    /** 最终融合返回数量 */
    private static final int FINAL_SIZE = 10;

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * 混合检索（自动生成查询向量）
     *
     * @param queryText     查询文本
     * @param bm25BoostField BM25 重点加权字段（如 "name"、"ingredientsText"），null 则均衡加权
     * @return 融合排序后的结果列表
     */
    public List<Map<String, Object>> hybridSearch(String queryText, String bm25BoostField) {
        // 1. 生成查询向量
        float[] queryVec = embeddingService.embedForQuery(queryText);
        if (queryVec.length == 0) {
            return Collections.emptyList();
        }

        return hybridSearchWithVector(queryText, queryVec, bm25BoostField);
    }

    /**
     * 混合检索（使用外部传入的向量）
     *
     * @param queryText     查询文本
     * @param queryVec      查询向量
     * @param bm25BoostField BM25 重点加权字段
     * @return 融合排序后的结果列表
     */
    public List<Map<String, Object>> hybridSearchWithVector(String queryText, float[] queryVec,
                                                             String bm25BoostField) {
        try {
            // ---- 1. BM25 关键词检索 ----
            List<Hit<Map>> bm25Hits = executeBM25(queryText, bm25BoostField);

            // ---- 2. 向量语义检索 ----
            List<Hit<Map>> vectorHits;
            try {
                vectorHits = executeVectorSearch(queryVec, "contentVector");
            } catch (Exception e) {
                // 向量不可用时降级为 BM25，避免搜索接口整体为空
                log.warn("向量检索失败，降级为 BM25 检索: {}", e.getMessage());
                vectorHits = Collections.emptyList();
            }

            // ---- 3. RRF 融合排序 ----
            List<Map<String, Object>> fused = fuseResults(bm25Hits, vectorHits);

            log.debug("混合检索: query='{}', BM25={}条, 向量={}条, 融合后={}条",
                    queryText, bm25Hits.size(), vectorHits.size(), fused.size());
            return fused;

        } catch (Exception e) {
            log.error("混合检索异常: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 执行 BM25 多字段检索
     */
    @SuppressWarnings("unchecked")
    private List<Hit<Map>> executeBM25(String queryText, String boostField) throws Exception {
        // 构建多字段 query
        List<Query> shouldQueries = new ArrayList<>();

        // 名称字段（如果指定了 boostField 是 name，给予更高权重）
        double nameBoost = "name".equals(boostField) ? 3.0 : 1.5;
        shouldQueries.add(Query.of(q -> q.match(m -> m
                .field("name").query(queryText).boost((float) nameBoost))));

        // 描述字段
        shouldQueries.add(Query.of(q -> q.match(m -> m
                .field("description").query(queryText).boost(1.0f))));

        // 食材字段（如果指定了 boostField 是 ingredientsText，给予更高权重）
        double ingredientBoost = "ingredientsText".equals(boostField) ? 3.0 : 1.5;
        shouldQueries.add(Query.of(q -> q.match(m -> m
                .field("ingredientsText").query(queryText).boost((float) ingredientBoost))));

        // 步骤字段
        shouldQueries.add(Query.of(q -> q.match(m -> m
                .field("stepsText").query(queryText).boost(0.8f))));

        Query boolQuery = Query.of(q -> q.bool(b -> b.should(shouldQueries)));

        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX)
                .query(boolQuery)
                .size(BM25_SIZE)
                .source(sc -> sc.filter(f -> f
                        .includes("recipeId", "name", "cuisine", "difficulty",
                                "cookMethod", "cookTime", "calories", "coverImage",
                                "description", "ingredientsText", "stepsText"))));

        SearchResponse<Map> response = esClient.search(request, Map.class);
        return response.hits().hits();
    }

    /**
     * 执行向量语义检索
     */
    @SuppressWarnings("unchecked")
    private List<Hit<Map>> executeVectorSearch(float[] queryVec, String vectorField) throws Exception {
        List<Float> queryVectorList = new ArrayList<>(queryVec.length);
        for (float v : queryVec) {
            queryVectorList.add(v);
        }

        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX)
                .knn(k -> k
                        .field(vectorField)
                        .queryVector(queryVectorList)
                        .k(VECTOR_SIZE)
                        .numCandidates(100)
                )
                .size(VECTOR_SIZE)
                .source(sc -> sc.filter(f -> f
                        .includes("recipeId", "name", "cuisine", "difficulty",
                                "cookMethod", "cookTime", "calories", "coverImage",
                                "description", "ingredientsText", "stepsText"))));

        SearchResponse<Map> response = esClient.search(request, Map.class);
        return response.hits().hits();
    }

    /**
     * RRF 融合排序
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fuseResults(List<Hit<Map>> bm25Hits, List<Hit<Map>> vectorHits) {
        // recipeId -> { score, source }
        Map<String, RrfEntry> fused = new LinkedHashMap<>();

        // BM25 排名
        for (int i = 0; i < bm25Hits.size(); i++) {
            Hit<Map> hit = bm25Hits.get(i);
            String id = hit.id();
            Map<String, Object> source = hit.source();
            if (source == null) continue;
            source.put("_rankType", "bm25");
            fused.computeIfAbsent(id, k -> new RrfEntry(source))
                    .addScore(1.0 / (RRF_K + i + 1));
        }

        // 向量排名
        for (int i = 0; i < vectorHits.size(); i++) {
            Hit<Map> hit = vectorHits.get(i);
            String id = hit.id();
            Map<String, Object> source = hit.source();
            if (source == null) continue;
            source.put("_rankType", "vector");
            fused.computeIfAbsent(id, k -> new RrfEntry(source))
                    .addScore(1.0 / (RRF_K + i + 1));
        }

        // 按融合分数排序，取 top K
        return fused.values().stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(FINAL_SIZE)
                .map(e -> {
                    e.source.put("_rrfScore", e.score);
                    return e.source;
                })
                .collect(Collectors.toList());
    }

    /**
     * RRF 排名条目
     */
    private static class RrfEntry {
        final Map<String, Object> source;
        double score = 0.0;

        RrfEntry(Map<String, Object> source) {
            this.source = source;
        }

        void addScore(double s) {
            this.score += s;
        }
    }
}

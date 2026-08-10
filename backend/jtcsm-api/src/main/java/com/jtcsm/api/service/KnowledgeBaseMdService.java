package com.jtcsm.api.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Markdown 知识库服务
 * <p>管理 jtcsm_knowledge_md 索引，提供 Markdown 全文检索能力。</p>
 */
@Service
@Order(2)
public class KnowledgeBaseMdService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseMdService.class);

    /** Markdown 知识库索引名称 */
    public static final String INDEX_NAME = "jtcsm_knowledge_md";

    private static final int VECTOR_DIM = 1024;
    private static final int SEARCH_SIZE = 10;
    private static final int BATCH_SIZE = 50;

    /** Markdown 知识库文件路径，可通过 KNOWLEDGE_BASE_FILE 覆盖 */
    @Value("${knowledge-base.file:docs/recipes_knowledge_base.md}")
    private String knowledgeBaseFile;

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * 初始化索引（不存在则创建）
     */
    public void initIndex() throws Exception {
        boolean exists = esClient.indices()
                .exists(ExistsRequest.of(e -> e.index(INDEX_NAME)))
                .value();
        if (exists) {
            log.info("Markdown 知识库索引 '{}' 已存在", INDEX_NAME);
            return;
        }

        CreateIndexRequest request = CreateIndexRequest.of(i -> i
                .index(INDEX_NAME)
                .mappings(m -> m
                        .properties("recipe_name", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
                        .properties("content", Property.of(p -> p.text(
                                TextProperty.of(t -> t.analyzer("standard"))
                        )))
                        .properties("content_vector", Property.of(p -> p.denseVector(
                                DenseVectorProperty.of(d -> d
                                        .dims(VECTOR_DIM)
                                        .similarity("cosine")
                                )
                        )))
                )
        );

        esClient.indices().create(request);
        log.info("Markdown 知识库索引 '{}' 创建成功 (dims={})", INDEX_NAME, VECTOR_DIM);
    }

    @Override
    public void run(String... args) {
        try {
            initIndex();
            fullSync();
        } catch (Exception e) {
            log.error("Markdown 知识库初始化失败，应用继续运行但 RAG 知识库不可用: {}", e.getMessage(), e);
        }
    }

    /**
     * 全量同步 docs/recipes_knowledge_base.md 到 ES
     */
    public void fullSync() throws IOException {
        Path file = resolveKnowledgeBaseFile();
        if (!Files.isRegularFile(file)) {
            log.warn("Markdown 知识库文件不存在: {}", file.toAbsolutePath());
            return;
        }

        String markdown = Files.readString(file, StandardCharsets.UTF_8);
        List<String[]> sections = parseMarkdown(markdown);
        if (sections.isEmpty()) {
            log.warn("Markdown 知识库文件中未解析到菜谱章节");
            return;
        }

        log.info("开始同步 {} 条 Markdown 知识库文档到 ES", sections.size());
        for (int i = 0; i < sections.size(); i += BATCH_SIZE) {
            List<String[]> batch = sections.subList(i, Math.min(i + BATCH_SIZE, sections.size()));
            syncBatch(batch);
        }
        log.info("Markdown 知识库全量同步完成，共 {} 条", sections.size());
    }

    /**
     * 批量写入 Markdown 知识库文档
     */
    private void syncBatch(List<String[]> sections) throws IOException {
        List<BulkOperation> operations = new ArrayList<>();

        for (String[] section : sections) {
            String recipeName = section[0];
            String content = section[1];

            Map<String, Object> doc = new HashMap<>();
            doc.put("recipe_name", recipeName);
            doc.put("content", content);

            float[] vector = embeddingService.embed(recipeName + " " + content);
            if (vector.length == VECTOR_DIM && !isZeroVector(vector)) {
                doc.put("content_vector", toList(vector));
            } else {
                // 向量不可用时仍保留全文，BM25 检索与 RAG 降级可继续工作
                log.warn("知识库文档 '{}' 向量无效，仅索引全文", recipeName);
            }

            operations.add(BulkOperation.of(b -> b
                    .index(idx -> idx
                            .index(INDEX_NAME)
                            .id(idOf(recipeName))
                            .document(doc)
                    )));
        }

        if (operations.isEmpty()) {
            return;
        }

        BulkResponse response = esClient.bulk(BulkRequest.of(b -> b.operations(operations)));
        if (response.errors()) {
            log.warn("Markdown 知识库批量同步存在错误");
            response.items().forEach(item -> {
                if (item.error() != null) {
                    log.warn("  -> ID {} 错误: {}", item.id(), item.error().reason());
                }
            });
        }
    }

    /**
     * 按 Markdown 二级标题拆分知识库章节
     */
    private List<String[]> parseMarkdown(String markdown) {
        List<String[]> sections = new ArrayList<>();
        String[] blocks = markdown.split("(?m)(?=^## )");

        for (String block : blocks) {
            if (!block.startsWith("## ")) {
                continue;
            }

            int newline = block.indexOf('\n');
            String recipeName = (newline < 0 ? block.substring(3) : block.substring(3, newline)).trim();
            if (recipeName.isEmpty()) {
                continue;
            }
            sections.add(new String[]{recipeName, block.trim()});
        }
        return sections;
    }

    /**
     * 兼容不同 Maven 工作目录下定位 docs 目录
     */
    private Path resolveKnowledgeBaseFile() {
        Path direct = Path.of(knowledgeBaseFile);
        if (Files.isRegularFile(direct)) {
            return direct;
        }

        for (String prefix : new String[]{"..", "../.."}) {
            Path candidate = Path.of(prefix, knowledgeBaseFile);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return direct;
    }

    private boolean isZeroVector(float[] vector) {
        for (float value : vector) {
            if (value != 0f) {
                return false;
            }
        }
        return true;
    }

    private String idOf(String recipeName) {
        return Integer.toUnsignedString(recipeName.hashCode());
    }

    private List<Float> toList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }

    /**
     * 搜索 Markdown 知识库（BM25 全文检索 + 向量语义检索融合）
     *
     * @param queryText 搜索文本
     * @param size      返回条数
     * @return 搜索结果列表，每项包含 recipe_name, content, _score
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(String queryText, int size) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return List.of();
        }
        int finalSize = size > 0 ? size : SEARCH_SIZE;

        try {
            // BM25 全文检索：在 content 字段上搜索
            Query boolQuery = Query.of(q -> q.bool(b -> b
                    .should(Query.of(q1 -> q1.match(m -> m
                            .field("content").query(queryText).operator(Operator.And).boost(2.0f))))
                    .should(Query.of(q1 -> q1.match(m -> m
                            .field("recipe_name").query(queryText).boost(3.0f))))
            ));

            SearchRequest request = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(boolQuery)
                    .size(finalSize)
                    .source(sc -> sc.filter(f -> f
                            .includes("recipe_name", "content")))
            );

            SearchResponse<Map> response = esClient.search(request, Map.class);
            return response.hits().hits().stream()
                    .map(hit -> {
                        Map<String, Object> source = hit.source();
                        if (source != null) {
                            source.put("_score", hit.score());
                            source.put("_id", hit.id());
                        }
                        return source;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Markdown 知识库检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 搜索知识库并提取纯文本片段（供 RAG 使用）
     *
     * @param queryText 搜索文本
     * @param maxChars  最大上下文字符数
     * @return 格式化后的知识库上下文文本
     */
    public String searchAsContext(String queryText, int maxChars) {
        List<Map<String, Object>> results = search(queryText, 5);
        if (results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【食谱知识库参考】\n\n");
        int total = 0;
        for (Map<String, Object> r : results) {
            String name = (String) r.get("recipe_name");
            String content = (String) r.get("content");
            if (name == null || content == null) continue;

            // 截取 content 的前 500 字作为参考片段
            String clip = content.length() > 500 ? content.substring(0, 500) + "..." : content;
            sb.append(clip).append("\n\n---\n\n");
            total += clip.length();
            if (total >= maxChars) break;
        }
        return sb.toString();
    }
}

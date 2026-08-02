package com.jtcsm.api.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jtcsm.api.mapper.RecipeIngredientMapper;
import com.jtcsm.api.mapper.RecipeMapper;
import com.jtcsm.api.mapper.RecipeStepMapper;
import com.jtcsm.common.entity.Recipe;
import com.jtcsm.common.entity.RecipeIngredient;
import com.jtcsm.common.entity.RecipeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 食谱索引管理服务
 * <p>负责创建 ES 索引、全量同步/增量同步食谱数据到 ES。</p>
 */
@Service
@Order(1)
public class RecipeIndexService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RecipeIndexService.class);

    /** ES 索引名称 */
    public static final String INDEX_NAME = "jtcsm_recipe";

    /** dense_vector 维度，text-embedding-v3 输出 1024 维 */
    private static final int VECTOR_DIM = 1024;

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private RecipeMapper recipeMapper;

    @Autowired
    private RecipeIngredientMapper ingredientMapper;

    @Autowired
    private RecipeStepMapper stepMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Override
    public void run(String... args) {
        try {
            initIndex();
            fullSync();
        } catch (Exception e) {
            log.error("ES 索引初始化失败，应用将继续运行但 RAG 检索不可用: {}", e.getMessage());
        }
    }

    /**
     * 初始化索引（不存在则创建）
     */
    public void initIndex() throws IOException {
        boolean exists = esClient.indices().exists(ExistsRequest.of(e -> e.index(INDEX_NAME))).value();
        if (exists) {
            log.info("ES 索引 '{}' 已存在，跳过创建", INDEX_NAME);
            return;
        }

        CreateIndexRequest request = CreateIndexRequest.of(i -> i
                .index(INDEX_NAME)
                .mappings(m -> m
                        // 文本字段用于 BM25 关键词检索
                        .properties("name", Property.of(p -> p.text(TextProperty.of(t -> t.analyzer("standard")))))
                        .properties("description", Property.of(p -> p.text(TextProperty.of(t -> t.analyzer("standard")))))
                        .properties("cuisine", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
                        .properties("difficulty", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
                        .properties("cookMethod", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
                        .properties("ingredientsText", Property.of(p -> p.text(TextProperty.of(t -> t.analyzer("standard")))))
                        .properties("stepsText", Property.of(p -> p.text(TextProperty.of(t -> t.analyzer("standard")))))
                        // 向量字段用于语义检索
                        .properties("contentVector", Property.of(p -> p.denseVector(
                                DenseVectorProperty.of(d -> d.dims(VECTOR_DIM).similarity("cosine"))
                        )))
                        .properties("nameVector", Property.of(p -> p.denseVector(
                                DenseVectorProperty.of(d -> d.dims(VECTOR_DIM).similarity("cosine"))
                        )))
                )
        );

        esClient.indices().create(request);
        log.info("ES 索引 '{}' 创建成功 (dims={})", INDEX_NAME, VECTOR_DIM);
    }

    /**
     * 全量同步所有上架食谱到 ES
     */
    public void fullSync() throws IOException {
        List<Recipe> recipes = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>()
                        .eq(Recipe::getStatus, 1)
        );

        if (recipes.isEmpty()) {
            log.info("无可同步的食谱");
            return;
        }

        log.info("开始全量同步 {} 条食谱到 ES", recipes.size());
        int batchSize = 50;
        for (int i = 0; i < recipes.size(); i += batchSize) {
            List<Recipe> batch = recipes.subList(i, Math.min(i + batchSize, recipes.size()));
            syncBatch(batch);
        }
        log.info("全量同步完成，共 {} 条", recipes.size());
    }

    /**
     * 同步单条食谱到 ES
     */
    public void syncOne(Long recipeId) throws IOException {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null || recipe.getStatus() != 1) {
            deleteFromIndex(recipeId);
            return;
        }
        syncBatch(Collections.singletonList(recipe));
    }

    /**
     * 从索引中删除食谱
     */
    public void deleteFromIndex(Long recipeId) throws IOException {
        esClient.delete(d -> d.index(INDEX_NAME).id(String.valueOf(recipeId)));
    }

    /**
     * 批量同步食谱到 ES
     */
    private void syncBatch(List<Recipe> recipes) {
        try {
            List<BulkOperation> operations = new ArrayList<>();

            for (Recipe recipe : recipes) {
                // 构造全文本用于 embedding
                List<RecipeIngredient> ingredients = ingredientMapper.selectList(
                        new LambdaQueryWrapper<RecipeIngredient>()
                                .eq(RecipeIngredient::getRecipeId, recipe.getId())
                                .orderByAsc(RecipeIngredient::getSortOrder));
                List<RecipeStep> steps = stepMapper.selectList(
                        new LambdaQueryWrapper<RecipeStep>()
                                .eq(RecipeStep::getRecipeId, recipe.getId())
                                .orderByAsc(RecipeStep::getStepNo));

                String ingredientsText = ingredients.stream()
                        .map(ig -> ig.getName() + " " + (ig.getAmount() != null ? ig.getAmount() : ""))
                        .collect(Collectors.joining(" "));
                String stepsText = steps.stream()
                        .map(RecipeStep::getContent)
                        .collect(Collectors.joining(" "));

                // 生成语义向量
                String contentForEmbed = recipe.getName() + " " +
                        (recipe.getDescription() != null ? recipe.getDescription() : "") + " " +
                        ingredientsText + " " + stepsText;
                float[] contentVec = embeddingService.embed(contentForEmbed);
                float[] nameVec = embeddingService.embed(recipe.getName());
                boolean hasContentVector = contentVec.length == VECTOR_DIM && !isZeroVector(contentVec);
                boolean hasNameVector = nameVec.length == VECTOR_DIM && !isZeroVector(nameVec);
                if (!hasContentVector || !hasNameVector) {
                    log.warn("食谱 '{}' 向量无效，跳过向量字段，仅保留全文索引", recipe.getName());
                }

                Map<String, Object> doc = new HashMap<>();
                doc.put("recipeId", recipe.getId());
                doc.put("name", recipe.getName());
                doc.put("description", recipe.getDescription() != null ? recipe.getDescription() : "");
                doc.put("cuisine", recipe.getCuisine() != null ? recipe.getCuisine() : "");
                doc.put("difficulty", recipe.getDifficulty() != null ? recipe.getDifficulty() : "");
                doc.put("cookMethod", recipe.getCookMethod() != null ? recipe.getCookMethod() : "");
                doc.put("cookTime", recipe.getCookTime() != null ? recipe.getCookTime() : 0);
                doc.put("calories", recipe.getCalories() != null ? recipe.getCalories() : 0);
                doc.put("coverImage", recipe.getCoverImage() != null ? recipe.getCoverImage() : "");
                doc.put("viewCount", recipe.getViewCount() != null ? recipe.getViewCount() : 0);
                doc.put("favoriteCount", recipe.getFavoriteCount() != null ? recipe.getFavoriteCount() : 0);
                doc.put("ingredientsText", ingredientsText);
                doc.put("stepsText", stepsText);
                if (hasContentVector) {
                    doc.put("contentVector", toList(contentVec));
                }
                if (hasNameVector) {
                    doc.put("nameVector", toList(nameVec));
                }

                operations.add(BulkOperation.of(b -> b
                        .index(idx -> idx
                                .index(INDEX_NAME)
                                .id(String.valueOf(recipe.getId()))
                                .document(doc)
                        )));
            }

            if (!operations.isEmpty()) {
                BulkResponse response = esClient.bulk(BulkRequest.of(b -> b.operations(operations)));
                if (response.errors()) {
                    log.warn("ES 批量同步存在错误");
                    response.items().forEach(item -> {
                        if (item.error() != null) {
                            log.warn("  -> ID {} 错误: {}", item.id(), item.error().reason());
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("ES 批量同步异常: {}", e.getMessage(), e);
        }
    }

    /**
     * float[] 转 List<Float>（ES dense_vector 需要）
     */
    private List<Float> toList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float v : arr) {
            list.add(v);
        }
        return list;
    }

    /**
     * 判断向量是否全为零值
     */
    private boolean isZeroVector(float[] vector) {
        for (float value : vector) {
            if (value != 0f) {
                return false;
            }
        }
        return true;
    }
}

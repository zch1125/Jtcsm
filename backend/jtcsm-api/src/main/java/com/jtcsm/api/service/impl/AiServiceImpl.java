package com.jtcsm.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jtcsm.api.mapper.AiGeneratedRecipeMapper;
import com.jtcsm.api.service.AiService;
import com.jtcsm.api.service.FavoriteService;
import com.jtcsm.api.service.RagPipelineService;
import com.jtcsm.common.dto.AiFeedbackRequest;
import com.jtcsm.common.dto.AiGenerateRecordVO;
import com.jtcsm.common.dto.AiGenerateRequest;
import com.jtcsm.common.dto.AiGenerateResponse;
import com.jtcsm.common.dto.AiRecipeItem;
import com.jtcsm.common.dto.RagContext;
import com.jtcsm.common.entity.AiGeneratedRecipe;
import com.jtcsm.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 服务实现 —— 调用 DeepSeek 生成菜谱、RAG 检索增强、缓存、限流、收藏和反馈
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** 每日生成次数上限 */
    private static final int DAILY_LIMIT = 3;

    @Autowired
    private RestClient.Builder restClientBuilder;
    @Autowired
    private AiGeneratedRecipeMapper aiMapper;
    @Autowired
    private FavoriteService favoriteService;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private RagPipelineService ragPipelineService;

    @Override
    public AiGenerateResponse generate(Long userId, AiGenerateRequest request) {
        checkRateLimit(userId);

        String inputJson = toJson(request);
        // 尝试从缓存读取结果（MD5 作为缓存键，24小时有效）
        String cacheKey = "jtcsm:ai:cache:" + md5(inputJson);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                log.info("AI cache hit: userId={}", userId);
                List<AiRecipeItem> items = MAPPER.readValue(cached, new TypeReference<List<AiRecipeItem>>() {});
                return buildResponse(null, items, true);
            } catch (Exception e) {
                log.warn("Cache parse failed, regenerating", e);
            }
        }

        // RAG 检索：从食谱知识库中检索相关菜谱作为上下文
        RagContext ragContext = ragPipelineService.retrieve(request);
        log.info("RAG 检索完成: 参考菜谱={}条, 耗时={}ms",
                ragContext.getReferenceRecipes() != null ? ragContext.getReferenceRecipes().size() : 0,
                ragContext.getRetrievalMs());

        List<AiRecipeItem> recipes = callDeepSeek(request, ragContext);
        String resultJson;
        try { resultJson = MAPPER.writeValueAsString(recipes); }
        catch (Exception e) { resultJson = "[]"; }

        // 保存 AI 生成记录
        AiGeneratedRecipe record = new AiGeneratedRecipe();
        record.setUserId(userId);
        record.setMode(request.getMode());
        record.setInputContent(inputJson);
        record.setResultJson(resultJson);
        aiMapper.insert(record);

        // 写入缓存 24 小时
        redis.opsForValue().set(cacheKey, resultJson, Duration.ofHours(24));

        // 记录当日生成次数
        String today = LocalDate.now().toString();
        redis.opsForValue().increment("jtcsm:ai:limit:" + userId + ":" + today);

        return buildResponse(record.getId(), recipes, false);
    }

    @SuppressWarnings("unchecked")
    private List<AiRecipeItem> callDeepSeek(AiGenerateRequest request, RagContext ragContext) {
        try {
            // 调用 DeepSeek API 获取菜谱推荐（使用 RAG 增强 Prompt）
            RestClient client = restClientBuilder.build();
            String enhancedPrompt = buildPrompt(request, ragContext);
            String rawResp = client.post()
                    .uri("https://api.deepseek.com/chat/completions")
                    .header("Authorization", "Bearer " + System.getenv("DEEPSEEK_API_KEY"))
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "model", "deepseek-chat",
                            "messages", List.of(
                                    Map.of("role", "user", "content", enhancedPrompt)
                            ),
                            "temperature", 0.7,
                            "max_tokens", 4096
                    ))
                    .retrieve()
                    .body(String.class);
            String response = "";
            try {
                response = MAPPER.readTree(rawResp)
                        .get("choices").get(0).get("message").get("content").asText();
            } catch (Exception e) {
                log.warn("Response parse failed", e);
            }
            if (!StringUtils.hasText(response)) throw new BusinessException("AI 返回了空响应");
            // 提取 JSON 代码块（可能有 markdown 包装）
            String json = extractJson(response);
            return MAPPER.readValue(json, new TypeReference<List<AiRecipeItem>>() {});
        } catch (Exception e) {
            // API 调用失败时使用降级方案
            log.warn("DeepSeek 调用失败，使用降级方案: {}", e.getMessage());
            return getFallbackRecipes(request);
        }
    }

    private List<AiRecipeItem> getFallbackRecipes(AiGenerateRequest req) {
        // DeepSeek 调用失败后的降级：从 ES 知识库取已有菜谱
        try {
            RagContext ctx = ragPipelineService.retrieve(req);
            List<Map<String, Object>> refs = ctx.getReferenceRecipes();
            if (refs != null && !refs.isEmpty()) {
                List<AiRecipeItem> items = new ArrayList<>();
                for (Map<String, Object> ref : refs) {
                    AiRecipeItem item = mapToAiItem(ref);
                    if (item != null) items.add(item);
                }
                if (!items.isEmpty()) return items;
            }
        } catch (Exception ignored) {}

        // 最终兜底
        AiRecipeItem item = new AiRecipeItem();
        item.setName("番茄炒蛋");
        item.setCuisine("家常菜");
        item.setDifficulty("简单");
        item.setCookTime(15);
        item.setIngredients(new ArrayList<>());
        item.setSteps(new ArrayList<>());
        return Collections.singletonList(item);
    }

    /**
     * 构建 RAG 增强的 Prompt（中文）
     */
    private String buildPrompt(AiGenerateRequest r, RagContext context) {
        String userPrompt = buildUserPrompt(r);
        return ragPipelineService.buildEnhancedPrompt(userPrompt, context);
    }

    /**
     * 构建用户需求文本（中文）
     */
    private String buildUserPrompt(AiGenerateRequest r) {
        StringBuilder sb = new StringBuilder();
        if ("ingredients".equals(r.getMode())) {
            sb.append("我有以下食材：");
            if (r.getIngredients() != null && !r.getIngredients().isEmpty()) {
                sb.append(String.join("、", r.getIngredients()));
            }
        } else if ("name".equals(r.getMode())) {
            sb.append("我想做").append(r.getName() != null ? r.getName() : "").append("这道菜");
        } else if ("creative".equals(r.getMode())) {
            sb.append("我想融合");
            if (r.getCuisineA() != null) sb.append(r.getCuisineA());
            sb.append("和");
            if (r.getCuisineB() != null) sb.append(r.getCuisineB());
            sb.append("菜系");
        } else {
            sb.append("请推荐菜谱");
        }
        if (StringUtils.hasText(r.getConditions())) {
            sb.append("。要求：").append(r.getConditions());
        }
        sb.append("，请推荐 2 道菜谱");
        return sb.toString();
    }

    /**
     * 从 AI 响应中提取 JSON
     */
    private String extractJson(String response) {
        int s = response.indexOf("```json");
        if (s >= 0) {
            int e = response.indexOf("```", s + 7);
            if (e > s) return response.substring(s + 7, e).trim();
        } else if (response.contains("```")) {
            int s2 = response.indexOf("```");
            int e2 = response.indexOf("```", s2 + 3);
            if (e2 > s2) return response.substring(s2 + 3, e2).trim();
        }
        // 如果不是 markdown 代码块，尝试直接解析
        String trimmed = response.trim();
        if (trimmed.startsWith("[")) return trimmed;
        return response;
    }

    /**
     * 将 ES 检索结果映射为 AiRecipeItem（降级方案使用）
     */
    @SuppressWarnings("unchecked")
    private AiRecipeItem mapToAiItem(Map<String, Object> source) {
        try {
            AiRecipeItem item = new AiRecipeItem();
            item.setName(objStr(source.get("name")));
            item.setCuisine(objStr(source.get("cuisine")));
            item.setDifficulty(objStr(source.get("difficulty")));
            Number cookTime = (Number) source.get("cookTime");
            item.setCookTime(cookTime != null ? cookTime.intValue() : 0);
            item.setIngredients(new ArrayList<>());
            item.setSteps(new ArrayList<>());
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    private static String objStr(Object o) { return o != null ? o.toString() : ""; }

    // ==================== 以下方法保持原实现 ====================

    private void checkRateLimit(Long userId) {
        String k = "jtcsm:ai:limit:" + userId + ":" + LocalDate.now().toString();
        String v = redis.opsForValue().get(k);
        int used = v == null ? 0 : Integer.parseInt(v);
        if (used >= DAILY_LIMIT) throw new BusinessException(429, "每日 AI 生成次数已达上限（" + DAILY_LIMIT + "次/天）");
    }

    @Override
    public List<AiGenerateRecordVO> getHistory(Long userId, int page, int size) {
        int off = (page - 1) * size;
        return aiMapper.selectList(new LambdaQueryWrapper<AiGeneratedRecipe>()
                .eq(AiGeneratedRecipe::getUserId, userId)
                .orderByDesc(AiGeneratedRecipe::getCreateTime)
                .last("LIMIT " + off + "," + size))
            .stream().map(r -> {
                AiGenerateRecordVO vo = new AiGenerateRecordVO();
                vo.setId(r.getId());
                vo.setMode(r.getMode());
                vo.setInputContent(r.getInputContent());
                vo.setResultJson(r.getResultJson());
                vo.setRating(r.getRating());
                vo.setFeedback(r.getFeedback());
                vo.setCreatedAt(r.getCreateTime());
                return vo;
            }).collect(Collectors.toList());
    }

    @Override
    public AiGenerateRecordVO getHistoryDetail(Long userId, Long id) {
        AiGeneratedRecipe r = aiMapper.selectById(id);
        if (r == null || !r.getUserId().equals(userId)) throw new BusinessException(404, "记录不存在");
        AiGenerateRecordVO vo = new AiGenerateRecordVO();
        vo.setId(r.getId());
        vo.setMode(r.getMode());
        vo.setInputContent(r.getInputContent());
        vo.setResultJson(r.getResultJson());
        vo.setRating(r.getRating());
        vo.setFeedback(r.getFeedback());
        vo.setCreatedAt(r.getCreateTime());
        return vo;
    }

    @Override
    public void saveToFavorite(Long userId, Long historyId) {
        AiGeneratedRecipe r = aiMapper.selectById(historyId);
        if (r == null || !r.getUserId().equals(userId)) throw new BusinessException(404, "记录不存在");
        log.info("AI save-to-favorite: userId={}, historyId={}", userId, historyId);
    }

    @Override
    public void submitFeedback(Long userId, AiFeedbackRequest req) {
        AiGeneratedRecipe r = aiMapper.selectById(req.getHistoryId());
        if (r == null || !r.getUserId().equals(userId)) throw new BusinessException(404, "记录不存在");
        r.setRating(req.getRating());
        r.setFeedback(req.getFeedback());
        aiMapper.updateById(r);
    }

    private AiGenerateResponse buildResponse(Long id, List<AiRecipeItem> recipes, boolean fromCache) {
        AiGenerateResponse r = new AiGenerateResponse();
        r.setId(id);
        r.setRecipes(recipes);
        r.setFromCache(fromCache);
        return r;
    }

    private String toJson(Object obj) {
        try { return MAPPER.writeValueAsString(obj); }
        catch (Exception e) { return obj.toString(); }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return Integer.toHexString(input.hashCode()); }
    }
}

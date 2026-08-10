package com.jtcsm.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jtcsm.api.mapper.AiGeneratedRecipeMapper;
import com.jtcsm.api.mapper.RecipeIngredientMapper;
import com.jtcsm.api.mapper.RecipeMapper;
import com.jtcsm.api.mapper.RecipeStepMapper;
import com.jtcsm.api.service.AiService;
import com.jtcsm.api.service.FavoriteService;
import com.jtcsm.api.service.RagPipelineService;
import com.jtcsm.common.dto.AiFeedbackRequest;
import com.jtcsm.common.dto.AiGenerateRecordVO;
import com.jtcsm.common.dto.AiGenerateRequest;
import com.jtcsm.common.dto.AiGenerateResponse;
import com.jtcsm.common.dto.AiIngredientItem;
import com.jtcsm.common.dto.AiRecipeItem;
import com.jtcsm.common.dto.AiStepItem;
import com.jtcsm.common.dto.RagContext;
import com.jtcsm.common.entity.AiGeneratedRecipe;
import com.jtcsm.common.entity.Recipe;
import com.jtcsm.common.entity.RecipeIngredient;
import com.jtcsm.common.entity.RecipeStep;
import com.jtcsm.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 服务实现 —— 调用 DeepSeek 生成菜谱、RAG 检索增强、缓存、收藏和反馈
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // 会员/次数限制已注释：所有功能全部开放
    // /** 每日生成次数上限 */
    // private static final int DAILY_LIMIT = 3;

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
    @Autowired
    private RecipeMapper recipeMapper;
    @Autowired
    private RecipeIngredientMapper recipeIngredientMapper;
    @Autowired
    private RecipeStepMapper recipeStepMapper;

    @Override
    public AiGenerateResponse generate(Long userId, AiGenerateRequest request) {
        // checkRateLimit(userId); // 会员限制已注释，全部功能开放
        request.setIngredients(normalizeIngredientList(request.getIngredients()));

        String inputJson = toJson(request);
        // 尝试从缓存读取结果（MD5 作为缓存键，24小时有效）
        String cacheKey = "jtcsm:ai:cache:" + md5(inputJson);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                log.info("AI cache hit: userId={}", userId);
                List<AiRecipeItem> items = MAPPER.readValue(cached, new TypeReference<List<AiRecipeItem>>() {});
                enrichRecipeIds(items);
                items = completePlan(items, request);
                items = enrichWithDbDetails(items);
                items = limitRecipes(items, request);
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

        List<AiRecipeItem> recipes = completePlan(
                callDeepSeek(request, ragContext), request);
        recipes = enrichWithDbDetails(recipes);
        recipes = limitRecipes(recipes, request);
        enrichRecipeIds(recipes);
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

        // 记录当日生成次数（会员限制已注释，全部功能开放）
        // String today = LocalDate.now().toString();
        // redis.opsForValue().increment("jtcsm:ai:limit:" + userId + ":" + today);

        return buildResponse(record.getId(), recipes, false);
    }

    @Override
    public void generateStream(Long userId, AiGenerateRequest request, SseEmitter emitter) {
        try {
            doGenerateStream(userId, request, emitter);
        } catch (BusinessException e) {
            sendSseEvent(emitter, "error", Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.warn("AI 流式生成失败: {}", e.getMessage());
            sendSseEvent(emitter, "error", Map.of("message", "AI 生成失败，请重试"));
        } finally {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 客户端可能已断开
            }
        }
    }

    private void doGenerateStream(Long userId, AiGenerateRequest request, SseEmitter emitter) throws Exception {
        // checkRateLimit(userId); // 会员限制已注释，全部功能开放
        request.setIngredients(normalizeIngredientList(request.getIngredients()));

        String inputJson = toJson(request);
        String cacheKey = "jtcsm:ai:cache:" + md5(inputJson);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                log.info("AI stream cache hit: userId={}", userId);
                List<AiRecipeItem> items = MAPPER.readValue(cached, new TypeReference<List<AiRecipeItem>>() {});
                enrichRecipeIds(items);
                items = completePlan(items, request);
                items = enrichWithDbDetails(items);
                items = limitRecipes(items, request);
                sendSseEvent(emitter, "delta", Map.of("content", buildRecommendationReason(request, items)));
                sendSseEvent(emitter, "done", buildResponse(null, items, true));
                return;
            } catch (Exception e) {
                log.warn("Cache parse failed, regenerating", e);
            }
        }

        RagContext ragContext = ragPipelineService.retrieve(request);
        String fullText;
        try {
            fullText = streamDeepSeek(request, ragContext, emitter);
        } catch (Exception e) {
            log.warn("DeepSeek 流式调用失败，使用降级方案: {}", e.getMessage());
            sendSseEvent(emitter, "delta", Map.of("content", "AI 服务暂时不可用，以下是根据菜谱库找到的备选："));
            List<AiRecipeItem> fallback = completePlan(getFallbackRecipes(request), request);
            fallback = enrichWithDbDetails(fallback);
            fallback = limitRecipes(fallback, request);
            Long fallbackId = persistAiResult(userId, request, inputJson, fallback);
            sendSseEvent(emitter, "done", buildResponse(fallbackId, fallback, false));
            return;
        }

        List<AiRecipeItem> recipes;
        try {
            recipes = MAPPER.readValue(normalizeAiJson(extractJson(fullText)), new TypeReference<List<AiRecipeItem>>() {});
        } catch (Exception e) {
            log.warn("AI 流式结果解析失败，使用降级方案: {}", e.getMessage());
            recipes = getFallbackRecipes(request);
        }
        recipes = completePlan(recipes, request);
        recipes = enrichWithDbDetails(recipes);
        recipes = limitRecipes(recipes, request);
        enrichRecipeIds(recipes);
        sendSseEvent(emitter, "delta", Map.of("content", buildRecommendationReason(request, recipes)));
        Long recordId = persistAiResult(userId, request, inputJson, recipes);
        sendSseEvent(emitter, "done", buildResponse(recordId, recipes, false));
    }

    /** 根据请求和实际推荐结果生成推荐说明，保证说明与卡片一致 */
    private String buildRecommendationReason(AiGenerateRequest request, List<AiRecipeItem> recipes) {
        List<String> names = recipes.stream()
                .map(AiRecipeItem::getName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (names.isEmpty()) return "已为你生成以下推荐菜品：";

        StringBuilder sb = new StringBuilder("已为你推荐");
        if ("ingredients".equals(request.getMode())
                && request.getIngredients() != null
                && !request.getIngredients().isEmpty()) {
            sb.append("（结合你提供的食材 ").append(String.join("、", request.getIngredients())).append("）");
        } else if ("name".equals(request.getMode()) && StringUtils.hasText(request.getName())) {
            sb.append("「").append(request.getName()).append("」的做法");
        }
        sb.append("：").append(String.join("、", names)).append("。");
        sb.append("理由：这几道菜食材搭配合理、步骤清晰，适合家庭操作。");
        return sb.toString();
    }

    private String streamDeepSeek(AiGenerateRequest request, RagContext ragContext, SseEmitter emitter) throws Exception {
        RestClient client = restClientBuilder.build();
        String enhancedPrompt = buildPrompt(request, ragContext);
        StringBuilder full = new StringBuilder();
        client.post()
                .uri("https://api.deepseek.com/chat/completions")
                .header("Authorization", "Bearer " + System.getenv("DEEPSEEK_API_KEY"))
                .header("Content-Type", "application/json")
                .body(Map.of(
                        "model", "deepseek-chat",
                        "messages", List.of(
                                Map.of("role", "user", "content", enhancedPrompt)
                        ),
                        "temperature", 0.7,
                        "max_tokens", 4096,
                        "stream", true
                ))
                .exchange((httpRequest, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new BusinessException("DeepSeek API 错误: " + response.getStatusCode());
                    }
                        try (InputStream in = response.getBody();
                             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        // 只把推荐说明推送给前端，JSON 代码块不进入 delta
                        int[] sentLength = {0};
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("data:")) continue;
                            String payload = line.substring(5).trim();
                            if (payload.isEmpty() || "[DONE]".equals(payload)) continue;
                            JsonNode node = MAPPER.readTree(payload);
                            JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                            if (!delta.isMissingNode() && delta.isTextual()) {
                                String content = delta.asText();
                                if (content.isEmpty()) continue;
                                full.append(content);
                                int marker = full.indexOf("```");
                                int from = Math.min(sentLength[0], full.length());
                                if (marker < 0) {
                                    String sendable = full.substring(from);
                                    if (!sendable.isEmpty()) {
                                        sendSseEvent(emitter, "delta", Map.of("content", sendable));
                                        sentLength[0] = full.length();
                                    }
                                } else {
                                    int to = Math.min(marker, full.length());
                                    if (to > from) {
                                        sendSseEvent(emitter, "delta", Map.of("content", full.substring(from, to)));
                                    }
                                    sentLength[0] = to;
                                }
                            }
                        }
                    }
                    return null;
                });
        if (full.length() == 0) throw new BusinessException("AI 返回了空响应");
        return full.toString();
    }

    private Long persistAiResult(Long userId, AiGenerateRequest request, String inputJson, List<AiRecipeItem> recipes) throws Exception {
        enrichRecipeIds(recipes);
        AiGeneratedRecipe record = new AiGeneratedRecipe();
        record.setUserId(userId);
        record.setMode(request.getMode());
        record.setInputContent(inputJson);
        String resultJson = MAPPER.writeValueAsString(recipes);
        record.setResultJson(resultJson);
        aiMapper.insert(record);

        redis.opsForValue().set("jtcsm:ai:cache:" + md5(inputJson), resultJson, Duration.ofHours(24));
        // 会员限制已注释，全部功能开放
        // redis.opsForValue().increment("jtcsm:ai:limit:" + userId + ":" + LocalDate.now().toString());
        return record.getId();
    }

    /** 按菜名匹配数据库 recipe，给推荐卡片补上详情页跳转所需的 recipeId */
    private void enrichRecipeIds(List<AiRecipeItem> recipes) {
        if (recipes == null || recipes.isEmpty()) return;
        List<Recipe> all = recipeMapper.selectList(null);
        Map<String, Recipe> byName = all.stream()
                .collect(Collectors.toMap(Recipe::getName, r -> r, (a, b) -> a));
        for (AiRecipeItem item : recipes) {
            if (item == null || item.getRecipeId() != null || !StringUtils.hasText(item.getName())) continue;
            Recipe matched = byName.get(item.getName());
            if (matched == null) {
                // 名称不完全一致时，取最长的包含匹配，尽量命中已有菜谱
                String best = null;
                for (Recipe r : all) {
                    String dbName = r.getName();
                    if (dbName == null) continue;
                    if (item.getName().contains(dbName) || dbName.contains(item.getName())) {
                        if (best == null || dbName.length() > best.length()) {
                            best = dbName;
                        }
                    }
                }
                if (best != null) matched = byName.get(best);
            }
            if (matched != null) {
                item.setRecipeId(matched.getId());
                item.setCoverImage(matched.getCoverImage());
            }
        }
    }

    private void sendSseEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (Exception e) {
            throw new IllegalStateException("SSE 发送失败", e);
        }
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
            String json = normalizeAiJson(extractJson(response));
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
        AiRecipeItem first = new AiRecipeItem();
        first.setName("西红柿炒蛋");
        first.setCuisine("家常菜");
        first.setDifficulty("简单");
        first.setCookTime(15);
        AiRecipeItem second = new AiRecipeItem();
        second.setName("青椒肉丝");
        second.setCuisine("家常菜");
        second.setDifficulty("普通");
        second.setCookTime(20);
        return new ArrayList<>(List.of(first, second));
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
        int[] plan = parseDishPlan(r);
        if (plan[0] > 0 || plan[1] > 0) {
            sb.append("。请严格按照要求推荐：");
            if (plan[0] > 0) sb.append(plan[0]).append(" 道菜");
            if (plan[0] > 0 && plan[1] > 0) sb.append("、");
            if (plan[1] > 0) sb.append(plan[1]).append(" 道汤");
        } else {
            sb.append("，请推荐 1-3 道菜谱");
        }
        return sb.toString();
    }

    /** 根据用户要求解析菜品数量，返回 [菜数, 汤数] */
    private int[] parseDishPlan(AiGenerateRequest request) {
        StringBuilder text = new StringBuilder();
        if (StringUtils.hasText(request.getConditions())) text.append(request.getConditions());
        if (StringUtils.hasText(request.getName())) text.append(' ').append(request.getName());
        String t = text.toString();

        int dishes = 0;
        int soups = 0;
        Matcher m = Pattern.compile("([0-9一二三四五六七八九十两]+)\\s*菜").matcher(t);
        if (m.find()) dishes = chineseToInt(m.group(1));
        m = Pattern.compile("([0-9一二三四五六七八九十两]+)?\\s*(?:道)?\\s*汤").matcher(t);
        if (m.find()) {
            soups = (m.group(1) == null || m.group(1).isEmpty()) ? 1 : chineseToInt(m.group(1));
        }
        return new int[]{dishes, soups};
    }

    private int chineseToInt(String s) {
        if (s == null) return 0;
        if (s.matches("\\d+")) return Integer.parseInt(s);
        switch (s) {
            case "一": return 1;
            case "二": case "两": return 2;
            case "三": return 3;
            case "四": return 4;
            case "五": return 5;
            case "六": return 6;
            case "七": return 7;
            case "八": return 8;
            case "九": return 9;
            case "十": return 10;
            default: return 0;
        }
    }

    /** 统一常见食材别名，避免把西红柿/番茄、土豆/马铃薯等当成不同食材 */
    private String normalizeIngredientAliases(String name) {
        if (name == null) return null;
        return name.replace("西红柿", "番茄")
                .replace("马铃薯", "土豆")
                .replace("卷心菜", "包菜")
                .replace("圆白菜", "包菜")
                .replace("大头菜", "包菜");
    }

    private List<String> normalizeIngredientList(List<String> ingredients) {
        if (ingredients == null) return null;
        return ingredients.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(this::normalizeIngredientAliases)
                .distinct()
                .collect(Collectors.toList());
    }

    /** 按用户要求限制返回数量（默认 1-3 道，最多 8 道） */
    private List<AiRecipeItem> limitRecipes(List<AiRecipeItem> recipes, AiGenerateRequest request) {
        if (recipes == null || recipes.isEmpty()) return recipes;
        int[] plan = parseDishPlan(request);
        int total = plan[0] + plan[1];
        int max = total > 0 ? Math.min(total, 8) : 3;
        return recipes.size() > max ? new ArrayList<>(recipes.subList(0, max)) : recipes;
    }

    /** 补齐用户要求的菜/汤数量，不足时从数据库按食材匹配度补充 */
    private List<AiRecipeItem> completePlan(List<AiRecipeItem> recipes, AiGenerateRequest request) {
        if (recipes == null) recipes = new ArrayList<>();
        recipes = new ArrayList<>(recipes);
        int[] plan = parseDishPlan(request);
        int total = plan[0] + plan[1];
        if (total <= 0 || recipes.size() >= total) return recipes;

        List<String> usedNames = recipes.stream()
                .map(AiRecipeItem::getName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        int soupSlots = plan[1] - (int) recipes.stream()
                .filter(r -> r.getName() != null && r.getName().endsWith("汤"))
                .count();
        if (soupSlots < 0) soupSlots = 0;

        List<Recipe> allRecipes = recipeMapper.selectList(null);
        List<RecipeIngredient> allIngredients = recipeIngredientMapper.selectList(null);
        Map<Long, List<RecipeIngredient>> byRecipe = allIngredients.stream()
                .collect(Collectors.groupingBy(RecipeIngredient::getRecipeId));
        List<String> tokens = request.getIngredients() == null
                ? List.of()
                : request.getIngredients().stream().filter(StringUtils::hasText).collect(Collectors.toList());

        List<Recipe> candidates = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
            if (recipe == null || recipe.getName() == null) continue;
            if (usedNames.contains(recipe.getName())) continue;
            candidates.add(recipe);
        }

        candidates.sort((a, b) -> Integer.compare(
                scoreDbRecipe(b, tokens, byRecipe), scoreDbRecipe(a, tokens, byRecipe)));

        for (Recipe recipe : candidates) {
            if (recipes.size() >= total) break;
            boolean isSoup = recipe.getName().endsWith("汤");
            if (soupSlots > 0 && !isSoup) continue;
            if (soupSlots <= 0 && isSoup) continue;
            recipes.add(buildDbItem(recipe));
            usedNames.add(recipe.getName());
            if (isSoup) soupSlots--;
        }
        return recipes;
    }

    private int scoreDbRecipe(Recipe recipe, List<String> tokens,
                              Map<Long, List<RecipeIngredient>> byRecipe) {
        int score = 0;
        String normalizedName = normalizeIngredientAliases(recipe.getName());
        if (normalizedName != null) {
            for (String token : tokens) {
                if (normalizedName.contains(token)) score += 2;
            }
        }
        List<RecipeIngredient> ings = byRecipe.getOrDefault(recipe.getId(), List.of());
        for (String token : tokens) {
            for (RecipeIngredient ing : ings) {
                String normalizedIng = normalizeIngredientAliases(ing.getName());
                if (normalizedIng != null && normalizedIng.contains(token)) {
                    score += 1;
                    break;
                }
            }
        }
        return score;
    }

    /** 把 LLM 只返回菜名的卡片，用数据库完整菜谱补全用料和步骤 */
    private List<AiRecipeItem> enrichWithDbDetails(List<AiRecipeItem> recipes) {
        if (recipes == null || recipes.isEmpty()) return recipes;
        List<Recipe> all = recipeMapper.selectList(null);
        Map<String, Recipe> byName = all.stream()
                .filter(r -> r.getName() != null)
                .collect(Collectors.toMap(Recipe::getName, r -> r, (a, b) -> a));
        for (int i = 0; i < recipes.size(); i++) {
            AiRecipeItem item = recipes.get(i);
            if (item == null || item.getName() == null) continue;
            if (item.getIngredients() != null && !item.getIngredients().isEmpty()) continue;
            Recipe matched = byName.get(item.getName());
            if (matched == null) {
                String normalizedItemName = normalizeIngredientAliases(item.getName());
                for (Recipe r : all) {
                    String normalizedDbName = normalizeIngredientAliases(r.getName());
                    boolean nameHit = normalizedItemName != null && normalizedDbName != null
                            && (normalizedDbName.contains(normalizedItemName)
                            || normalizedItemName.contains(normalizedDbName));
                    if (nameHit) {
                        if (matched == null || r.getName().length() > matched.getName().length()) {
                            matched = r;
                        }
                    }
                }
            }
            if (matched != null) recipes.set(i, buildDbItem(matched));
        }
        return recipes;
    }

    /** 将数据库菜谱组装为 AI 卡片数据（含用料和步骤） */
    private AiRecipeItem buildDbItem(Recipe recipe) {
        AiRecipeItem item = new AiRecipeItem();
        item.setRecipeId(recipe.getId());
        item.setName(recipe.getName());
        item.setCuisine(recipe.getCuisine());
        item.setDifficulty(recipe.getDifficulty());
        item.setCookTime(recipe.getCookTime());
        item.setCoverImage(recipe.getCoverImage());

        List<AiIngredientItem> ingredients = recipeIngredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>()
                        .eq(RecipeIngredient::getRecipeId, recipe.getId())
                        .orderByAsc(RecipeIngredient::getSortOrder))
                .stream().map(ing -> {
                    AiIngredientItem ii = new AiIngredientItem();
                    ii.setName(ing.getName());
                    ii.setAmount(ing.getAmount());
                    return ii;
                }).collect(Collectors.toList());
        item.setIngredients(ingredients);

        List<AiStepItem> steps = recipeStepMapper.selectList(
                new LambdaQueryWrapper<RecipeStep>()
                        .eq(RecipeStep::getRecipeId, recipe.getId())
                        .orderByAsc(RecipeStep::getStepNo))
                .stream().map(st -> {
                    AiStepItem si = new AiStepItem();
                    si.setStepNo(st.getStepNo());
                    si.setContent(st.getContent());
                    si.setDuration(st.getDuration());
                    return si;
                }).collect(Collectors.toList());
        item.setSteps(steps);
        return item;
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
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1);
        return trimmed;
    }

    /**
     * 兼容 LLM 偶发的非标准输出：cookTime / duration 写成 "15分钟" 等字符串时，
     * 统一转成数字字符串，避免 Jackson 反序列化失败触发降级。
     */
    private String normalizeAiJson(String json) {
        if (json == null || json.isEmpty()) return json;
        return json
                .replaceAll("(\"(?:cookTime|duration)\"\\s*:\\s*)\"(\\d+)[^\"]*\"", "$1$2")
                .replaceAll("(\"(?:cookTime|duration)\"\\s*:\\s*)\"([^\"]*?[^0-9])(\\d+)([^0-9][^\"]*?)\"", "$1$3");
    }

    /**
     * 将 ES 检索结果映射为 AiRecipeItem（降级方案使用）
     */
    @SuppressWarnings("unchecked")
    private AiRecipeItem mapToAiItem(Map<String, Object> source) {
        try {
            AiRecipeItem item = new AiRecipeItem();
            item.setName(objStr(source.getOrDefault("recipe_name", source.get("name"))));
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

    // 会员/次数限制已注释：所有功能全部开放
    // private void checkRateLimit(Long userId) {
    //     String k = "jtcsm:ai:limit:" + userId + ":" + LocalDate.now().toString();
    //     String v = redis.opsForValue().get(k);
    //     int used = v == null ? 0 : Integer.parseInt(v);
    //     if (used >= DAILY_LIMIT) throw new BusinessException(429, "每日 AI 生成次数已达上限（" + DAILY_LIMIT + "次/天）");
    // }

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

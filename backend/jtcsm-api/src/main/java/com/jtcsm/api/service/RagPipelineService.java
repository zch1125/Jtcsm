package com.jtcsm.api.service;


import com.jtcsm.common.dto.AiGenerateRequest;
import com.jtcsm.common.dto.RagContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * RAG 流水线编排服务
 * <p>根据用户输入构建查询、检索 Markdown 知识库、组装增强上下文。</p>
 */
@Service
public class RagPipelineService {

    private static final Logger log = LoggerFactory.getLogger(RagPipelineService.class);

    /** Markdown 知识库服务 */
    @Autowired
    private KnowledgeBaseMdService knowledgeBaseMdService;

    /**
     * 执行 RAG 检索（搜索 Markdown 知识库）
     *
     * @param request 用户 AI 请求
     * @return RAG 上下文
     */
    public RagContext retrieve(AiGenerateRequest request) {
        long start = System.currentTimeMillis();

        // 1. 构建检索查询
        String query = buildSearchQuery(request);

        // 2. 检索 Markdown 知识库
        List<Map<String, Object>> results;
        if (StringUtils.hasText(query)) {
            results = knowledgeBaseMdService.search(query, 5);
        } else {
            results = List.of();
        }

        long elapsed = System.currentTimeMillis() - start;

        log.info("RAG 检索完成: mode={}, query='{}', 结果={}条, 耗时={}ms",
                request.getMode(), truncate(query, 30), results.size(), elapsed);

        return new RagContext(results, elapsed);
    }

    /**
     * 构建 RAG 增强的 System Prompt（使用 Markdown 知识库内容）
     *
     * @param userPrompt 原始用户 prompt
     * @param context    RAG 上下文
     * @return 增强后的完整 Prompt
     */
    public String buildEnhancedPrompt(String userPrompt, RagContext context) {
        // 使用 Markdown 知识库的内容作为上下文
        String contextText = formatMarkdownKnowledge(context);

        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业厨师。请根据以下食谱知识库中检索到的菜谱内容作为参考，");
        sb.append("结合用户的需求，推荐合适的菜谱。\n\n");

        if (!contextText.isEmpty()) {
            sb.append(contextText).append('\n');
        }

        sb.append("用户需求：").append(userPrompt).append("\n\n");
        sb.append("要求：\n");
        sb.append("1. 可以参考参考菜谱的搭配思路和烹饪方法，但不要直接照搬\n");
        sb.append("2. 输出必须符合用户的具体要求（食材、菜名、口味、烹饪限制等）\n");
        sb.append("3. 如果用户指定了食材，优先使用这些食材\n");
        sb.append("4. 每道菜谱的步骤应清晰完整，包含步骤序号、操作说明和预估时长\n");
        sb.append("5. 确保烹饪时间、难度评估合理\n");
        sb.append("6. 推荐数量和类型以用户要求为准（例如“四菜一汤”就推荐 4 道菜和 1 道汤）\n");
        sb.append("7. 可以自由搭配并适当补充食材，保证菜谱搭配合理、可操作\n\n");
        sb.append("不要输出任何文字说明，直接输出 ```json 代码块，包含以下格式的菜谱 JSON 数组：\n");
        sb.append("[{name,cuisine,difficulty,cookTime,ingredients:[{name,amount}],steps:[{stepNo,content,duration}]}]");

        return sb.toString();
    }

    /**
     * 从 Markdown 知识库结果中格式化上下文文本
     */
    private String formatMarkdownKnowledge(RagContext context) {
        List<Map<String, Object>> refs = context.getReferenceRecipes();
        if (refs == null || refs.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【食谱知识库参考】\n\n");

        for (int i = 0; i < refs.size(); i++) {
            Map<String, Object> r = refs.get(i);
            String name = r != null ? (String) r.get("recipe_name") : null;
            String content = r != null ? (String) r.get("content") : null;
            if (name == null || content == null) continue;

            sb.append("=== 参考菜谱 ").append(i + 1).append("：").append(name).append(" ===\n\n");
            // 截取内容的前 600 字作为参考
            String clip = content.length() > 600 ? content.substring(0, 600) + "\n...(截断)" : content;
            sb.append(clip).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 构建检索查询
     */
    private String buildSearchQuery(AiGenerateRequest request) {
        String mode = request.getMode();
        StringBuilder query = new StringBuilder();

        if ("ingredients".equals(mode)) {
            if (request.getIngredients() != null) {
                query.append(String.join(" ", request.getIngredients()));
            }
        } else if ("name".equals(mode)) {
            if (request.getName() != null) {
                query.append(request.getName());
            }
        } else if ("creative".equals(mode)) {
            if (request.getCuisineA() != null) query.append(request.getCuisineA()).append(" ");
            if (request.getCuisineB() != null) query.append(request.getCuisineB());
        } else {
            if (request.getIngredients() != null) query.append(String.join(" ", request.getIngredients())).append(" ");
            if (request.getName() != null) query.append(request.getName());
        }
        if (StringUtils.hasText(request.getConditions())) {
            query.append(" ").append(request.getConditions());
        }

        return query.toString().trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}

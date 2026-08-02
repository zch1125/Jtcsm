package com.jtcsm.common.dto;

import java.util.List;
import java.util.Map;

/**
 * RAG 上下文对象
 * <p>封装从食谱知识库中检索到的参考菜谱，用于构建增强型 Prompt。</p>
 */
public class RagContext {

    /** 检索到的菜谱列表（ES source Map，包含 recipeId/name/description/ingredientsText/stepsText 等） */
    private List<Map<String, Object>> referenceRecipes;

    /** 检索耗时（毫秒） */
    private long retrievalMs;

    public RagContext() {}

    public RagContext(List<Map<String, Object>> referenceRecipes, long retrievalMs) {
        this.referenceRecipes = referenceRecipes;
        this.retrievalMs = retrievalMs;
    }

    public List<Map<String, Object>> getReferenceRecipes() {
        return referenceRecipes;
    }

    public void setReferenceRecipes(List<Map<String, Object>> referenceRecipes) {
        this.referenceRecipes = referenceRecipes;
    }

    public long getRetrievalMs() {
        return retrievalMs;
    }

    public void setRetrievalMs(long retrievalMs) {
        this.retrievalMs = retrievalMs;
    }

    /**
     * 将检索结果格式化为文本上下文
     */
    public String formatContext() {
        if (referenceRecipes == null || referenceRecipes.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【食谱知识库参考】\n");
        for (int i = 0; i < referenceRecipes.size(); i++) {
            Map<String, Object> r = referenceRecipes.get(i);
            sb.append("--- 参考菜谱 ").append(i + 1).append(" ---\n");
            safeAppend(sb, "菜名", r.get("name"));
            safeAppend(sb, "菜系", r.get("cuisine"));
            safeAppend(sb, "难度", r.get("difficulty"));
            safeAppend(sb, "烹饪方式", r.get("cookMethod"));
            safeAppend(sb, "烹饪时间(分钟)", r.get("cookTime"));
            safeAppend(sb, "热量(千卡)", r.get("calories"));
            safeAppend(sb, "食材", r.get("ingredientsText"));
            safeAppend(sb, "做法", r.get("stepsText"));
            Object desc = r.get("description");
            if (desc != null && !desc.toString().isEmpty()) {
                safeAppend(sb, "简介", desc);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private void safeAppend(StringBuilder sb, String label, Object value) {
        if (value != null && !value.toString().isEmpty()) {
            sb.append(label).append(": ").append(value).append('\n');
        }
    }
}

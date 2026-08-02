package com.jtcsm.common.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 添加收藏请求体
 */
public class FavoriteAddRequest {

    /** 菜谱 ID */
    @NotNull(message = "菜谱ID不能为空")
    private Long recipeId;

    // ==================== Getter / Setter ====================

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
}

package com.jtcsm.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 收藏实体，对应表 user_favorite
 */
@TableName("user_favorite")
public class UserFavorite extends BaseEntity {

    /** 用户 ID */
    private Long userId;

    /** 菜谱 ID */
    private Long recipeId;

    // ==================== Getter / Setter ====================

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
}

package com.jtcsm.common.dto;

import java.time.LocalDateTime;

/**
 * 收藏列表视图对象 — 包含菜谱基本信息 + 收藏时间
 */
public class FavoriteListVO {

    /** 收藏记录 ID */
    private Long favoriteId;

    /** 菜谱 ID */
    private Long recipeId;

    /** 菜名 */
    private String name;

    /** 封面图 URL */
    private String coverImage;

    /** 菜系 */
    private String cuisine;

    /** 难度 */
    private String difficulty;

    /** 烹饪方式 */
    private String cookMethod;

    /** 烹饪时间（分钟） */
    private Integer cookTime;

    /** 收藏时间 */
    private LocalDateTime createdAt;

    // ==================== Getter / Setter ====================

    public Long getFavoriteId() { return favoriteId; }
    public void setFavoriteId(Long favoriteId) { this.favoriteId = favoriteId; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getCookMethod() { return cookMethod; }
    public void setCookMethod(String cookMethod) { this.cookMethod = cookMethod; }
    public Integer getCookTime() { return cookTime; }
    public void setCookTime(Integer cookTime) { this.cookTime = cookTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

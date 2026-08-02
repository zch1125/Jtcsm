package com.jtcsm.common.dto;

import java.time.LocalDateTime;

/**
 * 浏览历史列表视图对象 — 包含菜谱基本信息 + 浏览时间
 */
public class HistoryListVO {

    /** 历史记录 ID */
    private Long historyId;

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

    /** 浏览时间 */
    private LocalDateTime viewedAt;

    // ==================== Getter / Setter ====================

    public Long getHistoryId() { return historyId; }
    public void setHistoryId(Long historyId) { this.historyId = historyId; }
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
    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }
}

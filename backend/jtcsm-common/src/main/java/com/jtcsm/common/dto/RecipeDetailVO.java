package com.jtcsm.common.dto;

import com.jtcsm.common.entity.Recipe;
import com.jtcsm.common.entity.RecipeIngredient;
import com.jtcsm.common.entity.RecipeStep;

import java.util.List;

/**
 * 菜谱详情视图 —— 包含菜谱基本信息 + 用料清单 + 步骤列表
 */
public class RecipeDetailVO {

    private Long id;
    private String name;
    private String coverImage;
    private String description;
    private String cuisine;
    private String difficulty;
    private String cookMethod;
    private Integer cookTime;
    private Integer calories;
    private Boolean isVipOnly;
    private Integer viewCount;
    private Integer favoriteCount;

    private List<RecipeIngredient> ingredients;
    private List<RecipeStep> steps;

    /** 从 Recipe 实体构建基本信息 */
    public static RecipeDetailVO from(Recipe r) {
        RecipeDetailVO vo = new RecipeDetailVO();
        vo.id = r.getId();
        vo.name = r.getName();
        vo.coverImage = r.getCoverImage();
        vo.description = r.getDescription();
        vo.cuisine = r.getCuisine();
        vo.difficulty = r.getDifficulty();
        vo.cookMethod = r.getCookMethod();
        vo.cookTime = r.getCookTime();
        vo.calories = r.getCalories();
        vo.isVipOnly = r.getIsVipOnly();
        vo.viewCount = r.getViewCount();
        vo.favoriteCount = r.getFavoriteCount();
        return vo;
    }

    // ==================== Getter / Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getCookMethod() { return cookMethod; }
    public void setCookMethod(String cookMethod) { this.cookMethod = cookMethod; }
    public Integer getCookTime() { return cookTime; }
    public void setCookTime(Integer cookTime) { this.cookTime = cookTime; }
    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }
    public Boolean getIsVipOnly() { return isVipOnly; }
    public void setIsVipOnly(Boolean isVipOnly) { this.isVipOnly = isVipOnly; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Integer getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Integer favoriteCount) { this.favoriteCount = favoriteCount; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<RecipeIngredient> ingredients) { this.ingredients = ingredients; }
    public List<RecipeStep> getSteps() { return steps; }
    public void setSteps(List<RecipeStep> steps) { this.steps = steps; }
}

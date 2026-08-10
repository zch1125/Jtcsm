package com.jtcsm.common.dto;

import java.util.List;

public class AiRecipeItem {
    private Long recipeId;
    private String coverImage;
    private String name;
    private String cuisine;
    private String difficulty;
    private Integer cookTime;
    private List<AiIngredientItem> ingredients;
    private List<AiStepItem> steps;

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Integer getCookTime() { return cookTime; }
    public void setCookTime(Integer cookTime) { this.cookTime = cookTime; }
    public List<AiIngredientItem> getIngredients() { return ingredients; }
    public void setIngredients(List<AiIngredientItem> ingredients) { this.ingredients = ingredients; }
    public List<AiStepItem> getSteps() { return steps; }
    public void setSteps(List<AiStepItem> steps) { this.steps = steps; }
}

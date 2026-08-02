package com.jtcsm.common.dto;

import java.util.List;

public class AiGenerateResponse {
    private Long id;
    private List<AiRecipeItem> recipes;
    private boolean fromCache;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public List<AiRecipeItem> getRecipes() { return recipes; }
    public void setRecipes(List<AiRecipeItem> recipes) { this.recipes = recipes; }
    public boolean isFromCache() { return fromCache; }
    public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
}
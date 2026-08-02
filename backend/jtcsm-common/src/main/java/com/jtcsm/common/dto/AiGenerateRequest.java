package com.jtcsm.common.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class AiGenerateRequest {
    @NotBlank(message = "mode cannot be blank")
    private String mode;
    private List<String> ingredients;
    private String name;
    private String cuisineA;
    private String cuisineB;
    private String conditions;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCuisineA() { return cuisineA; }
    public void setCuisineA(String cuisineA) { this.cuisineA = cuisineA; }
    public String getCuisineB() { return cuisineB; }
    public void setCuisineB(String cuisineB) { this.cuisineB = cuisineB; }
    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }
}
package com.jtcsm.common.dto;

/**
 * 更新偏好设置请求体
 */
public class PreferenceUpdateRequest {

    private String taste;
    private String taboo;
    private String cuisine;
    private String difficulty;
    private String cookMethod;

    public String getTaste() { return taste; }
    public void setTaste(String taste) { this.taste = taste; }
    public String getTaboo() { return taboo; }
    public void setTaboo(String taboo) { this.taboo = taboo; }
    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getCookMethod() { return cookMethod; }
    public void setCookMethod(String cookMethod) { this.cookMethod = cookMethod; }
}

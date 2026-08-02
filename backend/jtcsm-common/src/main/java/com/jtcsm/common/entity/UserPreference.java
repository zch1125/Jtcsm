package com.jtcsm.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户偏好实体，对应表 user_preference
 */
@TableName("user_preference")
public class UserPreference extends BaseEntity {

    /** 用户 ID */
    private Long userId;

    /** 口味偏好（麻辣/清淡/酸甜等） */
    private String taste;

    /** 忌口食材（JSON 数组，如 ["辣椒","香菜"]） */
    private String taboo;

    /** 偏好菜系（JSON 数组，如 ["川菜","粤菜"]） */
    private String cuisine;

    /** 难度偏好（简单/普通/困难） */
    private String difficulty;

    /** 烹饪方式偏好（JSON 数组，如 ["炒","蒸"]） */
    private String cookMethod;

    // ==================== Getter / Setter ====================

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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

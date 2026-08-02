package com.jtcsm.common.dto;

/**
 * 管理后台仪表盘 VO
 */
public class AdminDashboardVO {

    /** 菜谱总数 */
    private long totalRecipes;

    /** 注册用户数 */
    private long totalUsers;

    // ==================== Getter / Setter ====================

    public long getTotalRecipes() { return totalRecipes; }
    public void setTotalRecipes(long totalRecipes) { this.totalRecipes = totalRecipes; }
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
}

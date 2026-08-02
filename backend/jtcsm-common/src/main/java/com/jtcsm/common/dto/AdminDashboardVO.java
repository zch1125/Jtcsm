package com.jtcsm.common.dto;

import java.math.BigDecimal;

/**
 * 管理后台仪表盘 VO
 */
public class AdminDashboardVO {

    /** 菜谱总数 */
    private long totalRecipes;

    /** 注册用户数 */
    private long totalUsers;

    /** 今日新增订单数 */
    private long todayOrders;

    /** VIP 会员数 */
    private long vipUsers;

    /** 今日营收 */
    private BigDecimal todayRevenue;

    /** 总订单数 */
    private long totalOrders;

    /** 总营收 */
    private BigDecimal totalRevenue;

    // ==================== Getter / Setter ====================

    public long getTotalRecipes() { return totalRecipes; }
    public void setTotalRecipes(long totalRecipes) { this.totalRecipes = totalRecipes; }
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    public long getTodayOrders() { return todayOrders; }
    public void setTodayOrders(long todayOrders) { this.todayOrders = todayOrders; }
    public long getVipUsers() { return vipUsers; }
    public void setVipUsers(long vipUsers) { this.vipUsers = vipUsers; }
    public BigDecimal getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
}

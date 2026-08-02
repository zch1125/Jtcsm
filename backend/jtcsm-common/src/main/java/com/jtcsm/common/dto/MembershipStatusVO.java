package com.jtcsm.common.dto;

import java.time.LocalDateTime;

/**
 * 会员状态视图对象
 */
public class MembershipStatusVO {

    /** 是否 VIP */
    private boolean isVip;

    /** 会员过期时间 */
    private LocalDateTime vipExpireTime;

    /** 套餐名称 */
    private String planName;

    /** 剩余天数 */
    private Long remainingDays;

    public boolean getIsVip() { return isVip; }
    public void setIsVip(boolean isVip) { this.isVip = isVip; }
    public LocalDateTime getVipExpireTime() { return vipExpireTime; }
    public void setVipExpireTime(LocalDateTime vipExpireTime) { this.vipExpireTime = vipExpireTime; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public Long getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Long remainingDays) { this.remainingDays = remainingDays; }
}

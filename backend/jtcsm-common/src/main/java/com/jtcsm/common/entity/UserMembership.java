package com.jtcsm.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 用户会员实体，对应表 user_membership
 */
@TableName("user_membership")
public class UserMembership extends BaseEntity {

    /** 用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 套餐 ID */
    @TableField("plan_id")
    private Long planId;

    /** 开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 过期时间 */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 状态 0过期 1有效 */
    private Integer status;

    // ==================== Getter / Setter ====================

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}

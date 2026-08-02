package com.jtcsm.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，对应表 `order`（MySQL 保留字，需反引号转义）
 */
@TableName("`order`")
public class Order extends BaseEntity {

    /** 订单号 */
    @TableField("order_no")
    private String orderNo;

    /** 用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 套餐 ID */
    @TableField("plan_id")
    private Long planId;

    /** 实付金额 */
    private BigDecimal amount;

    /** 状态 0待支付 1已支付 2已取消 */
    private Integer status;

    /** 支付时间 */
    @TableField("pay_time")
    private LocalDateTime payTime;

    // ==================== Getter / Setter ====================

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }
}

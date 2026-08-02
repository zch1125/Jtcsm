package com.jtcsm.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水实体，对应表 payment_record
 */
@TableName("payment_record")
public class PaymentRecord extends BaseEntity {

    /** 订单 ID */
    @TableField("order_id")
    private Long orderId;

    /** 微信支付流水号 */
    @TableField("transaction_id")
    private String transactionId;

    /** 支付方式 */
    @TableField("pay_type")
    private String payType;

    /** 支付金额 */
    private BigDecimal amount;

    /** 状态 0失败 1成功 */
    private Integer status;

    /** 支付时间 */
    @TableField("pay_time")
    private LocalDateTime payTime;

    // ==================== Getter / Setter ====================

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }
}

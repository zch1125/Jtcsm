package com.jtcsm.common.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 支付回调请求体（开发环境模拟）
 */
public class PaymentCallbackRequest {

    /** 订单号 */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /** 微信支付流水号（模拟） */
    private String transactionId;

    /** 支付方式 */
    private String payType;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
}

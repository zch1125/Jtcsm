package com.jtcsm.common.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 创建订单请求体
 */
public class CreateOrderRequest {

    /** 套餐 ID */
    @NotNull(message = "套餐ID不能为空")
    private Long planId;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
}

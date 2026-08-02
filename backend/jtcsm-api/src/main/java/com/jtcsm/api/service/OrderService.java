package com.jtcsm.api.service;

import com.jtcsm.common.dto.OrderVO;
import com.jtcsm.common.dto.PaymentCallbackRequest;

import java.util.List;

/**
 * 订单服务接口 —— 创建订单、支付回调处理和订单历史查询
 */
public interface OrderService {

    String createOrder(Long userId, Long planId);

    void handlePaymentCallback(PaymentCallbackRequest request);

    List<OrderVO> listOrders(Long userId, int page, int size);

}

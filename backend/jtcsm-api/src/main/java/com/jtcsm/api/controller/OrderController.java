package com.jtcsm.api.controller;

import com.jtcsm.api.service.OrderService;
import com.jtcsm.common.Result;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.dto.CreateOrderRequest;
import com.jtcsm.common.dto.OrderVO;
import com.jtcsm.common.dto.PaymentCallbackRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     * POST /api/v1/order/create
     */
    @PostMapping("/create")
    public Result<String> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = UserContext.getUserId();
        String orderNo = orderService.createOrder(userId, request.getPlanId());
        return Result.ok("创建成功", orderNo);
    }

    /**
     * 支付回调（开发环境模拟，无需登录）
     * POST /api/v1/order/callback
     */
    @PostMapping("/callback")
    public Result<String> paymentCallback(@Valid @RequestBody PaymentCallbackRequest request) {
        orderService.handlePaymentCallback(request);
        return Result.ok("支付成功，会员已激活", null);
    }

    /**
     * 订单历史
     * GET /api/v1/order/history?page=1&size=20
     */
    @GetMapping("/history")
    public Result<List<OrderVO>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        List<OrderVO> list = orderService.listOrders(userId, page, size);
        return Result.ok(list);
    }
}

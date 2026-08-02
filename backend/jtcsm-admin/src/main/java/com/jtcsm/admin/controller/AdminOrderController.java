package com.jtcsm.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jtcsm.admin.util.PageHelper;
import com.jtcsm.api.mapper.OrderMapper;
import com.jtcsm.common.Result;
import com.jtcsm.common.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台 — 订单管理
 * <p>提供订单列表、搜索、详情查看功能</p>
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 订单列表（分页 + 关键词/状态筛选）
     * GET /api/admin/orders?page=1&size=20&keyword=订单号&status=1
     */
    @GetMapping
    public Result<IPage<Order>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

        // 按订单号或用户ID搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Order::getOrderNo, keyword)
                    .or().eq(Order::getUserId, tryParseLong(keyword)));
        }

        // 按状态筛选
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }

        wrapper.orderByDesc(Order::getCreateTime);
        return Result.ok(PageHelper.selectPage(orderMapper, wrapper, page, size));
    }

    /**
     * 订单详情
     * GET /api/admin/orders/{id}
     */
    @GetMapping("/{id}")
    public Result<Order> get(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.notFound("订单不存在");
        }
        return Result.ok(order);
    }

    /** 安全转换 Long，避免 NumberFormatException */
    private Long tryParseLong(String s) {
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return -1L; }
    }
}

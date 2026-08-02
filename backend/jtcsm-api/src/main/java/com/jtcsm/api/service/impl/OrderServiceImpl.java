package com.jtcsm.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jtcsm.api.mapper.MembershipPlanMapper;
import com.jtcsm.api.mapper.OrderMapper;
import com.jtcsm.api.mapper.PaymentRecordMapper;
import com.jtcsm.api.mapper.UserMapper;
import com.jtcsm.api.mapper.UserMembershipMapper;
import com.jtcsm.api.service.OrderService;
import com.jtcsm.common.dto.OrderVO;
import com.jtcsm.common.dto.PaymentCallbackRequest;
import com.jtcsm.common.entity.MembershipPlan;
import com.jtcsm.common.entity.Order;
import com.jtcsm.common.entity.PaymentRecord;
import com.jtcsm.common.entity.User;
import com.jtcsm.common.entity.UserMembership;
import com.jtcsm.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MembershipPlanMapper planMapper;

    @Autowired
    private UserMembershipMapper userMembershipMapper;

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public String createOrder(Long userId, Long planId) {
        // 校验套餐是否存在且启用
        MembershipPlan plan = planMapper.selectById(planId);
        if (plan == null || plan.getIsEnabled() != 1) {
            throw new BusinessException(404, "套餐不存在或已下架");
        }

        // 生成订单号：JTCSM + 日期时间 + 6位随机数
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", (int) (Math.random() * 1000000));
        String orderNo = "JTCSM" + dateStr + random;

        // 创建订单，初始状态为待支付
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPlanId(planId);
        order.setAmount(plan.getPrice());
        order.setStatus(0); // 待支付
        orderMapper.insert(order);

        log.info("订单创建成功: orderNo={}, userId={}, planId={}, amount={}", orderNo, userId, planId, plan.getPrice());
        return orderNo;
    }

    @Override
    @Transactional
    public void handlePaymentCallback(PaymentCallbackRequest request) {
        // 根据订单号查询订单
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, request.getOrderNo()));
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(400, "订单状态异常，无法支付");
        }

        // 查询套餐信息
        MembershipPlan plan = planMapper.selectById(order.getPlanId());
        if (plan == null) {
            throw new BusinessException(404, "套餐不存在");
        }

        // 更新订单状态为已支付（status=1）
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 创建支付流水记录
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setTransactionId(request.getTransactionId());
        record.setPayType(request.getPayType() != null ? request.getPayType() : "wechat");
        record.setAmount(order.getAmount());
        record.setStatus(1); // 成功
        record.setPayTime(LocalDateTime.now());
        paymentRecordMapper.insert(record);

        // 计算会员有效期：续费场景在现有到期时间上叠加
        LocalDateTime now = LocalDateTime.now();
        UserMembership membership = userMembershipMapper.selectOne(
                new LambdaQueryWrapper<UserMembership>()
                        .eq(UserMembership::getUserId, order.getUserId()));
        LocalDateTime expireTime;
        if (membership != null && membership.getExpireTime() != null
                && membership.getExpireTime().isAfter(now)) {
            // 已有有效会员：在现有过期时间上叠加
            expireTime = membership.getExpireTime().plusDays(plan.getDays());
            membership.setExpireTime(expireTime);
            membership.setPlanId(plan.getId());
            membership.setStatus(1);
            userMembershipMapper.updateById(membership);
        } else {
            // 首次开通或已过期
            expireTime = now.plusDays(plan.getDays());
            UserMembership newMembership = new UserMembership();
            newMembership.setUserId(order.getUserId());
            newMembership.setPlanId(plan.getId());
            newMembership.setStartTime(now);
            newMembership.setExpireTime(expireTime);
            newMembership.setStatus(1);
            if (membership != null) {
                newMembership.setId(membership.getId());
                userMembershipMapper.updateById(newMembership);
            } else {
                userMembershipMapper.insert(newMembership);
            }
        }

        // 更新 user 表的 VIP 状态和过期时间
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            user.setIsVip(1);
            user.setVipExpireTime(expireTime);
            userMapper.updateById(user);
        }

        // 写入 Redis VIP 缓存，TTL 设为距离过期时间的秒数
        long ttlSeconds = Duration.between(now, expireTime).getSeconds();
        String vipKey = "jtcsm:vip:" + order.getUserId();
        stringRedisTemplate.opsForValue().set(vipKey, "1", Duration.ofSeconds(ttlSeconds));

        log.info("支付回调处理成功: orderNo={}, userId={}, expireTime={}",
                request.getOrderNo(), order.getUserId(), expireTime);
    }

    @Override
    public List<OrderVO> listOrders(Long userId, int page, int size) {
        // 分页查询用户订单，按创建时间倒序
        int offset = (page - 1) * size;
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime)
                        .last("LIMIT " + offset + "," + size));

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询套餐名称并组装 VO
        List<Long> planIds = orders.stream()
                .map(Order::getPlanId)
                .distinct()
                .collect(Collectors.toList());
        List<MembershipPlan> plans = planMapper.selectBatchIds(planIds);
        Map<Long, String> planNameMap = plans.stream()
                .collect(Collectors.toMap(MembershipPlan::getId, MembershipPlan::getName));

        return orders.stream().map(o -> {
            OrderVO vo = new OrderVO();
            vo.setId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setPlanName(planNameMap.getOrDefault(o.getPlanId(), "未知套餐"));
            vo.setAmount(o.getAmount());
            vo.setStatus(o.getStatus());
            vo.setPayTime(o.getPayTime());
            vo.setCreatedAt(o.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }
}

package com.jtcsm.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jtcsm.api.mapper.OrderMapper;
import com.jtcsm.api.mapper.RecipeMapper;
import com.jtcsm.api.mapper.UserMapper;
import com.jtcsm.common.Result;
import com.jtcsm.common.dto.AdminDashboardVO;
import com.jtcsm.common.entity.Order;
import com.jtcsm.common.entity.Recipe;
import com.jtcsm.common.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
public class DashboardController {

    @Autowired private UserMapper userMapper;
    @Autowired private RecipeMapper recipeMapper;
    @Autowired private OrderMapper orderMapper;

    @GetMapping("/dashboard")
    public Result<AdminDashboardVO> dashboard() {
        AdminDashboardVO vo = new AdminDashboardVO();

        // 用户总数（状态正常的）
        vo.setTotalUsers(userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getStatus, 1)));

        // 菜谱总数（已上架的）
        vo.setTotalRecipes(recipeMapper.selectCount(
            new LambdaQueryWrapper<Recipe>().eq(Recipe::getStatus, 1)));

        // VIP 会员数
        vo.setVipUsers(userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getIsVip, 1).eq(User::getStatus, 1)));

        // 总订单数
        vo.setTotalOrders(orderMapper.selectCount(null));

        // 今日订单数（已支付的）
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        vo.setTodayOrders(orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 1)
                .ge(Order::getPayTime, todayStart)));

        // 总营收（已支付订单金额之和）
        Double totalRevenue = orderMapper.selectList(
            new LambdaQueryWrapper<Order>().eq(Order::getStatus, 1))
            .stream().map(o -> o.getAmount().doubleValue()).reduce(0.0, Double::sum);
        vo.setTotalRevenue(BigDecimal.valueOf(totalRevenue));

        // 今日营收
        Double todayRevenue = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 1)
                .ge(Order::getPayTime, todayStart))
            .stream().map(o -> o.getAmount().doubleValue()).reduce(0.0, Double::sum);
        vo.setTodayRevenue(BigDecimal.valueOf(todayRevenue));

        return Result.ok(vo);
    }
}

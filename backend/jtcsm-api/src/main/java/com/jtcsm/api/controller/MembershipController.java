package com.jtcsm.api.controller;

import com.jtcsm.api.service.MembershipService;
import com.jtcsm.common.Result;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.dto.MembershipStatusVO;
import com.jtcsm.common.entity.MembershipPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员控制器
 */
@RestController
@RequestMapping("/api/v1/membership")
public class MembershipController {

    @Autowired
    private MembershipService membershipService;

    /**
     * 获取所有已启用的会员套餐列表（无需登录）
     * GET /api/v1/membership/plans
     */
    @GetMapping("/plans")
    public Result<List<MembershipPlan>> listPlans() {
        List<MembershipPlan> plans = membershipService.listPlans();
        return Result.ok(plans);
    }

    /**
     * 查询当前登录用户的会员状态（有效期、剩余天数等）
     * GET /api/v1/membership/status
     */
    @GetMapping("/status")
    public Result<MembershipStatusVO> getStatus() {
        Long userId = UserContext.getUserId();
        MembershipStatusVO vo = membershipService.getStatus(userId);
        return Result.ok(vo);
    }
}

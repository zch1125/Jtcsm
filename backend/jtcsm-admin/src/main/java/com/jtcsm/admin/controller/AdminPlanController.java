package com.jtcsm.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jtcsm.admin.util.PageHelper;
import com.jtcsm.api.mapper.MembershipPlanMapper;
import com.jtcsm.common.Result;
import com.jtcsm.common.entity.MembershipPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台 — 会员套餐管理
 * <p>提供套餐列表、创建、更新、删除功能</p>
 */
@RestController
@RequestMapping("/api/admin/plans")
public class AdminPlanController {

    @Autowired
    private MembershipPlanMapper planMapper;

    /**
     * 套餐列表（分页）
     * GET /api/admin/plans?page=1&size=20
     */
    @GetMapping
    public Result<IPage<MembershipPlan>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        LambdaQueryWrapper<MembershipPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(MembershipPlan::getId);
        return Result.ok(PageHelper.selectPage(planMapper, wrapper, page, size));
    }

    /**
     * 套餐详情
     * GET /api/admin/plans/{id}
     */
    @GetMapping("/{id}")
    public Result<MembershipPlan> get(@PathVariable Long id) {
        MembershipPlan plan = planMapper.selectById(id);
        if (plan == null) {
            return Result.notFound("套餐不存在");
        }
        return Result.ok(plan);
    }

    /**
     * 新增套餐
     * POST /api/admin/plans
     */
    @PostMapping
    public Result<Void> create(@RequestBody MembershipPlan plan) {
        // 默认启用
        if (plan.getIsEnabled() == null) {
            plan.setIsEnabled(1);
        }
        planMapper.insert(plan);
        return Result.ok();
    }

    /**
     * 更新套餐
     * PUT /api/admin/plans/{id}
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MembershipPlan plan) {
        plan.setId(id);
        planMapper.updateById(plan);
        return Result.ok();
    }

    /**
     * 删除套餐
     * DELETE /api/admin/plans/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        planMapper.deleteById(id);
        return Result.ok();
    }
}

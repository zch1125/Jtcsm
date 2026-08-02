package com.jtcsm.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jtcsm.admin.util.PageHelper;
import com.jtcsm.api.mapper.UserMapper;
import com.jtcsm.common.Result;
import com.jtcsm.common.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台 — 用户管理
 * <p>提供用户列表、搜索、状态切换功能</p>
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户列表（分页 + 关键词搜索）
     * GET /api/admin/users?page=1&size=20&keyword=昵称/手机号
     */
    @GetMapping
    public Result<IPage<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索：模糊匹配昵称或手机号
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }

        wrapper.orderByDesc(User::getCreateTime);
        return Result.ok(PageHelper.selectPage(userMapper, wrapper, page, size));
    }

    /**
     * 切换用户状态（启用/禁用）
     * PUT /api/admin/users/{id}/status
     */
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody User body) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        // 切换状态：1→0 或 0→1
        Integer newStatus = (user.getStatus() != null && user.getStatus() == 1) ? 0 : 1;
        user.setStatus(body.getStatus() != null ? body.getStatus() : newStatus);
        userMapper.updateById(user);
        return Result.ok();
    }
}

package com.jtcsm.api.controller;

import com.jtcsm.api.service.UserService;
import com.jtcsm.common.Result;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.dto.UserUpdateRequest;
import com.jtcsm.common.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器 —— 个人信息
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户的个人信息
     * GET /api/v1/user/profile
     */
    @GetMapping("/profile")
    public Result<User> getProfile() {
        Long userId = UserContext.getUserId();
        User user = userService.getProfile(userId);
        return Result.ok(user);
    }

    /**
     * 更新当前登录用户的个人信息（仅更新有值的字段）
     * PUT /api/v1/user/profile
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UserUpdateRequest request) {
        Long userId = UserContext.getUserId();
        userService.updateProfile(userId, request);
        return Result.ok();
    }
}

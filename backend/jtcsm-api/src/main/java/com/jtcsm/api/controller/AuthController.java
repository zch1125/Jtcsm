package com.jtcsm.api.controller;

import com.jtcsm.api.service.UserService;
import com.jtcsm.common.Result;
import com.jtcsm.common.dto.LoginRequest;
import com.jtcsm.common.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器 —— 微信登录
 */
@RestController
@RequestMapping("/api/v1/user")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 微信登录 / 模拟登录
     * <p>开发环境支持模拟登录（code 传 mock 或不传即可）</p>
     * POST /api/v1/user/login
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.ok(response);
    }
}

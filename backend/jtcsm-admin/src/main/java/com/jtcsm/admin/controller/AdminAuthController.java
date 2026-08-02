package com.jtcsm.admin.controller;

import com.jtcsm.common.Result;
import com.jtcsm.common.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    @Autowired private JwtUtil jwtUtil;
    @Value("${admin.username:admin}") private String adminUser;
    @Value("${admin.password:admin123}") private String adminPass;

    @PostMapping("/login")
    public Result<Map<String,String>> login(@RequestBody Map<String,String> body) {
        String u = body.get("username"), p = body.get("password");
        if (!adminUser.equals(u) || !adminPass.equals(p))
            return Result.fail(401, "Invalid credentials");
        String token = jwtUtil.generateToken(0L, "admin");
        return Result.ok(Map.of("token", token));
    }
}
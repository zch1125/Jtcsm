package com.jtcsm.admin.interceptor;

import com.jtcsm.common.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminJwtInterceptor implements HandlerInterceptor {
    @Autowired private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        // 登录接口放行
        if ("/api/admin/login".equals(req.getRequestURI())) return true;

        // OPTIONS 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;

        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) { resp.setStatus(401); return false; }
        try { return jwtUtil.validateToken(auth.substring(7)); }
        catch (Exception e) { resp.setStatus(401); return false; }
    }
}

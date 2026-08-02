package com.jtcsm.common.interceptor;

import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * JWT 鉴权拦截器
 * <p>
 * 从请求头中提取 token 并校验，通过后将用户 ID 写入 UserContext。
 * 白名单路径（jwt.exclude-paths）直接放行。
 * </p>
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Autowired
    private JwtUtil jwtUtil;

    /** 白名单路径列表，各模块在 application.yml 中独立配置 */
    @Value("#{'${jwt.exclude-paths:}'.split(',')}")
    private List<String> excludePaths;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        // 白名单路径直接放行
        for (String pattern : excludePaths) {
            if (pattern != null && !pattern.isEmpty() && PATH_MATCHER.match(pattern.trim(), path)) {
                return true;
            }
        }

        // 从请求头获取 token（格式：Bearer xxxxx.yyyyy.zzzzz）
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("缺少 token 或格式错误: path={}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(7);

        // 解析并验证 token
        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            String openId = claims.get("openId", String.class);

            UserContext.setUserId(userId);
            UserContext.setOpenId(openId);

            log.debug("JWT 鉴权通过: userId={}, path={}", userId, path);
            return true;
        } catch (Exception e) {
            log.warn("token 无效: path={}, error={}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}

package com.jtcsm.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局鉴权过滤器
 * <p>
 * 对所有进入网关的请求进行鉴权校验（白名单路径跳过），
 * 后续 Step 3 实现完整的 JWT 解析逻辑。
 * </p>
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);

    /** 白名单路径前缀（不校验 token） */
    private static final String[] WHITE_LIST = {
            "/api/v1/auth/",
            "/api/admin/login",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("网关请求: method={}, path={}", request.getMethod(), path);

        // 白名单路径跳过鉴权
        for (String prefix : WHITE_LIST) {
            if (path.startsWith(prefix)) {
                return chain.filter(exchange);
            }
        }

        // TODO: Step 3 实现 JWT token 校验，解析后注入 X-User-Id 请求头
        // String token = extractToken(request.getHeaders());
        // if (token == null) { return unauthorized(exchange, "缺少 token"); }
        // Claims claims = JwtUtil.parseToken(token);
        // exchange.getRequest().mutate().header("X-User-Id", claims.getSubject());

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 优先级最高，确保在其他过滤器之前执行
        return -100;
    }
}

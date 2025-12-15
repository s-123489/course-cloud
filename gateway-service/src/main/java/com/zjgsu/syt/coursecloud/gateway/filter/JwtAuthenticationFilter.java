package com.zjgsu.syt.coursecloud.gateway.filter;

import com.zjgsu.syt.coursecloud.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证过滤器
 * 拦截所有请求，验证 JWT Token，并将用户信息添加到请求头
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    // 白名单：无需认证的路径
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        log.debug("Processing request: {} {}", request.getMethod(), path);

        // 1. 白名单路径直接放行
        if (isWhiteListPath(path)) {
            log.debug("White list path, skipping authentication: {}", path);
            return chain.filter(exchange);
        }

        // 2. 获取 Authorization 请求头
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return unauthorized(exchange.getResponse(), "Missing or invalid Authorization header");
        }

        // 3. 提取 Token（去掉 "Bearer " 前缀）
        String token = authHeader.substring(7);

        // 4. 验证 Token 有效性
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid or expired JWT token for path: {}", path);
            return unauthorized(exchange.getResponse(), "Invalid or expired token");
        }

        // 5. 解析 Token 获取用户信息
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (Exception e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            return unauthorized(exchange.getResponse(), "Failed to parse token");
        }

        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        log.info("Authenticated user: {} (ID: {}, Role: {}) for path: {}", username, userId, role, path);

        // 6. 将用户信息添加到请求头
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .header("X-User-Role", role)
                .build();

        // 7. 转发请求到下游服务
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhiteListPath(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        log.debug("Returning 401 Unauthorized: {}", message);
        return response.setComplete();
    }

    /**
     * 设置过滤器优先级（数值越小，优先级越高）
     * -100 确保在其他过滤器之前执行
     */
    @Override
    public int getOrder() {
        return -100;
    }
}

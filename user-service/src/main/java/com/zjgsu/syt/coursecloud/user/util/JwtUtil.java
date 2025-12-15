package com.zjgsu.syt.coursecloud.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类
 * 用于生成、解析和验证 JWT Token
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 生成 JWT Token
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     用户角色
     * @return JWT Token 字符串
     */
    public String generateToken(String userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        String token = Jwts.builder()
                .setSubject(userId)  // 设置主题（用户 ID）
                .claim("username", username)  // 添加自定义声明：用户名
                .claim("role", role)  // 添加自定义声明：角色
                .setIssuedAt(now)  // 设置签发时间
                .setExpiration(expiryDate)  // 设置过期时间
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS512)  // 使用 HS512 算法签名
                .compact();

        log.debug("Generated JWT token for user: {} (ID: {}), expires at: {}", username, userId, expiryDate);
        return token;
    }

    /**
     * 解析 JWT Token
     *
     * @param token JWT Token 字符串
     * @return Claims 对象，包含 Token 中的所有声明
     */
    public Claims parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.debug("Successfully parsed JWT token for user: {} (ID: {})",
                    claims.get("username"), claims.getSubject());
            return claims;
        } catch (Exception e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证 JWT Token 的有效性
     *
     * @param token JWT Token 字符串
     * @return true 如果 Token 有效，false 如果 Token 无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            boolean isValid = expiration.after(new Date());

            if (!isValid) {
                log.warn("JWT token has expired: {}", expiration);
            }

            return isValid;
        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }
}

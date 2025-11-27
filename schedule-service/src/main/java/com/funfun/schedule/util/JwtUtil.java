package com.funfun.schedule.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与解析工具（适配 JJWT 0.12+ 新 API）
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret; // 建议配置为至少 32 字符的随机字符串

    @Value("${jwt.expire}")
    private long expire; // 过期时间（秒）

    /**
     * 生成 SecretKey（自动处理密钥长度不足问题）
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Token（存储 userId，这里用小程序 openid 作为 userId）
     */
    public String generateToken(String userId) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expire * 1000);

        return Jwts.builder()
                .subject(userId)               // 替代 setSubject()
                .issuedAt(now)
                .expiration(expireDate)        // 替代 setExpiration()
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token，获取 userId
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSignInKey())   // 替代 setSigningKey()
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();                // 替代 getBody()

            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT 格式不支持: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT 结构损坏: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("JWT 签名无效: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT 参数非法: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT 解析未知错误", e);
        }
        return null;
    }

    /**
     * 验证 Token 是否有效（未过期且签名正确）
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token 无效: {}", e.getMessage());
            return false;
        }
    }
}
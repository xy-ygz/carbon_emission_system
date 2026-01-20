package com.bjfu.carbon.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token提供者
 * 单一职责：只负责JWT Token的生成和解析
 *
 * @author xgy
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:carbon-emission-secret-key-this-is-a-very-long-secret-key-for-jwt-token-generation}")
    private String secret;

    @Value("${jwt.expiration:1800000}") // 30分钟
    private Long expiration;

    @Value("${jwt.refresh.expiration:86400000}") // 1天
    private Long refreshExpiration;

    /**
     * 生成Token
     *
     * @param username 用户名
     * @param userId   用户ID
     * @return Token字符串
     */
    public String generateToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("userId", userId);
        return createToken(claims, username, expiration);
    }

    /**
     * 生成Refresh Token
     *
     * @param username 用户名
     * @param userId   用户ID
     * @return Refresh Token字符串
     */
    public String generateRefreshToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("userId", userId);
        return createToken(claims, username, refreshExpiration);
    }

    /**
     * 创建Token
     *
     * @param claims      声明
     * @param subject     主题（用户名）
     * @param expireTime  过期时间（毫秒）
     * @return Token字符串
     */
    private String createToken(Map<String, Object> claims, String subject, Long expireTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 从Token中获取用户名
     *
     * @param token Token字符串
     * @return 用户名，如果Token无效返回null
     */
    public String getUsernameFromToken(String token) {
        try {
            return getClaimFromToken(token, Claims::getSubject);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token Token字符串
     * @return 用户ID，如果Token无效返回null
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return Long.valueOf(claims.get("userId").toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从Token中获取过期时间
     *
     * @param token Token字符串
     * @return 过期时间，如果Token无效返回null
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            return getClaimFromToken(token, Claims::getExpiration);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从Token中获取声明
     *
     * @param token          Token字符串
     * @param claimsResolver 声明解析器
     * @param <T>            返回类型
     * @return 声明值，如果Token无效返回null
     */
    public <T> T getClaimFromToken(String token, java.util.function.Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = getAllClaimsFromToken(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从Token中获取所有声明
     *
     * @param token Token字符串
     * @return 所有声明，如果Token无效抛出异常
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证Token是否过期
     *
     * @param token Token字符串
     * @return 是否过期
     */
    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration == null || expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证Token是否有效
     *
     * @param token    Token字符串
     * @param username 用户名
     * @return 是否有效
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = getUsernameFromToken(token);
            return (tokenUsername != null && tokenUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
}


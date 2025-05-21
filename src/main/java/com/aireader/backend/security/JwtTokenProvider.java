package com.aireader.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT令牌提供者，负责生成、验证和解析JWT令牌
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        // 基于配置的密钥创建HMAC SHA-256密钥
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建访问令牌
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param roles 角色列表
     * @return JWT令牌
     */
    public String createAccessToken(String userId, String username, Collection<? extends GrantedAuthority> roles) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("username", username);
        claims.put("roles", roles.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * 创建刷新令牌
     * 
     * @param userId 用户ID
     * @return 刷新令牌
     */
    public String createRefreshToken(String userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * 从JWT令牌解析用户认证信息
     * 
     * @param token JWT令牌
     * @return 认证信息
     */
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("roles", String[].class))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        User principal = new User(username, "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * 验证JWT令牌是否有效
     * 
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            log.error("无效的JWT签名");
        } catch (MalformedJwtException ex) {
            log.error("无效的JWT令牌");
        } catch (ExpiredJwtException ex) {
            log.error("过期的JWT令牌");
        } catch (UnsupportedJwtException ex) {
            log.error("不支持的JWT令牌");
        } catch (IllegalArgumentException ex) {
            log.error("JWT声明为空");
        }
        return false;
    }

    /**
     * 从令牌中获取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
} 
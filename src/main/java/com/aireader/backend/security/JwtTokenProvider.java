package com.aireader.backend.security;

import com.aireader.backend.model.security.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT令牌提供者，负责生成、验证和解析JWT令牌
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKeyString;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        key = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建访问令牌
     * 
     * @param userId 用户ID (UUID)
     * @param actualUsername 用户的实际登录名
     * @param roles 角色列表
     * @return JWT令牌
     */
    public String createAccessToken(String userId, String actualUsername, Collection<? extends GrantedAuthority> roles) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("username", actualUsername);
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
     * @param userId 用户ID (UUID)
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
        String actualUsername = claims.get("username", String.class);
        
        List<String> rolesList = claims.get("roles", List.class);
        Collection<? extends GrantedAuthority> authorities = rolesList.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        UserDetails principal = new CustomUserDetails(userId, actualUsername, "", authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ANONYMOUS")), true, true, true, authorities);

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
            log.error("无效的JWT签名: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("无效的JWT令牌: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("过期的JWT令牌: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("不支持的JWT令牌: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT声明为空: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * 从令牌中获取用户ID (Subject)
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
    
    /**
     * 从令牌中获取实际登录用户名 (Custom "username" claim)
     * 
     * @param token JWT令牌
     * @return 实际登录用户名
     */
    public String getActualUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("username", String.class);
    }
    
    /**
     * 根据认证信息生成令牌 (主要由登录成功后调用)
     * 
     * @param authentication 认证信息 (principal 应为 CustomUserDetails)
     * @return JWT访问令牌
     */
    public String generateToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            log.warn("Authentication principal is not CustomUserDetails. Falling back to getUsername() for userId, which might be incorrect if not user ID.");
            UserDetails userDetails = (UserDetails) principal;
            return createAccessToken(userDetails.getUsername(), userDetails.getUsername(), userDetails.getAuthorities());
        }
        
        CustomUserDetails customUserDetails = (CustomUserDetails) principal;
        return createAccessToken(
                customUserDetails.getId(),
                customUserDetails.getActualUsername(),
                customUserDetails.getAuthorities()
        );
    }
    
    /**
     * 根据认证信息生成刷新令牌 (主要由登录成功后调用)
     * 
     * @param authentication 认证信息 (principal 的 getUsername() 应返回 userId)
     * @return JWT刷新令牌
     */
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return createRefreshToken(userDetails.getUsername());
    }
    
    /**
     * 根据用户ID和实际用户名生成访问令牌。
     * 用于刷新令牌等场景，其中我们有用户ID和用户名，但不一定有完整的Authentication对象。
     * 
     * @param userId 用户的唯一ID (UUID)
     * @param actualUsername 用户的实际登录名
     * @param authorities 用户的权限
     * @return JWT访问令牌
     */
    public String generateTokenFromUserIdAndUsername(String userId, String actualUsername, Collection<? extends GrantedAuthority> authorities) {
        return createAccessToken(userId, actualUsername, authorities);
    }
} 
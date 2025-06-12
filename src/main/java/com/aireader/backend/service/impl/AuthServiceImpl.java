package com.aireader.backend.service.impl;

import com.aireader.backend.dto.auth.JwtAuthenticationResponseDto;
import com.aireader.backend.dto.auth.LoginRequestDto;
import com.aireader.backend.dto.auth.UserRegistrationRequestDto;
import com.aireader.backend.dto.auth.UserResponseDto;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.security.JwtTokenProvider;
import com.aireader.backend.service.AuthService;
import com.aireader.backend.service.CustomUserDetailsService;
import com.aireader.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 认证服务实现类
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;
    
    /**
     * 用户注册
     * 
     * @param registrationRequest 注册请求
     * @return 注册成功的用户信息
     */
    @Override
    public UserResponseDto register(UserRegistrationRequestDto registrationRequest) {
        return userService.registerUser(registrationRequest);
    }
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return JWT认证响应
     */
    @Override
    public JwtAuthenticationResponseDto login(LoginRequestDto loginRequest) {
        // 进行身份验证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );
        
        // 设置认证信息到安全上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 生成JWT令牌
        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);
        
        // 返回JWT认证响应
        return JwtAuthenticationResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .build();
    }
    
    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的JWT认证响应
     */
    @Override
    public JwtAuthenticationResponseDto refreshToken(String refreshToken) {
        // 验证刷新令牌
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("刷新令牌无效或已过期");
        }
        
        // 从刷新令牌中获取用户ID (subject) 和 实际用户名 (custom claim)
        String userId = tokenProvider.getUserIdFromToken(refreshToken);
        String actualUsername = tokenProvider.getActualUsernameFromToken(refreshToken);

        // 为了生成包含权限的新access token，我们需要用户的权限信息
        com.aireader.backend.model.jpa.User userEntity = userService.findById(userId)
            .orElseThrow(() -> new RuntimeException("刷新令牌时找不到用户ID: " + userId));
        Collection<? extends GrantedAuthority> authorities = userEntity.getRoles().stream()
            .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.toString()))
            .collect(java.util.stream.Collectors.toList());

        // 使用新的方法生成访问令牌
        String accessToken = tokenProvider.generateTokenFromUserIdAndUsername(userId, actualUsername, authorities);
        
        // 返回新的JWT认证响应
        return JwtAuthenticationResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken) // 保持相同的刷新令牌
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .build();
    }
    
    /**
     * 获取当前登录用户信息
     * 
     * @return 当前用户信息
     */
    @Override
    public UserResponseDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("未找到有效认证信息或用户未登录");
        }
        
        Object principal = authentication.getPrincipal();
        String userId;
        if (principal instanceof UserDetails) {
            userId = ((UserDetails) principal).getUsername(); // 这现在是用户ID
        } else {
            userId = principal.toString();
        }
        
        com.aireader.backend.model.jpa.User user = userService.findById(userId) 
                .orElseThrow(() -> new RuntimeException("未找到当前用户 (ID: " + userId + ")"));
        
        return userService.convertToDto(user);
    }
    
    /**
     * 检查用户名是否已存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    @Override
    public boolean existsByUsername(String username) {
        return userService.existsByUsername(username);
    }
    
    /**
     * 检查邮箱是否已存在
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    @Override
    public boolean existsByEmail(String email) {
        return userService.existsByEmail(email);
    }
} 
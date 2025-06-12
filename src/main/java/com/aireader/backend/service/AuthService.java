package com.aireader.backend.service;

import com.aireader.backend.dto.auth.JwtAuthenticationResponseDto;
import com.aireader.backend.dto.auth.LoginRequestDto;
import com.aireader.backend.dto.auth.UserRegistrationRequestDto;
import com.aireader.backend.dto.auth.UserResponseDto;

/**
 * 认证服务接口
 * 提供用户认证相关的业务逻辑
 */
public interface AuthService {
    
    /**
     * 用户注册
     * 
     * @param registrationRequest 注册请求
     * @return 注册成功的用户信息
     */
    UserResponseDto register(UserRegistrationRequestDto registrationRequest);
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return JWT认证响应
     */
    JwtAuthenticationResponseDto login(LoginRequestDto loginRequest);
    
    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的JWT认证响应
     */
    JwtAuthenticationResponseDto refreshToken(String refreshToken);
    
    /**
     * 获取当前登录用户信息
     * 
     * @return 当前用户信息
     */
    UserResponseDto getCurrentUser();
    
    /**
     * 检查用户名是否已存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否已存在
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);
} 
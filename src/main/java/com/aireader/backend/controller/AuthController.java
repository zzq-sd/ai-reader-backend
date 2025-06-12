package com.aireader.backend.controller;

import com.aireader.backend.dto.auth.JwtAuthenticationResponseDto;
import com.aireader.backend.dto.auth.LoginRequestDto;
import com.aireader.backend.dto.auth.UserRegistrationRequestDto;
import com.aireader.backend.dto.auth.UserResponseDto;
import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理用户注册、登录、令牌刷新等认证相关请求
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证接口", description = "提供用户注册、登录、令牌刷新等功能")
public class AuthController extends BaseController {
    
    @Autowired
    private AuthService authService;
    
    /**
     * 用户注册
     * 
     * @param registrationRequest 注册请求
     * @return 注册成功的用户信息
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账号")
    public ResponseEntity<ApiResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequestDto registrationRequest) {
        UserResponseDto userResponse = authService.register(registrationRequest);
        return new ResponseEntity<>(ApiResponse.success(userResponse), HttpStatus.CREATED);
    }
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return JWT认证响应
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "验证用户凭证并返回JWT令牌")
    public ResponseEntity<ApiResponse> login(
            @Valid @RequestBody LoginRequestDto loginRequest) {
        JwtAuthenticationResponseDto response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的JWT认证响应
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public ResponseEntity<ApiResponse> refreshToken(
            @RequestParam String refreshToken) {
        JwtAuthenticationResponseDto response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 获取当前登录用户信息
     * 
     * @return 当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "返回当前登录用户的详细信息")
    public ResponseEntity<ApiResponse> getCurrentUser() {
        UserResponseDto userResponse = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
    
    /**
     * 检查用户名是否可用
     * 
     * @param username 用户名
     * @return 检查结果
     */
    @GetMapping("/check-username")
    @Operation(summary = "检查用户名是否可用", description = "检查指定的用户名是否已被注册")
    public ResponseEntity<ApiResponse> checkUsernameAvailability(
            @RequestParam String username) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", !authService.existsByUsername(username));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 检查邮箱是否可用
     * 
     * @param email 邮箱
     * @return 检查结果
     */
    @GetMapping("/check-email")
    @Operation(summary = "检查邮箱是否可用", description = "检查指定的邮箱是否已被注册")
    public ResponseEntity<ApiResponse> checkEmailAvailability(
            @RequestParam String email) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", !authService.existsByEmail(email));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
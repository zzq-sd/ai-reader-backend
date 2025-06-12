package com.aireader.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应DTO
 * 用于向前端返回用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    
    /**
     * 用户ID
     */
    private String id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 姓名
     */
    private String fullName;
    
    /**
     * 用户角色列表
     */
    private List<String> roles;
    
    /**
     * 账号是否启用
     */
    private boolean enabled;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
} 
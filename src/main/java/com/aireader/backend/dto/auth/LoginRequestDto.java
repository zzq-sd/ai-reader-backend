package com.aireader.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    
    /**
     * 用户名或邮箱
     */
    @NotBlank(message = "用户名或邮箱不能为空")
    private String usernameOrEmail;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
} 
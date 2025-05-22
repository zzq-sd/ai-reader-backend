package com.aireader.backend.controller;

import com.aireader.backend.model.jpa.Role;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.repository.RoleRepository;
import com.aireader.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 用于验证数据库配置和基本功能
 * 仅在开发环境中使用
 */
@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    /**
     * 测试数据库连接和数据
     * 
     * @return 测试结果
     */
    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 测试MySQL连接
            long userCount = userRepository.count();
            long roleCount = roleRepository.count();
            
            response.put("status", "success");
            response.put("mysqlConnection", "ok");
            response.put("userCount", userCount);
            response.put("roleCount", roleCount);
            
            // 获取管理员用户信息（如果存在）
            userRepository.findByUsername("admin").ifPresent(admin -> {
                Map<String, Object> adminInfo = new HashMap<>();
                adminInfo.put("id", admin.getId());
                adminInfo.put("email", admin.getEmail());
                adminInfo.put("roles", admin.getRoles().stream().map(Role::getName).toArray());
                response.put("adminUser", adminInfo);
            });
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 测试安全配置
     * 
     * @return 测试结果
     */
    @GetMapping("/secured")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testSecured() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "您已成功访问需要管理员权限的资源");
        return ResponseEntity.ok(response);
    }
} 
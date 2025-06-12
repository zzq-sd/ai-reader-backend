package com.aireader.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * 根路径控制器
 * 用于处理不带/api/v1前缀的根路径请求
 * 确保这些请求不会被错误地视为静态资源请求
 * 
 * 注意：此控制器使用低优先级，只有在其他控制器无法处理请求时才会生效
 */
@RestController
@RequestMapping(path = "/direct") // 添加/direct前缀，避免与/api/v1冲突
@Slf4j
public class RootPathController {

    private final AiChatController aiChatController;
    
    @Autowired
    public RootPathController(AiChatController aiChatController) {
        this.aiChatController = aiChatController;
    }
    
    /**
     * 处理根路径下的AI聊天健康检查请求
     * 这确保/ai/chat/health请求能够被控制器方法处理
     * 而不是被错误地交给静态资源处理器
     */
    @GetMapping("/ai/chat/health")
    public ResponseEntity<Map<String, Object>> rootAiChatHealthCheck() {
        log.info("接收到根路径下的AI聊天健康检查请求: /direct/ai/chat/health");
        
        // 直接返回可用状态，避免调用AI模型
        Map<String, Object> response = new HashMap<>();
        response.put("status", "redirected");
        response.put("message", "请使用正确的API路径: /api/v1/ai/chat/health");
        response.put("note", "这是从直接路径处理的请求，功能可能受限");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 处理根路径下的流式聊天请求
     * 避免因错误的URL导致的静态资源未找到异常
     */
    @GetMapping("/chat/stream")
    public ResponseEntity<Map<String, Object>> handleChatStreamGet() {
        log.info("接收到GET请求: /direct/chat/stream，此端点需要POST请求");
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Method not allowed");
        response.put("message", "此端点需要POST请求而不是GET请求");
        response.put("correctPath", "/api/v1/ai/chat/stream");
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 处理根路径下的流式聊天请求(POST方法)
     */
    @PostMapping("/chat/stream")
    public ResponseEntity<Map<String, Object>> handleChatStreamPost() {
        log.info("接收到根路径下的POST请求: /direct/chat/stream");
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid API path");
        response.put("message", "请使用正确的API路径发送请求");
        response.put("correctPath", "/api/v1/ai/chat/stream");
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 通用处理未映射的API请求
     * 这将捕获所有尝试访问/api/v1下不存在的路径的请求
     */
    @RequestMapping("/api/v1/**")
    public ResponseEntity<Map<String, Object>> handleUnmappedApiRequests() {
        String requestURI = "/api/v1/**"; // 实际URI会在日志中打印
        try {
            // 使用Spring的RequestContextHolder获取当前请求的URI
            HttpServletRequest request = 
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
            requestURI = request.getRequestURI();
        } catch (Exception e) {
            // 忽略错误
        }
        
        log.info("接收到未映射的API请求: {}", requestURI);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Endpoint not found");
        response.put("message", "请求的API端点不存在");
        response.put("path", requestURI);
        response.put("supportedPaths", new String[] {
            "/api/v1/ai/chat/health",
            "/api/v1/ai/chat/stream",
            "/api/v1/ai/chat",
            "/api/v1/ai/chat/advanced",
            "/api/v1/ai/chat/advanced/stream"
        });
        
        // 对于SSE请求，返回SSE格式的错误
        if (requestURI.contains("/stream")) {
            return ResponseEntity
                .status(404)
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(response);
        }
        
        return ResponseEntity.status(404).body(response);
    }
} 
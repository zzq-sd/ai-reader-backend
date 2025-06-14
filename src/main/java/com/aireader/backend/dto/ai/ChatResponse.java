package com.aireader.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    /**
     * 响应ID
     */
    private String id;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 响应内容
     */
    private String content;
    
    /**
     * 创建时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 响应角色（如"assistant"）
     */
    private String role;
    
    /**
     * 是否为流式响应的最后一部分
     */
    private boolean done;
} 
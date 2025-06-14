package com.aireader.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 聊天请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    /**
     * 用户消息
     */
    private String message;
    
    /**
     * 会话ID（可选）
     * 用于关联同一会话的多个消息
     */
    private String sessionId;
    
    /**
     * 历史消息（可选）
     * 用于保持上下文连续性
     */
    private List<Map<String, String>> history;
    
    /**
     * 系统提示（可选）
     * 用于设置AI助手的行为和角色
     */
    private String systemPrompt;
    
    /**
     * 流式响应标志（可选）
     * 是否使用流式响应
     */
    private boolean stream;
} 
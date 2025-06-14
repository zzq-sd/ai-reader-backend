package com.aireader.backend.controller;

import com.aireader.backend.dto.ai.ChatRequest;
import com.aireader.backend.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import org.springframework.http.MediaType;

import java.util.Map;

/**
 * AI聊天控制器
 * 已重构以完全依赖ChatClientManager进行动态模型选择
 */
@RestController
@RequestMapping("/ai/chat")
@Tag(name = "AI 对话接口", description = "提供与AI助手的交互式对话功能")
@Slf4j
@CrossOrigin(origins = "*")
public class AiChatController {

    private final AiService aiService;

    @Autowired
    public AiChatController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * @deprecated 该阻塞式接口已被废弃，请统一使用 /stream 接口
     */
    @Deprecated
    @PostMapping("/message")
    public com.aireader.backend.dto.ai.ChatResponse chat(@RequestBody ChatRequest request) {
        log.warn("警告：检测到对已废弃的阻塞式 /ai/chat/message 接口的调用。请将客户端更新至 /ai/chat/stream。");
        return aiService.chatWithAi(request);
    }
    
    /**
     * 接收流式聊天请求
     * 优化：直接返回Flux<ChatResponse>，并指定produces为TEXT_EVENT_STREAM_VALUE。
     * Spring WebFlux将自动处理SSE的复杂性，这是最健壮和推荐的方式。
     *
     * @param request 聊天请求
     * @return 包含聊天响应的响应式流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<com.aireader.backend.dto.ai.ChatResponse> streamChat(@RequestBody ChatRequest request) {
        log.info("AiChatController 接收到SSE聊天请求，转交给 AiService 处理。会话ID: {}", request.getSessionId());
        return aiService.streamChatWithAi(request);
    }

    /**
     * AI助手健康检查接口
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        log.info("执行AI助手健康检查");
        try {
            // 注意：健康检查现在将使用服务层来获取客户端，从而测试动态选择逻辑
            ChatRequest healthCheckRequest = ChatRequest.builder()
                    .message("Respond with just 'OK'.")
                    .sessionId("health-check")
                    .build();
            String testResponse = aiService.chatWithAi(healthCheckRequest).getContent();
            
            return Map.of(
                "status", "healthy", 
                "message", "AI assistant service is running normally.",
                "test_response_length", testResponse.length()
            );
        } catch (Exception e) {
            log.error("AI助手健康检查失败", e);
            return Map.of(
                "status", "unhealthy", 
                "message", "AI assistant service failed health check: " + e.getMessage()
            );
        }
    }
    
    /**
     * 简单聊天接口
     * 发送一条消息并返回AI的回复
     */
    @PostMapping("/simple")
    @Operation(summary = "简单阻塞式对话", description = "与AI助手进行一次简单的请求-响应式对话")
    public com.aireader.backend.dto.ai.ChatResponse simpleChat(@RequestBody ChatRequest request) {
        log.info("接收到简单聊天请求，将转交给 AiService 处理。会话ID: {}", request.getSessionId());
        return aiService.chatWithAi(request);
    }
} 
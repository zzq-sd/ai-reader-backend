package com.aireader.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * AI聊天控制器
 * 基于Spring AI 1.0正式版实现
 * 支持GLM模型和流式传输
 */
@RestController
@RequestMapping("/ai/chat")
@Slf4j
@CrossOrigin(origins = "*")
public class AiChatController {

    private final ChatModel chatModel;
    private final ChatClient chatClient;

    @Autowired
    public AiChatController(
            ChatModel chatModel, 
            ChatClient chatClient) {
        this.chatModel = chatModel;
        this.chatClient = chatClient;
    }

    /**
     * 使用ChatClient处理聊天请求
     */
    @PostMapping("/message")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        log.info("收到聊天请求: {}", message);
        
        try {
            if (chatClient == null) {
                log.error("ChatClient未配置，无法处理请求");
                return Map.of(
                    "error", "AI服务未正确配置", 
                    "message", "请检查API配置，确保API密钥有效"
                );
            }
            
            String response = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
            
            log.info("AI回复 (前50个字符): {}", 
                response.substring(0, Math.min(50, response.length())));
            
            return Map.of("response", response);
        } catch (Exception e) {
            log.error("聊天处理失败", e);
            
            // 根据错误类型提供更具体的错误信息
            String errorMessage = "处理请求时发生错误";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized")) {
                    errorMessage = "API密钥验证失败，请检查API密钥配置";
                } else if (e.getMessage().contains("timeout") || e.getMessage().contains("connect")) {
                    errorMessage = "连接API服务超时，请检查网络配置";
                } else if (e.getMessage().contains("server authentication")) {
                    errorMessage = "服务器身份验证失败，请检查API密钥和base-url配置";
                }
            }
            
            return Map.of(
                "error", errorMessage,
                "fallback", "AI服务暂不可用，请稍后再试或联系管理员"
            );
        }
    }

    /**
     * 使用ChatModel直接处理聊天请求
     */
    @PostMapping("/direct")
    public Map<String, String> directChat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        log.info("收到直接聊天请求: {}", message);
        
        try {
            String response = chatModel.call(message);
            
            log.info("AI直接回复 (前50个字符): {}", 
                response.substring(0, Math.min(50, response.length())));
            
            return Map.of("response", response);
        } catch (Exception e) {
            log.error("直接聊天处理失败", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    /**
     * 流式处理聊天请求 - SSE格式
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        log.info("收到流式聊天请求: {}", message);
        
        try {
            Prompt prompt = new Prompt(new UserMessage(message));
            return chatModel.stream(prompt)
                .map(chatResponse -> {
                    // 提取消息内容
                    String content = "";
                    if (chatResponse.getResult() != null && 
                        chatResponse.getResult().getOutput() != null) {
                        content = chatResponse.getResult().getOutput().getText();
                    }
                    
                    // 转换为SSE格式  
                    return "data: " + content + "\n\n";
                })
                .concatWith(Flux.just("data: [DONE]\n\n"))
                .doOnError(error -> log.error("流式聊天处理失败", error))
                .onErrorReturn("data: {\"error\": \"处理请求时发生错误\"}\n\n");
        } catch (Exception e) {
            log.error("流式聊天初始化失败", e);
            return Flux.just("data: {\"error\": \"" + e.getMessage() + "\"}\n\n");
        }
    }

    /**
     * 简化的流式聊天接口 - 直接返回ChatResponse
     */
    @PostMapping(value = "/stream-simple", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChatSimple(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        log.info("收到简化流式聊天请求: {}", message);
        
        try {
            Prompt prompt = new Prompt(new UserMessage(message));
            return chatModel.stream(prompt)
                .doOnError(error -> log.error("简化流式聊天处理失败", error));
        } catch (Exception e) {
            log.error("简化流式聊天初始化失败", e);
            return Flux.error(e);
        }
    }

    /**
     * AI助手健康检查接口
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        try {
            // 测试基本连接
            String testResponse = chatClient.prompt()
                .user("Hello")
                .call()
                .content();
            
            return Map.of(
                "status", "healthy",
                "message", "AI助手服务正常",
                "test_response_length", testResponse.length()
            );
        } catch (Exception e) {
            log.error("AI助手健康检查失败", e);
            return Map.of(
                "status", "unhealthy",
                "message", "AI助手服务异常: " + e.getMessage()
            );
        }
    }
} 
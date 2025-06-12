package com.aireader.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * AI模型控制器
 * 智谱AI GLM模型演示
 */
@RestController
@RequestMapping("/api/v1/ai")
@Slf4j
public class AiController {

    private final ChatClient chatClient; // GLM模型 (智谱AI)

    @Autowired
    public AiController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 使用GLM模型回答问题
     */
    @GetMapping("/ask")
    public Map<String, Object> ask(@RequestParam String question) {
        log.info("使用GLM模型回答问题: {}", question);
        
        try {
            String answer = chatClient.prompt()
                    .user(question)
                    .call()
                    .content();

            Map<String, Object> response = new HashMap<>();
            response.put("model", "GLM");
            response.put("question", question);
            response.put("answer", answer);
            return response;
        } catch (Exception e) {
            log.error("GLM模型调用失败", e);
            return createErrorResponse("GLM", question, e.getMessage());
        }
    }

    /**
     * 使用指定模型回答问题
     * 注意：当前仅支持GLM模型
     */
    @GetMapping("/ask/model")
    public Map<String, Object> askWithModel(
            @RequestParam String question,
            @RequestParam(defaultValue = "glm") String model) {
        
        log.info("使用GLM模型回答问题: {}", question);
        
        if (!"glm".equalsIgnoreCase(model)) {
            log.warn("请求使用不支持的模型: {}，将使用GLM模型代替", model);
        }
        
        try {
            String answer = chatClient.prompt()
                    .user(question)
                    .call()
                    .content();

            Map<String, Object> response = new HashMap<>();
            response.put("model", "GLM");
            response.put("question", question);
            response.put("answer", answer);
            return response;
        } catch (Exception e) {
            log.error("GLM模型调用失败", e);
            return createErrorResponse("GLM", question, e.getMessage());
        }
    }
    
    /**
     * 健康检查API (生产环境)
     * 与AiChatController中的同名方法功能相同，但URL为/api/v1/ai/chat/health
     */
    @GetMapping("/chat/health")
    public Map<String, Object> healthCheck() {
        try {
            String response = chatClient.prompt()
                    .user("简单回复'AI系统正常'，不要有其他内容")
                    .call()
                    .content();
            return Map.of(
                "status", "healthy",
                "message", "AI服务正常运行",
                "response", response
            );
        } catch (Exception e) {
            log.error("AI助手健康检查失败", e);
            return Map.of(
                "status", "unhealthy",
                "message", "AI服务运行异常: " + e.getMessage()
            );
        }
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String model, String question, String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("model", model);
        response.put("question", question);
        response.put("error", errorMessage);
        return response;
    }
} 
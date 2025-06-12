package com.aireader.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI测试控制器
 * 用于测试和验证AI服务状态
 */
@RestController
@RequestMapping("/api/v1/test/ai")
@Slf4j
@CrossOrigin(origins = "*")
public class AiTestController {

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    @Autowired
    public AiTestController(
            ChatClient chatClient,
            ChatModel chatModel) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
    }

    /**
     * 检查AI服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAiStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("timestamp", System.currentTimeMillis());
            status.put("chatClientAvailable", chatClient != null);
            status.put("chatModelAvailable", chatModel != null);
            status.put("chatModelType", chatModel != null ? chatModel.getClass().getSimpleName() : "N/A");
            
            if (chatClient != null) {
                try {
                    String testResponse = chatClient.prompt()
                            .user("Hello")
                            .call()
                            .content();
                    status.put("aiServiceWorking", true);
                    status.put("testResponse", testResponse.substring(0, Math.min(100, testResponse.length())));
                } catch (Exception e) {
                    status.put("aiServiceWorking", false);
                    status.put("error", e.getMessage());
                }
            } else {
                status.put("aiServiceWorking", false);
                status.put("error", "ChatClient not available");
            }
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("AI状态检查失败", e);
            status.put("error", e.getMessage());
            return ResponseEntity.ok(status);
        }
    }

    /**
     * 测试笔记分析
     */
    @PostMapping("/test-note-analysis")
    public ResponseEntity<Map<String, Object>> testNoteAnalysis(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                result.put("error", "内容不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            
            result.put("chatClientAvailable", chatClient != null);
            
            if (chatClient != null) {
                try {
                    String analysisPrompt = String.format("""
                        请分析以下笔记内容，提取关键信息：
                        
                        内容：%s
                        
                        请返回JSON格式的分析结果，包含：
                        - summary: 摘要
                        - keyPoints: 关键点数组
                        - tags: 标签数组
                        - sentiment: 情感分析
                        """, content);
                    
                    String response = chatClient.prompt()
                            .user(analysisPrompt)
                            .call()
                            .content();
                    
                    result.put("success", true);
                    result.put("analysisResult", response);
                } catch (Exception e) {
                    result.put("success", false);
                    result.put("error", e.getMessage());
                }
            } else {
                result.put("success", false);
                result.put("error", "ChatClient不可用");
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("笔记分析测试失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
} 
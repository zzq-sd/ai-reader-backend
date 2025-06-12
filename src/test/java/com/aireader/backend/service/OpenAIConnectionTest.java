package com.aireader.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OpenAI API连接测试
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class OpenAIConnectionTest {

    private static final String API_KEY = "sk-e89fd4b07228445c9c5c2a14cb900a18";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/models";

    @Test
    public void testOpenAIConnection() {
        log.info("🔍 开始测试OpenAI API连接...");
        
        // 1. 检查API密钥格式
        validateApiKeyFormat(API_KEY);
        
        // 2. 测试API连接
        testApiConnection();
        
        // 3. 测试简单的聊天请求
        testChatCompletion();
    }

    private void validateApiKeyFormat(String apiKey) {
        log.info("🔑 验证API密钥格式...");
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("❌ API密钥为空");
            return;
        }
        
        if (!apiKey.startsWith("sk-")) {
            log.warn("⚠️ API密钥格式异常：标准OpenAI密钥应以'sk-'开头");
            log.warn("   当前密钥: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
            log.warn("   这可能是第三方API密钥或自定义密钥");
        } else {
            log.info("✅ API密钥格式正确");
        }
        
        log.info("📏 API密钥长度: {}", apiKey.length());
    }

    private void testApiConnection() {
        log.info("🌐 测试API连接...");
        
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            log.info("📊 HTTP状态码: {}", response.statusCode());
            log.info("📄 响应头: {}", response.headers().map());
            
            if (response.statusCode() == 200) {
                log.info("✅ API连接成功");
                log.info("📋 响应内容预览: {}", response.body().substring(0, Math.min(200, response.body().length())));
            } else if (response.statusCode() == 401) {
                log.error("❌ API认证失败 (401)");
                log.error("🔑 请检查API密钥是否正确");
                log.error("📄 错误响应: {}", response.body());
            } else if (response.statusCode() == 429) {
                log.error("❌ API调用频率限制 (429)");
                log.error("⏰ 请稍后重试");
            } else {
                log.error("❌ API请求失败，状态码: {}", response.statusCode());
                log.error("📄 错误响应: {}", response.body());
            }
            
        } catch (IOException e) {
            log.error("❌ 网络连接失败:", e);
            log.error("🌐 请检查网络连接和防火墙设置");
        } catch (InterruptedException e) {
            log.error("❌ 请求被中断:", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("❌ 未知错误:", e);
        }
    }

    private void testChatCompletion() {
        log.info("💬 测试聊天完成API...");
        
        String chatApiUrl = "https://api.openai.com/v1/chat/completions";
        String requestBody = """
            {
                "model": "gpt-3.5-turbo",
                "messages": [
                    {
                        "role": "user",
                        "content": "Hello, this is a test message."
                    }
                ],
                "max_tokens": 50,
                "temperature": 0.7
            }
            """;
        
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chatApiUrl))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            log.info("📊 聊天API状态码: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                log.info("✅ 聊天API测试成功");
                log.info("📄 响应内容: {}", response.body());
            } else {
                log.error("❌ 聊天API测试失败，状态码: {}", response.statusCode());
                log.error("📄 错误响应: {}", response.body());
            }
            
        } catch (Exception e) {
            log.error("❌ 聊天API测试异常:", e);
        }
    }
} 
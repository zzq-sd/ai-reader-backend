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
 * 阿里云百炼（DashScope）API连接测试
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class DashScopeConnectionTest {

    private static final String API_KEY = "sk-e89fd4b07228445c9c5c2a14cb900a18";
    private static final String DASHSCOPE_BASE_URL = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1";
    private static final String CHAT_COMPLETIONS_URL = DASHSCOPE_BASE_URL + "/chat/completions";

    @Test
    public void testDashScopeConnection() {
        log.info("🔍 开始测试阿里云百炼API连接...");
        
        // 1. 检查API密钥格式
        validateApiKeyFormat(API_KEY);
        
        // 2. 测试聊天完成API
        testChatCompletion();
    }

    private void validateApiKeyFormat(String apiKey) {
        log.info("🔑 验证阿里云百炼API密钥格式...");
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("❌ API密钥为空");
            return;
        }
        
        if (!apiKey.startsWith("sk-")) {
            log.warn("⚠️ API密钥格式异常：阿里云百炼密钥通常以'sk-'开头");
            log.warn("   当前密钥: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
        } else {
            log.info("✅ API密钥格式正确");
        }
        
        log.info("📏 API密钥长度: {}", apiKey.length());
    }

    private void testChatCompletion() {
        log.info("💬 测试阿里云百炼聊天完成API...");
        log.info("🌐 请求URL: {}", CHAT_COMPLETIONS_URL);
        
        String requestBody = """
            {
                "model": "qwen-max",
                "messages": [
                    {
                        "role": "user",
                        "content": "你好，这是一个测试消息。请简单回复。"
                    }
                ],
                "max_tokens": 100,
                "temperature": 0.7
            }
            """;
        
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_COMPLETIONS_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            log.info("📤 发送请求到阿里云百炼API...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            log.info("📊 HTTP状态码: {}", response.statusCode());
            log.info("📋 响应头: {}", response.headers().map());
            
            if (response.statusCode() == 200) {
                log.info("✅ 阿里云百炼API测试成功！");
                log.info("📄 响应内容: {}", response.body());
            } else if (response.statusCode() == 401) {
                log.error("❌ API认证失败 (401)");
                log.error("🔑 请检查阿里云百炼API密钥是否正确");
                log.error("📄 错误响应: {}", response.body());
                
                // 解析错误信息
                if (response.body().contains("invalid_api_key")) {
                    log.error("💡 解决方案:");
                    log.error("   1. 登录阿里云百炼控制台");
                    log.error("   2. 检查API密钥是否正确");
                    log.error("   3. 确认密钥是否已激活");
                    log.error("   4. 检查账户余额是否充足");
                }
            } else if (response.statusCode() == 429) {
                log.error("❌ API调用频率限制 (429)");
                log.error("⏰ 请稍后重试");
                log.error("📄 错误响应: {}", response.body());
            } else if (response.statusCode() == 400) {
                log.error("❌ 请求参数错误 (400)");
                log.error("📄 错误响应: {}", response.body());
            } else {
                log.error("❌ API请求失败，状态码: {}", response.statusCode());
                log.error("📄 错误响应: {}", response.body());
            }
            
        } catch (IOException e) {
            log.error("❌ 网络连接失败:", e);
            log.error("🌐 请检查网络连接和防火墙设置");
            log.error("💡 可能的解决方案:");
            log.error("   1. 检查网络连接");
            log.error("   2. 检查防火墙设置");
            log.error("   3. 检查代理配置");
        } catch (InterruptedException e) {
            log.error("❌ 请求被中断:", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("❌ 未知错误:", e);
        }
    }

    /**
     * 测试可用的模型列表
     */
    @Test
    public void testAvailableModels() {
        log.info("🤖 测试阿里云百炼可用模型...");
        
        String[] models = {"qwen-max", "qwen-plus", "qwen-turbo", "qwen-turbo-latest"};
        
        for (String model : models) {
            log.info("🔍 测试模型: {}", model);
            testModelWithSimpleRequest(model);
        }
    }
    
    private void testModelWithSimpleRequest(String model) {
        String requestBody = String.format("""
            {
                "model": "%s",
                "messages": [
                    {
                        "role": "user",
                        "content": "Hello"
                    }
                ],
                "max_tokens": 10
            }
            """, model);
        
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_COMPLETIONS_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                log.info("✅ 模型 {} 可用", model);
            } else {
                log.warn("⚠️ 模型 {} 不可用，状态码: {}", model, response.statusCode());
            }
            
        } catch (Exception e) {
            log.warn("⚠️ 模型 {} 测试失败: {}", model, e.getMessage());
        }
    }
} 
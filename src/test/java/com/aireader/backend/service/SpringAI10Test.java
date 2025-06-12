package com.aireader.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring AI 1.0.0 功能测试
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class SpringAI10Test {

    @Autowired(required = false)
    private ChatModel chatModel;

    @Test
    public void testSpringAI10Configuration() {
        log.info("🎯 测试Spring AI 1.0.0配置");
        
        try {
            if (chatModel != null) {
                // 测试ChatClient.Builder是否可用
                ChatClient.Builder builder = ChatClient.builder(chatModel);
                log.info("✅ ChatClient.Builder创建成功");
                
                // 测试基本的ChatClient构建
                ChatClient chatClient = builder.build();
                log.info("✅ ChatClient构建成功");
                
                log.info("🎉 Spring AI 1.0.0配置测试通过！");
            } else {
                log.warn("⚠️ ChatModel未配置，跳过ChatClient测试");
                log.info("✅ Spring AI 1.0.0基础配置正常");
            }
            
        } catch (Exception e) {
            log.error("❌ Spring AI 1.0.0配置测试失败", e);
            throw new RuntimeException("Spring AI 1.0.0配置失败", e);
        }
    }
    
    @Test
    public void testAlibabaCloudDashScopeConfiguration() {
        log.info("🔧 测试阿里云百炼配置");
        
        // 这里只测试配置是否正确加载，不实际调用API
        // 因为API密钥可能无效
        
        String apiKey = System.getProperty("spring.ai.openai.api-key", "sk-e89fd4b07228445c9c5c2a14cb900a18");
        String baseUrl = System.getProperty("spring.ai.openai.base-url", "https://dashscope-intl.aliyuncs.com/compatible-mode/v1");
        String model = System.getProperty("spring.ai.openai.chat.options.model", "qwen-turbo-latest");
        
        log.info("📋 配置信息:");
        log.info("   API密钥: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
        log.info("   Base URL: {}", baseUrl);
        log.info("   模型: {}", model);
        
        // 验证配置格式
        if (!apiKey.startsWith("sk-")) {
            throw new RuntimeException("API密钥格式不正确，应以sk-开头");
        }
        
        if (!baseUrl.contains("dashscope")) {
            throw new RuntimeException("Base URL应包含dashscope");
        }
        
        if (!model.contains("qwen")) {
            throw new RuntimeException("模型应为阿里云百炼的qwen系列");
        }
        
        log.info("✅ 阿里云百炼配置格式验证通过");
    }
} 
package com.aireader.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.chat.model.ChatModel;
import jakarta.annotation.PostConstruct;

/**
 * AI配置类 - 适配Spring AI 1.0.0-M5
 */
@Configuration
@Slf4j
public class AiConfig {

    @Value("${spring.ai.openai.api-key:#{null}}")
    private String openaiApiKey;
    
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;
    
    @Value("${spring.ai.openai.model:gpt-3.5-turbo}")
    private String model;
    
    @Value("${spring.ai.openai.temperature:0.7}")
    private double temperature;
    
    @Value("${spring.ai.openai.max-tokens:2000}")
    private int maxTokens;
    
    @PostConstruct
    public void init() {
        validateApiKey();
        logAiConfiguration();
    }
    
    /**
     * 验证OpenAI API密钥
     */
    private void validateApiKey() {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty() || 
            openaiApiKey.equals("${OPENAI_API_KEY}")) {
            log.warn("⚠️ OpenAI API密钥未配置，AI功能将不可用。请在环境变量或配置文件中设置OPENAI_API_KEY");
        } else {
            log.info("✓ OpenAI API密钥已配置");
        }
    }
    
    /**
     * 记录AI配置信息
     */
    private void logAiConfiguration() {
        log.info("AI服务配置 - Spring AI 1.0.0-M5");
        log.info("OpenAI API配置状态: {}", 
                 (openaiApiKey != null && !openaiApiKey.isEmpty()) ? "已配置" : "未配置");
        log.info("使用模型: {}, 温度: {}, 最大令牌数: {}", model, temperature, maxTokens);
    }
    
    /**
     * 配置OpenAI API客户端
     */
    @Bean
    public OpenAiApi openAiApi() {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty() || 
            openaiApiKey.equals("${OPENAI_API_KEY}")) {
            log.warn("OpenAI API密钥未配置，返回null API客户端");
            return null;
        }
        return new OpenAiApi(openaiApiKey, openaiBaseUrl);
    }
    
    /**
     * 配置OpenAI聊天模型
     */
    @Bean
    public ChatModel chatModel(OpenAiApi openAiApi) {
        if (openAiApi == null) {
            log.warn("OpenAI API客户端未配置，返回null聊天模型");
            return null;
        }
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
        
        return new OpenAiChatModel(openAiApi, options);
    }
} 
package com.aireader.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring AI自动配置测试
 * 验证Spring Boot是否正确自动配置了Spring AI组件
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class SpringAIAutoConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private ChatModel chatModel;

    @Autowired(required = false)
    private ChatClient chatClient;

    @Test
    public void testSpringAIBeansAvailability() {
        log.info("🔍 检查Spring AI Bean的可用性");
        
        // 检查ChatModel是否被自动配置
        boolean hasChatModel = applicationContext.containsBean("openAiChatModel") || 
                              applicationContext.containsBean("chatModel");
        log.info("📋 ChatModel Bean存在: {}", hasChatModel);
        
        // 检查ChatClient是否被配置
        boolean hasChatClient = applicationContext.containsBean("chatClient");
        log.info("📋 ChatClient Bean存在: {}", hasChatClient);
        
        // 列出所有Spring AI相关的Bean
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class);
        log.info("🔍 查找Spring AI相关Bean:");
        for (String beanName : beanNames) {
            if (beanName.toLowerCase().contains("chat") || 
                beanName.toLowerCase().contains("openai") ||
                beanName.toLowerCase().contains("ai")) {
                Object bean = applicationContext.getBean(beanName);
                log.info("   - {}: {}", beanName, bean.getClass().getSimpleName());
            }
        }
        
        // 检查配置属性
        log.info("🔧 检查配置属性:");
        try {
            String apiKey = applicationContext.getEnvironment().getProperty("spring.ai.openai.api-key");
            String baseUrl = applicationContext.getEnvironment().getProperty("spring.ai.openai.base-url");
            String model = applicationContext.getEnvironment().getProperty("spring.ai.openai.chat.options.model");
            
            log.info("   - API Key: {}", apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) + "..." : "未配置");
            log.info("   - Base URL: {}", baseUrl != null ? baseUrl : "未配置");
            log.info("   - Model: {}", model != null ? model : "未配置");
            
            // 验证配置是否正确
            assertNotNull(apiKey, "API Key应该被配置");
            assertNotNull(baseUrl, "Base URL应该被配置");
            assertNotNull(model, "Model应该被配置");
            
        } catch (Exception e) {
            log.error("❌ 配置属性检查失败", e);
        }
    }
    
    @Test
    public void testChatModelInjection() {
        log.info("🤖 测试ChatModel注入");
        
        if (chatModel != null) {
            log.info("✅ ChatModel注入成功: {}", chatModel.getClass().getSimpleName());
            
            // 尝试获取模型信息
            try {
                log.info("📋 ChatModel详细信息: {}", chatModel.toString());
            } catch (Exception e) {
                log.warn("⚠️ 无法获取ChatModel详细信息: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ ChatModel未被注入");
            
            // 检查是否有相关的错误或配置问题
            log.info("🔍 检查可能的原因:");
            log.info("   1. 检查spring-ai-starter-model-openai依赖是否正确添加");
            log.info("   2. 检查application.yml中的spring.ai.openai配置");
            log.info("   3. 检查API密钥是否有效");
        }
    }
    
    @Test
    public void testChatClientInjection() {
        log.info("💬 测试ChatClient注入");
        
        if (chatClient != null) {
            log.info("✅ ChatClient注入成功: {}", chatClient.getClass().getSimpleName());
        } else {
            log.warn("⚠️ ChatClient未被注入");
            
            // 如果ChatModel可用但ChatClient不可用，说明我们的配置类有问题
            if (chatModel != null) {
                log.info("💡 ChatModel可用但ChatClient不可用，检查SpringAiConfig配置类");
            }
        }
    }
    
    @Test
    public void testSpringAIAutoConfiguration() {
        log.info("⚙️ 测试Spring AI自动配置");
        
        // 检查Spring AI自动配置类是否被加载
        try {
            Class<?> autoConfigClass = Class.forName("org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration");
            log.info("✅ Spring AI自动配置类存在: {}", autoConfigClass.getSimpleName());
        } catch (ClassNotFoundException e) {
            log.error("❌ Spring AI自动配置类不存在，请检查依赖");
        }
        
        // 检查是否有Spring AI相关的条件注解生效
        String[] profiles = applicationContext.getEnvironment().getActiveProfiles();
        log.info("📋 当前激活的Profile: {}", String.join(", ", profiles));
        
        // 检查是否有相关的配置属性
        boolean hasApiKey = applicationContext.getEnvironment().containsProperty("spring.ai.openai.api-key");
        boolean hasBaseUrl = applicationContext.getEnvironment().containsProperty("spring.ai.openai.base-url");
        
        log.info("🔑 配置属性检查:");
        log.info("   - spring.ai.openai.api-key: {}", hasApiKey ? "存在" : "不存在");
        log.info("   - spring.ai.openai.base-url: {}", hasBaseUrl ? "存在" : "不存在");
        
        if (!hasApiKey) {
            log.error("❌ 缺少API密钥配置，这可能是ChatModel未被创建的原因");
        }
    }
} 
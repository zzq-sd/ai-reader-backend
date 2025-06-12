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
 * ChatClient.Builder测试
 * 验证Spring AI 1.0.0的ChatClient.Builder配置
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class ChatClientBuilderTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private ChatClient.Builder chatClientBuilder;

    @Autowired(required = false)
    private ChatModel chatModel;

    @Test
    public void testChatClientBuilderAvailability() {
        log.info("🔍 测试ChatClient.Builder可用性");
        
        // 检查ChatClient.Builder是否被注入
        if (chatClientBuilder != null) {
            log.info("✅ ChatClient.Builder注入成功: {}", chatClientBuilder.getClass().getSimpleName());
            
            // 尝试使用Builder创建ChatClient
            try {
                ChatClient chatClient = chatClientBuilder
                        .defaultSystem("测试系统消息")
                        .build();
                
                log.info("✅ 使用ChatClient.Builder创建ChatClient成功: {}", chatClient.getClass().getSimpleName());
                
                // 测试基本功能
                String response = chatClient.prompt()
                        .user("你好")
                        .call()
                        .content();
                
                log.info("✅ ChatClient基本功能测试成功: {}", response.substring(0, Math.min(50, response.length())));
                
            } catch (Exception e) {
                log.error("❌ 使用ChatClient.Builder创建ChatClient失败", e);
            }
        } else {
            log.warn("⚠️ ChatClient.Builder未被注入");
            
            // 检查是否有相关的Bean
            String[] builderBeans = applicationContext.getBeanNamesForType(ChatClient.Builder.class);
            log.info("📋 ChatClient.Builder类型的Bean数量: {}", builderBeans.length);
            for (String beanName : builderBeans) {
                log.info("   - {}", beanName);
            }
        }
    }
    
    @Test
    public void testManualChatClientCreation() {
        log.info("🛠️ 测试手动创建ChatClient");
        
        if (chatModel != null) {
            log.info("✅ ChatModel可用: {}", chatModel.getClass().getSimpleName());
            
            try {
                // 手动创建ChatClient
                ChatClient chatClient = ChatClient.builder(chatModel)
                        .defaultSystem("手动创建的ChatClient")
                        .build();
                
                log.info("✅ 手动创建ChatClient成功: {}", chatClient.getClass().getSimpleName());
                
                // 测试基本功能
                String response = chatClient.prompt()
                        .user("测试消息")
                        .call()
                        .content();
                
                log.info("✅ 手动创建的ChatClient功能测试成功: {}", response.substring(0, Math.min(50, response.length())));
                
            } catch (Exception e) {
                log.error("❌ 手动创建ChatClient失败", e);
            }
        } else {
            log.warn("⚠️ ChatModel不可用，无法手动创建ChatClient");
        }
    }
    
    @Test
    public void testBeanDefinitions() {
        log.info("🔍 检查所有相关Bean定义");
        
        // 检查ChatModel相关Bean
        String[] chatModelBeans = applicationContext.getBeanNamesForType(ChatModel.class);
        log.info("📋 ChatModel类型Bean: {}", chatModelBeans.length);
        for (String beanName : chatModelBeans) {
            log.info("   - {}: {}", beanName, applicationContext.getBean(beanName).getClass().getSimpleName());
        }
        
        // 检查ChatClient相关Bean
        String[] chatClientBeans = applicationContext.getBeanNamesForType(ChatClient.class);
        log.info("📋 ChatClient类型Bean: {}", chatClientBeans.length);
        for (String beanName : chatClientBeans) {
            log.info("   - {}: {}", beanName, applicationContext.getBean(beanName).getClass().getSimpleName());
        }
        
        // 检查ChatClient.Builder相关Bean
        String[] builderBeans = applicationContext.getBeanNamesForType(ChatClient.Builder.class);
        log.info("📋 ChatClient.Builder类型Bean: {}", builderBeans.length);
        for (String beanName : builderBeans) {
            log.info("   - {}: {}", beanName, applicationContext.getBean(beanName).getClass().getSimpleName());
        }
        
        // 检查我们的配置类是否被加载
        boolean hasSpringAiConfig = applicationContext.containsBean("springAiConfig");
        log.info("📋 SpringAiConfig配置类存在: {}", hasSpringAiConfig);
        
        if (hasSpringAiConfig) {
            Object config = applicationContext.getBean("springAiConfig");
            log.info("   - SpringAiConfig: {}", config.getClass().getSimpleName());
        }
    }
} 
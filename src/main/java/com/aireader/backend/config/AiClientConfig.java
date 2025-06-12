package com.aireader.backend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.aireader.backend.ai.AiConfigService;
import lombok.extern.slf4j.Slf4j;

/**
 * AI客户端配置类
 * 为不同的AI功能配置不同的ChatClient实例
 */
@Configuration
@Slf4j
public class AiClientConfig {

    @Autowired
    @Qualifier("aiAssistantConfigService")
    private AiConfigService aiAssistantConfigService;
    
    @Autowired
    @Qualifier("readerAssistantConfigService")
    private AiConfigService readerAssistantConfigService;
    
    @Autowired
    @Qualifier("noteAnalysisConfigService")
    private AiConfigService noteAnalysisConfigService;
    
    @Autowired
    private ChatModel chatModel;
    
    /**
     * 默认聊天客户端
     * 使用AI助手配置
     */
    @Bean
    @Primary
    public ChatClient chatClient() {
        try {
            log.info("创建默认ChatClient (AI助手配置): 最大令牌数={}", 
                    aiAssistantConfigService.getMaxTokens());
            
            return ChatClient.builder(chatModel)
                    .defaultSystem("你是AI阅读系统的智能助手，擅长回答用户关于文章和知识的问题")
                    .build();
        } catch (Exception e) {
            log.error("创建默认ChatClient失败", e);
            return null;
        }
    }
    
    /**
     * 阅读助手专用聊天客户端
     */
    @Bean
    @Qualifier("readerChatClient")
    public ChatClient readerChatClient() {
        try {
            log.info("创建阅读助手ChatClient: 最大令牌数={}", 
                    readerAssistantConfigService.getMaxTokens());
            
            return ChatClient.builder(chatModel)
                    .defaultSystem("你是AI阅读系统的阅读助手，专注于帮助用户理解文章内容")
                    .build();
        } catch (Exception e) {
            log.error("创建阅读助手ChatClient失败", e);
            return null;
        }
    }
    
    /**
     * 笔记分析专用聊天客户端
     * 使用固定配置，不受管理员面板影响
     */
    @Bean
    @Qualifier("noteChatClient")
    public ChatClient noteChatClient() {
        try {
            log.info("创建笔记分析ChatClient (固定配置): 最大令牌数={}", 
                    noteAnalysisConfigService.getMaxTokens());
            
            return ChatClient.builder(chatModel)
                    .defaultSystem("你是AI阅读系统的笔记分析助手，专注于分析用户笔记内容")
                    .build();
        } catch (Exception e) {
            log.error("创建笔记分析ChatClient失败", e);
            return null;
        }
    }
} 
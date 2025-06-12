package com.aireader.backend.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import lombok.extern.slf4j.Slf4j;

/**
 * AI服务配置类
 * 负责配置不同用途的AI服务使用不同的配置源
 */
@Configuration
@Slf4j
public class AiServiceConfig {

    @Autowired
    private DefaultAiConfigService defaultAiConfigService;
    
    @Autowired
    private SystemConfigAiConfigService systemConfigAiConfigService;
    
    /**
     * AI助手专用的配置服务
     * 使用数据库配置，由管理员面板控制
     */
    @Bean
    @Qualifier("aiAssistantConfigService")
    public AiConfigService aiAssistantConfigService() {
        log.info("初始化AI助手配置服务 - 使用数据库配置");
        return systemConfigAiConfigService;
    }
    
    /**
     * 阅读助手专用的配置服务
     * 使用数据库配置，由管理员面板控制
     */
    @Bean
    @Qualifier("readerAssistantConfigService")
    public AiConfigService readerAssistantConfigService() {
        log.info("初始化阅读助手配置服务 - 使用数据库配置");
        return systemConfigAiConfigService;
    }
    
    /**
     * 笔记分析专用的配置服务
     * 使用默认配置，不受管理员面板控制
     */
    @Bean
    @Qualifier("noteAnalysisConfigService")
    public AiConfigService noteAnalysisConfigService() {
        log.info("初始化笔记分析配置服务 - 使用默认固定配置");
        return defaultAiConfigService;
    }
    
    /**
     * 默认配置服务，用于其他AI功能
     * 优先使用数据库配置，确保系统的一致性
     */
    @Bean
    @Qualifier("defaultConfigService")
    public AiConfigService defaultConfigService() {
        log.info("初始化默认AI配置服务");
        return systemConfigAiConfigService;
    }
} 
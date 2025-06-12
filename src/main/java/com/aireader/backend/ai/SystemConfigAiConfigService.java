package com.aireader.backend.ai;

import com.aireader.backend.model.jpa.SystemConfig;
import com.aireader.backend.repository.SystemConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.PostConstruct;

/**
 * 基于数据库的AI配置服务实现
 * 从数据库中读取配置，支持动态更新
 */
@Service
@Primary  // 标记为主要实现
@Order(10) // 高优先级
@Slf4j
public class SystemConfigAiConfigService implements AiConfigService {

    @Autowired
    private SystemConfigRepository systemConfigRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    // 默认AI配置
    private static final Map<String, String> DEFAULT_AI_CONFIG = new HashMap<>();
    
    // 当前配置缓存
    private final Map<String, String> configCache = new HashMap<>();
    
    static {
        // 初始化默认配置
        DEFAULT_AI_CONFIG.put("ai.default.model", "zhipuai");
        DEFAULT_AI_CONFIG.put("ai.model.version", "GLM-4-Flash");
        DEFAULT_AI_CONFIG.put("ai.api.url", "https://open.bigmodel.cn/api/paas");
        DEFAULT_AI_CONFIG.put("ai.api.key", "");
        DEFAULT_AI_CONFIG.put("ai.timeout", "10");
        DEFAULT_AI_CONFIG.put("ai.max.tokens", "2000");
        DEFAULT_AI_CONFIG.put("ai.stream.response", "true");
        DEFAULT_AI_CONFIG.put("ai.temperature", "0.7");
    }
    
    /**
     * 初始化方法，在服务启动时加载配置
     */
    @PostConstruct
    public void init() {
        log.info("初始化基于数据库的AI配置服务...");
        loadAllConfigurations();
        log.info("AI配置加载完成");
    }
    
    /**
     * 从数据库加载所有AI配置
     */
    public void loadAllConfigurations() {
        // 清空配置缓存
        configCache.clear();
        
        // 加载所有AI相关配置
        for (String key : DEFAULT_AI_CONFIG.keySet()) {
            Optional<SystemConfig> config = systemConfigRepository.findByKey(key);
            if (config.isPresent()) {
                configCache.put(key, config.get().getValue());
                log.debug("加载配置: {} = {}", key, config.get().getValue());
            } else {
                // 如果配置不存在，使用默认值并保存到数据库
                String defaultValue = DEFAULT_AI_CONFIG.get(key);
                configCache.put(key, defaultValue);
                saveSystemConfig(key, defaultValue);
                log.debug("创建默认配置: {} = {}", key, defaultValue);
            }
        }
        
        // 发布配置加载完成事件
        eventPublisher.publishEvent(new AiConfigChangedEvent(this));
        log.info("AI配置已加载到缓存，共{}项", configCache.size());
    }
    
    /**
     * 获取配置值
     * 
     * @param key 配置键
     * @return 配置值
     */
    private String getConfig(String key) {
        return configCache.getOrDefault(key, DEFAULT_AI_CONFIG.getOrDefault(key, ""));
    }

    @Override
    public String getCurrentModel() {
        return getConfig("ai.default.model") + "/" + getConfig("ai.model.version");
    }

    @Override
    public String getApiUrl() {
        return getConfig("ai.api.url");
    }

    @Override
    public int getTimeoutSeconds() {
        try {
            return Integer.parseInt(getConfig("ai.timeout"));
        } catch (NumberFormatException e) {
            log.warn("无效的超时配置值，使用默认值: 10");
            return 10; // 默认10秒
        }
    }

    @Override
    public int getMaxTokens() {
        try {
            return Integer.parseInt(getConfig("ai.max.tokens"));
        } catch (NumberFormatException e) {
            log.warn("无效的最大令牌数配置值，使用默认值: 2000");
            return 2000; // 默认2000
        }
    }
    
    /**
     * 保存系统配置
     */
    private void saveSystemConfig(String key, String value) {
        Optional<SystemConfig> configOpt = systemConfigRepository.findByKey(key);
        
        SystemConfig config;
        if (configOpt.isPresent()) {
            config = configOpt.get();
            config.setValue(value);
        } else {
            config = new SystemConfig();
            config.setKey(key);
            config.setValue(value);
        }
        
        systemConfigRepository.save(config);
    }
} 
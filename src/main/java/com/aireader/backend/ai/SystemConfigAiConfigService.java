package com.aireader.backend.ai;

import com.aireader.backend.model.jpa.SystemConfig;
import com.aireader.backend.repository.SystemConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.context.event.EventListener;

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
        // 初始化默认配置，默认使用DeepSeek，但不会覆盖数据库中的已有配置
        DEFAULT_AI_CONFIG.put("ai.default.model", "deepseek");
        DEFAULT_AI_CONFIG.put("ai.model.version", "deepseek-chat");
        DEFAULT_AI_CONFIG.put("ai.api.url", "https://api.deepseek.com");
        DEFAULT_AI_CONFIG.put("ai.api.key", "");
        DEFAULT_AI_CONFIG.put("ai.timeout", "10");
        DEFAULT_AI_CONFIG.put("ai.max.tokens", "7000");
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
        
        // 发布配置加载完成事件 -  此处发布事件会导致无限递归，应由实际配置变更方发布
        // eventPublisher.publishEvent(new AiConfigChangedEvent(this));
        log.info("AI配置已加载到缓存，共{}项", configCache.size());
        
        // 记录当前使用的模型信息
        String currentModel = getConfig("ai.default.model");
        String modelVersion = getConfig("ai.model.version");
        log.info("当前使用的AI模型: {}/{}", currentModel, modelVersion);
    }
    
    /**
     * 监听AI配置变更事件，并刷新缓存
     * @param event 配置变更事件
     */
    @EventListener
    public void handleAiConfigChanged(AiConfigChangedEvent event) {
        log.info("检测到AI配置发生变更，正在重新加载配置...");
        loadAllConfigurations();
        log.info("AI配置已刷新。");
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
    public String getActiveChatModelName() {
        return getConfig("ai.default.model");
    }

    @Override
    public String getApiUrl() {
        // 根据当前模型返回正确的API URL
        String model = getConfig("ai.default.model");
        if ("deepseek".equals(model)) {
            return getConfig("ai.api.url").contains("deepseek") ? 
                getConfig("ai.api.url") : "https://api.deepseek.com";
        } else if ("zhipuai".equals(model)) {
            return getConfig("ai.api.url").contains("bigmodel") ? 
                getConfig("ai.api.url") : "https://open.bigmodel.cn/api/paas";
        } else {
            // 其他模型使用配置中的API URL
            return getConfig("ai.api.url");
        }
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
            // 根据当前模型返回合适的默认令牌数
            String model = getConfig("ai.default.model");
            if ("deepseek".equals(model)) {
                log.warn("无效的最大令牌数配置值，使用DeepSeek默认值: 7000");
                return 7000; // DeepSeek默认值
            } else if ("zhipuai".equals(model)) {
                log.warn("无效的最大令牌数配置值，使用GLM默认值: 6000");
                return 6000; // GLM默认值
            } else {
                log.warn("无效的最大令牌数配置值，使用通用默认值: 4000");
                return 4000; // 通用默认值
            }
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
package com.aireader.backend.ai;

import com.aireader.backend.model.jpa.SystemConfig;
import com.aireader.backend.repository.SystemConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.PostConstruct;

/**
 * 知识关联配置服务
 * 负责加载、管理和应用知识关联算法配置
 */
@Service
@Slf4j
public class KnowledgeConfigService {

    @Autowired
    private SystemConfigRepository systemConfigRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    // 默认知识关联配置
    private static final Map<String, String> DEFAULT_KNOWLEDGE_CONFIG = new HashMap<>();
    
    // 当前配置缓存
    private final Map<String, String> configCache = new HashMap<>();
    
    static {
        // 初始化默认配置
        DEFAULT_KNOWLEDGE_CONFIG.put("knowledge.extract.prompt", "请从以下内容中抽取重要的知识实体:\n\n{{content}}");
        DEFAULT_KNOWLEDGE_CONFIG.put("knowledge.relation.prompt", "请分析以下实体之间的关联关系:\n\n{{entities}}");
        DEFAULT_KNOWLEDGE_CONFIG.put("knowledge.summary.prompt", "请对以下内容进行简洁摘要:\n\n{{content}}");
        DEFAULT_KNOWLEDGE_CONFIG.put("knowledge.similarity.threshold", "0.75");
        DEFAULT_KNOWLEDGE_CONFIG.put("knowledge.max.related.nodes", "20");
        DEFAULT_KNOWLEDGE_CONFIG.put("knowledge.enable.auto.relation", "true");
    }
    
    /**
     * 初始化方法，在服务启动时加载配置
     */
    @PostConstruct
    public void init() {
        log.info("初始化知识关联配置服务...");
        loadAllConfigurations();
        log.info("知识关联配置加载完成");
    }
    
    /**
     * 从数据库加载所有知识关联配置
     */
    public void loadAllConfigurations() {
        // 清空配置缓存
        configCache.clear();
        
        // 加载所有知识关联相关配置
        for (String key : DEFAULT_KNOWLEDGE_CONFIG.keySet()) {
            Optional<SystemConfig> config = systemConfigRepository.findByKey(key);
            if (config.isPresent()) {
                configCache.put(key, config.get().getValue());
                log.debug("加载配置: {} = {}", key, config.get().getValue());
            } else {
                // 如果配置不存在，使用默认值并保存到数据库
                String defaultValue = DEFAULT_KNOWLEDGE_CONFIG.get(key);
                configCache.put(key, defaultValue);
                saveSystemConfig(key, defaultValue);
                log.debug("创建默认配置: {} = {}", key, defaultValue);
            }
        }
        
        // 发布配置加载完成事件
        eventPublisher.publishEvent(new KnowledgeConfigChangedEvent(this));
        log.info("知识关联配置已加载到缓存，共{}项", configCache.size());
    }
    
    /**
     * 获取配置值
     * 
     * @param key 配置键
     * @return 配置值
     */
    public String getConfig(String key) {
        return configCache.getOrDefault(key, DEFAULT_KNOWLEDGE_CONFIG.getOrDefault(key, ""));
    }
    
    /**
     * 获取配置值（整数）
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public int getIntConfig(String key, int defaultValue) {
        try {
            return Integer.parseInt(getConfig(key));
        } catch (NumberFormatException e) {
            log.warn("配置值转换为整数失败: {}，使用默认值: {}", key, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * 获取配置值（布尔值）
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = getConfig(key);
        if (value != null && !value.isEmpty()) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    /**
     * 获取配置值（浮点数）
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public double getDoubleConfig(String key, double defaultValue) {
        try {
            return Double.parseDouble(getConfig(key));
        } catch (NumberFormatException e) {
            log.warn("配置值转换为浮点数失败: {}，使用默认值: {}", key, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * 更新配置值
     * 
     * @param key 配置键
     * @param value 配置值
     */
    public void updateConfig(String key, String value) {
        // 更新数据库
        saveSystemConfig(key, value);
        
        // 更新缓存
        configCache.put(key, value);
        
        // 发布配置变更事件
        eventPublisher.publishEvent(new KnowledgeConfigChangedEvent(this));
        
        log.info("知识关联配置已更新: {} = {}", key, value);
    }
    
    /**
     * 批量更新配置
     * 
     * @param configs 配置映射
     */
    public void updateConfigs(Map<String, String> configs) {
        // 批量更新数据库和缓存
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            saveSystemConfig(entry.getKey(), entry.getValue());
            configCache.put(entry.getKey(), entry.getValue());
        }
        
        // 发布配置变更事件
        eventPublisher.publishEvent(new KnowledgeConfigChangedEvent(this));
        
        log.info("知识关联配置批量更新完成，共更新{}项", configs.size());
    }
    
    /**
     * 重置为默认配置
     */
    public void resetToDefault() {
        // 批量更新数据库和缓存
        for (Map.Entry<String, String> entry : DEFAULT_KNOWLEDGE_CONFIG.entrySet()) {
            saveSystemConfig(entry.getKey(), entry.getValue());
            configCache.put(entry.getKey(), entry.getValue());
        }
        
        // 发布配置变更事件
        eventPublisher.publishEvent(new KnowledgeConfigChangedEvent(this));
        
        log.info("知识关联配置已重置为默认值");
    }
    
    /**
     * 获取知识抽取提示词
     */
    public String getExtractPrompt() {
        return getConfig("knowledge.extract.prompt");
    }
    
    /**
     * 获取关系构建提示词
     */
    public String getRelationPrompt() {
        return getConfig("knowledge.relation.prompt");
    }
    
    /**
     * 获取摘要生成提示词
     */
    public String getSummaryPrompt() {
        return getConfig("knowledge.summary.prompt");
    }
    
    /**
     * 获取相似度阈值
     */
    public double getSimilarityThreshold() {
        return getDoubleConfig("knowledge.similarity.threshold", 0.75);
    }
    
    /**
     * 获取最大关联节点数
     */
    public int getMaxRelatedNodes() {
        return getIntConfig("knowledge.max.related.nodes", 20);
    }
    
    /**
     * 是否启用自动关联
     */
    public boolean isAutoRelationEnabled() {
        return getBooleanConfig("knowledge.enable.auto.relation", true);
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
    
    /**
     * 知识关联配置变更事件
     * 当知识关联配置发生变化时触发此事件
     */
    public static class KnowledgeConfigChangedEvent extends ApplicationEvent {
        public KnowledgeConfigChangedEvent(Object source) {
            super(source);
        }
    }
} 
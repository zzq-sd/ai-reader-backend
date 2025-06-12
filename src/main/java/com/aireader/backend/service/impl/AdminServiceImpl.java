package com.aireader.backend.service.impl;

import com.aireader.backend.dto.admin.SystemStatsDTO;
import com.aireader.backend.dto.admin.RssSourceStatusDTO;
import com.aireader.backend.dto.admin.AiConfigDTO;
import com.aireader.backend.dto.admin.KnowledgeConfigDTO;
import com.aireader.backend.dto.admin.AiConnectionTestDTO;
import com.aireader.backend.dto.admin.ReaderAssistantPromptsDTO;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.model.jpa.RssSource;
import com.aireader.backend.model.jpa.SystemConfig;
import com.aireader.backend.repository.UserRepository;
import com.aireader.backend.repository.RssSourceRepository;
import com.aireader.backend.repository.ArticleMetadataRepository;
import com.aireader.backend.repository.SystemConfigRepository;
import com.aireader.backend.repository.neo4j.ConceptNodeRepository;
import com.aireader.backend.service.AdminService;
import com.aireader.backend.ai.AiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.Duration;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 管理员服务实现类
 */
@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RssSourceRepository rssSourceRepository;
    
    @Autowired
    private ArticleMetadataRepository articleRepository;
    
    @Autowired
    private ConceptNodeRepository conceptRepository;
    
    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 获取系统统计数据
     */
    @Override
    public SystemStatsDTO getSystemStats() {
        long userCount = userRepository.count();
        long rssCount = rssSourceRepository.count();
        long articleCount = articleRepository.count();
        long conceptCount = conceptRepository.count();
        
        return SystemStatsDTO.builder()
                .userCount((int) userCount)
                .rssCount((int) rssCount)
                .articleCount((int) articleCount)
                .conceptCount((int) conceptCount)
                .build();
    }

    /**
     * 更新用户状态
     */
    @Override
    @Transactional
    public boolean updateUserStatus(String userId, boolean enabled) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        user.setEnabled(enabled);
        userRepository.save(user);
        
        log.info("用户 {} 状态已更新为 {}", userId, enabled ? "启用" : "禁用");
        return true;
    }

    /**
     * 获取所有RSS源状态
     */
    @Override
    public List<RssSourceStatusDTO> getAllRssSourcesStatus() {
        List<RssSource> sources = rssSourceRepository.findAll();
        
        return sources.stream()
                .map(source -> {
                    return RssSourceStatusDTO.builder()
                            .id(source.getId())
                            .title(source.getName())
                            .url(source.getUrl())
                            .active(source.isActive())
                            .articleCount(source.getArticleCount() != null ? source.getArticleCount() : 0)
                            .lastUpdate(source.getLastFetchedAt())
                            .errorMessage(source.getErrorMessage())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取AI配置
     */
    @Override
    public AiConfigDTO getAiConfig() {
        AiConfigDTO config = AiConfigDTO.builder()
                .defaultModel("zhipuai")
                .modelVersion("GLM-4-Flash")
                .apiUrl("https://open.bigmodel.cn/api/paas")
                .apiKey("")
                .timeout(10)
                .maxTokens(2000)
                .streamResponse(true)
                .temperature(0.7f)
                .build();
        
        // 从数据库加载配置
        Optional<SystemConfig> aiModelConfig = systemConfigRepository.findByKey("ai.default.model");
        if (aiModelConfig.isPresent()) {
            config.setDefaultModel(aiModelConfig.get().getValue());
        }
        
        Optional<SystemConfig> modelVersionConfig = systemConfigRepository.findByKey("ai.model.version");
        if (modelVersionConfig.isPresent()) {
            config.setModelVersion(modelVersionConfig.get().getValue());
        }
        
        Optional<SystemConfig> apiUrlConfig = systemConfigRepository.findByKey("ai.api.url");
        if (apiUrlConfig.isPresent()) {
            config.setApiUrl(apiUrlConfig.get().getValue());
        }
        
        Optional<SystemConfig> apiKeyConfig = systemConfigRepository.findByKey("ai.api.key");
        if (apiKeyConfig.isPresent()) {
            config.setApiKey(apiKeyConfig.get().getValue());
        }
        
        Optional<SystemConfig> timeoutConfig = systemConfigRepository.findByKey("ai.timeout");
        if (timeoutConfig.isPresent()) {
            try {
                config.setTimeout(Integer.parseInt(timeoutConfig.get().getValue()));
            } catch (NumberFormatException e) {
                log.warn("无效的超时配置值: {}", timeoutConfig.get().getValue());
            }
        }
        
        Optional<SystemConfig> maxTokensConfig = systemConfigRepository.findByKey("ai.max.tokens");
        if (maxTokensConfig.isPresent()) {
            try {
                config.setMaxTokens(Integer.parseInt(maxTokensConfig.get().getValue()));
            } catch (NumberFormatException e) {
                log.warn("无效的最大令牌数配置值: {}", maxTokensConfig.get().getValue());
            }
        }
        
        Optional<SystemConfig> streamResponseConfig = systemConfigRepository.findByKey("ai.stream.response");
        if (streamResponseConfig.isPresent()) {
            config.setStreamResponse(Boolean.parseBoolean(streamResponseConfig.get().getValue()));
        }
        
        Optional<SystemConfig> temperatureConfig = systemConfigRepository.findByKey("ai.temperature");
        if (temperatureConfig.isPresent()) {
            try {
                config.setTemperature(Float.parseFloat(temperatureConfig.get().getValue()));
            } catch (NumberFormatException e) {
                log.warn("无效的温度参数配置值: {}", temperatureConfig.get().getValue());
            }
        }
        
        return config;
    }

    /**
     * 更新AI配置
     */
    @Override
    @Transactional
    public AiConfigDTO updateAiConfig(AiConfigDTO config) {
        saveSystemConfig("ai.default.model", config.getDefaultModel());
        saveSystemConfig("ai.model.version", config.getModelVersion());
        saveSystemConfig("ai.api.url", config.getApiUrl());
        saveSystemConfig("ai.api.key", config.getApiKey());
        saveSystemConfig("ai.timeout", String.valueOf(config.getTimeout()));
        saveSystemConfig("ai.max.tokens", String.valueOf(config.getMaxTokens()));
        saveSystemConfig("ai.stream.response", String.valueOf(config.isStreamResponse()));
        saveSystemConfig("ai.temperature", String.valueOf(config.getTemperature()));
        
        // 发布AI配置变更事件
        eventPublisher.publishEvent(new AiConfigService.AiConfigChangedEvent(this));
        
        log.info("AI配置已更新");
        return config;
    }

    /**
     * 测试AI配置连接
     */
    @Override
    public AiConnectionTestDTO testAiConnection(AiConfigDTO config) {
        try {
            // 使用HTTP请求直接测试AI API连接
            log.info("测试AI连接: 模型={}, URL={}", config.getDefaultModel(), config.getApiUrl());
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            // 准备一个简单的测试请求
            Map<String, Object> requestBody = new HashMap<>();
            
            // 根据不同的模型类型构建不同的请求
            String endpoint = config.getApiUrl();
            
            if ("zhipuai".equalsIgnoreCase(config.getDefaultModel())) {
                // 智谱AI格式的请求
                requestBody.put("model", config.getModelVersion());
                
                // 添加认证信息
                headers.set("Authorization", "Bearer " + config.getApiKey());
                
                // 构建消息
                Map<String, String> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", "你好，这是一个连接测试。请回复\"连接成功\"。");
                requestBody.put("messages", List.of(message));
                
                // 设置温度参数
                requestBody.put("temperature", (float)config.getTemperature());
                
                // 确保endpoint是完整的URL
                if (!endpoint.endsWith("/chat/completions")) {
                    endpoint = endpoint + "/chat/completions";
                }
            } else if ("deepseek".equalsIgnoreCase(config.getDefaultModel())) {
                // DeepSeek格式的请求
                requestBody.put("model", config.getModelVersion());
                
                // 添加认证信息
                headers.set("Authorization", "Bearer " + config.getApiKey());
                
                // 构建消息
                Map<String, String> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", "你好，这是一个连接测试。请回复\"连接成功\"。");
                requestBody.put("messages", List.of(message));
                
                // 设置温度参数
                requestBody.put("temperature", (float)config.getTemperature());
                
                // 确保endpoint是完整的URL
                if (!endpoint.endsWith("/v1/chat/completions")) {
                    endpoint = endpoint + "/v1/chat/completions";
                }
            } else {
                return AiConnectionTestDTO.builder()
                        .success(false)
                        .message("不支持的AI模型: " + config.getDefaultModel())
                        .build();
            }
            
            // 添加最大令牌数
            requestBody.put("max_tokens", config.getMaxTokens());
            
            // 设置超时
            int timeoutMs = config.getTimeout() * 1000;
            
            try {
                // 发送测试请求
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
                
                // 使用Exchange避免异常导致无法继续执行
                ResponseEntity<String> response = restTemplate.exchange(
                        endpoint,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );
                
                // 检查响应状态
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("AI连接测试成功: {}", response.getStatusCode());
                    
                    return AiConnectionTestDTO.builder()
                            .success(true)
                            .message("连接成功：AI服务器响应正常")
                            .build();
                } else {
                    log.warn("AI连接测试失败: 状态码={}", response.getStatusCode());
                    
                    return AiConnectionTestDTO.builder()
                            .success(false)
                            .message("连接失败：服务器返回状态码 " + response.getStatusCode())
                            .build();
                }
            } catch (Exception e) {
                log.error("AI连接测试请求失败", e);
                
                return AiConnectionTestDTO.builder()
                        .success(false)
                        .message("连接失败：" + e.getMessage())
                        .build();
            }
        } catch (Exception e) {
            log.error("测试AI连接时发生异常", e);
            
            return AiConnectionTestDTO.builder()
                    .success(false)
                    .message("连接测试发生错误：" + e.getMessage())
                    .build();
        }
    }

    /**
     * 获取知识关联配置
     */
    @Override
    public KnowledgeConfigDTO getKnowledgeConfig() {
        KnowledgeConfigDTO config = KnowledgeConfigDTO.builder()
                .extractPrompt("请从以下内容中抽取重要的知识实体:\n\n{{content}}")
                .relationPrompt("请分析以下实体之间的关联关系:\n\n{{entities}}")
                .summaryPrompt("请对以下内容进行简洁摘要:\n\n{{content}}")
                .similarityThreshold(0.75)
                .maxRelatedNodes(20)
                .enableAutoRelation(true)
                .build();
        
        // 从数据库加载配置
        Optional<SystemConfig> extractPromptConfig = systemConfigRepository.findByKey("knowledge.extract.prompt");
        if (extractPromptConfig.isPresent()) {
            config.setExtractPrompt(extractPromptConfig.get().getValue());
        }
        
        Optional<SystemConfig> relationPromptConfig = systemConfigRepository.findByKey("knowledge.relation.prompt");
        if (relationPromptConfig.isPresent()) {
            config.setRelationPrompt(relationPromptConfig.get().getValue());
        }
        
        Optional<SystemConfig> summaryPromptConfig = systemConfigRepository.findByKey("knowledge.summary.prompt");
        if (summaryPromptConfig.isPresent()) {
            config.setSummaryPrompt(summaryPromptConfig.get().getValue());
        }
        
        Optional<SystemConfig> similarityThresholdConfig = systemConfigRepository.findByKey("knowledge.similarity.threshold");
        if (similarityThresholdConfig.isPresent()) {
            try {
                config.setSimilarityThreshold(Double.parseDouble(similarityThresholdConfig.get().getValue()));
            } catch (NumberFormatException e) {
                log.warn("无效的相似度阈值配置: {}", similarityThresholdConfig.get().getValue());
            }
        }
        
        Optional<SystemConfig> maxRelatedNodesConfig = systemConfigRepository.findByKey("knowledge.max.related.nodes");
        if (maxRelatedNodesConfig.isPresent()) {
            try {
                config.setMaxRelatedNodes(Integer.parseInt(maxRelatedNodesConfig.get().getValue()));
            } catch (NumberFormatException e) {
                log.warn("无效的最大关联节点数配置: {}", maxRelatedNodesConfig.get().getValue());
            }
        }
        
        Optional<SystemConfig> enableAutoRelationConfig = systemConfigRepository.findByKey("knowledge.enable.auto.relation");
        if (enableAutoRelationConfig.isPresent()) {
            config.setEnableAutoRelation(Boolean.parseBoolean(enableAutoRelationConfig.get().getValue()));
        }
        
        return config;
    }

    /**
     * 更新知识关联配置
     */
    @Override
    @Transactional
    public KnowledgeConfigDTO updateKnowledgeConfig(KnowledgeConfigDTO config) {
        saveSystemConfig("knowledge.extract.prompt", config.getExtractPrompt());
        saveSystemConfig("knowledge.relation.prompt", config.getRelationPrompt());
        saveSystemConfig("knowledge.summary.prompt", config.getSummaryPrompt());
        saveSystemConfig("knowledge.similarity.threshold", String.valueOf(config.getSimilarityThreshold()));
        saveSystemConfig("knowledge.max.related.nodes", String.valueOf(config.getMaxRelatedNodes()));
        saveSystemConfig("knowledge.enable.auto.relation", String.valueOf(config.isEnableAutoRelation()));
        
        log.info("知识关联配置已更新");
        return config;
    }
    
    /**
     * 获取AI阅读助手快捷提示词配置
     */
    @Override
    public ReaderAssistantPromptsDTO getReaderAssistantPrompts() {
        List<String> defaultPrompts = List.of(
                "总结这篇文章的主要内容",
                "提取文章的关键观点",
                "解释文章中的专业术语",
                "分析文章的论证逻辑",
                "这篇文章有什么启发？"
        );
        
        List<String> prompts = new ArrayList<>(defaultPrompts);
        
        // 从数据库加载配置
        Optional<SystemConfig> promptsConfig = systemConfigRepository.findByKey("reader.assistant.quick.prompts");
        if (promptsConfig.isPresent()) {
            try {
                // 将存储的JSON字符串转换为List<String>
                ObjectMapper objectMapper = new ObjectMapper();
                prompts = objectMapper.readValue(promptsConfig.get().getValue(), 
                        new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("无效的快捷提示词配置格式: {}", e.getMessage());
            }
        }
        
        return ReaderAssistantPromptsDTO.builder()
                .quickPrompts(prompts)
                .build();
    }

    /**
     * 更新AI阅读助手快捷提示词配置
     */
    @Override
    @Transactional
    public ReaderAssistantPromptsDTO updateReaderAssistantPrompts(ReaderAssistantPromptsDTO prompts) {
        try {
            // 将List<String>转换为JSON字符串
            ObjectMapper objectMapper = new ObjectMapper();
            String promptsJson = objectMapper.writeValueAsString(prompts.getQuickPrompts());
            
            // 保存到数据库
            saveSystemConfig("reader.assistant.quick.prompts", promptsJson);
            
            log.info("AI阅读助手快捷提示词配置已更新");
            return prompts;
        } catch (Exception e) {
            log.error("更新AI阅读助手快捷提示词配置失败", e);
            throw new RuntimeException("更新快捷提示词配置失败: " + e.getMessage(), e);
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
            config.setUpdatedAt(LocalDateTime.now());
        } else {
            config = new SystemConfig();
            config.setKey(key);
            config.setValue(value);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
        }
        
        systemConfigRepository.save(config);
    }
} 
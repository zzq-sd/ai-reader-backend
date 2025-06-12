package com.aireader.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI内容聚合策略配置数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfigDTO {
    /**
     * 默认模型提供商（zhipuai或deepseek）
     */
    private String defaultModel;
    
    /**
     * 模型版本
     */
    private String modelVersion;
    
    /**
     * API端点URL
     */
    private String apiUrl;
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * 请求超时（秒）
     */
    private int timeout;
    
    /**
     * 最大令牌数
     */
    private int maxTokens;
    
    /**
     * 温度参数（0.0-1.0）
     */
    private float temperature;
    
    /**
     * 是否启用流式响应
     */
    private boolean streamResponse;
} 
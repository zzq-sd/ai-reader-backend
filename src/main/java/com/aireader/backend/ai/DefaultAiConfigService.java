package com.aireader.backend.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

/**
 * AI配置服务的默认实现
 * 从application配置文件中读取配置
 * 注意：此实现优先级较低，当存在其他实现时会被覆盖
 */
@Service
@Order(100) // 低优先级
public class DefaultAiConfigService implements AiConfigService {

    @Value("${spring.ai.chat.default-model:zhipuai}")
    private String defaultModel;
    
    @Value("${spring.ai.chat.options.model:GLM-4-Flash}")
    private String modelVersion;
    
    @Value("${spring.ai.zhipuai.base-url:https://open.bigmodel.cn/api/paas}")
    private String apiUrl;
    
    @Value("${spring.ai.chat.options.timeout:10s}")
    private String timeout;
    
    @Value("${spring.ai.chat.options.maxTokens:2000}")
    private int maxTokens;

    @Override
    public String getCurrentModel() {
        return defaultModel + "/" + modelVersion;
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    public int getTimeoutSeconds() {
        if (timeout.endsWith("s")) {
            try {
                return Integer.parseInt(timeout.substring(0, timeout.length() - 1));
            } catch (NumberFormatException e) {
                return 10; // 默认10秒
            }
        }
        return 10; // 默认10秒
    }

    @Override
    public int getMaxTokens() {
        return maxTokens;
    }
} 
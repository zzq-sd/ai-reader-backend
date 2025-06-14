package com.aireader.backend.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 提供一个绝对可靠的、硬编码的默认AI配置。
 * 该类不应从 application.yml 读取配置，以避免与Spring AI的自动配置产生任何可能的冲突。
 * 它只在没有数据库配置 (SystemConfigAiConfigService) 且处于非生产环境时作为最后的备胎。
 */
@Service
@Order(100) // 低优先级
@Slf4j
public class DefaultAiConfigService implements AiConfigService {

    private static final String DEFAULT_MODEL = "deepseek";
    private static final String DEEPSEEK_MODEL_VERSION = "deepseek-chat";
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com";
    private static final String ZHIPUAI_MODEL_VERSION = "glm-4-flash";
    private static final String ZHIPUAI_API_URL = "https://open.bigmodel.cn/api/paas";
    private static final int TIMEOUT_SECONDS = 30;
    private static final int MAX_TOKENS = 4096;


    @Override
    public String getCurrentModel() {
        // 在没有数据库配置的情况下，提供一个硬编码的默认模型
        log.warn("正在使用来自 DefaultAiConfigService 的硬编码默认模型: {}", DEFAULT_MODEL);
        return DEFAULT_MODEL;
    }

    @Override
    public String getActiveChatModelName() {
        return getCurrentModel();
    }

    @Override
    public String getApiUrl() {
        String model = getCurrentModel();
        if ("zhipuai".equalsIgnoreCase(model)) {
            return ZHIPUAI_API_URL;
        }
        return DEEPSEEK_API_URL;
    }

    @Override
    public int getTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    public int getMaxTokens() {
        return MAX_TOKENS;
    }
} 
package com.aireader.backend.ai;

import org.springframework.context.ApplicationEvent;

/**
 * AI配置服务接口
 * 提供AI服务所需的配置信息
 */
public interface AiConfigService {
    
    /**
     * 获取当前使用的模型
     * 
     * @return 模型名称
     */
    String getCurrentModel();
    
    /**
     * 获取当前激活的聊天模型名称 (例如 "zhipuai", "deepseek")
     * @return 模型名称
     */
    String getActiveChatModelName();
    
    /**
     * 获取API URL
     * 
     * @return API URL
     */
    String getApiUrl();
    
    /**
     * 获取超时时间（秒）
     * 
     * @return 超时秒数
     */
    int getTimeoutSeconds();
    
    /**
     * 获取最大token数量
     * 
     * @return 最大token数
     */
    int getMaxTokens();
    
    /**
     * AI配置变更事件
     */
    class AiConfigChangedEvent extends ApplicationEvent {
        public AiConfigChangedEvent(Object source) {
            super(source);
        }
    }
} 
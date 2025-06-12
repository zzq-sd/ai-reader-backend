package com.aireader.backend.service;

import com.aireader.backend.dto.admin.SystemStatsDTO;
import com.aireader.backend.dto.admin.RssSourceStatusDTO;
import com.aireader.backend.dto.admin.AiConfigDTO;
import com.aireader.backend.dto.admin.KnowledgeConfigDTO;
import com.aireader.backend.dto.admin.AiConnectionTestDTO;
import com.aireader.backend.dto.admin.ReaderAssistantPromptsDTO;

import java.util.List;

/**
 * 管理员服务接口
 * 提供系统管理相关的业务逻辑
 */
public interface AdminService {
    
    /**
     * 获取系统统计数据
     * 
     * @return 系统统计数据
     */
    SystemStatsDTO getSystemStats();
    
    /**
     * 更新用户状态
     * 
     * @param userId 用户ID
     * @param enabled 是否启用
     * @return 是否成功
     */
    boolean updateUserStatus(String userId, boolean enabled);
    
    /**
     * 获取所有RSS源状态
     * 
     * @return RSS源状态列表
     */
    List<RssSourceStatusDTO> getAllRssSourcesStatus();
    
    /**
     * 获取AI配置
     * 
     * @return AI配置
     */
    AiConfigDTO getAiConfig();
    
    /**
     * 更新AI配置
     * 
     * @param config AI配置
     * @return 更新后的AI配置
     */
    AiConfigDTO updateAiConfig(AiConfigDTO config);
    
    /**
     * 测试AI配置连接
     * 
     * @param config AI配置
     * @return 测试结果
     */
    AiConnectionTestDTO testAiConnection(AiConfigDTO config);
    
    /**
     * 获取知识关联配置
     * 
     * @return 知识关联配置
     */
    KnowledgeConfigDTO getKnowledgeConfig();
    
    /**
     * 更新知识关联配置
     * 
     * @param config 知识关联配置
     * @return 更新后的知识关联配置
     */
    KnowledgeConfigDTO updateKnowledgeConfig(KnowledgeConfigDTO config);
    
    /**
     * 获取AI阅读助手快捷提示词配置
     * 
     * @return AI阅读助手快捷提示词配置
     */
    ReaderAssistantPromptsDTO getReaderAssistantPrompts();
    
    /**
     * 更新AI阅读助手快捷提示词配置
     * 
     * @param prompts AI阅读助手快捷提示词配置
     * @return 更新后的AI阅读助手快捷提示词配置
     */
    ReaderAssistantPromptsDTO updateReaderAssistantPrompts(ReaderAssistantPromptsDTO prompts);
} 
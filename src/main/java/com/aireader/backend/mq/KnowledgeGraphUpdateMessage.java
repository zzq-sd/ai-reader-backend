package com.aireader.backend.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 知识图谱更新消息
 * 用于异步处理知识图谱的实时更新
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeGraphUpdateMessage {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 实体ID（文章ID或笔记ID）
     */
    private String entityId;
    
    /**
     * 实体类型（article、note）
     */
    private String entityType;
    
    /**
     * 更新类型（CREATE、UPDATE、DELETE）
     */
    private UpdateType updateType;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 额外数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 重试次数
     */
    private int retryCount;
    
    /**
     * 更新类型枚举
     */
    public enum UpdateType {
        CREATE,    // 创建新实体
        UPDATE,    // 更新现有实体
        DELETE,    // 删除实体
        REANALYZE  // 重新分析
    }
    
    /**
     * 便捷构造方法
     */
    public KnowledgeGraphUpdateMessage(String entityId, String entityType) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.updateType = UpdateType.CREATE;
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }
    
    /**
     * 便捷构造方法
     */
    public KnowledgeGraphUpdateMessage(String entityId, String entityType, UpdateType updateType) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.updateType = updateType;
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    /**
     * 是否超过最大重试次数
     */
    public boolean isMaxRetryExceeded(int maxRetry) {
        return this.retryCount >= maxRetry;
    }
} 
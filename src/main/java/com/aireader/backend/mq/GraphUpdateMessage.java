package com.aireader.backend.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 图谱更新消息
 * 用于WebSocket实时推送给前端
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphUpdateMessage {
    
    /**
     * 更新类型
     */
    private UpdateType type;
    
    /**
     * 节点数据
     */
    private Object node;
    
    /**
     * 关系数据
     */
    private Object relationship;
    
    /**
     * 旧概念（用于合并操作）
     */
    private Object oldConcept;
    
    /**
     * 新概念（用于合并操作）
     */
    private Object newConcept;
    
    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 额外数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 更新类型枚举
     */
    public enum UpdateType {
        NODE_ADDED,           // 节点添加
        NODE_UPDATED,         // 节点更新
        NODE_REMOVED,         // 节点删除
        RELATIONSHIP_CREATED, // 关系创建
        RELATIONSHIP_UPDATED, // 关系更新
        RELATIONSHIP_REMOVED, // 关系删除
        CONCEPT_MERGED,       // 概念合并
        GRAPH_REBUILT         // 图谱重建
    }
    
    /**
     * 创建节点添加消息
     */
    public static GraphUpdateMessage nodeAdded(Object node) {
        return GraphUpdateMessage.builder()
                .type(UpdateType.NODE_ADDED)
                .node(node)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建关系创建消息
     */
    public static GraphUpdateMessage relationshipCreated(Object relationship) {
        return GraphUpdateMessage.builder()
                .type(UpdateType.RELATIONSHIP_CREATED)
                .relationship(relationship)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建概念合并消息
     */
    public static GraphUpdateMessage conceptMerged(Object oldConcept, Object newConcept) {
        return GraphUpdateMessage.builder()
                .type(UpdateType.CONCEPT_MERGED)
                .oldConcept(oldConcept)
                .newConcept(newConcept)
                .timestamp(LocalDateTime.now())
                .build();
    }
} 
package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱数据传输对象
 * 用于前端图谱可视化
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphDataDTO {
    
    /**
     * 图谱节点列表
     */
    private List<NodeDTO> nodes;
    
    /**
     * 图谱边列表  
     */
    private List<EdgeDTO> edges;
    
    /**
     * 图谱统计信息
     */
    private StatisticsDTO statistics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDTO {
        private String id;           // 节点ID
        private String label;        // 显示标签
        private String type;         // 节点类型: ARTICLE, NOTE, CONCEPT
        private String category;     // 分类信息
        private Double size;         // 节点大小
        private String color;        // 节点颜色
        private Double importance;   // 重要性分数
        private Map<String, Object> properties; // 节点属性
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeDTO {
        private String id;           // 边ID
        private String source;       // 源节点ID
        private String target;       // 目标节点ID
        private String label;        // 关系标签
        private String type;         // 关系类型
        private Double weight;       // 关系权重
        private String color;        // 边颜色
        private Map<String, Object> properties; // 关系属性
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticsDTO {
        private int totalNodes;      // 总节点数
        private int totalEdges;      // 总边数
        private int articleCount;    // 文章节点数
        private int noteCount;       // 笔记节点数
        private int conceptCount;    // 概念节点数
        private Double avgConnectivity; // 平均连接度
    }
} 
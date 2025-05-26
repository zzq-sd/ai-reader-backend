package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 图谱可视化数据传输对象
 * 用于封装知识图谱可视化所需的数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphVisualizationDto {

    /**
     * 图谱节点列表
     */
    private List<Node> nodes;

    /**
     * 图谱边（关系）列表
     */
    private List<Edge> edges;

    /**
     * 节点类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        /**
         * 节点ID
         */
        private String id;
        
        /**
         * 节点标签
         */
        private String label;
        
        /**
         * 节点类型（ARTICLE-文章、NOTE-笔记、CONCEPT-概念、USER-用户、TAG-标签）
         */
        private String type;
        
        /**
         * 节点属性
         */
        private Map<String, Object> properties;
    }

    /**
     * 边类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Edge {
        /**
         * 边ID
         */
        private String id;
        
        /**
         * 起始节点ID
         */
        private String source;
        
        /**
         * 目标节点ID
         */
        private String target;
        
        /**
         * 边标签（关系类型）
         */
        private String label;
        
        /**
         * 边权重
         */
        private Double weight;
        
        /**
         * 边属性
         */
        private Map<String, Object> properties;
    }
} 
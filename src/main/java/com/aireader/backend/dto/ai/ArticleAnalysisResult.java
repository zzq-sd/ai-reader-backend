package com.aireader.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 文章分析结果DTO
 * 包含文章分析的各种信息，如摘要、关键点、概念等
 * 
 * 注意：此类替换了原有的两个重复的ArticleAnalysisResult类
 * （之前分别位于com.aireader.backend.dto.ai和com.aireader.backend.ai包中）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleAnalysisResult {
    
    /**
     * 文章ID
     */
    private String articleId;
    
    /**
     * 文章摘要
     */
    private String summary;
    
    /**
     * 关键点列表
     */
    private List<String> keyPoints;
    
    /**
     * 关键词列表
     */
    private List<String> keywords;
    
    /**
     * 智能标签
     */
    private List<String> intelligentTags;
    
    /**
     * 情感分析结果
     */
    private String sentiment;
    
    /**
     * 分类结果
     */
    private String category;
    
    /**
     * 阅读时间（分钟）
     */
    private Integer readingTimeMinutes;
    
    /**
     * 实体列表
     */
    private List<Map<String, Object>> entities;
    
    /**
     * 概念列表
     */
    private List<ConceptEntity> concepts;
    
    /**
     * 概念实体内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConceptEntity {
        /**
         * 概念名称
         */
        private String name;
        
        /**
         * 概念类型
         */
        private String type;
        
        /**
         * 置信度
         */
        private Double confidence;
        
        /**
         * 出现频次
         */
        private Integer frequency;
        
        /**
         * 上下文描述
         */
        private String context;
        
        /**
         * 同义词数组
         */
        private String[] synonyms;
    }
} 
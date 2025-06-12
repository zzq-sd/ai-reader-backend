package com.aireader.backend.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 文章分析结果
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
     * 提取的关键词列表
     */
    private List<String> keywords;

    /**
     * 提取的实体列表，包含名称、类型等信息
     */
    private List<Map<String, Object>> entities;

    /**
     * 文章主题分类
     */
    private String category;

    /**
     * 文章情感倾向 (POSITIVE, NEUTRAL, NEGATIVE)
     */
    private String sentiment;

    /**
     * 估计阅读时间（分钟）
     */
    private Integer readingTimeMinutes;

    /**
     * 提取的概念列表
     */
    private List<ConceptEntity> concepts;

    /**
     * 文章摘要
     */
    private String summary;
    
    /**
     * 关键点列表
     */
    private List<String> keyPoints;
    
    /**
     * 智能标签
     */
    private List<String> intelligentTags;
    
    /**
     * 概念实体类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConceptEntity {
        private String name;                  // 概念名称
        private String type;                  // 概念类型：TECHNOLOGY, PERSON, ORGANIZATION等
        private Double confidence;            // 置信度 0-1
        private Integer frequency;            // 在文章中出现频次
        private String context;               // 概念上下文
        private String[] synonyms;            // 同义词
    }
} 
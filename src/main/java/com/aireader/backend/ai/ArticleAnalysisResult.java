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
    private List<Map<String, String>> concepts;

    /**
     * 文章摘要
     */
    private String summary;
} 
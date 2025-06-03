package com.aireader.backend.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 笔记分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteAnalysisResult {

    /**
     * 笔记ID
     */
    private String noteId;

    /**
     * 提取的关键词列表
     */
    private List<String> keywords;

    /**
     * 提取的实体列表，包含名称、类型等信息
     */
    private List<Map<String, Object>> entities;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 笔记主题
     */
    private String topic;

    /**
     * 关键观点列表
     */
    private List<String> keyPoints;

    /**
     * 相关概念列表
     */
    private List<ArticleAnalysisResult.ConceptEntity> extractedConcepts;
    
    /**
     * 智能标签
     */
    private List<String> intelligentTags;
    
    /**
     * AI摘要
     */
    private String enhancedSummary;
    
    /**
     * 情感分析
     */
    private String sentimentAnalysis;
} 
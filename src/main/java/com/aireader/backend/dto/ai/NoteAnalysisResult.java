package com.aireader.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 笔记分析结果DTO
 * 
 * 注意：此类替换了原有的两个重复的NoteAnalysisResult类
 * （之前分别位于com.aireader.backend.dto.ai和com.aireader.backend.ai包中）
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
     * 用户ID
     */
    private String userId;
    
    /**
     * 关键词列表
     */
    private List<String> keywords;
    
    /**
     * 实体列表
     */
    private List<Map<String, Object>> entities;
    
    /**
     * 增强摘要
     */
    private String enhancedSummary;
    
    /**
     * 关键点列表
     */
    private List<String> keyPoints;
    
    /**
     * 主题/分类
     */
    private String topic;
    
    /**
     * 智能标签
     */
    private List<String> intelligentTags;
    
    /**
     * 提取的概念
     */
    private List<ArticleAnalysisResult.ConceptEntity> extractedConcepts;
    
    /**
     * 情感分析结果
     */
    private String sentimentAnalysis;
} 
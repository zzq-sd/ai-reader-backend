package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记AI分析结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteAnalysisResultDto {
    
    /**
     * 笔记ID
     */
    private String noteId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * AI增强摘要
     */
    private String enhancedSummary;
    
    /**
     * 关键点列表
     */
    private List<String> keyPoints;
    
    /**
     * 智能标签列表
     */
    private List<String> intelligentTags;
    
    /**
     * 情感分析结果
     */
    private String sentimentAnalysis;
    
    /**
     * 分析版本
     */
    private String analysisVersion;
    
    /**
     * 分析时间
     */
    private LocalDateTime analyzedAt;
    
    /**
     * 分析状态
     */
    private String analysisStatus;
    
    /**
     * 状态消息
     */
    private String message;
    
    /**
     * 提取的概念实体
     */
    private List<ConceptEntityDto> extractedConcepts;
    
    /**
     * 概念实体DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConceptEntityDto {
        private String name;
        private String type;
        private Double confidence;
        private Integer frequency;
        private String context;
        private List<String> synonyms;
    }
} 
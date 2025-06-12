package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 概念统计信息传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptStatisticsDTO {
    
    /**
     * 概念ID
     */
    private String conceptId;
    
    /**
     * 概念名称
     */
    private String conceptName;
    
    /**
     * 概念类型
     */
    private String conceptType;
    
    /**
     * 概念描述
     */
    private String description;
    
    /**
     * 相关文章数量
     */
    private int relatedArticleCount;
    
    /**
     * 相关笔记数量
     */
    private int relatedNoteCount;
    
    /**
     * 相关概念数量
     */
    private int relatedConceptCount;
    
    /**
     * 概念重要性分数
     */
    private Double importanceScore;
    
    /**
     * 概念流行度（基于引用频次）
     */
    private Double popularityScore;
    
    /**
     * 平均相关性分数
     */
    private Double avgRelevanceScore;
    
    /**
     * 首次出现时间
     */
    private LocalDateTime firstMentionAt;
    
    /**
     * 最近提及时间
     */
    private LocalDateTime lastMentionAt;
    
    /**
     * 相关的文章标题列表（最近5篇）
     */
    private List<String> recentArticleTitles;
    
    /**
     * 相关的笔记标题列表（最近5篇）
     */
    private List<String> recentNoteTitles;
    
    /**
     * 相关概念名称列表（最相关的5个）
     */
    private List<String> relatedConceptNames;
    
    /**
     * 趋势信息
     */
    private TrendInfo trendInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendInfo {
        /**
         * 增长趋势：INCREASING, STABLE, DECREASING
         */
        private String trend;
        
        /**
         * 过去7天提及次数
         */
        private int mentionsLast7Days;
        
        /**
         * 过去30天提及次数
         */
        private int mentionsLast30Days;
        
        /**
         * 增长率
         */
        private Double growthRate;
    }
} 
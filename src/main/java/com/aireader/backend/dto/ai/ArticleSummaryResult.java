package com.aireader.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文章摘要结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSummaryResult {
    
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
     * 阅读时间（分钟）
     */
    private Integer readingTimeMinutes;
} 
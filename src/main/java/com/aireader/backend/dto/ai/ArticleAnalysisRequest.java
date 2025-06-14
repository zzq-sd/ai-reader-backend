package com.aireader.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章分析请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleAnalysisRequest {
    
    /**
     * 文章ID
     */
    private String articleId;
    
    /**
     * 文章标题
     */
    private String title;
    
    /**
     * 文章内容
     */
    private String content;
    
    /**
     * 文章URL（可选）
     */
    private String url;
    
    /**
     * 分析选项（可选）
     * 可以指定需要分析的内容，如"summary,keywords,sentiment"
     */
    private String options;
} 
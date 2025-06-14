package com.aireader.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章摘要请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSummaryRequest {
    
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
     * 摘要最大长度（可选）
     */
    private Integer maxLength;
} 
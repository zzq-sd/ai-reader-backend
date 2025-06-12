package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 概念搜索结果传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptSearchResultDTO {
    
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
     * 搜索匹配分数
     */
    private Double matchScore;
    
    /**
     * 相关文章数量
     */
    private int articleCount;
    
    /**
     * 相关笔记数量
     */
    private int noteCount;
    
    /**
     * 概念重要性分数
     */
    private Double importanceScore;
    
    /**
     * 是否为精确匹配
     */
    private boolean exactMatch;
    
    /**
     * 匹配的字段类型：NAME, DESCRIPTION, SYNONYM
     */
    private String matchType;
    
    /**
     * 高亮显示的文本片段
     */
    private String highlightText;
} 
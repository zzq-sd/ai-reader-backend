package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 概念详情传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptDetailDTO {
    
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
     * 概念重要性分数
     */
    private Double importanceScore;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 相关文章列表
     */
    private List<RelatedArticleDTO> relatedArticles;
    
    /**
     * 相关笔记列表
     */
    private List<RelatedNoteDTO> relatedNotes;
    
    /**
     * 相关概念列表
     */
    private List<RelatedConceptDTO> relatedConcepts;
    
    /**
     * 同义词列表
     */
    private List<String> synonyms;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedArticleDTO {
        private String id;
        private String title;
        private String summary;
        private Double relevanceScore;
        private LocalDateTime publishedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedNoteDTO {
        private String id;
        private String title;
        private String summary;
        private Double relevanceScore;
        private LocalDateTime createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedConceptDTO {
        private String id;
        private String name;
        private String type;
        private Double relationStrength;
        private String relationType;
    }
} 
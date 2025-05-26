package com.aireader.backend.dto;

import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.mongo.ArticleContent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDTO {

    // 基本信息
    private String id;
    private String title;
    private String author;
    private LocalDateTime publicationDate;
    private String summary;
    private String originalUrl;
    private String imageUrl;
    private String coverImageUrl;
    private String categories;
    private LocalDateTime createdAt;

    // RSS源信息
    private String rssSourceId;
    private String rssSourceName;

    // 内容信息
    private String htmlContent;
    private String plainTextContent;

    // AI处理信息
    private String aiProcessingStatus;
    private String[] extractedKeywords;
    private String[] extractedEntities;
    private String[] extractedTopics;
    private String language;
    private Integer wordCount;
    private Integer readingTimeMinutes;

    // 用户交互状态
    private Boolean isFavorited;
    private LocalDateTime favoritedAt;
    private Boolean isRead;
    private LocalDateTime lastReadAt;

    // 知识图谱相关信息
    private List<RelatedArticleDTO> relatedArticles;
    private List<ConceptDTO> relatedConcepts;

    /**
     * 将文章元数据和内容组合转换为DTO
     * 
     * @param metadata 文章元数据
     * @param content 文章内容
     * @return 文章DTO
     */
    public static ArticleDTO fromEntity(ArticleMetadata metadata, ArticleContent content) {
        if (metadata == null) {
            return null;
        }
        
        // 手动构建ArticleDTO
        ArticleDTO dto = new ArticleDTO();
        dto.setId(metadata.getId());
        dto.setTitle(metadata.getTitle());
        dto.setAuthor(metadata.getAuthor());
        dto.setPublicationDate(metadata.getPublicationDate());
        dto.setSummary(metadata.getSummaryText());
        dto.setOriginalUrl(metadata.getLinkToOriginal());
        dto.setCreatedAt(metadata.getCreatedAt());
        dto.setAiProcessingStatus(metadata.getAiProcessingStatus());
        
        // 设置封面图URL - 同时设置到imageUrl和coverImageUrl字段
        if (metadata.getCoverImageUrl() != null && !metadata.getCoverImageUrl().isEmpty()) {
            dto.setImageUrl(metadata.getCoverImageUrl());
            dto.setCoverImageUrl(metadata.getCoverImageUrl());
        }
        
        if (metadata.getRssSource() != null) {
            dto.setRssSourceId(metadata.getRssSource().getId().toString());
            dto.setRssSourceName(metadata.getRssSource().getName());
        }

        // 添加内容信息（如果可用）
        if (content != null) {
            dto.setHtmlContent(content.getFullHtmlContent());
            dto.setPlainTextContent(content.getPlainTextContent());
            
            // 如果文章没有封面图而内容有图片，则使用内容中的图片作为封面
            if ((dto.getImageUrl() == null || dto.getImageUrl().isEmpty()) && 
                content.getExtractedImagesUrls() != null && 
                !content.getExtractedImagesUrls().isEmpty()) {
                dto.setImageUrl(content.getExtractedImagesUrls().get(0));
                dto.setCoverImageUrl(content.getExtractedImagesUrls().get(0));
            }
            
            if (content.getProcessingInfo() != null) {
                dto.setExtractedKeywords(content.getProcessingInfo().getExtractedKeywords());
                dto.setExtractedEntities(content.getProcessingInfo().getExtractedEntities());
                dto.setExtractedTopics(content.getProcessingInfo().getExtractedTopics());
                dto.setLanguage(content.getProcessingInfo().getLanguage());
                dto.setWordCount(content.getProcessingInfo().getWordCount());
                dto.setReadingTimeMinutes(content.getProcessingInfo().getReadingTimeMinutes());
            }
        }
        
        return dto;
    }

    /**
     * 相关文章DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedArticleDTO {
        private String id;
        private String title;
        private String summary;
        private LocalDateTime publicationDate;
        private double relevanceScore;
        private String[] commonConcepts;
    }

    /**
     * 概念DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConceptDTO {
        private Long id;
        private String name;
        private String type;
        private Integer frequency;
        private Double confidence;
    }

    // 设置收藏状态
    public void setFavorited(Boolean favorited) {
        this.isFavorited = favorited;
    }

    // 设置收藏时间
    public void setFavoritedAt(LocalDateTime favoritedAt) {
        this.favoritedAt = favoritedAt;
    }

    // 设置阅读状态
    public void setRead(Boolean read) {
        this.isRead = read;
    }

    // 设置最后阅读时间
    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }
}
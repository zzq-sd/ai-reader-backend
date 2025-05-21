package com.aireader.backend.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 文章内容MongoDB文档
 */
@Document(collection = "articles_content")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleContent {

    @Id
    private String id;

    private String mysqlMetadataId;

    private String title;

    private String fullHtmlContent;

    private String plainTextContent;

    private String originalUrl;

    private String author;

    private LocalDateTime publicationDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private ArticleProcessingInfo processingInfo;

    /**
     * 文章处理信息，包含AI分析结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleProcessingInfo {
        private String language;
        private Integer wordCount;
        private Integer readingTimeMinutes;
        private String[] extractedKeywords;
        private String[] extractedEntities;
        private String[] extractedTopics;
    }
} 
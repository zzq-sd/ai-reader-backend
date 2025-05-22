package com.aireader.backend.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文章内容MongoDB实体类
 */
@Data
@Document(collection = "articles_content")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleContent {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String mysqlMetadataId; // 对应MySQL article_metadata.id的UUID字符串
    
    @Indexed
    private String originalUrl; // 文章的原始URL
    
    private String titleFromContent; // 从文章实际内容中提取的标题
    
    private String fullHtmlContent; // 文章的完整HTML内容
    
    private String plainTextContent; // 从HTML中提取的纯文本内容
    
    private List<String> extractedImagesUrls; // 从文章内容中提取的图片URL列表
    
    private Map<String, Object> rawFeedItemData; // 存储从RSS源解析得到的原始文章条目数据
    
    @Indexed
    private LocalDateTime fetchedAt; // 内容抓取入库的时间
    
    @Indexed
    private String contentHash; // 文章plainTextContent的哈希值，用于检测重复

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
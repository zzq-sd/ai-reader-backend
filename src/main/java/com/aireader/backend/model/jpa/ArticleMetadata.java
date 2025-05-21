package com.aireader.backend.model.jpa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 文章元数据实体类
 */
@Entity
@Table(name = "article_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleMetadata {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "author")
    private String author;

    @Column(name = "publication_date")
    private LocalDateTime publicationDate;

    @Column(name = "summary", length = 1000)
    private String summary;

    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    @Column(name = "guid", length = 255)
    private String guid;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "categories", length = 500)
    private String categories;

    @Column(name = "mongodb_content_id")
    private String mongodbContentId;

    @Column(name = "ai_processing_status", length = 20)
    @Enumerated(EnumType.STRING)
    private AiProcessingStatus aiProcessingStatus = AiProcessingStatus.PENDING;

    @Column(name = "ai_processing_error", length = 500)
    private String aiProcessingError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rss_source_id", nullable = false)
    private RssSource rssSource;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * AI处理状态枚举
     */
    public enum AiProcessingStatus {
        PENDING,    // 待处理
        PROCESSING, // 处理中
        COMPLETED,  // 处理完成
        ERROR       // 处理错误
    }
} 
package com.aireader.backend.model.jpa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文章元数据实体类
 */
@Data
@Entity
@Table(name = "article_metadata", uniqueConstraints = {
    @UniqueConstraint(name = "uq_article_metadata_source_link", columnNames = {"rss_source_id", "link_to_original"})
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleMetadata {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rss_source_id", nullable = false)
    private RssSource rssSource;
    
    @Column(name = "title", length = 512, nullable = false)
    private String title;
    
    @Column(name = "link_to_original", length = 2048, nullable = false)
    private String linkToOriginal;
    
    @Column(name = "publication_date")
    private LocalDateTime publicationDate;
    
    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;
    
    @Column(name = "author", length = 255)
    private String author;
    
    /**
     * 文章阅读时间（分钟）
     */
    @Column(name = "reading_time_minutes")
    private Integer readingTimeMinutes;
    
    /**
     * 文章分类
     */
    @Column(name = "category", length = 100)
    private String category;
    
    /**
     * 文章封面图片URL
     */
    @Column(name = "cover_image_url", length = 2048)
    private String coverImageUrl;
    
    /**
     * 获取文章发布日期
     * @return 发布日期
     */
    public LocalDateTime getPublishDate() {
        return this.publicationDate;
    }
    
    /**
     * 获取RSS源ID
     * @return RSS源ID
     */
    public String getRssSourceId() {
        return this.rssSource != null ? this.rssSource.getId() : null;
    }
    
    /**
     * 获取文章摘要
     * @return 文章摘要
     */
    public String getSummary() {
        return this.summaryText;
    }
    
    /**
     * 设置文章摘要
     * @param summary 文章摘要
     */
    public void setSummary(String summary) {
        this.summaryText = summary;
    }
    
    @Column(name = "mongodb_content_id", length = 24, unique = true)
    private String mongodbContentId;
    
    @Column(name = "ai_processing_status", length = 50, nullable = false)
    @Builder.Default
    private String aiProcessingStatus = "PENDING";
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 
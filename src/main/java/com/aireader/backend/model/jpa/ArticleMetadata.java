package com.aireader.backend.model.jpa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

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
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
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
    
    @Column(name = "mongodb_content_id", length = 24, unique = true)
    private String mongodbContentId;
    
    @Column(name = "ai_processing_status", length = 50, nullable = false)
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
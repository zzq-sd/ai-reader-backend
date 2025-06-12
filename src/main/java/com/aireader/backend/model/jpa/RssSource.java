package com.aireader.backend.model.jpa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * RSS源信息实体类
 */
@Data
@Entity
@Table(name = "rss_sources", uniqueConstraints = {
    @UniqueConstraint(name = "uq_rss_sources_user_url", columnNames = {"user_id", "url"})
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssSource {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "url", length = 2048, nullable = false)
    private String url;
    
    @Column(name = "name", length = 255)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;
    
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
    
    @Column(name = "fetch_interval")
    private Integer fetchInterval;
    
    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;
    
    @Column(name = "fetch_status", length = 50)
    private String fetchStatus;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 文章数量
     */
    @Column(name = "article_count")
    private Integer articleCount;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // RSSHub特定字段
    @Column(name = "is_rsshub")
    @Builder.Default
    private Boolean isRsshub = false;
    
    @Column(name = "rsshub_route", length = 512)
    private String rsshubRoute;
    
    @Column(name = "rsshub_instance", length = 255)
    private String rsshubInstance;
    
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
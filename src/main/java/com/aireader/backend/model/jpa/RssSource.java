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
 * RSS源实体类
 */
@Entity
@Table(name = "rss_sources", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"url", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssSource {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private String id;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "fetch_status", length = 20)
    @Enumerated(EnumType.STRING)
    private FetchStatus fetchStatus = FetchStatus.PENDING;

    @Column(name = "fetch_error", length = 500)
    private String fetchError;

    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;

    @Column(name = "active")
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * RSS源抓取状态枚举
     */
    public enum FetchStatus {
        PENDING,    // 待抓取
        SUCCESS,    // 抓取成功
        ERROR_FETCHING, // 抓取错误
        ERROR_PARSING   // 解析错误
    }
} 
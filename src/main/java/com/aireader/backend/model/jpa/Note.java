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
import java.util.HashSet;
import java.util.Set;

/**
 * 笔记实体类
 */
@Entity
@Table(name = "notes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Note {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_metadata_id")
    private ArticleMetadata articleMetadata;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "content_rich_text", nullable = false, columnDefinition = "TEXT")
    private String contentRichText;

    @Column(name = "ai_processing_status", length = 20)
    @Enumerated(EnumType.STRING)
    private AiProcessingStatus aiProcessingStatus = AiProcessingStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "notes")
    private Set<Tag> tags = new HashSet<>();

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
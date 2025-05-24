package com.aireader.backend.model.jpa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户笔记实体类
 */
@Data
@Entity
@Table(name = "notes")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Note {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_metadata_id")
    private ArticleMetadata articleMetadata;
    
    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public String getUserId() {
        return this.user != null ? this.user.getId() : null;
    }
    
    @Column(name = "title", length = 512)
    private String title;
    
    @Column(name = "content_rich_text", columnDefinition = "TEXT", nullable = false)
    private String contentRichText;
    
    /**
     * 获取笔记内容
     *
     * @return 笔记内容
     */
    public String getContent() {
        return this.contentRichText;
    }
    
    @Column(name = "ai_processing_status", length = 50, nullable = false)
    private String aiProcessingStatus = "PENDING";
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "note_tags",
        joinColumns = @JoinColumn(name = "note_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();
    
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
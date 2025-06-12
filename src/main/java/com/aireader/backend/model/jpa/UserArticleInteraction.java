package com.aireader.backend.model.jpa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户文章交互实体类
 */
@Data
@Entity
@Table(name = "user_article_interactions",
    indexes = {
        @Index(name = "idx_user_article_interactions_user_id", columnList = "user_id"),
        @Index(name = "idx_user_article_interactions_article_id", columnList = "article_metadata_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserArticleInteraction {
    
    @EmbeddedId
    private UserArticleInteractionId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("articleMetadataId")
    @JoinColumn(name = "article_metadata_id")
    private ArticleMetadata articleMetadata;
    
    @Column(name = "is_favorite", nullable = false)
    private Boolean isFavorite = false;
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "favorited_at")
    private LocalDateTime favoritedAt;
    
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 复合主键类
     */
    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserArticleInteractionId implements Serializable {
        
        @Column(name = "user_id", columnDefinition = "CHAR(36)")
        private String userId;
        
        @Column(name = "article_metadata_id", columnDefinition = "CHAR(36)")
        private String articleMetadataId;
    }
} 
package com.aireader.backend.model.jpa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户文章交互实体类
 */
@Entity
@Table(name = "user_article_interactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserArticleInteraction {

    @EmbeddedId
    private UserArticleInteractionId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("articleMetadataId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_metadata_id")
    private ArticleMetadata articleMetadata;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite = false;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "favorited_at")
    private LocalDateTime favoritedAt;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 复合主键类
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserArticleInteractionId implements Serializable {
        private static final long serialVersionUID = 1L;

        @Column(name = "user_id")
        private String userId;

        @Column(name = "article_metadata_id")
        private String articleMetadataId;
    }
} 
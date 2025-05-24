package com.aireader.backend.model.jpa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * 文章-用户自定义标签关联实体类
 */
@Data
@Entity
@Table(name = "article_tags", 
    indexes = {
        @Index(name = "idx_article_tags_article_id", columnList = "article_metadata_id"),
        @Index(name = "idx_article_tags_tag_id", columnList = "tag_id"),
        @Index(name = "idx_article_tags_user_id", columnList = "user_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleTag {
    
    @EmbeddedId
    private ArticleTagId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("articleMetadataId")
    @JoinColumn(name = "article_metadata_id")
    private ArticleMetadata articleMetadata;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    private Tag tag;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    
    /**
     * 复合主键类
     */
    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleTagId implements Serializable {
        
        @Column(name = "article_metadata_id", columnDefinition = "CHAR(36)")
        private String articleMetadataId;
        
        @Column(name = "tag_id", columnDefinition = "CHAR(36)")
        private String tagId;
        
        @Column(name = "user_id", columnDefinition = "CHAR(36)")
        private String userId;
    }
} 
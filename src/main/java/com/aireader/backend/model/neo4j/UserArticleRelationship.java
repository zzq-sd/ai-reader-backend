package com.aireader.backend.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

/**
 * 用户与文章之间的关系
 * FAVORITED关系：用户收藏了某文章
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserArticleRelationship {

    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private ArticleNode article;
    
    @Property("favorited_at")
    private LocalDateTime favoritedAt; // 收藏时间
    
    @Property("read_count")
    private Integer readCount; // 阅读次数
    
    @Property("last_read_at")
    private LocalDateTime lastReadAt; // 最后阅读时间
    
    /**
     * 构造函数（收藏关系）
     * 
     * @param user 用户节点
     * @param article 文章节点
     * @param favoritedAt 收藏时间
     */
    public UserArticleRelationship(UserNode user, ArticleNode article, LocalDateTime favoritedAt) {
        this.article = article;
        this.favoritedAt = favoritedAt;
        this.readCount = 0;
    }
    
    /**
     * 更新阅读信息
     * 
     * @param readTime 阅读时间
     * @return 当前关系
     */
    public UserArticleRelationship updateReadInfo(LocalDateTime readTime) {
        this.lastReadAt = readTime;
        this.readCount = this.readCount == null ? 1 : this.readCount + 1;
        return this;
    }
} 
package com.aireader.backend.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Neo4j用户节点实体
 * 表示知识图谱中的用户
 */
@Node("User")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNode {

    @Id
    @GeneratedValue
    private Long id;
    
    @Property("mysql_id")
    private String mysqlId; // 对应MySQL中User的id
    
    @Property("username")
    private String username;
    
    @Property("email")
    private String email;
    
    @Property("display_name")
    private String displayName;
    
    @Property("created_at")
    private LocalDateTime createdAt;
    
    @Property("last_active_at")
    private LocalDateTime lastActiveAt;
    
    // 用户创建的笔记 (通过 NoteNode 的 "CREATED_BY" 关系反向查找)
    @Relationship(type = "CREATED_BY", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private Set<NoteNode> createdNotes = new HashSet<>();
    
    // 用户收藏的文章
    @Relationship(type = "FAVORITED", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<UserArticleRelationship> favorites = new HashSet<>();
    
    // 用户关注的主题/概念
    @Relationship(type = "INTERESTED_IN", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<UserConceptRelationship> interests = new HashSet<>();
    
    // 用户与用户之间的关注关系
    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<UserUserRelationship> following = new HashSet<>();
    
    /**
     * 添加收藏文章关系
     * 
     * @param article 文章节点
     * @param favoritedAt 收藏时间
     * @return 当前用户节点
     */
    public UserNode addFavorite(ArticleNode article, LocalDateTime favoritedAt) {
        if (favorites == null) {
            favorites = new HashSet<>();
        }
        UserArticleRelationship relationship = new UserArticleRelationship(this, article, favoritedAt);
        favorites.add(relationship);
        return this;
    }
    
    /**
     * 添加关注概念关系
     * 
     * @param concept 概念节点
     * @param interestLevel 兴趣级别 (0-1)
     * @param followedAt 关注时间
     * @return 当前用户节点
     */
    public UserNode addInterest(ConceptNode concept, Double interestLevel, LocalDateTime followedAt) {
        if (interests == null) {
            interests = new HashSet<>();
        }
        UserConceptRelationship relationship = new UserConceptRelationship(this, concept, interestLevel, followedAt);
        interests.add(relationship);
        return this;
    }
    
    /**
     * 添加关注用户关系
     * 
     * @param targetUser 目标用户节点
     * @param followedAt 关注时间
     * @return 当前用户节点
     */
    public UserNode followUser(UserNode targetUser, LocalDateTime followedAt) {
        if (following == null) {
            following = new HashSet<>();
        }
        UserUserRelationship relationship = new UserUserRelationship(this, targetUser, followedAt);
        following.add(relationship);
        return this;
    }
} 
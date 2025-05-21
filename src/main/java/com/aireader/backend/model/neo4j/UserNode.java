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
 * 知识图谱中的用户节点
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

    @Property("mysqlId")
    private String mysqlId;

    @Property("username")
    private String username;

    @Relationship(type = "AUTHORED", direction = Relationship.Direction.OUTGOING)
    private Set<NoteNode> notes = new HashSet<>();

    @Relationship(type = "READ", direction = Relationship.Direction.OUTGOING)
    private Set<ReadArticleRelation> readArticles = new HashSet<>();

    @Relationship(type = "FAVORITED", direction = Relationship.Direction.OUTGOING)
    private Set<FavoritedArticleRelation> favoritedArticles = new HashSet<>();

    /**
     * 用户阅读文章的关系
     */
    @RelationshipProperties
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadArticleRelation {
        
        @Id
        @GeneratedValue
        private Long id;
        
        @TargetNode
        private ArticleNode article;
        
        @Property("lastReadAt")
        private LocalDateTime lastReadAt;
        
        @Property("readCount")
        private Integer readCount = 1;
    }

    /**
     * 用户收藏文章的关系
     */
    @RelationshipProperties
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoritedArticleRelation {
        
        @Id
        @GeneratedValue
        private Long id;
        
        @TargetNode
        private ArticleNode article;
        
        @Property("favoritedAt")
        private LocalDateTime favoritedAt;
    }
} 
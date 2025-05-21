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
 * 知识图谱中的笔记节点
 */
@Node("Note")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("mysqlId")
    private String mysqlId;

    @Property("title")
    private String title;

    @Property("createdAt")
    private LocalDateTime createdAt;

    @Property("userId")
    private String userId;

    @Property("aiLastProcessedAt")
    private LocalDateTime aiLastProcessedAt;

    @Relationship(type = "AUTHORED_BY", direction = Relationship.Direction.OUTGOING)
    private UserNode user;

    @Relationship(type = "REFERS_TO", direction = Relationship.Direction.OUTGOING)
    private ArticleNode article;

    @Relationship(type = "MENTIONS_CONCEPT", direction = Relationship.Direction.OUTGOING)
    private Set<ConceptRelation> conceptRelations = new HashSet<>();

    /**
     * 笔记与概念间的关系
     */
    @RelationshipProperties
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConceptRelation {
        
        @Id
        @GeneratedValue
        private Long id;
        
        @TargetNode
        private Concept concept;
        
        @Property("frequency")
        private Integer frequency = 1;
        
        @Property("confidence")
        private Double confidence = 1.0;
    }
} 
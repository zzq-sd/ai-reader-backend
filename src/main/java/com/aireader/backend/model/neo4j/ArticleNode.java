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
 * 知识图谱中的文章节点
 */
@Node("Article")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("mysqlId")
    private String mysqlId;

    @Property("mongoId")
    private String mongoId;

    @Property("title")
    private String title;

    @Property("author")
    private String author;

    @Property("publicationDate")
    private LocalDateTime publicationDate;

    @Property("summary")
    private String summary;

    @Property("url")
    private String url;

    @Property("rssSourceId")
    private String rssSourceId;
    
    @Relationship(type = "MENTIONS_CONCEPT", direction = Relationship.Direction.OUTGOING)
    private Set<ConceptRelation> conceptRelations = new HashSet<>();

    /**
     * 文章与概念间的关系
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
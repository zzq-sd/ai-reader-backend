package com.aireader.backend.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;
import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

/**
 * 文章节点Neo4j实体类
 */
@Data
@Node("Article")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleNode {
    
    @Id
    private String id; // 自动生成的Neo4j ID
    
    @Property("mysqlId")
    private String mysqlId; // 对应MySQL article_metadata.id
    
    @Property("mongoId")
    private String mongoId; // 对应MongoDB articles_content._id
    
    @Property("title")
    private String title;
    
    @Property("publicationDate")
    private LocalDateTime publicationDate;
    
    @Property("originalUrl")
    private String originalUrl;
    
    @Property("aiLastProcessedAt")
    private LocalDateTime aiLastProcessedAt;
    
    @Relationship(type = "MENTIONS", direction = OUTGOING)
    private Set<ConceptRelationship> concepts = new HashSet<>();
    
    @Relationship(type = "BELONGS_TO", direction = OUTGOING)
    private RssSourceNode rssSource;
    
    @Relationship(type = "TAGGED_WITH", direction = OUTGOING)
    private Set<TagRelationship> tags = new HashSet<>();
} 
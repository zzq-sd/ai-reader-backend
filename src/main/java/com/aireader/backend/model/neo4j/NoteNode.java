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

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

/**
 * 笔记节点Neo4j实体类
 */
@Data
@Node("Note")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteNode {
    
    @Id
    private String id; // 自动生成的Neo4j ID
    
    @Property("mysqlId")
    private String mysqlId; // 对应MySQL notes.id
    
    @Property("title")
    private String title; // 笔记标题
    
    @Property("createdAt")
    private LocalDateTime createdAt; // 创建时间
    
    @Relationship(type = "BELONGS_TO", direction = OUTGOING)
    private UserNode user; // 创建该笔记的用户
    
    @Relationship(type = "REFERENCES", direction = OUTGOING)
    private ArticleNode referencedArticle; // 引用的文章
    
    @Relationship(type = "MENTIONS", direction = OUTGOING)
    private Set<ConceptRelationship> concepts = new HashSet<>(); // 提及的概念
    
    @Relationship(type = "TAGGED_WITH", direction = OUTGOING)
    private Set<TagRelationship> tags = new HashSet<>(); // 标签
} 
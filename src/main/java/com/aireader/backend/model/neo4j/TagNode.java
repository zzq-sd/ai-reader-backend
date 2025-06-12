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
 * Neo4j标签节点实体
 * 表示知识图谱中的标签
 */
@Node("Tag")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagNode {

    @Id
    @GeneratedValue
    private Long id;
    
    @Property("mysql_id")
    private String mysqlId; // 对应MySQL中Tag的id
    
    @Property("name")
    private String name;
    
    @Property("description")
    private String description;
    
    @Property("color")
    private String color;
    
    @Property("created_at")
    private LocalDateTime createdAt;
    
    @Property("updated_at")
    private LocalDateTime updatedAt;
    
    @Property("created_by")
    private String createdBy; // 创建此标签的用户ID
    
    // 文章应用了此标签 (从 ArticleNode 指向此 TagNode 的 "TAGGED" 关系的另一端)
    @Relationship(type = "TAGGED", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private Set<ArticleNode> articles = new HashSet<>();
    
    // 标签与概念的关联
    @Relationship(type = "RELATED_TO", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<TagConceptRelationship> relatedConcepts = new HashSet<>();
} 
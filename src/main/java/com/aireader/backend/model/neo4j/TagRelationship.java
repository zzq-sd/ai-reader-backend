package com.aireader.backend.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * 文章与标签的关系属性
 */
@Data
@RelationshipProperties
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagRelationship {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private TagNode tag;
    
    private String userId; // 标记该标签的用户ID
} 
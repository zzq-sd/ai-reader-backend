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
 * 文章与概念的关系属性
 */
@Data
@RelationshipProperties
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptRelationship {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private Concept concept;
    
    private Float confidence; // AI分析的置信度
    
    private Integer count; // 提及次数
    
    private Float relevance; // 概念在该文章中的重要性/相关度
} 
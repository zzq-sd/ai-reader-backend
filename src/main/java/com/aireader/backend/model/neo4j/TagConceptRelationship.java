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

/**
 * 标签与概念之间的关系
 * RELATED_TO关系：标签与概念相关
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagConceptRelationship {

    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private ConceptNode concept;
    
    @Property("relevance_score")
    private Double relevanceScore; // 相关性得分，范围0-1
    
    /**
     * 构造函数
     * 
     * @param tag 标签节点
     * @param concept 概念节点
     * @param relevanceScore 相关性得分
     */
    public TagConceptRelationship(TagNode tag, ConceptNode concept, Double relevanceScore) {
        this.concept = concept;
        this.relevanceScore = relevanceScore;
    }
} 
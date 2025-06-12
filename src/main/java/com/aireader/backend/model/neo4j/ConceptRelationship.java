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
 * 文章与概念之间的关系属性实体。
 * 代表文章 "ABOUT" 某个概念，或者概念被文章 "MENTIONED_IN"。
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptRelationship {

    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode // This target node is the ConceptNode when relationship is from ArticleNode
    private ConceptNode concept;
    
    @Property("relevance_score")
    private Double relevanceScore; // 相关性得分，范围0-1
    
    @Property("occurrence_count") // Renamed from mentionCount
    private Integer occurrenceCount; // 在文章中提到的次数

    @Property("is_primary")
    private Boolean isPrimary; // 是否为文章的主要概念
    
    /**
     * 构造函数 (通常由 ArticleNode.addConcept 调用)
     * 
     * @param concept 目标概念节点
     * @param relevanceScore 相关性得分
     * @param occurrenceCount 提到次数
     * @param isPrimary 是否为主要概念
     */
    public ConceptRelationship(ConceptNode concept, Double relevanceScore, Integer occurrenceCount, Boolean isPrimary) {
        // ArticleNode (source) is implicit when this relationship is part of ArticleNode.concepts
        this.concept = concept;
        this.relevanceScore = relevanceScore;
        this.occurrenceCount = occurrenceCount;
        this.isPrimary = isPrimary;
    }
} 
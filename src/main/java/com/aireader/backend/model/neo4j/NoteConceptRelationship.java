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
 * 笔记与概念之间的关系
 * 支持多种关系类型：MENTIONS, EXTRACTED_CONCEPT, INTELLIGENT_TAG等
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteConceptRelationship {

    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private ConceptNode concept;
    
    @Property("relevance_score")
    private Double relevanceScore; // 相关性得分，范围0-1
    
    @Property("relationship_type")
    private String relationshipType; // 关系类型：EXTRACTED_CONCEPT, INTELLIGENT_TAG, KEYWORD等
    
    @Property("entity_type")
    private String entityType; // 实体类型
    
    @Property("created_at")
    private java.time.LocalDateTime createdAt;

    /**
     * 构造函数
     * 
     * @param concept 概念节点
     * @param relevanceScore 相关性得分
     */
    public NoteConceptRelationship(ConceptNode concept, Double relevanceScore) {
        this.concept = concept;
        this.relevanceScore = relevanceScore;
        this.createdAt = java.time.LocalDateTime.now();
    }

    /**
     * 构造函数（带关系类型）
     * 
     * @param concept 概念节点
     * @param relevanceScore 相关性得分
     * @param relationshipType 关系类型
     */
    public NoteConceptRelationship(ConceptNode concept, Double relevanceScore, String relationshipType) {
        this.concept = concept;
        this.relevanceScore = relevanceScore;
        this.relationshipType = relationshipType;
        this.createdAt = java.time.LocalDateTime.now();
    }

    /**
     * 设置关系类型
     * @param relationshipType 关系类型
     */
    public void setRelationType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    /**
     * 获取关系类型
     * @return 关系类型
     */
    public String getRelationType() {
        return this.relationshipType;
    }

    /**
     * 设置实体类型
     * @param entityType 实体类型
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * 设置创建时间
     * @param createdAt 创建时间
     */
    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 设置概念节点
     * @param concept 概念节点
     */
    public void setConcept(ConceptNode concept) {
        this.concept = concept;
    }

    /**
     * 设置相关性得分
     * @param relevanceScore 相关性得分
     */
    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
} 
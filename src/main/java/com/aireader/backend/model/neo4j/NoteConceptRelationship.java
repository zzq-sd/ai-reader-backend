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
 * MENTIONS关系：笔记中提到了某概念
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteConceptRelationship {

    @Id
    @GeneratedValue
    private String id;
    
    @TargetNode
    private ConceptNode concept;
    
    @Property("relevance_score")
    private Double relevanceScore; // 相关性得分，范围0-1
    
    private NoteNode noteNode;
    private String relationshipType;
    private String entityType;
    private java.time.LocalDateTime createdAt;

    /**
     * 构造函数
     * 
     * @param note 笔记节点
     * @param concept 概念节点
     * @param relevanceScore 相关性得分
     */
    public NoteConceptRelationship(NoteNode note, ConceptNode concept, Double relevanceScore) {
        this.concept = concept;
        this.relevanceScore = relevanceScore;
    }

    /**
     * 设置笔记节点
     * @param note 笔记节点
     */
    public void setNote(NoteNode note) {
        this.noteNode = note;
    }

    /**
     * 设置关系类型
     * @param relationshipType 关系类型
     */
    public void setRelationType(String relationshipType) {
        this.relationshipType = relationshipType;
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
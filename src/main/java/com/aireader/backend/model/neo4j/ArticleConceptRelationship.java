package com.aireader.backend.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

/**
 * 文章和概念之间的关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RelationshipProperties
public class ArticleConceptRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private ConceptNode concept;

    private String relationshipType; // 关系类型，如"主题"、"涉及"等

    private Double relevance; // 相关度分数（0-1）

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer occurrences; // 概念在文章中出现的次数

    @Builder.Default
    private boolean manuallyAdded = false; // 是否手动添加的关系

    private String context; // 关系上下文描述

    private ArticleNode articleNode;
    private String entityType;

    /**
     * 设置文章节点
     * @param article 文章节点
     */
    public void setArticle(ArticleNode article) {
        this.articleNode = article;
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
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void incrementOccurrences() {
        if (this.occurrences == null) {
            this.occurrences = 1;
        } else {
            this.occurrences++;
        }
    }

    /**
     * 设置概念节点
     * @param concept 概念节点
     */
    public void setConcept(ConceptNode concept) {
        this.concept = concept;
    }

    /**
     * 设置关系类型
     * @param relationshipType 关系类型
     */
    public void setRelationType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    /**
     * 设置关系相关度
     * @param relevance 相关度分数
     */
    public void setRelevance(Double relevance) {
        this.relevance = relevance;
    }
} 
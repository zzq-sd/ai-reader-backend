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
 * Neo4j概念节点实体
 * 表示知识图谱中的概念或主题
 */
@Node("Concept")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptNode {

    @Id
    @GeneratedValue
    private Long id;
    
    @Property("name")
    private String name;
    
    @Property("description")
    private String description;
    
    @Property("type")
    private String type; // 概念类型：PERSON, ORGANIZATION, LOCATION, TECHNOLOGY, EVENT等
    
    @Property("created_at")
    private LocalDateTime createdAt;
    
    @Property("updated_at")
    private LocalDateTime updatedAt;
    
    @Property("relevance_score")
    private Double relevanceScore; // 概念的全局相关性评分
    
    // 概念间的关系（概念A与概念B相关）
    @Relationship(type = "RELATED_TO", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<ConceptConceptRelationship> relatedConcepts = new HashSet<>();
    
    // 指向那些以当前概念为主题的文章 (通过 ArticleNode 的 "ABOUT" 关系反向查找)
    @Relationship(type = "ABOUT", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private Set<ArticleNode> articlesAboutThisConcept = new HashSet<>();
    
    /**
     * 添加一个关联的概念节点
     * 
     * @param targetConcept 目标概念节点
     * @param relationType 关系类型
     * @param strength 关系强度
     * @return 当前概念节点
     */
    public ConceptNode addRelatedConcept(ConceptNode targetConcept, String relationType, Double strength) {
        if (relatedConcepts == null) {
            relatedConcepts = new HashSet<>();
        }
        ConceptConceptRelationship relationship = new ConceptConceptRelationship(this, targetConcept, strength, relationType);
        relatedConcepts.add(relationship);
        return this;
    }
} 
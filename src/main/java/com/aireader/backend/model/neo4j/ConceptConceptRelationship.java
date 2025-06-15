package com.aireader.backend.model.neo4j;

import lombok.*;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * 概念与概念之间的关系类
 * 表示概念与概念之间的"RELATED_TO"关系，即概念A与概念B相关
 */
@RelationshipProperties
@Getter
@Setter
@ToString(exclude = {"targetConcept"})
@EqualsAndHashCode(exclude = {"targetConcept"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptConceptRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private ConceptNode targetConcept;

    /**
     * 关系强度
     * 表示两个概念之间的关联强度，范围0-1
     */
    private Double strength;

    /**
     * 关系类型
     * 表示两个概念之间的关系类型，如"is-a"、"part-of"等
     */
    private String relationType;

    /**
     * 共现次数
     * 表示两个概念在同一上下文中出现的次数
     */
    private Integer coOccurrenceCount;

    /**
     * 构造函数
     *
     * @param sourceConcept 源概念节点
     * @param targetConcept 目标概念节点
     * @param strength 关系强度
     * @param relationType 关系类型
     */
    public ConceptConceptRelationship(ConceptNode sourceConcept, ConceptNode targetConcept, 
                                      Double strength, String relationType) {
        this.targetConcept = targetConcept;
        this.strength = strength;
        this.relationType = relationType;
        this.coOccurrenceCount = 1;
    }

    /**
     * 更新关系强度
     *
     * @param newStrength 新的关系强度
     */
    public void updateStrength(Double newStrength) {
        this.strength = newStrength;
    }

    /**
     * 增加共现次数
     */
    public void incrementCoOccurrence() {
        this.coOccurrenceCount++;
        // 可以根据共现次数更新关系强度
        this.strength = Math.min(1.0, this.strength + 0.01);
    }

    /**
     * 设置关系类型
     *
     * @param relationType 关系类型
     */
    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }
} 
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

import java.time.LocalDateTime;

/**
 * 用户与概念之间的关系
 * INTERESTED_IN关系：用户对某概念感兴趣
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConceptRelationship {

    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private ConceptNode concept;
    
    @Property("interest_level")
    private Double interestLevel; // 兴趣级别，范围0-1
    
    @Property("followed_at")
    private LocalDateTime followedAt; // 开始关注时间
    
    @Property("interaction_count")
    private Integer interactionCount; // 交互次数
    
    /**
     * 构造函数
     * 
     * @param user 用户节点
     * @param concept 概念节点
     * @param interestLevel 兴趣级别
     * @param followedAt 关注时间
     */
    public UserConceptRelationship(UserNode user, ConceptNode concept, Double interestLevel, LocalDateTime followedAt) {
        this.concept = concept;
        this.interestLevel = interestLevel;
        this.followedAt = followedAt;
        this.interactionCount = 1;
    }
    
    /**
     * 更新交互信息
     * 
     * @param newInterestLevel 新的兴趣级别
     * @return 当前关系
     */
    public UserConceptRelationship updateInteraction(Double newInterestLevel) {
        // 加权平均计算新的兴趣级别
        this.interestLevel = (this.interestLevel * this.interactionCount + newInterestLevel) / (this.interactionCount + 1);
        this.interactionCount++;
        return this;
    }
} 
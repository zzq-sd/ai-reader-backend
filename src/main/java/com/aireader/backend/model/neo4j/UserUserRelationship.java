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
 * 用户与用户之间的关系
 * FOLLOWS关系：用户A关注用户B
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUserRelationship {

    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private UserNode targetUser;
    
    @Property("followed_at")
    private LocalDateTime followedAt; // 关注时间
    
    /**
     * 构造函数
     * 
     * @param sourceUser 源用户节点
     * @param targetUser 目标用户节点
     * @param followedAt 关注时间
     */
    public UserUserRelationship(UserNode sourceUser, UserNode targetUser, LocalDateTime followedAt) {
        this.targetUser = targetUser;
        this.followedAt = followedAt;
    }
} 
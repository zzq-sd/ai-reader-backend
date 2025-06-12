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
 * 标签与文章之间的关系
 * TAGGED关系：文章被标记为某标签
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagRelationship {

    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private TagNode tag;
    
    @Property("added_by")
    private String addedBy; // 添加标签的用户ID
    
    @Property("added_at")
    private LocalDateTime addedAt; // 添加时间
    
    /**
     * 构造函数
     * 
     * @param article 文章节点
     * @param tag 标签节点
     * @param addedBy 添加者ID
     * @param addedAt 添加时间
     */
    public TagRelationship(ArticleNode article, TagNode tag, String addedBy, LocalDateTime addedAt) {
        this.tag = tag;
        this.addedBy = addedBy;
        this.addedAt = addedAt;
    }
} 
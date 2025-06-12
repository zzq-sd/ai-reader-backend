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
 * 笔记与标签之间的关系
 * TAGGED关系：笔记被标记为某标签
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteTagRelationship {

    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private TagNode tag;
    
    @Property("added_at")
    private LocalDateTime addedAt; // 添加时间
    
    /**
     * 构造函数
     * 
     * @param note 笔记节点
     * @param tag 标签节点
     * @param addedAt 添加时间
     */
    public NoteTagRelationship(NoteNode note, TagNode tag, LocalDateTime addedAt) {
        this.tag = tag;
        this.addedAt = addedAt;
    }
} 
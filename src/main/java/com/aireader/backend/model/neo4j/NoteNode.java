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
 * Neo4j笔记节点实体
 * 表示知识图谱中的用户笔记
 */
@Node("Note")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteNode {

    @Id
    @GeneratedValue
    private Long id;
    
    @Property("mysql_id")
    private String mysqlId; // 对应MySQL中Note的id
    
    @Property("user_id")
    private String userId; // 对应MySQL中用户ID
    
    /**
     * 设置MySQL ID
     * @param mysqlId MySQL数据库中的ID字符串
     */
    public void setMysqlId(String mysqlId) {
        this.mysqlId = mysqlId;
    }
    
    /**
     * 获取MySQL ID
     * @return MySQL数据库中的ID字符串
     */
    public String getMysqlId() {
        return this.mysqlId;
    }
    
    /**
     * 设置用户ID
     * @param userId 用户ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    @Property("title")
    private String title;
    
    @Property("content_summary")
    private String contentSummary; // 笔记内容摘要
    
    @Property("created_at")
    private LocalDateTime createdAt;
    
    @Property("updated_at")
    private LocalDateTime updatedAt;
    
    // 笔记与文章的关联 - 使用标准化的关系类型字符串
    @Relationship(type = "REFERS_TO", direction = Relationship.Direction.OUTGOING)
    private ArticleNode article;
    
    // 笔记的创建者 - 使用标准化的关系类型字符串
    @Relationship(type = "CREATED_BY", direction = Relationship.Direction.OUTGOING)
    private UserNode user;
    
    // 笔记中提到的概念 - 使用标准化的关系类型字符串
    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<NoteConceptRelationship> concepts = new HashSet<>();
    
    // 笔记的标签 - 使用更规范的HAS_TAG，并使用标准化的关系类型字符串
    @Relationship(type = "HAS_TAG", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<NoteTagRelationship> tags = new HashSet<>();
    
    /**
     * 添加与概念的关系
     * 
     * @param concept 概念节点
     * @param relevanceScore 相关性分数
     * @return 当前笔记节点
     */
    public NoteNode addConcept(ConceptNode concept, Double relevanceScore) {
        if (concepts == null) {
            concepts = new HashSet<>();
        }
        NoteConceptRelationship relationship = new NoteConceptRelationship(concept, relevanceScore);
        concepts.add(relationship);
        return this;
    }
    
    /**
     * 添加与概念的关系（带关系类型）
     * 
     * @param concept 概念节点
     * @param relevanceScore 相关性分数
     * @param relationshipType 关系类型
     * @return 当前笔记节点
     */
    public NoteNode addConcept(ConceptNode concept, Double relevanceScore, String relationshipType) {
        if (concepts == null) {
            concepts = new HashSet<>();
        }
        NoteConceptRelationship relationship = new NoteConceptRelationship(concept, relevanceScore, relationshipType);
        concepts.add(relationship);
        return this;
    }
    
    /**
     * 添加标签
     * 
     * @param tag 标签节点
     * @param addedAt 添加时间
     * @return 当前笔记节点
     */
    public NoteNode addTag(TagNode tag, LocalDateTime addedAt) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        NoteTagRelationship relationship = new NoteTagRelationship(this, tag, addedAt);
        tags.add(relationship);
        return this;
    }
} 
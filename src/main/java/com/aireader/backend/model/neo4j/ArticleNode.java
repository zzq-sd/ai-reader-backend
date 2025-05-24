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
 * Neo4j文章节点实体
 * 表示知识图谱中的文章节点
 */
@Node("Article")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("mysql_id")
    private String mysqlId; // 对应MySQL中ArticleMetadata的id

    @Property("title")
    private String title;

    @Property("author")
    private String author;

    @Property("published_at")
    private LocalDateTime publishedAt;

    @Property("url")
    private String url;

    @Property("summary")
    private String summary;

    @Property("created_at")
    private LocalDateTime createdAt;

    @Property("updated_at")
    private LocalDateTime updatedAt;

    @Property("sentiment_score")
    private Double sentimentScore; // 情感分析分数，正面为正，负面为负

    @Property("reading_time_minutes")
    private Integer readingTimeMinutes;
    
    // 一篇文章可以涉及多个主题/概念
    @Relationship(type = "ABOUT", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<ConceptRelationship> concepts = new HashSet<>();
    
    // 文章标签关系
    @Relationship(type = "TAGGED", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<TagRelationship> tags = new HashSet<>();
    
    // 文章的作者关系 (可以有多个作者，每个作者被视为一个ConceptNode)
    @Relationship(type = "AUTHORED_BY", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<AuthorRelationship> authorRelationships = new HashSet<>();

    private String sourceId;

    /**
     * 设置ID
     * @param id ID字符串
     */
    public void setId(String id) {
        if (id != null) {
            this.mysqlId = id;
        }
    }
    
    /**
     * 获取源ID
     * @return 源ID
     */
    public String getSourceId() {
        return null; // 此处默认返回null，由子类实现或者在需要时覆盖
    }
    
    /**
     * 设置来源ID（RSS源ID）
     * @param sourceId RSS源ID
     */
    public void setSourceId(String sourceId) {
        if (sourceId != null) {
            this.sourceId = sourceId;
        }
    }
    
    /**
     * 设置发布日期
     * @param publishDate 发布日期
     */
    public void setPublishDate(LocalDateTime publishDate) {
        this.publishedAt = publishDate;
    }

    /**
     * 添加与概念的关系
     * 
     * @param concept 概念节点
     * @param relevanceScore 相关性分数
     * @param occurrenceCount 概念在文章中出现的次数
     * @param isPrimary 是否为主要概念
     * @return 当前文章节点
     */
    public ArticleNode addConcept(ConceptNode concept, Double relevanceScore, Integer occurrenceCount, Boolean isPrimary) {
        if (concepts == null) {
            concepts = new HashSet<>();
        }
        // Source node (this ArticleNode) is implicit for the relationship properties
        ConceptRelationship relationship = new ConceptRelationship(concept, relevanceScore, occurrenceCount, isPrimary);
        concepts.add(relationship);
        return this;
    }

    /**
     * 添加与标签的关系
     * 
     * @param tag 标签节点
     * @param addedBy 谁添加的标签
     * @param addedAt 何时添加的标签
     * @return 当前文章节点
     */
    public ArticleNode addTag(TagNode tag, String addedBy, LocalDateTime addedAt) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        TagRelationship relationship = new TagRelationship(this, tag, addedBy, addedAt);
        tags.add(relationship);
        return this;
    }

    /**
     * 添加作者关系
     * 
     * @param authorConcept 代表作者的概念节点
     * @param confidence 确信度
     * @return 当前文章节点
     */
    public ArticleNode addAuthor(ConceptNode authorConcept, Double confidence) {
        if (authorRelationships == null) {
            authorRelationships = new HashSet<>();
        }
        // The source node (this ArticleNode) is implicit in the relationship properties constructor for SDN 6+
        AuthorRelationship relationship = new AuthorRelationship(authorConcept, confidence);
        authorRelationships.add(relationship);
        return this;
    }
} 
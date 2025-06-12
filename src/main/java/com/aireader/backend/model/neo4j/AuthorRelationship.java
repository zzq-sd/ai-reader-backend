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
 * 文章与作者之间的关系
 * AUTHORED_BY关系：文章由某作者创作
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorRelationship {

    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private ConceptNode author; // 作者节点（特殊类型的概念节点）
    
    @Property("confidence")
    private Double confidence; // 确信度，表示确定这个作者是文章作者的概率
    
    /**
     * 构造函数
     * ArticleNode (源节点) 在这种关系属性的上下文中是隐式的。
     * 
     * @param author 作者概念节点
     * @param confidence 确信度
     */
    public AuthorRelationship(ConceptNode author, Double confidence) {
        this.author = author;
        this.confidence = confidence;
    }
} 
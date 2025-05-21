package com.aireader.backend.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * 知识图谱中的概念节点
 */
@Node("Concept")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Concept {

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Property("type")
    private ConceptType type;

    @Property("description")
    private String description;

    @Property("weight")
    private Integer weight = 1;

    /**
     * 概念类型枚举
     */
    public enum ConceptType {
        PERSON,        // 人物
        ORGANIZATION,  // 组织
        TECHNOLOGY,    // 技术
        PRODUCT,       // 产品
        LOCATION,      // 地点
        EVENT,         // 事件
        TOPIC,         // 主题
        KEYWORD,       // 关键词
        OTHER          // 其他
    }
}
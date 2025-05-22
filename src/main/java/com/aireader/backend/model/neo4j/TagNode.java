package com.aireader.backend.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * 标签节点Neo4j实体类
 */
@Data
@Node("Tag")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagNode {
    
    @Id
    private String id; // 自动生成的Neo4j ID
    
    @Property("mysqlId")
    private String mysqlId; // 对应MySQL tags.id
    
    @Property("name")
    private String name; // 标签名称
    
    @Property("userId")
    private String userId; // 创建该标签的用户ID
} 
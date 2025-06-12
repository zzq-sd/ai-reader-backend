package com.aireader.backend.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * RSS源节点Neo4j实体类
 */
@Data
@Node("RssSource")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssSourceNode {
    
    @Id
    private String id; // 自动生成的Neo4j ID
    
    @Property("mysqlId")
    private String mysqlId; // 对应MySQL rss_sources.id
    
    @Property("url")
    private String url; // RSS源的URL
    
    @Property("name")
    private String name; // RSS源名称
} 
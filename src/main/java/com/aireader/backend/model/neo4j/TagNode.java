package com.aireader.backend.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 知识图谱中的标签节点
 */
@Node("Tag")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("mysqlId")
    private String mysqlId;

    @Property("name")
    private String name;

    @Property("userId")
    private String userId;

    @Relationship(type = "TAGGED_NOTE", direction = Relationship.Direction.OUTGOING)
    private Set<NoteNode> notes = new HashSet<>();

    @Relationship(type = "TAGGED_ARTICLE", direction = Relationship.Direction.OUTGOING)
    private Set<ArticleNode> articles = new HashSet<>();

    @Relationship(type = "CREATED_BY", direction = Relationship.Direction.OUTGOING)
    private UserNode user;
} 
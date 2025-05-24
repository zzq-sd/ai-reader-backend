package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.TagNode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 标签节点资源库接口
 */
@Repository
public interface TagNodeRepository extends Neo4jRepository<TagNode, Long> {
    
    /**
     * 根据MySQL ID查找标签节点
     * 
     * @param mysqlId MySQL中的标签ID
     * @return 标签节点
     */
    Optional<TagNode> findByMysqlId(String mysqlId);
    
    /**
     * 查找用户的所有标签节点
     * 
     * @param userId 用户ID
     * @return 标签节点列表
     */
    List<TagNode> findByCreatedBy(String userId);
    
    /**
     * 查找用户特定名称的标签
     * 
     * @param userId 用户ID
     * @param name 标签名称
     * @return 标签节点
     */
    Optional<TagNode> findByCreatedByAndName(String userId, String name);
    
    /**
     * 查找带有指定标签的所有笔记
     * 
     * @param tagId 标签节点ID
     * @return 笔记节点列表
     */
    @Query("MATCH (t:Tag)-[:TAGGED_NOTE]->(n:Note) WHERE ID(t) = $tagId RETURN n")
    List<Map<String, Object>> findNotesByTagId(@Param("tagId") Long tagId);
    
    /**
     * 查找带有指定标签的所有文章
     * 
     * @param tagId 标签节点ID
     * @return 文章节点列表
     */
    @Query("MATCH (t:Tag)-[:TAGGED_ARTICLE]->(a:Article) WHERE ID(t) = $tagId RETURN a")
    List<Map<String, Object>> findArticlesByTagId(@Param("tagId") Long tagId);
    
    /**
     * 查找用户最常使用的标签
     * 
     * @param userId 用户ID
     * @param limit 返回结果限制
     * @return 标签和使用次数的列表
     */
    @Query("MATCH (u:User {mysqlId: $userId})<-[:CREATED_BY]-(t:Tag) " +
           "OPTIONAL MATCH (t)-[:TAGGED_NOTE]->(n:Note) " +
           "OPTIONAL MATCH (t)-[:TAGGED_ARTICLE]->(a:Article) " +
           "RETURN t, count(n) + count(a) as usageCount " +
           "ORDER BY usageCount DESC LIMIT $limit")
    List<Map<String, Object>> findMostUsedTags(@Param("userId") String userId, @Param("limit") int limit);
    
    /**
     * 删除指定MySQL ID的标签节点
     * 
     * @param mysqlId MySQL中的标签ID
     */
    void deleteByMysqlId(String mysqlId);
} 
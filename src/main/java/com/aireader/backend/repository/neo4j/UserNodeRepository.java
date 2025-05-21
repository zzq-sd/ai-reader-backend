package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.UserNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户节点资源库接口
 */
@Repository
public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {
    
    /**
     * 根据MySQL ID查找用户节点
     * 
     * @param mysqlId MySQL中的用户ID
     * @return 用户节点
     */
    Optional<UserNode> findByMysqlId(String mysqlId);
    
    /**
     * 查找用户的阅读兴趣（最常阅读的文章概念）
     * 
     * @param userId MySQL中的用户ID
     * @param limit 返回结果限制
     * @return 概念名和权重的列表
     */
    @Query("MATCH (u:User {mysqlId: $userId})-[r:READ]->(a:Article)-[:MENTIONS_CONCEPT]->(c:Concept) " +
           "RETURN c.name as conceptName, count(*) as weight, sum(r.readCount) as readCount " +
           "ORDER BY weight DESC, readCount DESC LIMIT $limit")
    List<Map<String, Object>> findUserReadingInterests(@Param("userId") String userId, @Param("limit") int limit);
    
    /**
     * 查找用户阅读过的相似文章
     * 
     * @param articleId 文章节点ID
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 相似文章节点列表
     */
    @Query("MATCH (a:Article)<-[r:READ]-(u:User {mysqlId: $userId}) " +
           "WITH a, r " +
           "MATCH (a)-[:MENTIONS_CONCEPT]->(c:Concept)<-[:MENTIONS_CONCEPT]-(similar:Article) " +
           "WHERE ID(a) = $articleId AND ID(similar) <> $articleId " +
           "RETURN similar, count(c) as commonConcepts " +
           "ORDER BY commonConcepts DESC")
    Page<Map<String, Object>> findSimilarReadArticles(
            @Param("articleId") Long articleId, 
            @Param("userId") String userId, 
            Pageable pageable);
    
    /**
     * 查找用户最近阅读的文章
     * 
     * @param userId MySQL中的用户ID
     * @param limit 返回结果限制
     * @return 文章和阅读关系信息列表
     */
    @Query("MATCH (u:User {mysqlId: $userId})-[r:READ]->(a:Article) " +
           "RETURN a, r ORDER BY r.lastReadAt DESC LIMIT $limit")
    List<Map<String, Object>> findRecentReadArticles(@Param("userId") String userId, @Param("limit") int limit);
    
    /**
     * 获取用户的知识图谱统计信息
     * 
     * @param userId MySQL中的用户ID
     * @return 统计信息Map
     */
    @Query("MATCH (u:User {mysqlId: $userId}) " +
           "OPTIONAL MATCH (u)-[:READ]->(ra:Article) " +
           "OPTIONAL MATCH (u)-[:FAVORITED]->(fa:Article) " +
           "OPTIONAL MATCH (u)-[:AUTHORED]->(n:Note) " +
           "RETURN count(DISTINCT ra) as readArticles, count(DISTINCT fa) as favoritedArticles, count(DISTINCT n) as notes")
    Map<String, Object> getUserKnowledgeGraphStats(@Param("userId") String userId);
} 
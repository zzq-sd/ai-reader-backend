package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.ConceptNode;
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
 * Neo4j用户节点仓库接口
 */
@Repository
public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {

    /**
     * 根据MySQL ID查找用户节点
     * 
     * @param mysqlId MySQL中的用户ID
     * @return 可选的用户节点
     */
    Optional<UserNode> findByMysqlId(String mysqlId);
    
    /**
     * 查找用户关注的用户
     * 
     * @param mysqlId 用户MySQL ID
     * @return 关注的用户节点列表
     */
    @Query("MATCH (u1:User)-[r:FOLLOWS]->(u2:User) " +
           "WHERE u1.mysql_id = $mysqlId " +
           "RETURN u2")
    List<UserNode> findFollowing(@Param("mysqlId") String mysqlId);
    
    /**
     * 查找关注用户的粉丝
     * 
     * @param mysqlId 用户MySQL ID
     * @return 粉丝用户节点列表
     */
    @Query("MATCH (u1:User)-[r:FOLLOWS]->(u2:User) " +
           "WHERE u2.mysql_id = $mysqlId " +
           "RETURN u1")
    List<UserNode> findFollowers(@Param("mysqlId") String mysqlId);
    
    /**
     * 查找具有相似兴趣的用户
     * 
     * @param mysqlId 用户MySQL ID
     * @param limit 限制返回数量
     * @return 相似兴趣的用户节点列表
     */
    @Query("MATCH (u1:User)-[r1:INTERESTED_IN]->(c:Concept)<-[r2:INTERESTED_IN]-(u2:User) " +
           "WHERE u1.mysql_id = $mysqlId AND u1 <> u2 " +
           "WITH u2, COUNT(c) AS common_interests, AVG(r1.interest_level * r2.interest_level) AS interest_similarity " +
           "RETURN u2 " +
           "ORDER BY interest_similarity DESC, common_interests DESC " +
           "LIMIT $limit")
    List<UserNode> findUsersWithSimilarInterests(@Param("mysqlId") String mysqlId, @Param("limit") int limit);
    
    /**
     * 推荐可能感兴趣的用户
     * 
     * @param mysqlId 用户MySQL ID
     * @param limit 限制返回数量
     * @return 推荐的用户节点列表
     */
    @Query("MATCH (u1:User)-[:FOLLOWS]->(u2:User)-[:FOLLOWS]->(u3:User) " +
           "WHERE u1.mysql_id = $mysqlId AND NOT (u1)-[:FOLLOWS]->(u3) AND u1 <> u3 " +
           "WITH u3, COUNT(DISTINCT u2) AS common_connections " +
           "RETURN u3 " +
           "ORDER BY common_connections DESC " +
           "LIMIT $limit")
    List<UserNode> recommendUsers(@Param("mysqlId") String mysqlId, @Param("limit") int limit);
    
    /**
     * 获取用户感兴趣的概念
     * 
     * @param mysqlId 用户MySQL ID
     * @param limit 限制返回数量
     * @return 概念节点列表
     */
    @Query("MATCH (u:User)-[r:INTERESTED_IN]->(c:Concept) " +
           "WHERE u.mysql_id = $mysqlId " +
           "RETURN c " +
           "ORDER BY r.interest_level DESC " +
           "LIMIT $limit")
    List<ConceptNode> findUserInterests(@Param("mysqlId") String mysqlId, @Param("limit") int limit);
    
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
    @Query(value = "MATCH (a:Article)<-[r:READ]-(u:User {mysqlId: $userId}) " +
           "WITH a, r " +
           "MATCH (a)-[:MENTIONS_CONCEPT]->(c:Concept)<-[:MENTIONS_CONCEPT]-(similar:Article) " +
           "WHERE ID(a) = $articleId AND ID(similar) <> $articleId " +
           "RETURN similar, count(c) as commonConcepts " +
           "ORDER BY commonConcepts DESC " +
           "SKIP $skip LIMIT $limit",
           countQuery = "MATCH (a:Article)<-[r:READ]-(u:User {mysqlId: $userId}) " +
           "WITH a, r " +
           "MATCH (a)-[:MENTIONS_CONCEPT]->(c:Concept)<-[:MENTIONS_CONCEPT]-(similar:Article) " +
           "WHERE ID(a) = $articleId AND ID(similar) <> $articleId " +
           "RETURN count(DISTINCT similar)")
    Page<Map<String, Object>> findSimilarReadArticles(
            @Param("articleId") String articleId, 
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
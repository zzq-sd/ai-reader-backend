package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.ConceptNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Neo4j概念节点仓库接口
 */
@Repository
public interface ConceptNodeRepository extends Neo4jRepository<ConceptNode, Long> {

    /**
     * 根据名称查找概念节点
     * 
     * @param name 概念名称
     * @return 可选的概念节点
     */
    Optional<ConceptNode> findByName(String name);
    
    /**
     * 根据名称模糊查找概念节点
     * 
     * @param namePattern 概念名称模式
     * @return 概念节点列表
     */
    @Query("MATCH (c:Concept) WHERE c.name =~ $namePattern RETURN c")
    List<ConceptNode> findByNameLike(@Param("namePattern") String namePattern);
    
    /**
     * 根据类型查找概念节点
     * 
     * @param type 概念类型
     * @return 概念节点列表
     */
    List<ConceptNode> findByType(String type);
    
    /**
     * 查找与指定文章相关的概念
     * 
     * @param articleId 文章ID
     * @return 概念节点列表
     */
    @Query("MATCH (a:Article)-[r:ABOUT]->(c:Concept) " +
           "WHERE ID(a) = $articleId " +
           "RETURN c " +
           "ORDER BY r.relevance_score DESC")
    List<ConceptNode> findConceptsByArticle(@Param("articleId") Long articleId);
    
    /**
     * 查找相关概念
     * 
     * @param conceptId 概念节点ID
     * @param limit 限制返回数量
     * @return 概念节点列表
     */
    @Query("MATCH (c1:Concept)-[r:RELATED_TO]->(c2:Concept) " +
           "WHERE ID(c1) = $conceptId " +
           "RETURN c2 " +
           "ORDER BY r.strength DESC " +
           "LIMIT $limit")
    List<ConceptNode> findRelatedConcepts(@Param("conceptId") Long conceptId, @Param("limit") int limit);
    
    /**
     * 查找用户感兴趣的概念
     * 
     * @param userId 用户ID
     * @param limit 限制返回数量
     * @return 概念节点列表
     */
    @Query("MATCH (u:User)-[r:INTERESTED_IN]->(c:Concept) " +
           "WHERE u.mysql_id = $userId " +
           "RETURN c " +
           "ORDER BY r.interest_level DESC " +
           "LIMIT $limit")
    List<ConceptNode> findUserInterestConcepts(@Param("userId") String userId, @Param("limit") int limit);
    
    /**
     * 获取热门概念
     * 
     * @param limit 限制返回数量
     * @return 概念节点列表
     */
    @Query("MATCH (c:Concept)<-[r:ABOUT]-(a:Article) " +
           "WITH c, COUNT(a) AS article_count " +
           "RETURN c " +
           "ORDER BY article_count DESC, c.relevance_score DESC " +
           "LIMIT $limit")
    List<ConceptNode> findTrendingConcepts(@Param("limit") int limit);
} 
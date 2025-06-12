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
    
    /**
     * 根据名称或描述搜索概念（全文搜索）
     * 
     * @param searchTerm 搜索词
     * @param limit 限制返回数量
     * @return 概念节点列表
     */
    @Query("MATCH (c:Concept) " +
           "WHERE c.name CONTAINS $searchTerm OR c.description CONTAINS $searchTerm " +
           "RETURN c " +
           "ORDER BY " +
           "  CASE WHEN c.name = $searchTerm THEN 1 " +
           "       WHEN c.name CONTAINS $searchTerm THEN 2 " +
           "       ELSE 3 END, " +
           "  c.relevance_score DESC " +
           "LIMIT $limit")
    List<ConceptNode> searchConceptsByText(@Param("searchTerm") String searchTerm, @Param("limit") int limit);
    
    /**
     * 获取概念的统计信息
     * 
     * @param conceptName 概念名称
     * @return 统计信息Map
     */
    @Query("MATCH (c:Concept {name: $conceptName}) " +
           "OPTIONAL MATCH (c)<-[ar:ABOUT]-(a:Article) " +
           "OPTIONAL MATCH (c)<-[nr:MENTIONS]-(n:Note) " +
           "OPTIONAL MATCH (c)-[cr:RELATED_TO]-(rc:Concept) " +
           "RETURN c.name AS conceptName, " +
           "       c.type AS conceptType, " +
           "       c.description AS description, " +
           "       c.relevance_score AS importanceScore, " +
           "       COUNT(DISTINCT a) AS articleCount, " +
           "       COUNT(DISTINCT n) AS noteCount, " +
           "       COUNT(DISTINCT rc) AS relatedConceptCount, " +
           "       AVG(ar.relevance_score) AS avgRelevanceScore")
    Optional<Object[]> getConceptStatistics(@Param("conceptName") String conceptName);
    
    /**
     * 查找概念相关的文章（带分页）
     * 
     * @param conceptName 概念名称
     * @param limit 限制数量
     * @return 文章信息列表
     */
    @Query("MATCH (c:Concept {name: $conceptName})<-[r:ABOUT]-(a:Article) " +
           "RETURN a.mysql_id AS id, " +
           "       a.title AS title, " +
           "       a.summary AS summary, " +
           "       r.relevance_score AS relevanceScore, " +
           "       a.published_at AS publishDate " +
           "ORDER BY r.relevance_score DESC " +
           "LIMIT $limit")
    List<Object[]> findRelatedArticlesByConcept(@Param("conceptName") String conceptName, @Param("limit") int limit);
    
    /**
     * 计算概念重要性分数
     * 基于连接度、相关性等因素
     * 
     * @param conceptId 概念ID
     * @return 重要性分数
     */
    @Query("MATCH (c:Concept) WHERE ID(c) = $conceptId " +
           "OPTIONAL MATCH (c)<-[ar:ABOUT]-(a:Article) " +
           "OPTIONAL MATCH (c)<-[nr:MENTIONS]-(n:Note) " +
           "OPTIONAL MATCH (c)-[cr:RELATED_TO]-(rc:Concept) " +
           "WITH c, " +
           "     COUNT(DISTINCT a) AS articleCount, " +
           "     COUNT(DISTINCT n) AS noteCount, " +
           "     COUNT(DISTINCT rc) AS relatedCount, " +
           "     AVG(ar.relevance_score) AS avgArticleRelevance, " +
           "     AVG(nr.relevance_score) AS avgNoteRelevance " +
           "RETURN (articleCount * 0.4 + noteCount * 0.3 + relatedCount * 0.2 + " +
           "        COALESCE(avgArticleRelevance, 0) * 0.1) AS importanceScore")
    Optional<Double> calculateConceptImportance(@Param("conceptId") Long conceptId);
    
    /**
     * 发现概念集群
     * 基于关系强度发现相关概念群组
     * 
     * @param minClusterSize 最小集群大小
     * @return 概念集群
     */
    @Query("MATCH (c1:Concept)-[r:RELATED_TO]-(c2:Concept) " +
           "WHERE r.strength > 0.5 " +
           "WITH c1, COLLECT(c2) AS relatedConcepts " +
           "WHERE SIZE(relatedConcepts) >= $minClusterSize " +
           "RETURN c1, relatedConcepts " +
           "ORDER BY SIZE(relatedConcepts) DESC")
    List<Object[]> discoverConceptClusters(@Param("minClusterSize") int minClusterSize);
    
    /**
     * 根据用户兴趣推荐概念
     * 
     * @param userId 用户ID
     * @param limit 推荐数量
     * @return 推荐概念列表
     */
    @Query("MATCH (u:User {mysql_id: $userId})-[ir:INTERESTED_IN]->(ic:Concept) " +
           "MATCH (ic)-[r:RELATED_TO]->(rc:Concept) " +
           "WHERE NOT EXISTS((u)-[:INTERESTED_IN]->(rc)) " +
           "WITH rc, AVG(ir.interest_level * r.strength) AS score " +
           "RETURN rc " +
           "ORDER BY score DESC " +
           "LIMIT $limit")
    List<ConceptNode> recommendConceptsForUser(@Param("userId") String userId, @Param("limit") int limit);
} 
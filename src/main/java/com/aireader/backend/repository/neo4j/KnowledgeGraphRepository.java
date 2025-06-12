package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.ConceptNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识图谱专用Repository
 * 提供复杂的图谱查询和分析功能
 */
@Repository
public interface KnowledgeGraphRepository extends Neo4jRepository<ConceptNode, Long> {
    
    /**
     * 获取知识图谱数据（节点和边）
     * 
     * @param nodeType 节点类型过滤（可选）
     * @param searchTerm 搜索关键词（可选）
     * @param limit 结果限制
     * @return 图谱数据
     */
    @Query("MATCH (n) " +
           "WHERE ($nodeType IS NULL OR labels(n)[0] = $nodeType) " +
           "  AND ($searchTerm IS NULL OR n.name CONTAINS $searchTerm OR n.title CONTAINS $searchTerm) " +
           "OPTIONAL MATCH (n)-[r]-(m) " +
           "WHERE ($nodeType IS NULL OR labels(m)[0] = $nodeType) " +
           "  AND ($searchTerm IS NULL OR m.name CONTAINS $searchTerm OR m.title CONTAINS $searchTerm) " +
           "RETURN n, r, m " +
           "LIMIT $limit")
    List<Object[]> getGraphData(@Param("nodeType") String nodeType, 
                               @Param("searchTerm") String searchTerm, 
                               @Param("limit") int limit);
    
    /**
     * 获取图谱统计信息
     * 
     * @return 统计信息
     */
    @Query("MATCH (n) " +
           "OPTIONAL MATCH (n)-[r]-() " +
           "WITH labels(n)[0] AS nodeType, COUNT(DISTINCT n) AS nodeCount, COUNT(r) AS edgeCount " +
           "RETURN nodeType, nodeCount, edgeCount")
    List<Object[]> getGraphStatistics();
    
    /**
     * 查找节点的邻居节点
     * 
     * @param nodeId 节点ID
     * @param depth 深度
     * @param limit 限制数量
     * @return 邻居节点
     */
    @Query("MATCH (start) WHERE ID(start) = $nodeId " +
           "MATCH (start)-[*1..$depth]-(neighbor) " +
           "RETURN DISTINCT neighbor " +
           "LIMIT $limit")
    List<Object> findNeighborNodes(@Param("nodeId") Long nodeId, 
                                  @Param("depth") int depth, 
                                  @Param("limit") int limit);
    
    /**
     * 查找两个节点之间的最短路径
     * 
     * @param startNodeId 起始节点ID
     * @param endNodeId 结束节点ID
     * @param maxDepth 最大深度
     * @return 路径信息
     */
    @Query("MATCH (start), (end) " +
           "WHERE ID(start) = $startNodeId AND ID(end) = $endNodeId " +
           "MATCH path = shortestPath((start)-[*1..$maxDepth]-(end)) " +
           "RETURN path")
    Optional<Object> findShortestPath(@Param("startNodeId") Long startNodeId, 
                                     @Param("endNodeId") Long endNodeId, 
                                     @Param("maxDepth") int maxDepth);
    
    /**
     * 查找中心性最高的节点
     * 
     * @param nodeType 节点类型
     * @param limit 限制数量
     * @return 中心节点列表
     */
    @Query("MATCH (n) " +
           "WHERE ($nodeType IS NULL OR labels(n)[0] = $nodeType) " +
           "OPTIONAL MATCH (n)-[r]-() " +
           "WITH n, COUNT(r) AS degree " +
           "RETURN n " +
           "ORDER BY degree DESC " +
           "LIMIT $limit")
    List<Object> findCentralNodes(@Param("nodeType") String nodeType, @Param("limit") int limit);
    
    /**
     * 查找孤立节点（没有连接的节点）
     * 
     * @param nodeType 节点类型
     * @return 孤立节点列表
     */
    @Query("MATCH (n) " +
           "WHERE ($nodeType IS NULL OR labels(n)[0] = $nodeType) " +
           "  AND NOT (n)-[]-() " +
           "RETURN n")
    List<Object> findIsolatedNodes(@Param("nodeType") String nodeType);
    
    /**
     * 分析概念共现模式
     * 
     * @param conceptName 概念名称
     * @param limit 限制数量
     * @return 共现概念列表
     */
    @Query("MATCH (c1:Concept {name: $conceptName})<-[:ABOUT]-(a:Article)-[:ABOUT]->(c2:Concept) " +
           "WHERE c1 <> c2 " +
           "WITH c2, COUNT(a) AS cooccurrence " +
           "RETURN c2, cooccurrence " +
           "ORDER BY cooccurrence DESC " +
           "LIMIT $limit")
    List<Object[]> findConceptCooccurrence(@Param("conceptName") String conceptName, @Param("limit") int limit);
    
    /**
     * 查找概念演化路径
     * 基于时间序列分析概念的发展轨迹
     * 
     * @param conceptName 概念名称
     * @return 演化路径
     */
    @Query("MATCH (c:Concept {name: $conceptName})<-[:ABOUT]-(a:Article) " +
           "WITH c, a " +
           "ORDER BY a.published_at " +
           "MATCH (a)-[:ABOUT]->(related:Concept) " +
           "WHERE related <> c " +
           "WITH c, related, a.published_at AS publishTime, COUNT(*) AS frequency " +
           "RETURN related, publishTime, frequency " +
           "ORDER BY publishTime")
    List<Object[]> findConceptEvolutionPath(@Param("conceptName") String conceptName);
    
    /**
     * 发现新兴概念
     * 基于时间窗口分析新出现的概念
     * 
     * @param daysBefore 天数
     * @param limit 限制数量
     * @return 新兴概念列表
     */
    @Query("MATCH (c:Concept)<-[:ABOUT]-(a:Article) " +
           "WHERE a.published_at > datetime() - duration({days: $daysBefore}) " +
           "WITH c, COUNT(a) AS recentMentions " +
           "MATCH (c)<-[:ABOUT]-(allArticles:Article) " +
           "WITH c, recentMentions, COUNT(allArticles) AS totalMentions " +
           "WHERE recentMentions > totalMentions * 0.5 " +
           "RETURN c " +
           "ORDER BY recentMentions DESC " +
           "LIMIT $limit")
    List<ConceptNode> findEmergingConcepts(@Param("daysBefore") int daysBefore, @Param("limit") int limit);
} 
package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.ArticleNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Neo4j文章节点数据访问接口
 */
@Repository
public interface ArticleNodeRepository extends Neo4jRepository<ArticleNode, String> {

    /**
     * 根据MySQL ID查找文章节点
     *
     * @param mysqlId MySQL文章ID
     * @return 文章节点Optional包装
     */
    Optional<ArticleNode> findByMysqlId(String mysqlId);

    /**
     * 根据MongoDB ID查找文章节点
     *
     * @param mongoId MongoDB文章ID
     * @return 文章节点Optional包装
     */
    Optional<ArticleNode> findByMongoId(String mongoId);

    /**
     * 查找指定RSS源的所有文章
     *
     * @param rssSourceId RSS源ID
     * @return 文章节点列表
     */
    List<ArticleNode> findByRssSourceId(String rssSourceId);

    /**
     * 查找提及指定概念的文章
     *
     * @param conceptId 概念节点ID
     * @return 文章节点列表
     */
    @Query("MATCH (a:Article)-[r:MENTIONS_CONCEPT]->(c:Concept) WHERE id(c) = $conceptId RETURN a ORDER BY r.frequency DESC")
    List<ArticleNode> findArticlesByConceptId(@Param("conceptId") String conceptId);

    /**
     * 查找提及指定概念名称的文章
     *
     * @param conceptName 概念名称
     * @return 文章节点列表
     */
    @Query("MATCH (a:Article)-[r:MENTIONS_CONCEPT]->(c:Concept {name: $conceptName}) RETURN a ORDER BY r.frequency DESC")
    List<ArticleNode> findArticlesByConceptName(@Param("conceptName") String conceptName);
    
    /**
     * 查找与指定概念相关的文章并限制数量
     * 
     * @param conceptName 概念名称
     * @param limit 限制数量
     * @return 文章节点列表
     */
    @Query("MATCH (a:Article)-[r:MENTIONS]->(c:Concept {name: $conceptName}) " +
           "RETURN a ORDER BY r.relevance DESC LIMIT $limit")
    List<ArticleNode> findArticlesByConceptName(@Param("conceptName") String conceptName, @Param("limit") int limit);

    /**
     * 查找与指定文章相关的文章（通过共同概念）
     *
     * @param mysqlId MySQL文章ID
     * @param limit 返回数量限制
     * @return 文章节点列表
     */
    @Query("MATCH (a1:Article {mysqlId: $mysqlId})-[:MENTIONS_CONCEPT]->(c:Concept)<-[r:MENTIONS_CONCEPT]-(a2:Article) " +
           "WHERE a1 <> a2 " +
           "WITH a2, count(c) AS commonConcepts, sum(r.frequency) AS relevanceScore " +
           "RETURN a2 " +
           "ORDER BY commonConcepts DESC, relevanceScore DESC " +
           "LIMIT $limit")
    List<ArticleNode> findRelatedArticles(@Param("mysqlId") String mysqlId, @Param("limit") int limit);
    
    /**
     * 查找与指定文章相似的文章
     * 
     * @param articleId 文章ID
     * @param limit 限制数量
     * @return 文章节点列表
     */
    @Query("MATCH (a:Article {id: $articleId})-[:MENTIONS]->(c:Concept)<-[:MENTIONS]-(similar:Article) " +
           "WHERE a <> similar " +
           "WITH similar, count(c) AS commonConcepts " +
           "RETURN similar ORDER BY commonConcepts DESC LIMIT $limit")
    List<ArticleNode> findSimilarArticles(@Param("articleId") String articleId, @Param("limit") int limit);
    
    /**
     * 查找推荐给用户的文章
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 文章节点列表
     */
    @Query("MATCH (u:User {mysqlId: $userId})-[:HAS_READ]->(a:Article)-[:MENTIONS]->(c:Concept)<-[:MENTIONS]-(rec:Article) " +
           "WHERE NOT (u)-[:HAS_READ]->(rec) " +
           "WITH rec, count(DISTINCT c) AS relevance " +
           "RETURN rec ORDER BY relevance DESC LIMIT $limit")
    List<ArticleNode> findRecommendedArticlesForUser(@Param("userId") String userId, @Param("limit") int limit);

    /**
     * 获取知识图谱的子图数据（用于可视化）
     *
     * @param centerMysqlId 中心文章MySQL ID
     * @param depth 深度
     * @return 包含节点和关系的图数据（Map格式）
     */
    @Query("MATCH path = (a:Article {mysqlId: $centerMysqlId})-[:MENTIONS_CONCEPT*1..$depth]-(n) " +
           "WITH nodes(path) as nodes, relationships(path) as rels " +
           "RETURN collect(distinct nodes) as nodes, collect(distinct rels) as relationships")
    Object getGraphData(@Param("centerMysqlId") String centerMysqlId, @Param("depth") int depth);
} 
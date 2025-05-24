package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.ConceptNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Neo4j概念节点数据访问接口
 */
@Repository
public interface ConceptRepository extends Neo4jRepository<ConceptNode, Long> {

    /**
     * 根据概念名称查找概念
     *
     * @param name 概念名称
     * @return 概念节点Optional包装
     */
    Optional<ConceptNode> findByName(String name);

    /**
     * 根据概念名称和类型查找概念
     *
     * @param name 概念名称
     * @param type 概念类型
     * @return 概念节点Optional包装
     */
    Optional<ConceptNode> findByNameAndType(String name, String type);

    /**
     * 查找特定类型的所有概念
     *
     * @param type 概念类型
     * @return 概念列表
     */
    List<ConceptNode> findByType(String type);

    /**
     * 查找名称包含特定关键词的概念
     *
     * @param keyword 关键词
     * @return 概念列表
     */
    List<ConceptNode> findByNameContaining(String keyword);

    /**
     * 查找与特定文章相关的概念
     *
     * @param articleMysqlId 文章MySQL ID
     * @return 概念列表
     */
    @Query("MATCH (a:Article {mysqlId: $articleMysqlId})-[r:MENTIONS_CONCEPT]->(c:Concept) RETURN c ORDER BY r.frequency DESC")
    List<ConceptNode> findConceptsByArticle(@Param("articleMysqlId") String articleMysqlId);

    /**
     * 查找出现频率最高的概念
     *
     * @param limit 返回数量限制
     * @return 概念列表
     */
    @Query("MATCH (c:Concept) RETURN c ORDER BY c.relevanceScore DESC LIMIT $limit")
    List<ConceptNode> findTopConcepts(@Param("limit") int limit);

    /**
     * 查找两篇文章共同提及的概念
     *
     * @param articleMysqlId1 文章1 MySQL ID
     * @param articleMysqlId2 文章2 MySQL ID
     * @return 概念列表
     */
    @Query("MATCH (a1:Article {mysqlId: $articleMysqlId1})-[:MENTIONS_CONCEPT]->(c:Concept)<-[:MENTIONS_CONCEPT]-(a2:Article {mysqlId: $articleMysqlId2}) RETURN c")
    List<ConceptNode> findCommonConceptsBetweenArticles(
            @Param("articleMysqlId1") String articleMysqlId1, 
            @Param("articleMysqlId2") String articleMysqlId2);
} 
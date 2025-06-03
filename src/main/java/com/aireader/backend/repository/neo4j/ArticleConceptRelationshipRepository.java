package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.ArticleConceptRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Neo4j文章-概念关系仓库接口
 */
@Repository
public interface ArticleConceptRelationshipRepository extends Neo4jRepository<ArticleConceptRelationship, Long> {

    /**
     * 查找文章与概念之间的关系
     * 
     * @param articleId 文章节点ID
     * @param conceptId 概念节点ID
     * @return 可选的文章-概念关系
     */
    @Query("MATCH (a:Article)-[r:ABOUT]->(c:Concept) " +
           "WHERE ID(a) = $articleId AND ID(c) = $conceptId " +
           "RETURN r")
    Optional<ArticleConceptRelationship> findRelationship(@Param("articleId") Long articleId, @Param("conceptId") Long conceptId);
    
    /**
     * 查找文章的所有概念关系
     * 
     * @param articleId 文章节点ID
     * @return 文章-概念关系列表
     */
    @Query("MATCH (a:Article)-[r:ABOUT]->(c:Concept) " +
           "WHERE ID(a) = $articleId " +
           "RETURN r " +
           "ORDER BY r.relevanceScore DESC")
    List<ArticleConceptRelationship> findRelationshipsByArticleId(@Param("articleId") Long articleId);
    
    /**
     * 查找与概念相关的所有文章关系
     * 
     * @param conceptId 概念节点ID
     * @return 文章-概念关系列表
     */
    @Query("MATCH (a:Article)-[r:ABOUT]->(c:Concept) " +
           "WHERE ID(c) = $conceptId " +
           "RETURN r " +
           "ORDER BY r.relevanceScore DESC")
    List<ArticleConceptRelationship> findRelationshipsByConceptId(@Param("conceptId") Long conceptId);
    
    /**
     * 查找文章的主要概念关系（按相关性评分排序）
     * 
     * @param articleId 文章节点ID
     * @param limit 限制返回数量
     * @return 文章-概念关系列表
     */
    @Query("MATCH (a:Article)-[r:ABOUT]->(c:Concept) " +
           "WHERE ID(a) = $articleId " +
           "RETURN r " +
           "ORDER BY r.relevanceScore DESC " +
           "LIMIT $limit")
    List<ArticleConceptRelationship> findTopConceptRelationships(@Param("articleId") Long articleId, @Param("limit") int limit);
    
    /**
     * 更新文章与概念之间的关系评分
     * 
     * @param articleId 文章节点ID
     * @param conceptId 概念节点ID
     * @param relevanceScore 相关性评分
     * @return 是否更新成功
     */
    @Query("MATCH (a:Article)-[r:ABOUT]->(c:Concept) " +
           "WHERE ID(a) = $articleId AND ID(c) = $conceptId " +
           "SET r.relevanceScore = $relevanceScore " +
           "RETURN r")
    boolean updateRelevanceScore(
            @Param("articleId") Long articleId, 
            @Param("conceptId") Long conceptId, 
            @Param("relevanceScore") Double relevanceScore);
} 
package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.ArticleNode;
import com.aireader.backend.model.neo4j.ConceptNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Neo4j文章节点仓库接口
 */
@Repository
public interface ArticleNodeRepository extends Neo4jRepository<ArticleNode, Long> {

    /**
     * 根据MySQL ID查找文章节点
     * 
     * @param mysqlId MySQL数据库中的文章ID
     * @return 可选的文章节点
     */
    Optional<ArticleNode> findByMysqlId(String mysqlId);
    
    /**
     * 查找与指定概念相关的文章
     * 
     * @param conceptId 概念节点ID
     * @return 文章节点列表
     */
    @Query("MATCH (a:Article)-[r:ABOUT]->(c:Concept) " +
           "WHERE ID(c) = $conceptId " +
           "RETURN a " +
           "ORDER BY r.relevance_score DESC")
    List<ArticleNode> findArticlesByConcept(@Param("conceptId") Long conceptId);
    
    /**
     * 查找与多个概念相关的文章，基于相关性得分排序
     * 
     * @param conceptIds 概念节点ID列表
     * @param limit 限制返回数量
     * @return 文章节点列表
     */
    @Query("MATCH (a:Article)-[r:ABOUT]->(c:Concept) " +
           "WHERE ID(c) IN $conceptIds " +
           "WITH a, SUM(r.relevance_score) AS total_score " +
           "RETURN a " +
           "ORDER BY total_score DESC " +
           "LIMIT $limit")
    List<ArticleNode> findArticlesByMultipleConcepts(
            @Param("conceptIds") List<Long> conceptIds, 
            @Param("limit") int limit);
    
    /**
     * 查找与指定文章相似的文章
     * 基于共同概念的数量和相关性得分
     * 
     * @param articleId 文章节点ID
     * @param limit 限制返回数量
     * @return 文章节点列表
     */
    @Query("MATCH (a1:Article)-[r1:ABOUT]->(c:Concept)<-[r2:ABOUT]-(a2:Article) " +
           "WHERE ID(a1) = $articleId AND ID(a1) <> ID(a2) " +
           "WITH a2, COUNT(c) AS common_concepts, SUM(r1.relevance_score * r2.relevance_score) AS similarity_score " +
           "WHERE common_concepts > 3 " +
           "RETURN a2 " +
           "ORDER BY similarity_score DESC " +
           "LIMIT $limit")
    List<ArticleNode> findSimilarArticles(@Param("articleId") Long articleId, @Param("limit") int limit);
} 
package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.AuthorRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Neo4j作者关系仓库接口
 */
@Repository
public interface AuthorRelationshipRepository extends Neo4jRepository<AuthorRelationship, Long> {

    /**
     * 查找文章与作者之间的关系
     * 
     * @param articleId 文章节点ID
     * @param authorId 作者概念节点ID
     * @return 可选的作者关系
     */
    @Query("MATCH (a:Article)-[r:AUTHORED_BY]->(c:Concept) " +
           "WHERE ID(a) = $articleId AND ID(c) = $authorId " +
           "RETURN r")
    Optional<AuthorRelationship> findRelationship(@Param("articleId") Long articleId, @Param("authorId") Long authorId);
    
    /**
     * 查找文章的所有作者关系
     * 
     * @param articleId 文章节点ID
     * @return 作者关系列表
     */
    @Query("MATCH (a:Article)-[r:AUTHORED_BY]->(c:Concept) " +
           "WHERE ID(a) = $articleId " +
           "RETURN r " +
           "ORDER BY r.confidence DESC")
    List<AuthorRelationship> findAuthorsByArticleId(@Param("articleId") Long articleId);
    
    /**
     * 查找作者的所有文章关系
     * 
     * @param authorId 作者概念节点ID
     * @return 作者关系列表
     */
    @Query("MATCH (a:Article)-[r:AUTHORED_BY]->(c:Concept) " +
           "WHERE ID(c) = $authorId " +
           "RETURN r " +
           "ORDER BY r.confidence DESC")
    List<AuthorRelationship> findArticlesByAuthorId(@Param("authorId") Long authorId);
    
    /**
     * 查找文章的主要作者关系
     * 
     * @param articleId 文章节点ID
     * @return 可选的作者关系
     */
    @Query("MATCH (a:Article)-[r:AUTHORED_BY]->(c:Concept) " +
           "WHERE ID(a) = $articleId " +
           "RETURN r " +
           "ORDER BY r.confidence DESC " +
           "LIMIT 1")
    Optional<AuthorRelationship> findPrimaryAuthor(@Param("articleId") Long articleId);
    
    /**
     * 更新文章与作者之间的关系置信度
     * 
     * @param articleId 文章节点ID
     * @param authorId 作者概念节点ID
     * @param confidence 置信度
     * @return 是否更新成功
     */
    @Query("MATCH (a:Article)-[r:AUTHORED_BY]->(c:Concept) " +
           "WHERE ID(a) = $articleId AND ID(c) = $authorId " +
           "SET r.confidence = $confidence " +
           "RETURN r")
    boolean updateConfidence(
            @Param("articleId") Long articleId, 
            @Param("authorId") Long authorId, 
            @Param("confidence") Double confidence);
    
    /**
     * 查找特定作者的最新文章关系
     * 
     * @param authorId 作者概念节点ID
     * @param limit 限制返回数量
     * @return 作者关系列表
     */
    @Query("MATCH (a:Article)-[r:AUTHORED_BY]->(c:Concept) " +
           "WHERE ID(c) = $authorId " +
           "RETURN r " +
           "ORDER BY a.publishDate DESC " +
           "LIMIT $limit")
    List<AuthorRelationship> findLatestArticlesByAuthor(@Param("authorId") Long authorId, @Param("limit") int limit);
} 
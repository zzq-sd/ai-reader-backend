package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.NoteNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 笔记节点资源库接口
 */
@Repository
public interface NoteNodeRepository extends Neo4jRepository<NoteNode, Long> {
    
    /**
     * 根据MySQL ID查找笔记节点
     * 
     * @param mysqlId MySQL中的笔记ID
     * @return 笔记节点
     */
    Optional<NoteNode> findByMysqlId(String mysqlId);
    
    /**
     * 查找用户的所有笔记节点
     * 
     * @param userId 用户ID
     * @return 笔记节点列表
     */
    List<NoteNode> findByUserId(String userId);
    
    /**
     * 查找与特定概念相关的笔记
     * 
     * @param conceptId 概念ID
     * @param pageable 分页参数
     * @return 笔记节点列表
     */
    @Query(
        value = "MATCH (n:Note)-[r:MENTIONS_CONCEPT]->(c:Concept) WHERE ID(c) = $conceptId RETURN n, r, c ORDER BY r.frequency DESC SKIP $skip LIMIT $limit",
        countQuery = "MATCH (n:Note)-[r:MENTIONS_CONCEPT]->(c:Concept) WHERE ID(c) = $conceptId RETURN count(DISTINCT n)"
    )
    Page<NoteNode> findNotesByConceptId(@Param("conceptId") Long conceptId, Pageable pageable);
    
    /**
     * 查找与特定文章相关的笔记
     * 
     * @param articleNodeId 文章节点ID
     * @return 笔记节点列表
     */
    @Query("MATCH (n:Note)-[:REFERS_TO]->(a:Article) WHERE ID(a) = $articleNodeId RETURN n")
    List<NoteNode> findNotesByArticleId(@Param("articleNodeId") Long articleNodeId);
    
    /**
     * 查找含有特定概念的用户笔记
     * 
     * @param userId 用户ID
     * @param conceptName 概念名称
     * @param pageable 分页参数
     * @return 笔记节点列表
     */
    @Query(
        value = "MATCH (u:User)<-[:AUTHORED_BY]-(n:Note)-[r:MENTIONS_CONCEPT]->(c:Concept) " +
           "WHERE u.mysqlId = $userId AND c.name CONTAINS $conceptName " +
           "RETURN n, r, c ORDER BY r.frequency DESC " +
           "SKIP $skip LIMIT $limit",
        countQuery = "MATCH (u:User)<-[:AUTHORED_BY]-(n:Note)-[r:MENTIONS_CONCEPT]->(c:Concept) " +
           "WHERE u.mysqlId = $userId AND c.name CONTAINS $conceptName " +
           "RETURN count(DISTINCT n)"
    )
    Page<NoteNode> findUserNotesByConceptName(
            @Param("userId") String userId, 
            @Param("conceptName") String conceptName, 
            Pageable pageable);
    
    /**
     * 删除指定MySQL ID的笔记节点
     * 
     * @param mysqlId MySQL中的笔记ID
     */
    void deleteByMysqlId(String mysqlId);
} 
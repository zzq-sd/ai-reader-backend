package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.ConceptConceptRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Neo4j概念-概念关系仓库接口
 */
@Repository
public interface ConceptConceptRelationshipRepository extends Neo4jRepository<ConceptConceptRelationship, Long> {

    /**
     * 查找两个概念之间的关系
     * 
     * @param sourceConceptId 源概念节点ID
     * @param targetConceptId 目标概念节点ID
     * @return 可选的概念-概念关系
     */
    @Query("MATCH (c1:Concept)-[r:RELATED_TO]->(c2:Concept) " +
           "WHERE ID(c1) = $sourceConceptId AND ID(c2) = $targetConceptId " +
           "RETURN r")
    Optional<ConceptConceptRelationship> findRelationship(
            @Param("sourceConceptId") Long sourceConceptId, 
            @Param("targetConceptId") Long targetConceptId);
    
    /**
     * 查找概念的所有相关概念关系
     * 
     * @param conceptId 概念节点ID
     * @return 概念-概念关系列表
     */
    @Query("MATCH (c1:Concept)-[r:RELATED_TO]->(c2:Concept) " +
           "WHERE ID(c1) = $conceptId " +
           "RETURN r " +
           "ORDER BY r.strength DESC")
    List<ConceptConceptRelationship> findOutgoingRelationships(@Param("conceptId") Long conceptId);
    
    /**
     * 查找与概念相关的所有入向概念关系
     * 
     * @param conceptId 概念节点ID
     * @return 概念-概念关系列表
     */
    @Query("MATCH (c1:Concept)-[r:RELATED_TO]->(c2:Concept) " +
           "WHERE ID(c2) = $conceptId " +
           "RETURN r " +
           "ORDER BY r.strength DESC")
    List<ConceptConceptRelationship> findIncomingRelationships(@Param("conceptId") Long conceptId);
    
    /**
     * 查找概念的最强相关概念关系
     * 
     * @param conceptId 概念节点ID
     * @param limit 限制返回数量
     * @return 概念-概念关系列表
     */
    @Query("MATCH (c1:Concept)-[r:RELATED_TO]->(c2:Concept) " +
           "WHERE ID(c1) = $conceptId " +
           "RETURN r " +
           "ORDER BY r.strength DESC " +
           "LIMIT $limit")
    List<ConceptConceptRelationship> findTopRelatedConcepts(@Param("conceptId") Long conceptId, @Param("limit") int limit);
    
    /**
     * 更新概念之间的关系强度
     * 
     * @param sourceConceptId 源概念节点ID
     * @param targetConceptId 目标概念节点ID
     * @param strength 关系强度
     * @return 是否更新成功
     */
    @Query("MATCH (c1:Concept)-[r:RELATED_TO]->(c2:Concept) " +
           "WHERE ID(c1) = $sourceConceptId AND ID(c2) = $targetConceptId " +
           "SET r.strength = $strength " +
           "RETURN r")
    boolean updateRelationshipStrength(
            @Param("sourceConceptId") Long sourceConceptId, 
            @Param("targetConceptId") Long targetConceptId, 
            @Param("strength") Double strength);
    
    /**
     * 查找两个概念之间的路径
     * 
     * @param sourceConceptId 源概念节点ID
     * @param targetConceptId 目标概念节点ID
     * @param maxDepth 最大路径深度
     * @return 路径上的关系列表
     */
    @Query("MATCH path = shortestPath((c1:Concept)-[:RELATED_TO*1..$maxDepth]->(c2:Concept)) " +
           "WHERE ID(c1) = $sourceConceptId AND ID(c2) = $targetConceptId " +
           "RETURN relationships(path)")
    List<ConceptConceptRelationship> findConceptPath(
            @Param("sourceConceptId") Long sourceConceptId, 
            @Param("targetConceptId") Long targetConceptId, 
            @Param("maxDepth") int maxDepth);
} 
package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.UserConceptRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Neo4j用户-概念关系仓库接口
 */
@Repository
public interface UserConceptRelationshipRepository extends Neo4jRepository<UserConceptRelationship, Long> {

    /**
     * 查找用户与概念之间的关系
     * 
     * @param userId 用户MySQL ID
     * @param conceptId 概念节点ID
     * @return 可选的用户-概念关系
     */
    @Query("MATCH (u:User)-[r:INTERESTED_IN]->(c:Concept) " +
           "WHERE u.mysql_id = $userId AND ID(c) = $conceptId " +
           "RETURN r")
    Optional<UserConceptRelationship> findRelationship(@Param("userId") String userId, @Param("conceptId") Long conceptId);
    
    /**
     * 查找用户的所有兴趣概念关系
     * 
     * @param userId 用户MySQL ID
     * @return 用户-概念关系列表
     */
    @Query("MATCH (u:User)-[r:INTERESTED_IN]->(c:Concept) " +
           "WHERE u.mysql_id = $userId " +
           "RETURN r " +
           "ORDER BY r.interestLevel DESC")
    List<UserConceptRelationship> findUserInterests(@Param("userId") String userId);
    
    /**
     * 查找用户的主要兴趣概念关系
     * 
     * @param userId 用户MySQL ID
     * @param limit 限制返回数量
     * @return 用户-概念关系列表
     */
    @Query("MATCH (u:User)-[r:INTERESTED_IN]->(c:Concept) " +
           "WHERE u.mysql_id = $userId " +
           "RETURN r " +
           "ORDER BY r.interestLevel DESC " +
           "LIMIT $limit")
    List<UserConceptRelationship> findTopUserInterests(@Param("userId") String userId, @Param("limit") int limit);
    
    /**
     * 查找对特定概念感兴趣的用户关系
     * 
     * @param conceptId 概念节点ID
     * @return 用户-概念关系列表
     */
    @Query("MATCH (u:User)-[r:INTERESTED_IN]->(c:Concept) " +
           "WHERE ID(c) = $conceptId " +
           "RETURN r " +
           "ORDER BY r.interestLevel DESC")
    List<UserConceptRelationship> findUsersInterestedInConcept(@Param("conceptId") Long conceptId);
    
    /**
     * 更新用户对概念的兴趣级别
     * 
     * @param userId 用户MySQL ID
     * @param conceptId 概念节点ID
     * @param interestLevel 兴趣级别
     * @return 是否更新成功
     */
    @Query("MATCH (u:User)-[r:INTERESTED_IN]->(c:Concept) " +
           "WHERE u.mysql_id = $userId AND ID(c) = $conceptId " +
           "SET r.interestLevel = $interestLevel " +
           "RETURN r")
    boolean updateInterestLevel(
            @Param("userId") String userId, 
            @Param("conceptId") Long conceptId, 
            @Param("interestLevel") Double interestLevel);
    
    /**
     * 增加用户与概念的交互次数
     * 
     * @param userId 用户MySQL ID
     * @param conceptId 概念节点ID
     * @return 是否更新成功
     */
    @Query("MATCH (u:User)-[r:INTERESTED_IN]->(c:Concept) " +
           "WHERE u.mysql_id = $userId AND ID(c) = $conceptId " +
           "SET r.interactionCount = r.interactionCount + 1 " +
           "RETURN r")
    boolean incrementInteractionCount(@Param("userId") String userId, @Param("conceptId") Long conceptId);
} 
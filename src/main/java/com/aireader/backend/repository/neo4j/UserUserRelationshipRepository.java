package com.aireader.backend.repository.neo4j;

import com.aireader.backend.model.neo4j.UserUserRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Neo4j用户-用户关系仓库接口
 */
@Repository
public interface UserUserRelationshipRepository extends Neo4jRepository<UserUserRelationship, Long> {

    /**
     * 查找用户之间的关注关系
     * 
     * @param sourceUserId 源用户MySQL ID
     * @param targetUserId 目标用户MySQL ID
     * @return 可选的用户-用户关系
     */
    @Query("MATCH (u1:User)-[r:FOLLOWS]->(u2:User) " +
           "WHERE u1.mysql_id = $sourceUserId AND u2.mysql_id = $targetUserId " +
           "RETURN r")
    Optional<UserUserRelationship> findRelationship(
            @Param("sourceUserId") String sourceUserId, 
            @Param("targetUserId") String targetUserId);
    
    /**
     * 查找用户的所有关注关系
     * 
     * @param userId 用户MySQL ID
     * @return 用户-用户关系列表
     */
    @Query("MATCH (u1:User)-[r:FOLLOWS]->(u2:User) " +
           "WHERE u1.mysql_id = $userId " +
           "RETURN r")
    List<UserUserRelationship> findFollowingRelationships(@Param("userId") String userId);
    
    /**
     * 查找用户的所有粉丝关系
     * 
     * @param userId 用户MySQL ID
     * @return 用户-用户关系列表
     */
    @Query("MATCH (u1:User)-[r:FOLLOWS]->(u2:User) " +
           "WHERE u2.mysql_id = $userId " +
           "RETURN r")
    List<UserUserRelationship> findFollowerRelationships(@Param("userId") String userId);
    
    /**
     * 查找用户的共同关注
     * 
     * @param userId1 用户1 MySQL ID
     * @param userId2 用户2 MySQL ID
     * @return 共同关注的用户-用户关系列表
     */
    @Query("MATCH (u1:User)-[r1:FOLLOWS]->(common:User)<-[r2:FOLLOWS]-(u2:User) " +
           "WHERE u1.mysql_id = $userId1 AND u2.mysql_id = $userId2 " +
           "RETURN r1")
    List<UserUserRelationship> findCommonFollowings(
            @Param("userId1") String userId1, 
            @Param("userId2") String userId2);
    
    /**
     * 查找用户的推荐关注（关注的人也关注的用户）
     * 
     * @param userId 用户MySQL ID
     * @param limit 限制返回数量
     * @return 推荐关注的用户-用户关系列表
     */
    @Query("MATCH (u:User {mysql_id: $userId})-[:FOLLOWS]->(:User)-[r:FOLLOWS]->(recommended:User) " +
           "WHERE NOT (u)-[:FOLLOWS]->(recommended) AND u.mysql_id <> recommended.mysql_id " +
           "RETURN r " +
           "ORDER BY count(*) DESC " +
           "LIMIT $limit")
    List<UserUserRelationship> findRecommendedFollowings(
            @Param("userId") String userId, 
            @Param("limit") int limit);
    
    /**
     * 删除用户之间的关注关系
     * 
     * @param sourceUserId 源用户MySQL ID
     * @param targetUserId 目标用户MySQL ID
     * @return 是否删除成功
     */
    @Query("MATCH (u1:User {mysql_id: $sourceUserId})-[r:FOLLOWS]->(u2:User {mysql_id: $targetUserId}) " +
           "DELETE r " +
           "RETURN count(r) > 0")
    boolean deleteRelationship(
            @Param("sourceUserId") String sourceUserId, 
            @Param("targetUserId") String targetUserId);
} 
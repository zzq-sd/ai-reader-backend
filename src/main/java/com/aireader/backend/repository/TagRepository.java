package com.aireader.backend.repository;

import com.aireader.backend.model.jpa.Tag;
import com.aireader.backend.model.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 标签仓库接口
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, String> {
    
    /**
     * 根据用户查找所有标签
     * 
     * @param user 用户对象
     * @return 标签列表
     */
    List<Tag> findByUser(User user);
    
    /**
     * 根据名称和用户查找标签
     * 
     * @param name 标签名称
     * @param user 用户对象
     * @return 标签对象
     */
    Optional<Tag> findByNameAndUser(String name, User user);
    
    /**
     * 根据用户查找最常用的标签
     * 
     * @param user  用户对象
     * @param limit 限制数量
     * @return 标签列表
     */
    @Query(value = "SELECT t.* FROM tags t " +
            "JOIN note_tags nt ON t.id = nt.tag_id " +
            "JOIN notes n ON nt.note_id = n.id " +
            "WHERE t.user_id = :userId " +
            "GROUP BY t.id " +
            "ORDER BY COUNT(nt.note_id) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Tag> findMostUsedTagsByUser(@Param("userId") String userId, @Param("limit") int limit);
    
    /**
     * 根据名称开头和用户查找标签（自动完成功能）
     * 
     * @param nameStartsWith 标签名称开头
     * @param user           用户对象
     * @return 标签列表
     */
    List<Tag> findByNameStartsWithIgnoreCaseAndUser(String nameStartsWith, User user);
    
    /**
     * 查找用户的所有标签（分页）
     * 
     * @param user 用户
     * @param pageable 分页参数
     * @return 分页标签列表
     */
    Page<Tag> findByUser(User user, Pageable pageable);
    
    /**
     * 根据名称模糊搜索用户标签
     * 
     * @param user 用户
     * @param nameKeyword 名称关键词
     * @return 标签列表
     */
    List<Tag> findByUserAndNameContainingIgnoreCase(User user, String nameKeyword);
    
    /**
     * 检查标签是否属于特定用户
     * 
     * @param tagId 标签ID
     * @param userId 用户ID
     * @return 标签Optional包装
     */
    Optional<Tag> findByIdAndUserId(String tagId, String userId);
} 
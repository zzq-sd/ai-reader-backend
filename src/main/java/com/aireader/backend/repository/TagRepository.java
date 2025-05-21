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
 * 标签资源库接口
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, String> {
    
    /**
     * 查找用户的所有标签
     * 
     * @param user 用户
     * @return 标签列表
     */
    List<Tag> findByUser(User user);
    
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
     * 检查用户是否已有特定名称的标签
     * 
     * @param user 用户
     * @param name 标签名称
     * @return 标签Optional包装
     */
    Optional<Tag> findByUserAndName(User user, String name);
    
    /**
     * 查询使用最多的标签
     * 
     * @param user 用户
     * @param limit 限制条数
     * @return 标签列表
     */
    @Query("SELECT t FROM Tag t JOIN t.notes n WHERE t.user = :user GROUP BY t ORDER BY COUNT(n) DESC")
    List<Tag> findMostUsedTags(@Param("user") User user, Pageable pageable);
    
    /**
     * 检查标签是否属于特定用户
     * 
     * @param tagId 标签ID
     * @param userId 用户ID
     * @return 标签Optional包装
     */
    Optional<Tag> findByIdAndUserId(String tagId, String userId);
} 
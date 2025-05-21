package com.aireader.backend.repository;

import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.model.jpa.UserArticleInteraction;
import com.aireader.backend.model.jpa.UserArticleInteraction.UserArticleInteractionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户文章交互资源库接口
 */
@Repository
public interface UserArticleInteractionRepository extends JpaRepository<UserArticleInteraction, UserArticleInteractionId> {
    
    /**
     * 查找用户与文章的交互记录
     * 
     * @param user 用户
     * @param articleMetadata 文章元数据
     * @return 交互记录Optional包装
     */
    Optional<UserArticleInteraction> findByUserAndArticleMetadata(User user, ArticleMetadata articleMetadata);
    
    /**
     * 查找用户收藏的所有文章
     * 
     * @param user 用户
     * @param pageable 分页参数
     * @return 分页交互记录列表
     */
    Page<UserArticleInteraction> findByUserAndFavoriteTrue(User user, Pageable pageable);
    
    /**
     * 查找用户阅读过的所有文章
     * 
     * @param user 用户
     * @param pageable 分页参数
     * @return 分页交互记录列表
     */
    Page<UserArticleInteraction> findByUserAndReadTrue(User user, Pageable pageable);
    
    /**
     * 查找用户最近阅读的文章
     * 
     * @param user 用户
     * @param limit 限制条数
     * @return 交互记录列表
     */
    @Query("SELECT uai FROM UserArticleInteraction uai WHERE uai.user = :user AND uai.read = true ORDER BY uai.lastReadAt DESC")
    List<UserArticleInteraction> findRecentlyReadArticles(@Param("user") User user, Pageable pageable);
    
    /**
     * 查询特定时间段内用户收藏的文章
     * 
     * @param user 用户
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param pageable 分页参数
     * @return 分页交互记录列表
     */
    Page<UserArticleInteraction> findByUserAndFavoriteTrueAndFavoritedAtBetween(
            User user, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * 查询用户在特定时间之后阅读的文章
     * 
     * @param user 用户
     * @param date 时间下限
     * @param pageable 分页参数
     * @return 分页交互记录列表
     */
    Page<UserArticleInteraction> findByUserAndReadTrueAndLastReadAtAfter(
            User user, LocalDateTime date, Pageable pageable);
    
    /**
     * 统计用户收藏的文章数量
     * 
     * @param user 用户
     * @return 收藏文章数量
     */
    long countByUserAndFavoriteTrue(User user);
    
    /**
     * 统计用户阅读过的文章数量
     * 
     * @param user 用户
     * @return 阅读过的文章数量
     */
    long countByUserAndReadTrue(User user);
} 
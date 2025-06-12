package com.aireader.backend.repository;

import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.model.jpa.UserArticleInteraction;
import com.aireader.backend.model.jpa.UserArticleInteraction.UserArticleInteractionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户文章交互仓库接口
 */
@Repository
public interface UserArticleInteractionRepository extends JpaRepository<UserArticleInteraction, UserArticleInteractionId> {
    
    /**
     * 根据用户和文章查找交互记录
     * 
     * @param user            用户对象
     * @param articleMetadata 文章元数据对象
     * @return 交互记录对象
     */
    Optional<UserArticleInteraction> findByUserAndArticleMetadata(User user, ArticleMetadata articleMetadata);
    
    /**
     * 根据用户查找所有收藏的文章（分页）
     * 
     * @param user     用户对象
     * @param pageable 分页参数
     * @return 交互记录分页对象
     */
    Page<UserArticleInteraction> findByUserAndIsFavoriteTrue(User user, Pageable pageable);
    
    /**
     * 根据用户查找所有已读的文章（分页）
     * 
     * @param user     用户对象
     * @param pageable 分页参数
     * @return 交互记录分页对象
     */
    Page<UserArticleInteraction> findByUserAndIsReadTrue(User user, Pageable pageable);
    
    /**
     * 查找用户最近阅读的文章
     * 
     * @param user     用户对象
     * @param pageable 分页参数
     * @return 交互记录列表
     */
    @Query("SELECT uai FROM UserArticleInteraction uai WHERE uai.user = :user AND uai.isRead = true ORDER BY uai.lastReadAt DESC")
    List<UserArticleInteraction> findRecentlyReadArticles(@Param("user") User user, Pageable pageable);
    
    /**
     * 查询特定时间段内用户收藏的文章
     * 
     * @param user      用户对象
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @param pageable  分页参数
     * @return 交互记录分页对象
     */
    Page<UserArticleInteraction> findByUserAndIsFavoriteTrueAndFavoritedAtBetween(
            User user, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * 查询用户在特定时间之后阅读的文章
     * 
     * @param user     用户对象
     * @param date     时间下限
     * @param pageable 分页参数
     * @return 交互记录分页对象
     */
    Page<UserArticleInteraction> findByUserAndIsReadTrueAndLastReadAtAfter(
            User user, LocalDateTime date, Pageable pageable);
    
    /**
     * 统计用户收藏的文章数量
     * 
     * @param user 用户对象
     * @return 收藏文章数量
     */
    long countByUserAndIsFavoriteTrue(User user);
    
    /**
     * 统计用户阅读过的文章数量
     * 
     * @param user 用户对象
     * @return 阅读过的文章数量
     */
    long countByUserAndIsReadTrue(User user);
    
    /**
     * 根据文章元数据ID删除所有相关的用户交互记录
     * 
     * @param articleMetadataId 文章元数据ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserArticleInteraction uai WHERE uai.articleMetadata.id = :articleMetadataId")
    void deleteByArticleMetadataId(@Param("articleMetadataId") String articleMetadataId);
    
    /**
     * 根据文章元数据ID列表批量删除所有相关的用户交互记录
     * 
     * @param articleMetadataIds 文章元数据ID列表
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserArticleInteraction uai WHERE uai.articleMetadata.id IN :articleMetadataIds")
    void deleteByArticleMetadataIdIn(@Param("articleMetadataIds") List<String> articleMetadataIds);
    
    /**
     * 根据文章元数据对象删除所有相关的用户交互记录
     * 
     * @param articleMetadata 文章元数据对象
     */
    void deleteByArticleMetadata(ArticleMetadata articleMetadata);
    
    /**
     * 根据文章元数据对象列表批量删除所有相关的用户交互记录
     * 
     * @param articleMetadataList 文章元数据对象列表
     */
    void deleteByArticleMetadataIn(List<ArticleMetadata> articleMetadataList);
} 
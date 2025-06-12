package com.aireader.backend.repository;

import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.RssSource;
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
 * 文章元数据仓库接口
 */
@Repository
public interface ArticleMetadataRepository extends JpaRepository<ArticleMetadata, String> {
    
    /**
     * 根据RSS源查找文章元数据（分页）
     * 
     * @param rssSource RSS源对象
     * @param pageable  分页参数
     * @return 文章元数据分页对象
     */
    Page<ArticleMetadata> findByRssSource(RssSource rssSource, Pageable pageable);
    
    /**
     * 根据原始链接查找文章元数据
     * 
     * @param linkToOriginal 原始链接
     * @return 文章元数据对象
     */
    Optional<ArticleMetadata> findByLinkToOriginal(String linkToOriginal);
    
    /**
     * 根据原始链接和RSS源查找文章元数据
     * 
     * @param linkToOriginal 原始链接
     * @param rssSource      RSS源对象
     * @return 文章元数据对象
     */
    Optional<ArticleMetadata> findByLinkToOriginalAndRssSource(String linkToOriginal, RssSource rssSource);
    
    /**
     * 根据MongoDB内容ID查找文章元数据
     * 
     * @param mongodbContentId MongoDB内容ID
     * @return 文章元数据对象
     */
    Optional<ArticleMetadata> findByMongodbContentId(String mongodbContentId);
    
    /**
     * 查询需要AI处理的文章元数据
     * 
     * @param status   AI处理状态
     * @param pageable 分页参数
     * @return 文章元数据分页对象
     */
    Page<ArticleMetadata> findByAiProcessingStatus(String status, Pageable pageable);
    
    /**
     * 全文搜索文章元数据
     * 
     * @param query    搜索关键词
     * @param pageable 分页参数
     * @return 文章元数据分页对象
     */
    @Query("SELECT a FROM ArticleMetadata a WHERE " +
            "LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.summaryText) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ArticleMetadata> searchByKeyword(@Param("query") String query, Pageable pageable);
    
    /**
     * 获取指定RSS源的最新文章元数据（限制数量）
     * 
     * @param rssSource RSS源对象
     * @param limit     限制数量
     * @return 文章元数据列表
     */
    @Query("SELECT a FROM ArticleMetadata a WHERE a.rssSource = :rssSource " +
            "ORDER BY a.publicationDate DESC")
    List<ArticleMetadata> findLatestByRssSource(@Param("rssSource") RssSource rssSource, Pageable limit);
    
    /**
     * 查询用户收藏的且带有指定标签的文章
     * 
     * @param userId 用户ID
     * @param tagId  标签ID
     * @param pageable 分页参数
     * @return 文章元数据分页对象
     */
    @Query("SELECT a FROM ArticleMetadata a " +
            "JOIN UserArticleInteraction uai ON uai.articleMetadata = a AND uai.isFavorite = true " +
            "JOIN ArticleTag at ON at.articleMetadata = a " +
            "JOIN Tag t ON at.tag = t " +
            "WHERE uai.user.id = :userId AND t.id = :tagId")
    Page<ArticleMetadata> findFavoriteArticlesByUserAndTag(
            @Param("userId") String userId, 
            @Param("tagId") String tagId, 
            Pageable pageable);
    
    /**
     * 搜索用户收藏的文章
     * 
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 文章元数据分页对象
     */
    @Query("SELECT a FROM ArticleMetadata a " +
            "JOIN UserArticleInteraction uai ON uai.articleMetadata = a AND uai.isFavorite = true " +
            "WHERE uai.user.id = :userId AND " +
            "(LOWER(a.title) LIKE :keyword OR " +
            "LOWER(a.summaryText) LIKE :keyword OR " +
            "LOWER(a.author) LIKE :keyword)")
    Page<ArticleMetadata> searchFavoriteArticlesByUser(
            @Param("userId") String userId, 
            @Param("keyword") String keyword, 
            Pageable pageable);
    
    /**
     * 根据RSS源ID查找文章元数据（分页）
     * 
     * @param rssSourceId RSS源ID
     * @param pageable 分页参数
     * @return 文章元数据分页对象
     */
    @Query("SELECT a FROM ArticleMetadata a WHERE a.rssSource.id = :rssSourceId")
    Page<ArticleMetadata> findByRssSourceId(@Param("rssSourceId") String rssSourceId, Pageable pageable);
    
    /**
     * 根据多个RSS源ID查找文章元数据（分页）
     * 
     * @param rssSourceIds RSS源ID列表
     * @param pageable 分页参数
     * @return 文章元数据分页对象
     */
    @Query("SELECT a FROM ArticleMetadata a WHERE a.rssSource.id IN :rssSourceIds")
    Page<ArticleMetadata> findByRssSourceIdIn(@Param("rssSourceIds") List<String> rssSourceIds, Pageable pageable);
    
    /**
     * 根据RSS源ID查找所有文章元数据（不分页）
     * 
     * @param rssSourceId RSS源ID
     * @return 文章元数据列表
     */
    @Query("SELECT a FROM ArticleMetadata a WHERE a.rssSource.id = :rssSourceId")
    List<ArticleMetadata> findByRssSourceId(@Param("rssSourceId") String rssSourceId);
    
    /**
     * 根据用户ID查找所有RSS源的文章（通过RSS源的用户关联）
     * 
     * @param userId 用户ID
     * @return 文章元数据列表
     */
    @Query("SELECT a FROM ArticleMetadata a WHERE a.rssSource.user.id = :userId")
    List<ArticleMetadata> findByRssSourceUserId(@Param("userId") String userId);
    
    /**
     * 统计用户订阅的RSS源的文章总数
     * 
     * @param userId 用户ID
     * @return 文章总数
     */
    @Query("SELECT COUNT(a) FROM ArticleMetadata a WHERE a.rssSource.user.id = :userId")
    long countByRssSourceUserId(@Param("userId") String userId);
} 
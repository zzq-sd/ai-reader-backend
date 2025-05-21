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
 * 文章元数据数据访问接口
 */
@Repository
public interface ArticleMetadataRepository extends JpaRepository<ArticleMetadata, String> {

    /**
     * 查找特定RSS源的所有文章
     *
     * @param rssSource RSS源
     * @param pageable 分页参数
     * @return 文章列表分页结果
     */
    Page<ArticleMetadata> findByRssSource(RssSource rssSource, Pageable pageable);

    /**
     * 通过MongoDB ID查找文章元数据
     *
     * @param mongodbContentId MongoDB中的文章内容ID
     * @return 文章元数据
     */
    Optional<ArticleMetadata> findByMongodbContentId(String mongodbContentId);

    /**
     * 通过GUID查找特定RSS源的文章
     *
     * @param guid 文章的GUID
     * @param rssSource RSS源
     * @return 文章元数据
     */
    Optional<ArticleMetadata> findByGuidAndRssSource(String guid, RssSource rssSource);

    /**
     * 通过原始URL查找特定RSS源的文章
     *
     * @param originalUrl 文章的原始URL
     * @param rssSource RSS源
     * @return 文章元数据
     */
    Optional<ArticleMetadata> findByOriginalUrlAndRssSource(String originalUrl, RssSource rssSource);

    /**
     * 查找特定时间范围内发布的文章
     *
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param pageable 分页参数
     * @return 文章列表分页结果
     */
    Page<ArticleMetadata> findByPublicationDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * 查找待处理的文章列表
     *
     * @param status AI处理状态
     * @param pageable 分页参数
     * @return 文章列表分页结果
     */
    Page<ArticleMetadata> findByAiProcessingStatus(ArticleMetadata.AiProcessingStatus status, Pageable pageable);

    /**
     * 搜索文章
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 文章列表分页结果
     */
    @Query("SELECT a FROM ArticleMetadata a WHERE a.title LIKE %:keyword% OR a.summary LIKE %:keyword%")
    Page<ArticleMetadata> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 查询用户收藏的文章列表
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 文章列表分页结果
     */
    @Query("SELECT a FROM ArticleMetadata a JOIN UserArticleFavorite f ON f.article.id = a.id WHERE f.user.id = :userId")
    Page<ArticleMetadata> findFavoritesByUserId(@Param("userId") String userId, Pageable pageable);
} 
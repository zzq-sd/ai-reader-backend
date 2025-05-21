package com.aireader.backend.repository.mongo;

import com.aireader.backend.model.mongo.ArticleContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 文章内容MongoDB数据访问接口
 */
@Repository
public interface ArticleContentRepository extends MongoRepository<ArticleContent, String> {

    /**
     * 通过MySQL元数据ID查找文章内容
     *
     * @param mysqlMetadataId MySQL文章元数据ID
     * @return 文章内容
     */
    Optional<ArticleContent> findByMysqlMetadataId(String mysqlMetadataId);

    /**
     * 通过标题全文搜索文章
     *
     * @param title 标题关键词
     * @param pageable 分页参数
     * @return 文章列表分页结果
     */
    @Query("{'title': {$regex: ?0, $options: 'i'}}")
    Page<ArticleContent> findByTitleContaining(String title, Pageable pageable);

    /**
     * 通过正文全文搜索文章
     *
     * @param content 内容关键词
     * @param pageable 分页参数
     * @return 文章列表分页结果
     */
    @Query("{'plainTextContent': {$regex: ?0, $options: 'i'}}")
    Page<ArticleContent> findByPlainTextContentContaining(String content, Pageable pageable);

    /**
     * 通过发布日期范围查找文章
     *
     * @param start 开始时间
     * @param end 结束时间
     * @param pageable 分页参数
     * @return 文章列表分页结果
     */
    Page<ArticleContent> findByPublicationDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 查找特定语言的文章
     *
     * @param language 语言
     * @param pageable 分页参数
     * @return 文章列表分页结果
     */
    Page<ArticleContent> findByProcessingInfoLanguage(String language, Pageable pageable);

    /**
     * 查找包含特定关键词的文章
     *
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 文章列表分页结果
     */
    @Query("{'processingInfo.extractedKeywords': ?0}")
    Page<ArticleContent> findByKeyword(String keyword, Pageable pageable);

    /**
     * 查找包含特定实体的文章
     *
     * @param entity 实体
     * @param pageable 分页参数
     * @return 文章列表分页结果
     */
    @Query("{'processingInfo.extractedEntities': ?0}")
    Page<ArticleContent> findByEntity(String entity, Pageable pageable);
} 
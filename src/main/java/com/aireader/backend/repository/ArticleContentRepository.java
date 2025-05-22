package com.aireader.backend.repository;

import com.aireader.backend.model.mongo.ArticleContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文章内容MongoDB仓库接口
 */
@Repository
public interface ArticleContentRepository extends MongoRepository<ArticleContent, String> {
    
    /**
     * 根据MySQL元数据ID查找文章内容
     * 
     * @param mysqlMetadataId MySQL元数据ID（article_metadata表的id字段）
     * @return 文章内容对象
     */
    Optional<ArticleContent> findByMysqlMetadataId(String mysqlMetadataId);
    
    /**
     * 根据原始URL查找文章内容
     * 
     * @param originalUrl 原始URL
     * @return 文章内容对象
     */
    Optional<ArticleContent> findByOriginalUrl(String originalUrl);
    
    /**
     * 根据内容哈希值查找文章内容
     * 
     * @param contentHash 内容哈希值
     * @return 文章内容对象
     */
    Optional<ArticleContent> findByContentHash(String contentHash);
    
    /**
     * 全文搜索文章内容
     * 
     * @param query 搜索关键词
     * @return 文章内容列表
     */
    @Query("{ $text: { $search: ?0 } }")
    List<ArticleContent> fullTextSearch(String query);
} 
package com.aireader.backend.service;

import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.RssSource;

import java.util.List;
// import java.util.UUID; // To be removed

/**
 * 文章抓取服务接口
 * 负责从RSS源抓取文章并存储
 */
public interface ArticleFetchService {
    
    /**
     * 抓取单个RSS源的文章
     * 
     * @param rssSource RSS源
     * @return 抓取的文章列表
     */
    List<ArticleMetadata> fetchArticlesFromSource(RssSource rssSource);
    
    /**
     * 抓取所有RSS源的文章
     * 定时任务调用
     * 
     * @return 抓取的文章总数
     */
    int fetchAllSources();
    
    /**
     * 解析文章内容
     * 从原始HTML中提取正文、图片等
     * 
     * @param articleId 文章ID
     * @return 是否解析成功
     */
    boolean parseArticleContent(String articleId);
    
    /**
     * 获取文章全文内容
     * 
     * @param articleId 文章ID
     * @return 文章全文内容
     */
    String getArticleFullContent(String articleId);
    
    /**
     * 检查文章是否已存在
     * 
     * @param url 文章URL
     * @return 是否已存在
     */
    boolean isArticleExists(String url);
    
    /**
     * 将文章加入处理队列
     * 
     * @param articleId 文章ID
     */
    void queueArticleForProcessing(String articleId);
    
    /**
     * 通过RSS源ID抓取文章
     * 
     * @param sourceId RSS源ID
     * @return 抓取的文章数量
     */
    int fetchArticlesFromSourceById(String sourceId);
    
    /**
     * 根据ID获取文章详情
     * 
     * @param articleId 文章ID
     * @return 文章DTO
     */
    ArticleDTO getArticleById(String articleId);
} 
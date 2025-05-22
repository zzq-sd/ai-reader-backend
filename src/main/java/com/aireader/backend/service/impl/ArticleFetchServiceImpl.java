package com.aireader.backend.service.impl;

import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.RssSource;
import com.aireader.backend.model.mongo.ArticleContent;
import com.aireader.backend.repository.ArticleMetadataRepository;
import com.aireader.backend.repository.RssSourceRepository;
import com.aireader.backend.repository.mongo.ArticleContentRepository;
import com.aireader.backend.service.ArticleFetchService;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章抓取服务实现类
 */
@Service
public class ArticleFetchServiceImpl implements ArticleFetchService {
    
    private static final Logger logger = LoggerFactory.getLogger(ArticleFetchServiceImpl.class);
    
    @Autowired
    private RssSourceRepository rssSourceRepository;
    
    @Autowired
    private ArticleMetadataRepository articleMetadataRepository;
    
    @Autowired
    private ArticleContentRepository articleContentRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Value("${rss.fetch.timeout}")
    private int fetchTimeout;
    
    @Value("${spring.rabbitmq.template.exchange}")
    private String exchange;
    
    @Value("${spring.rabbitmq.template.routing-key.article-processing}")
    private String articleProcessingRoutingKey;
    
    /**
     * 抓取单个RSS源的文章
     * 
     * @param rssSource RSS源
     * @return 抓取的文章列表
     */
    @Override
    @Transactional
    public List<ArticleMetadata> fetchArticlesFromSource(RssSource rssSource) {
        List<ArticleMetadata> fetchedArticles = new ArrayList<>();
        
        try {
            // 获取RSS Feed
            URL feedUrl = new URL(rssSource.getUrl());
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));
            
            // 处理每个条目
            for (SyndEntry entry : feed.getEntries()) {
                // 检查文章是否已存在
                String articleUrl = entry.getLink();
                if (isArticleExists(articleUrl)) {
                    continue;
                }
                
                // 创建文章元数据
                ArticleMetadata article = new ArticleMetadata();
                article.setTitle(entry.getTitle());
                article.setUrl(articleUrl);
                article.setAuthor(entry.getAuthor());
                article.setRssSource(rssSource);
                
                // 设置发布时间
                if (entry.getPublishedDate() != null) {
                    article.setPublishedAt(
                            entry.getPublishedDate().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime());
                } else {
                    article.setPublishedAt(LocalDateTime.now());
                }
                
                // 设置摘要
                if (entry.getDescription() != null) {
                    article.setSummary(entry.getDescription().getValue());
                } else if (!entry.getContents().isEmpty()) {
                    SyndContent content = entry.getContents().get(0);
                    article.setSummary(content.getValue());
                }
                
                // 保存文章元数据
                ArticleMetadata savedArticle = articleMetadataRepository.save(article);
                fetchedArticles.add(savedArticle);
                
                // 抓取文章全文内容
                fetchAndSaveArticleContent(savedArticle);
                
                // 将文章加入处理队列
                queueArticleForProcessing(savedArticle.getId().toString());
            }
            
            // 更新RSS源状态
            rssSource.setLastFetchedAt(LocalDateTime.now());
            rssSource.setFetchStatus("SUCCESS");
            rssSource.setErrorMessage(null);
            rssSourceRepository.save(rssSource);
            
        } catch (Exception e) {
            logger.error("抓取RSS源失败: {}", rssSource.getUrl(), e);
            
            // 更新RSS源状态
            rssSource.setFetchStatus("ERROR");
            rssSource.setErrorMessage(e.getMessage());
            rssSourceRepository.save(rssSource);
        }
        
        return fetchedArticles;
    }
    
    /**
     * 抓取所有RSS源的文章
     * 定时任务调用
     * 
     * @return 抓取的文章总数
     */
    @Override
    @Scheduled(cron = "${rss.fetch.cron}")
    public int fetchAllSources() {
        logger.info("开始定时抓取RSS源...");
        
        // 获取所有需要更新的RSS源
        LocalDateTime threshold = LocalDateTime.now().minusHours(6);
        List<RssSource> sourcesToFetch = rssSourceRepository
                .findByLastFetchedAtBeforeOrLastFetchedAtIsNull(threshold);
        
        logger.info("找到{}个需要更新的RSS源", sourcesToFetch.size());
        
        int totalArticles = 0;
        for (RssSource source : sourcesToFetch) {
            try {
                List<ArticleMetadata> articles = fetchArticlesFromSource(source);
                totalArticles += articles.size();
                logger.info("从{}抓取了{}篇文章", source.getName(), articles.size());
            } catch (Exception e) {
                logger.error("抓取RSS源失败: {}", source.getUrl(), e);
            }
        }
        
        logger.info("定时抓取RSS源完成，共抓取{}篇文章", totalArticles);
        return totalArticles;
    }
    
    /**
     * 解析文章内容
     * 从原始HTML中提取正文、图片等
     * 
     * @param articleId 文章ID
     * @return 是否解析成功
     */
    @Override
    public boolean parseArticleContent(String articleId) {
        try {
            // 获取文章元数据
            Optional<ArticleMetadata> optionalArticle = articleMetadataRepository
                    .findById(UUID.fromString(articleId));
            
            if (!optionalArticle.isPresent()) {
                logger.error("文章不存在: {}", articleId);
                return false;
            }
            
            ArticleMetadata article = optionalArticle.get();
            
            // 获取文章内容
            Optional<ArticleContent> optionalContent = articleContentRepository
                    .findByArticleId(articleId);
            
            if (!optionalContent.isPresent()) {
                logger.error("文章内容不存在: {}", articleId);
                return false;
            }
            
            ArticleContent content = optionalContent.get();
            
            // TODO: 使用第三方库（如Jsoup）解析HTML，提取正文和图片
            // 这里简单实现，实际项目中可能需要更复杂的解析逻辑
            
            // 提取第一张图片作为封面
            String htmlContent = content.getHtmlContent();
            String imageUrl = extractFirstImage(htmlContent);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                article.setImageUrl(imageUrl);
                articleMetadataRepository.save(article);
            }
            
            return true;
        } catch (Exception e) {
            logger.error("解析文章内容失败: {}", articleId, e);
            return false;
        }
    }
    
    /**
     * 获取文章全文内容
     * 
     * @param articleId 文章ID
     * @return 文章全文内容
     */
    @Override
    public String getArticleFullContent(String articleId) {
        Optional<ArticleContent> optionalContent = articleContentRepository
                .findByArticleId(articleId);
        
        return optionalContent.map(ArticleContent::getHtmlContent)
                .orElse(null);
    }
    
    /**
     * 检查文章是否已存在
     * 
     * @param url 文章URL
     * @return 是否已存在
     */
    @Override
    public boolean isArticleExists(String url) {
        return articleMetadataRepository.existsByUrl(url);
    }
    
    /**
     * 将文章加入处理队列
     * 
     * @param articleId 文章ID
     */
    @Override
    public void queueArticleForProcessing(String articleId) {
        try {
            rabbitTemplate.convertAndSend(exchange, articleProcessingRoutingKey, articleId);
            logger.info("文章已加入处理队列: {}", articleId);
        } catch (Exception e) {
            logger.error("将文章加入处理队列失败: {}", articleId, e);
        }
    }
    
    /**
     * 抓取并保存文章全文内容
     * 
     * @param article 文章元数据
     */
    private void fetchAndSaveArticleContent(ArticleMetadata article) {
        try {
            String url = article.getUrl();
            String htmlContent = fetchHtmlContent(url);
            
            if (htmlContent != null && !htmlContent.isEmpty()) {
                ArticleContent content = new ArticleContent();
                content.setArticleId(article.getId().toString());
                content.setHtmlContent(htmlContent);
                content.setCreatedAt(LocalDateTime.now());
                
                articleContentRepository.save(content);
                logger.info("保存文章内容成功: {}", article.getTitle());
            }
        } catch (Exception e) {
            logger.error("抓取文章内容失败: {}", article.getUrl(), e);
        }
    }
    
    /**
     * 抓取HTML内容
     * 
     * @param url 文章URL
     * @return HTML内容
     */
    private String fetchHtmlContent(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            
            // 设置请求配置
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(fetchTimeout)
                    .setSocketTimeout(fetchTimeout)
                    .build();
            httpGet.setConfig(requestConfig);
            
            // 设置User-Agent
            httpGet.setHeader("User-Agent", 
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            
            // 执行请求
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity());
                }
            }
        } catch (Exception e) {
            logger.error("抓取HTML内容失败: {}", url, e);
        }
        
        return null;
    }
    
    /**
     * 从HTML中提取第一张图片
     * 
     * @param html HTML内容
     * @return 图片URL
     */
    private String extractFirstImage(String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }
        
        // 简单实现，使用正则表达式提取第一个img标签的src属性
        // 实际项目中可能需要更复杂的解析逻辑
        int imgIndex = html.indexOf("<img");
        if (imgIndex == -1) {
            return null;
        }
        
        int srcIndex = html.indexOf("src=", imgIndex);
        if (srcIndex == -1) {
            return null;
        }
        
        int startQuote = html.indexOf("\"", srcIndex);
        if (startQuote == -1) {
            return null;
        }
        
        int endQuote = html.indexOf("\"", startQuote + 1);
        if (endQuote == -1) {
            return null;
        }
        
        return html.substring(startQuote + 1, endQuote);
    }
}
package com.aireader.backend.service.impl;

import com.aireader.backend.dto.ArticleDTO;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import com.aireader.backend.util.TextEncodingUtil;

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
                article.setLinkToOriginal(articleUrl);
                article.setAuthor(entry.getAuthor());
                article.setRssSource(rssSource);
                
                // 设置发布时间
                if (entry.getPublishedDate() != null) {
                    article.setPublicationDate(
                            entry.getPublishedDate().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime());
                } else {
                    article.setPublicationDate(LocalDateTime.now());
                }
                
                // 设置摘要
                if (entry.getDescription() != null) {
                    article.setSummaryText(entry.getDescription().getValue());
                } else if (!entry.getContents().isEmpty()) {
                    SyndContent content = entry.getContents().get(0);
                    article.setSummaryText(content.getValue());
                }
                
                // 保存文章元数据
                ArticleMetadata savedArticle = articleMetadataRepository.save(article);
                fetchedArticles.add(savedArticle);
                
                // 抓取文章全文内容
                fetchAndSaveArticleContent(savedArticle);
                
                // 将文章加入处理队列
                queueArticleForProcessing(savedArticle.getId());
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
            Optional<ArticleMetadata> optionalArticle = articleMetadataRepository.findById(articleId);
            if (!optionalArticle.isPresent()) {
                logger.error("文章元数据不存在: {}", articleId);
                return false;
            }
            // ArticleMetadata article = optionalArticle.get(); // Not directly used here further

            Optional<ArticleContent> optionalContent = articleContentRepository.findByMysqlMetadataId(articleId);
            if (!optionalContent.isPresent()) {
                logger.error("文章内容不存在: {}", articleId);
                return false;
            }
            ArticleContent content = optionalContent.get();
            String htmlContent = content.getFullHtmlContent();

            if (!StringUtils.hasText(htmlContent)) {
                logger.warn("文章 {} 的HTML内容为空，无法解析。", articleId);
                content.setPlainTextContent(""); // 设置为空字符串
                articleContentRepository.save(content);
                return true; // 认为是处理完成，即使是空内容
            }

            Document doc = Jsoup.parse(htmlContent);

            // 尝试提取主要内容
            Element body = doc.body();
            String plainText;
            if (body != null) {
                 // 移除脚本、样式等非文本内容，然后获取文本
                body.select("script, style, link, noscript, iframe, header, footer, nav, aside, .advertisement, .ad, .banner, .sidebar").remove();
                // 简单地清理HTML标签后获取文本。更高级的提取器可能会更好。
                plainText = Jsoup.clean(body.html(), Safelist.none()); // 先移除所有HTML标签
                plainText = plainText.replaceAll("\n*\n", "\n"); // 替换多个空行为一个
            } else {
                plainText = ""; // 如果没有body，则纯文本为空
            }

            content.setPlainTextContent(plainText.trim());
            logger.info("为文章 {} 提取并设置了纯文本内容，长度: {}", articleId, plainText.length());

            // 提取第一张图片作为封面图 (如果需要，可以放入 ArticleMetadata)
            // String coverImageUrl = extractFirstImage(doc); // Pass Document to avoid re-parsing
            // if (coverImageUrl != null) {
            //     article.setCoverImageUrl(coverImageUrl); // 假设 ArticleMetadata 有这个字段
            //     articleMetadataRepository.save(article);
            // }

            articleContentRepository.save(content);
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
                .findByMysqlMetadataId(articleId);
        
        return optionalContent.map(content -> content.getFullHtmlContent())
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
        return articleMetadataRepository.findByLinkToOriginal(url).isPresent();
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
            logger.info("开始抓取文章内容: {} - {}", article.getId(), article.getLinkToOriginal());
            
            // 检查是否已存在内容
            if (articleContentRepository.existsByMysqlMetadataId(article.getId())) {
                logger.info("文章内容已存在，跳过抓取: {}", article.getId());
                return;
            }
            
            String htmlContent = fetchHtmlContent(article.getLinkToOriginal());
            if (htmlContent != null && !htmlContent.trim().isEmpty()) {
                // 使用Jsoup解析HTML并提取纯文本
                Document doc = Jsoup.parse(htmlContent);
                String plainTextContent = doc.text();
                
                // 提取图片URLs
                List<String> imageUrls = new ArrayList<>();
                Elements imgElements = doc.select("img[src]");
                for (Element img : imgElements) {
                    String src = img.attr("abs:src");
                    if (StringUtils.hasText(src) && (src.startsWith("http://") || src.startsWith("https://"))) {
                        imageUrls.add(src);
                    }
                }
                
                // 计算内容哈希
                String contentHash = DigestUtils.md5DigestAsHex(plainTextContent.getBytes());
                
                // 创建ArticleContent对象
                ArticleContent articleContent = ArticleContent.builder()
                        .mysqlMetadataId(article.getId())
                        .originalUrl(article.getLinkToOriginal())
                        .titleFromContent(article.getTitle())
                        .fullHtmlContent(htmlContent)
                        .plainTextContent(plainTextContent)
                        .extractedImagesUrls(imageUrls)
                        .contentHash(contentHash)
                        .author(article.getAuthor())
                        .publicationDate(article.getPublicationDate())
                        .fetchedAt(LocalDateTime.now())
                        .build();
                
                // 保存到MongoDB
                ArticleContent savedContent = articleContentRepository.save(articleContent);
                logger.info("成功保存文章内容到MongoDB: {} -> {}", article.getId(), savedContent.getId());
                
                // 更新MySQL中的mongodb_content_id字段（如果存在该字段）
                try {
                    article.setMongodbContentId(savedContent.getId());
                    articleMetadataRepository.save(article);
                    logger.info("已更新MySQL中的mongodb_content_id: {}", article.getId());
                } catch (Exception e) {
                    logger.warn("更新MySQL中的mongodb_content_id失败，但不影响主流程: {}", e.getMessage());
                }
                
            } else {
                logger.warn("未能获取到有效的HTML内容: {} - {}", article.getId(), article.getLinkToOriginal());
                
                // 即使没有获取到内容，也创建一个空的记录，避免重复尝试
                ArticleContent emptyContent = ArticleContent.builder()
                        .mysqlMetadataId(article.getId())
                        .originalUrl(article.getLinkToOriginal())
                        .titleFromContent(article.getTitle())
                        .fullHtmlContent("")
                        .plainTextContent("")
                        .author(article.getAuthor())
                        .publicationDate(article.getPublicationDate())
                        .fetchedAt(LocalDateTime.now())
                        .build();
                
                articleContentRepository.save(emptyContent);
                logger.info("已保存空内容记录，避免重复抓取: {}", article.getId());
            }
        } catch (Exception e) {
            logger.error("抓取或保存文章 {} 的内容时出错: {}", article.getId(), e.getMessage(), e);
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
     * 从HTML文档中提取第一张有效图片URL
     * @param doc Jsoup Document
     * @return 图片URL或null
     */
    private String extractFirstImage(Document doc) {
        if (doc == null || doc.body() == null) {
            return null;
        }
        Elements imgElements = doc.body().select("img[src]");
        for (Element img : imgElements) {
            String src = img.attr("abs:src"); // 获取绝对路径
            if (StringUtils.hasText(src) && (src.startsWith("http://") || src.startsWith("https://"))) {
                // 可选：可以添加更多检查，例如图片尺寸、是否是广告图片等
                if (src.length() > 2048) { // 避免URL过长
                    logger.warn("图片URL过长，已跳过: {}", src.substring(0, 100) + "...");
                    continue;
                }
                return src;
            }
        }
        return null;
    }
    
    /**
     * 通过RSS源ID抓取文章
     * 
     * @param sourceId RSS源ID
     * @return 抓取的文章数量
     */
    @Override
    @Transactional
    public int fetchArticlesFromSourceById(String sourceId) {
        logger.info("通过ID抓取RSS源文章, sourceId={}", sourceId);
        
        try {
            // 通过ID查找RSS源
            Optional<RssSource> optionalSource = rssSourceRepository.findById(sourceId);
            
            if (!optionalSource.isPresent()) {
                logger.error("RSS源不存在: {}", sourceId);
                return 0;
            }
            
            RssSource rssSource = optionalSource.get();
            
            // 调用已有方法抓取文章
            List<ArticleMetadata> fetchedArticles = fetchArticlesFromSource(rssSource);
            
            logger.info("通过ID抓取RSS源文章完成, sourceId={}, 抓取文章数量={}", 
                    sourceId, fetchedArticles.size());
            
            return fetchedArticles.size();
            
        } catch (Exception e) {
            logger.error("通过ID抓取RSS源文章失败: {}", sourceId, e);
            return 0;
        }
    }
    
    /**
     * 根据ID获取文章详情
     * 
     * @param articleId 文章ID
     * @return 文章DTO
     */
    @Override
    public ArticleDTO getArticleById(String articleId) {
        try {
            // 获取文章元数据
            Optional<ArticleMetadata> optionalMetadata = articleMetadataRepository.findById(articleId);
            if (!optionalMetadata.isPresent()) {
                logger.warn("文章元数据不存在: {}", articleId);
                return null;
            }
            
            ArticleMetadata metadata = optionalMetadata.get();
            
            // 获取文章内容
            Optional<ArticleContent> optionalContent = articleContentRepository.findByMysqlMetadataId(articleId);
            ArticleContent content = optionalContent.orElse(null);
            
            // 如果存在内容，进行字符编码处理
            if (content != null) {
                // 处理标题
                if (content.getTitleFromContent() != null) {
                    content.setTitleFromContent(TextEncodingUtil.processText(content.getTitleFromContent()));
                }
                
                // 处理HTML内容
                if (content.getFullHtmlContent() != null) {
                    content.setFullHtmlContent(TextEncodingUtil.processText(content.getFullHtmlContent()));
                }
                
                // 处理纯文本内容
                if (content.getPlainTextContent() != null) {
                    content.setPlainTextContent(TextEncodingUtil.processText(content.getPlainTextContent()));
                }
                
                // 处理作者信息
                if (content.getAuthor() != null) {
                    content.setAuthor(TextEncodingUtil.processText(content.getAuthor()));
                }
                
                logger.debug("已对文章内容进行字符编码处理: {}", articleId);
            }
            
            // 对元数据也进行字符编码处理
            if (metadata.getTitle() != null) {
                metadata.setTitle(TextEncodingUtil.processText(metadata.getTitle()));
            }
            if (metadata.getAuthor() != null) {
                metadata.setAuthor(TextEncodingUtil.processText(metadata.getAuthor()));
            }
            if (metadata.getSummaryText() != null) {
                metadata.setSummaryText(TextEncodingUtil.processText(metadata.getSummaryText()));
            }
            
            // 转换为DTO
            return ArticleDTO.fromEntity(metadata, content);
            
        } catch (Exception e) {
            logger.error("获取文章详情失败: {}", articleId, e);
            return null;
        }
    }
}
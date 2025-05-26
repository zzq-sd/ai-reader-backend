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
import java.net.URLConnection;
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
            logger.info("开始抓取RSS源: {}, URL: {}, 是否RSSHub: {}", 
                rssSource.getName(), rssSource.getUrl(), rssSource.getIsRsshub());
            
            // 获取RSS Feed
            URL feedUrl = new URL(rssSource.getUrl());

            // 对于RSSHub源，增加超时时间
            int timeout = rssSource.getIsRsshub() != null && rssSource.getIsRsshub() ? fetchTimeout * 2 : fetchTimeout;
            
            // 设置连接
            URLConnection connection = feedUrl.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 AI-Reader-System");
            
            SyndFeedInput input = new SyndFeedInput();
            
            // 使用更安全的编码处理方式
            try (XmlReader xmlReader = new XmlReader(connection.getInputStream(), "UTF-8")) {
                SyndFeed feed = input.build(xmlReader);
                
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
                    
                    // 优先从RSS中提取完整内容，如果失败再抓取原始URL
                    String rssFullContent = extractContentEncoded(entry);
                    if (rssFullContent != null && !rssFullContent.trim().isEmpty()) {
                        // 直接保存RSS中的完整内容
                        saveRssContentDirectly(savedArticle, rssFullContent);
                    } else {
                        // 抓取文章全文内容
                        fetchAndSaveArticleContent(savedArticle);
                    }
                    
                    // 将文章加入处理队列
                    queueArticleForProcessing(savedArticle.getId());
                }
            }
            
            // 更新RSS源状态
            rssSource.setLastFetchedAt(LocalDateTime.now());
            rssSource.setFetchStatus("SUCCESS");
            rssSource.setErrorMessage(null);
            rssSourceRepository.save(rssSource);
            
            logger.info("成功抓取RSS源: {}, 获取{}篇文章", 
                rssSource.getName(), fetchedArticles.size());
            
        } catch (Exception e) {
            logger.error("抓取RSS源失败: {}, 错误: {}", rssSource.getUrl(), e.getMessage());
            
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
            ArticleMetadata article = optionalArticle.get();

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

            // 提取第一张图片作为封面图
            String coverImageUrl = extractFirstImage(doc);
            if (coverImageUrl != null) {
                article.setCoverImageUrl(coverImageUrl);
                articleMetadataRepository.save(article);
                logger.info("为文章 {} 设置了封面图: {}", articleId, coverImageUrl);
            } else if (content.getExtractedImagesUrls() != null && !content.getExtractedImagesUrls().isEmpty()) {
                // 如果文档中没有直接提取到图片但MongoDB中有记录的图片，也使用第一张
                article.setCoverImageUrl(content.getExtractedImagesUrls().get(0));
                articleMetadataRepository.save(article);
                logger.info("为文章 {} 从已提取图片中设置封面图", articleId);
            }

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
                // 使用智能正文提取算法
                Document doc = Jsoup.parse(htmlContent);
                
                // 提取正文内容
                String extractedContent = extractMainContent(doc);
                String plainTextContent = Jsoup.parse(extractedContent).text();
                
                // 提取图片URLs（只从正文区域提取）
                List<String> imageUrls = new ArrayList<>();
                Document contentDoc = Jsoup.parse(extractedContent);
                Elements imgElements = contentDoc.select("img[src]");
                for (Element img : imgElements) {
                    String src = img.attr("abs:src");
                    if (StringUtils.hasText(src) && (src.startsWith("http://") || src.startsWith("https://"))) {
                        imageUrls.add(src);
                    }
                }
                
                // 提取第一张图片作为封面图
                if (!imageUrls.isEmpty()) {
                    article.setCoverImageUrl(imageUrls.get(0));
                    articleMetadataRepository.save(article);
                    logger.info("为文章 {} 设置了封面图: {}", article.getId(), imageUrls.get(0));
                } else {
                    // 如果正文区域没有图片，尝试从整个页面提取
                    String coverImageUrl = extractFirstImage(doc);
                    if (coverImageUrl != null) {
                        article.setCoverImageUrl(coverImageUrl);
                        articleMetadataRepository.save(article);
                        logger.info("为文章 {} 设置了整页面的封面图: {}", article.getId(), coverImageUrl);
                    }
                }
                
                // 计算内容哈希
                String contentHash = DigestUtils.md5DigestAsHex(plainTextContent.getBytes());
                
                // 创建ArticleContent对象
                ArticleContent articleContent = ArticleContent.builder()
                        .mysqlMetadataId(article.getId())
                        .originalUrl(article.getLinkToOriginal())
                        .titleFromContent(article.getTitle())
                        .fullHtmlContent(extractedContent) // 存储提取的正文HTML
                        .plainTextContent(plainTextContent)
                        .extractedImagesUrls(imageUrls)
                        .contentHash(contentHash)
                        .author(article.getAuthor())
                        .publicationDate(article.getPublicationDate())
                        .fetchedAt(LocalDateTime.now())
                        .build();
                
                // 保存到MongoDB
                ArticleContent savedContent = articleContentRepository.save(articleContent);
                logger.info("成功保存文章内容到MongoDB: {} -> {}, 正文长度: {}", 
                    article.getId(), savedContent.getId(), plainTextContent.length());
                
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
     * 智能提取文章正文内容
     * 移除导航、广告、侧边栏等无关内容，只保留核心正文
     * 
     * @param doc Jsoup Document
     * @return 提取的正文HTML内容
     */
    private String extractMainContent(Document doc) {
        // 使用改进的传统正文提取算法
        logger.info("使用改进的传统算法进行正文提取");
        return extractMainContentFallback(doc);
    }
    
    /**
     * 传统的正文提取算法（作为readability4j的回退方案）
     */
    private String extractMainContentFallback(Document doc) {
        // 移除明显的非内容元素
        doc.select("script, style, noscript, iframe, embed, object").remove();
        doc.select("nav, header, footer, aside").remove();
        doc.select(".nav, .navigation, .navbar, .menu").remove();
        doc.select(".sidebar, .side-bar, .aside").remove();
        doc.select(".advertisement, .ads, .ad, .banner, .sponsor").remove();
        doc.select(".comment, .comments, .comment-section").remove();
        doc.select(".social, .share, .sharing").remove();
        doc.select(".related, .related-posts, .recommended").remove();
        doc.select(".breadcrumb, .breadcrumbs").remove();
        doc.select(".tag, .tags, .category, .categories").remove();
        doc.select("[id*=ad], [class*=ad]").remove();
        doc.select("[id*=banner], [class*=banner]").remove();
        doc.select("[id*=sponsor], [class*=sponsor]").remove();
        
        // 常见的正文内容选择器（按优先级排序）
        String[] contentSelectors = {
            "article",
            ".post-content", ".entry-content", ".article-content", ".content",
            ".post-body", ".entry-body", ".article-body", ".body",
            ".main-content", ".primary-content", ".story-content",
            "#content", "#main-content", "#article-content", "#post-content",
            ".text", ".article-text", ".post-text",
            "main",
            ".asset-content", // 阮一峰博客使用的类名
            "#main-content", // 阮一峰博客使用的ID
            ".post", ".entry", ".article", // 美团技术博客可能使用的类名
            ".markdown-body", ".content-body" // 通用的内容容器
        };
        
        Element mainContent = null;
        
        // 尝试使用常见选择器找到正文
        for (String selector : contentSelectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                Element candidate = elements.first();
                // 检查内容长度，确保找到的是主要内容
                if (candidate.text().length() > 100) {
                    mainContent = candidate;
                    logger.debug("使用选择器 '{}' 找到正文内容，长度: {}", selector, candidate.text().length());
                    break;
                }
            }
        }
        
        // 如果没有找到明确的正文容器，使用启发式算法
        if (mainContent == null) {
            mainContent = findContentByHeuristics(doc);
        }
        
        // 如果还是没找到，使用body作为fallback
        if (mainContent == null) {
            mainContent = doc.body();
            logger.warn("未能找到明确的正文容器，使用body作为fallback");
        }
        
        // 进一步清理找到的内容
        if (mainContent != null) {
            cleanAndNormalizeContent(mainContent);
            return mainContent.html();
        }
        
        return "";
    }
    
    /**
     * 清理和标准化内容
     */
    private void cleanAndNormalizeContent(Element content) {
        if (content == null) return;
        
        // 移除内容中的其他干扰元素
        content.select("script, style, noscript").remove();
        content.select(".share, .sharing, .social-share").remove();
        content.select(".tags, .tag-list").remove();
        content.select(".author-info, .author-bio").remove();
        content.select(".next-prev, .pagination").remove();
        content.select(".comments, .comment-section").remove();
        content.select(".related-posts, .recommended").remove();
        
        // 清理空白段落和无意义的div
        content.select("p:empty, div:empty").remove();
        content.select("p:containsOwn(　), p:containsOwn(&nbsp;)").remove();
        
        // 清理只包含空白字符的元素
        Elements allElements = content.select("*");
        for (Element element : allElements) {
            if (element.text().trim().isEmpty() && 
                element.select("img, video, audio, iframe").isEmpty()) {
                element.remove();
            }
        }
    }
    
    /**
     * 使用启发式算法查找正文内容
     * 基于文本密度和段落数量来判断
     * 
     * @param doc Jsoup Document
     * @return 最可能包含正文的元素
     */
    private Element findContentByHeuristics(Document doc) {
        Elements candidates = doc.select("div, section, article");
        Element bestCandidate = null;
        int bestScore = 0;
        
        for (Element candidate : candidates) {
            int score = calculateContentScore(candidate);
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }
        
        logger.debug("启发式算法找到最佳候选者，得分: {}", bestScore);
        return bestCandidate;
    }
    
    /**
     * 计算元素的内容得分
     * 基于文本长度、段落数量、图片数量等因素
     * 
     * @param element 要评分的元素
     * @return 内容得分
     */
    private int calculateContentScore(Element element) {
        String text = element.text();
        int score = 0;
        
        // 基础文本长度得分
        score += Math.min(text.length() / 100, 50);
        
        // 段落数量得分
        int paragraphs = element.select("p").size();
        score += Math.min(paragraphs * 5, 25);
        
        // 图片数量得分（适量图片是好的）
        int images = element.select("img").size();
        score += Math.min(images * 2, 10);
        
        // 链接密度惩罚（太多链接可能是导航或广告）
        int links = element.select("a").size();
        int linkDensity = text.length() > 0 ? (links * 100) / text.length() : 0;
        if (linkDensity > 20) {
            score -= 20;
        }
        
        // 常见正文相关类名奖励
        String className = element.className().toLowerCase();
        if (className.contains("content") || className.contains("post") || 
            className.contains("article") || className.contains("body")) {
            score += 15;
        }
        
        // 常见干扰元素类名惩罚
        if (className.contains("nav") || className.contains("menu") || 
            className.contains("sidebar") || className.contains("ad")) {
            score -= 30;
        }
        
        return Math.max(score, 0);
    }
    
    /**
     * 从RSS条目中提取content:encoded内容
     * 增强版本，特别针对各种RSS格式进行优化，包括标准RSS 2.0格式
     * 
     * @param entry RSS条目
     * @return 完整HTML内容，如果没有则返回null
     */
    private String extractContentEncoded(SyndEntry entry) {
        try {
            logger.debug("开始提取RSS条目内容: {}", entry.getTitle());
            
            // 方法1: 首先检查content:encoded字段（通过SyndContent）
            List<SyndContent> contents = entry.getContents();
            if (contents != null && !contents.isEmpty()) {
                for (SyndContent content : contents) {
                    String value = content.getValue();
                    String type = content.getType();
                    
                    if (value != null && !value.trim().isEmpty()) {
                        // 优先选择HTML类型的内容
                        if (type != null && (type.contains("html") || type.contains("xhtml"))) {
                            if (value.length() > 100) { // 降低阈值，确保能获取到内容
                                logger.info("从RSS content字段提取到HTML内容，类型: {}, 长度: {}", type, value.length());
                                return processAndValidateContent(value);
                            }
                        }
                        // 检查是否包含HTML标签，且内容较长（可能是完整内容）
                        else if (value.contains("<") && value.length() > 100) {
                            logger.info("从RSS content字段提取到HTML内容，长度: {}", value.length());
                            return processAndValidateContent(value);
                        }
                        // 即使是纯文本，如果足够长也可能是完整内容
                        else if (value.length() > 300) { // 降低阈值
                            logger.info("从RSS content字段提取到长文本内容，长度: {}", value.length());
                            return processAndValidateContent(value);
                        }
                    }
                }
            }
            
            // 方法2: 尝试通过Rome库的扩展模块获取content:encoded
            Object wireEntry = entry.getWireEntry();
            if (wireEntry instanceof org.jdom2.Element) {
                org.jdom2.Element element = (org.jdom2.Element) wireEntry;
                
                // 检查标准RSS 2.0 item节点下的各种内容字段
                // 1. content:encoded 命名空间方式
                org.jdom2.Namespace contentNS = org.jdom2.Namespace.getNamespace("content", "http://purl.org/rss/1.0/modules/content/");
                org.jdom2.Element contentEncoded = element.getChild("encoded", contentNS);
                if (contentEncoded != null) {
                    String encodedContent = contentEncoded.getText();
                    if (encodedContent != null && !encodedContent.trim().isEmpty()) {
                        logger.info("从RSS content:encoded字段提取到完整内容，长度: {}", encodedContent.length());
                        return encodedContent;
                    }
                }
                
                // 2. RSS 2.0 <description>可能包含完整HTML内容
                org.jdom2.Element descElement = element.getChild("description");
                if (descElement != null) {
                    String descContent = descElement.getText();
                    if (descContent != null && descContent.length() > 200 && descContent.contains("<")) {
                        logger.info("从RSS 2.0 description字段提取到HTML内容，长度: {}", descContent.length());
                        return descContent;
                    }
                }
                
                // 3. 检查是否有CDATA包装的内容
                if (descElement != null) {
                    List<?> content = descElement.getContent();
                    for (Object o : content) {
                        if (o instanceof org.jdom2.CDATA) {
                            String cdataContent = ((org.jdom2.CDATA) o).getText();
                            if (cdataContent != null && !cdataContent.trim().isEmpty()) {
                                logger.info("从RSS description CDATA中提取到内容，长度: {}", cdataContent.length());
                                return cdataContent;
                            }
                        }
                    }
                }
                
                // 4. 检查全文元素(多个RSS规范变体)
                String[] fullContentElements = {"fulltext", "full-text", "full_text", "fullcontent", "full-content", "full_content"};
                for (String elementName : fullContentElements) {
                    org.jdom2.Element fullElement = element.getChild(elementName);
                    if (fullElement != null && fullElement.getText() != null && !fullElement.getText().trim().isEmpty()) {
                        logger.info("从RSS {}字段提取到内容，长度: {}", elementName, fullElement.getText().length());
                        return fullElement.getText();
                    }
                }
                
                // 5. 检查summary字段（Atom格式常用）
                org.jdom2.Element summaryElement = element.getChild("summary");
                if (summaryElement != null) {
                    String summaryContent = summaryElement.getText();
                    if (summaryContent != null && summaryContent.length() > 200) {
                        logger.info("从RSS summary字段提取到内容，长度: {}", summaryContent.length());
                        return summaryContent;
                    }
                }
                
                // 6. 尝试获取category标签内容用于元数据增强
                List<org.jdom2.Element> categories = element.getChildren("category");
                if (!categories.isEmpty()) {
                    logger.debug("找到分类标签: {}", categories.size());
                    // 这里不直接返回，而是在后续处理中使用
                }
            }
            
            // 方法3: 检查description是否包含完整HTML内容（某些RSS源将完整内容放在description中）
            if (entry.getDescription() != null) {
                String desc = entry.getDescription().getValue();
                if (desc != null && !desc.trim().isEmpty()) {
                    // 优先选择包含HTML标签的长内容
                    if (desc.contains("<") && desc.length() > 200) { // 降低阈值
                        logger.info("从RSS description字段提取到HTML内容，长度: {}", desc.length());
                        return processAndValidateContent(desc);
                    }
                    // 即使是纯文本，如果足够长也可能是完整内容
                    else if (desc.length() > 300) { // 降低阈值
                        logger.info("从RSS description字段提取到长文本内容，长度: {}", desc.length());
                        return processAndValidateContent(desc);
                    }
                }
            }
            
            // 方法4: 尝试直接从title获取更多信息
            if (entry.getTitle() != null && entry.getTitle().length() > 50) {
                // 长标题可能包含额外信息，但标题本身不是完整内容，所以不直接返回
                logger.debug("标题较长({})，但不作为完整内容处理", entry.getTitle().length());
            }
            
            logger.warn("未能从RSS条目中提取到有效的完整内容: {}", entry.getTitle());
            
        } catch (Exception e) {
            logger.error("提取RSS content:encoded内容时出错: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * 直接保存RSS中的完整内容到MongoDB
     * 增强版本能够处理XML格式的RSS内容
     * 
     * @param article 文章元数据
     * @param rssContent RSS中的完整内容
     */
    private void saveRssContentDirectly(ArticleMetadata article, String rssContent) {
        try {
            logger.info("直接保存RSS内容到MongoDB: {} - {}", article.getId(), article.getTitle());
            
            // 检查是否已存在内容
            if (articleContentRepository.existsByMysqlMetadataId(article.getId())) {
                logger.info("文章内容已存在，跳过保存: {}", article.getId());
                return;
            }
            
            // 内容质量检查
            if (rssContent == null || rssContent.trim().isEmpty()) {
                logger.warn("RSS内容为空，回退到URL抓取: {}", article.getId());
                fetchAndSaveArticleContent(article);
                return;
            }
            
            // 检查是否为未处理的XML格式
            if (rssContent.startsWith("<?xml") || rssContent.contains("<rss") || 
                rssContent.contains("<channel>") || rssContent.contains("<feed")) {
                
                logger.info("检测到可能的XML格式内容，尝试提取文章正文");
                String extractedContent = null;
                
                try {
                    // 使用XML解析器解析内容
                    Document xmlDoc = Jsoup.parse(rssContent, "", org.jsoup.parser.Parser.xmlParser());
                    
                    // 尝试提取<item>中的内容
                    Elements items = xmlDoc.select("item");
                    if (!items.isEmpty()) {
                        // 首先尝试找到当前文章对应的item
                        Element targetItem = null;
                        String articleTitle = article.getTitle();
                        String articleLink = article.getLinkToOriginal();
                        
                        for (Element item : items) {
                            // 基于标题或链接匹配
                            if ((articleTitle != null && item.select("title").text().equals(articleTitle)) ||
                                (articleLink != null && item.select("link").text().equals(articleLink))) {
                                targetItem = item;
                                break;
                            }
                        }
                        
                        // 如果找到了目标item
                        if (targetItem != null) {
                            // 按优先级尝试不同内容字段
                            // 1. 首先尝试content:encoded字段
                            Element contentEncoded = targetItem.selectFirst("encoded, content\\:encoded");
                            if (contentEncoded != null && !contentEncoded.text().trim().isEmpty()) {
                                extractedContent = contentEncoded.text();
                                logger.info("从XML item的content:encoded提取到内容，长度: {}", extractedContent.length());
                            }
                            
                            // 2. 然后尝试description字段
                            if (extractedContent == null) {
                                Element description = targetItem.selectFirst("description");
                                if (description != null && !description.text().trim().isEmpty()) {
                                    extractedContent = description.text();
                                    logger.info("从XML item的description提取到内容，长度: {}", extractedContent.length());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("XML解析失败: {}", e.getMessage());
                }
                
                // 如果提取成功，则使用提取后的内容
                if (extractedContent != null && !extractedContent.trim().isEmpty()) {
                    rssContent = extractedContent;
                } else {
                    logger.warn("无法从XML中提取文章内容，回退到URL抓取");
                    fetchAndSaveArticleContent(article);
                    return;
                }
            }
            
            // 检查内容长度，确保不是仅仅是摘要
            String textContent = Jsoup.parse(rssContent).text();
            if (textContent.length() < 50) {
                logger.warn("RSS内容过短（{}字符），可能只是摘要，回退到URL抓取: {}", textContent.length(), article.getId());
                fetchAndSaveArticleContent(article);
                return;
            }
            
            // 清理和处理RSS内容
            String cleanedContent = cleanRssContent(rssContent);
            Document contentDoc = Jsoup.parse(cleanedContent);
            String plainTextContent = contentDoc.text();
            
            // 再次验证清理后的内容
            if (plainTextContent.trim().isEmpty()) {
                logger.warn("清理后的内容为空，回退到URL抓取: {}", article.getId());
                fetchAndSaveArticleContent(article);
                return;
            }
            
            // 内容质量评估 - 检查基本内容质量
            boolean hasStructure = contentDoc.select("p, div, h1, h2, li, table").size() > 0;
            boolean hasMedia = contentDoc.select("img, video, iframe").size() > 0;
            
            // 如果文本太短且没有结构和媒体，可能不是有价值的内容
            if (plainTextContent.length() < 100 && !hasStructure && !hasMedia) {
                logger.warn("内容质量评估不通过，回退到URL抓取: {}", article.getId());
                fetchAndSaveArticleContent(article);
                return;
            }
            
            // 提取图片URLs
            List<String> imageUrls = new ArrayList<>();
            Elements imgElements = contentDoc.select("img[src]");
            for (Element img : imgElements) {
                String src = img.attr("src");
                // 处理相对路径图片
                if (StringUtils.hasText(src)) {
                    if (src.startsWith("http://") || src.startsWith("https://")) {
                        imageUrls.add(src);
                    } else if (src.startsWith("/") || src.startsWith("./")) {
                        // 尝试构建绝对路径（基于文章原始URL）
                        try {
                            String baseUrl = article.getLinkToOriginal();
                            if (StringUtils.hasText(baseUrl)) {
                                java.net.URL url = new java.net.URL(baseUrl);
                                String absoluteUrl = url.getProtocol() + "://" + url.getHost() + 
                                    (src.startsWith("/") ? src : "/" + src);
                                imageUrls.add(absoluteUrl);
                            }
                        } catch (Exception e) {
                            logger.debug("无法构建图片绝对路径: {}", src);
                        }
                    }
                }
            }
            
            // 设置封面图URL
            if (!imageUrls.isEmpty()) {
                article.setCoverImageUrl(imageUrls.get(0));
                articleMetadataRepository.save(article);
                logger.info("为RSS文章 {} 设置了封面图: {}", article.getId(), imageUrls.get(0));
            }
            
            // 计算内容哈希
            String contentHash = DigestUtils.md5DigestAsHex(plainTextContent.getBytes());
            
            // 创建ArticleContent对象
            ArticleContent articleContent = ArticleContent.builder()
                    .mysqlMetadataId(article.getId())
                    .originalUrl(article.getLinkToOriginal())
                    .titleFromContent(article.getTitle())
                    .fullHtmlContent(cleanedContent)
                    .plainTextContent(plainTextContent)
                    .extractedImagesUrls(imageUrls)
                    .contentHash(contentHash)
                    .author(article.getAuthor())
                    .publicationDate(article.getPublicationDate())
                    .fetchedAt(LocalDateTime.now())
                    .build();
            
            // 保存到MongoDB
            ArticleContent savedContent = articleContentRepository.save(articleContent);
            logger.info("成功保存RSS内容到MongoDB: {} -> {}, HTML长度: {}, 纯文本长度: {}, 图片数量: {}", 
                article.getId(), savedContent.getId(), cleanedContent.length(), plainTextContent.length(), imageUrls.size());
            
            // 更新MySQL中的mongodb_content_id字段
            try {
                article.setMongodbContentId(savedContent.getId());
                articleMetadataRepository.save(article);
                logger.info("已更新MySQL中的mongodb_content_id: {}", article.getId());
            } catch (Exception e) {
                logger.warn("更新MySQL中的mongodb_content_id失败，但不影响主流程: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("直接保存RSS内容时出错: {} - {}", article.getId(), e.getMessage(), e);
            // 如果直接保存失败，回退到原有的抓取逻辑
            logger.info("回退到原有的URL抓取逻辑: {}", article.getId());
            fetchAndSaveArticleContent(article);
        }
    }
    
    /**
     * 清理RSS内容
     * 增强版本，处理各种格式的RSS内容
     * 
     * @param rssContent 原始RSS内容
     * @return 清理后的内容
     */
    private String cleanRssContent(String rssContent) {
        if (rssContent == null || rssContent.trim().isEmpty()) {
            return "";
        }
        
        try {
            logger.debug("开始清理RSS内容，长度: {}", rssContent.length());
            
            // 1. 检查是否为XML结构（标准RSS/Atom响应）
            if ((rssContent.startsWith("<?xml") || rssContent.contains("<rss")) && 
                (rssContent.contains("<item>") || rssContent.contains("<entry>"))) {
                logger.debug("检测到XML结构，尝试提取内容");
                try {
                    Document xmlDoc = Jsoup.parse(rssContent, "", org.jsoup.parser.Parser.xmlParser());
                    // 提取所有item/entry中的内容
                    Elements items = xmlDoc.select("item");
                    if (!items.isEmpty()) {
                        StringBuilder extractedContent = new StringBuilder();
                        for (Element item : items) {
                            // 按优先级获取内容
                            String content = null;
                            Element contentElement = item.selectFirst("encoded, content\\:encoded");
                            if (contentElement != null) {
                                content = contentElement.text();
                            } else if (item.selectFirst("description") != null) {
                                content = item.selectFirst("description").text();
                            }
                            
                            if (content != null && !content.trim().isEmpty()) {
                                extractedContent.append(content).append("\n");
                            }
                        }
                        
                        if (extractedContent.length() > 0) {
                            rssContent = extractedContent.toString();
                            logger.debug("成功从XML提取内容，长度: {}", rssContent.length());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("XML解析失败: {}", e.getMessage());
                }
            }
            
            // 2. 使用Jsoup解析和清理HTML
            Document doc;
            if (rssContent.trim().startsWith("<") && rssContent.contains("</")) {
                // 内容看起来是HTML
                doc = Jsoup.parse(rssContent);
            } else {
                // 可能是纯文本，进行包装
                doc = Jsoup.parse("<div>" + rssContent + "</div>");
            }
            
            // 3. 移除一些可能的干扰元素
            doc.select("script, style, noscript").remove();
            doc.select(".share, .sharing, .social-share").remove();
            doc.select(".advertisement, .ads, .ad").remove();
            doc.select(".comment-section, .comments").remove();
            doc.select("footer, .footer, .site-footer").remove();
            
            // 4. 清理空白段落和无用标签
            doc.select("p:empty, div:empty").remove();
            
            // 5. 确保图片链接是绝对路径
            Elements images = doc.select("img[src]");
            for (Element img : images) {
                String src = img.attr("src");
                if (src.startsWith("data:")) {
                    // 数据URI，保留
                    continue;
                } else if (!src.startsWith("http")) {
                    // 相对路径，标记但不修改
                    logger.debug("发现相对路径图片: {}", src);
                }
            }
            
            // 6. 处理特殊字符和HTML实体
            doc.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);
            doc.outputSettings().charset("UTF-8");
            
            // 7. 基本格式规范化
            // 修复可能的H标签嵌套
            Elements headings = doc.select("h1, h2, h3, h4, h5, h6");
            for (Element heading : headings) {
                if (heading.parent() != null && heading.parent().tagName().startsWith("h")) {
                    // 修复嵌套标题
                    heading.unwrap();
                }
            }
            
            // 8. 处理特定的内容格式问题
            // 合并连续的空行
            Elements paragraphs = doc.select("p, div, span");
            for (Element p : paragraphs) {
                if (p.text().trim().isEmpty() && p.selectFirst("img") == null) {
                    p.remove();
                }
            }
            
            // 确保body存在
            Element body = doc.body();
            if (body == null) {
                logger.warn("HTML内容缺少body，创建一个");
                doc = Jsoup.parse("<html><body>" + rssContent + "</body></html>");
                body = doc.body();
            }
            
            // 内容检查 - 如果内容看起来过于简单（只有少量文本），回退到原始内容
            if (body.text().length() < 50 && body.select("img").isEmpty()) {
                logger.warn("清理后的内容过于简单，回退到原始格式，长度: {}", rssContent.length());
                return rssContent;
            }
            
            // 9. 选择返回方式：
            // - 如果原始内容就已经很简洁，直接返回原始内容
            // - 否则返回清理后的HTML
            if (rssContent.length() < 1000 && !rssContent.contains("<script") && !rssContent.contains("<style")) {
                return rssContent;
            } else {
                String cleanedHtml = body.html();
                logger.debug("内容清理完成，原始长度: {}, 清理后长度: {}", rssContent.length(), cleanedHtml.length());
                return cleanedHtml;
            }
            
        } catch (Exception e) {
            logger.warn("清理RSS内容时出错，返回原始内容: {}", e.getMessage());
            return rssContent;
        }
    }
    
    /**
     * 抓取HTML内容
     * 
     * @param url 文章URL
     * @return HTML内容
     */
    private String fetchHtmlContent(String url) {
        try {
            logger.info("抓取URL内容: {}", url);
            
            // 配置请求参数，包括较长的超时
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(fetchTimeout * 2)  // 增加超时时间
                    .setSocketTimeout(fetchTimeout * 2)
                    .build();
            
            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build()) {
                
                HttpGet httpGet = new HttpGet(url);
                
                // 添加更真实的User-Agent
                httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
                // 对于RSSHub内容，添加特殊的Referer
                if (url.contains("localhost:1200") || url.contains("rsshub.app")) {
                    httpGet.setHeader("Referer", "http://localhost:1200/");
                    httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                }
                
                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        // 使用标准的字符集检测方法
                        String charset = "UTF-8";
                        if (response.getEntity().getContentType() != null) {
                            String contentType = response.getEntity().getContentType().getValue();
                            if (contentType != null && contentType.contains("charset=")) {
                                charset = contentType.substring(contentType.indexOf("charset=") + 8).trim();
                            }
                        }
                        String content = EntityUtils.toString(response.getEntity(), charset);
                        // 使用TextEncodingUtil处理可能的编码问题
                        return TextEncodingUtil.processText(content);
                    } else {
                        logger.warn("抓取内容返回错误状态码: {}, URL: {}", statusCode, url);
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("抓取内容失败: {}, 错误: {}", url, e.getMessage());
            return null;
        }
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
            logger.debug("开始获取文章详情: {}", articleId);
            
            // 获取文章元数据
            Optional<ArticleMetadata> optionalMetadata = articleMetadataRepository.findById(articleId);
            if (!optionalMetadata.isPresent()) {
                logger.warn("文章元数据不存在: {}", articleId);
                return null;
            }
            
            ArticleMetadata metadata = optionalMetadata.get();
            logger.debug("找到文章元数据: {} - {}", articleId, metadata.getTitle());
            
            // 获取文章内容
            Optional<ArticleContent> optionalContent = articleContentRepository.findByMysqlMetadataId(articleId);
            ArticleContent content = optionalContent.orElse(null);
            
            // 内容存在性和质量检查
            boolean contentNeedsRefetch = false;
            
            if (content == null) {
                logger.warn("文章内容不存在，将尝试抓取: {}", articleId);
                contentNeedsRefetch = true;
            } else {
                logger.debug("找到文章内容: {}, HTML长度: {}, 纯文本长度: {}", 
                    articleId, 
                    content.getFullHtmlContent() != null ? content.getFullHtmlContent().length() : 0,
                    content.getPlainTextContent() != null ? content.getPlainTextContent().length() : 0);
                
                // 检查内容质量
                boolean hasValidContent = false;
                if (content.getFullHtmlContent() != null && !content.getFullHtmlContent().trim().isEmpty()) {
                    hasValidContent = true;
                } else if (content.getPlainTextContent() != null && !content.getPlainTextContent().trim().isEmpty()) {
                    hasValidContent = true;
                }
                
                if (!hasValidContent) {
                    logger.warn("文章内容为空或无效，将尝试重新抓取: {}", articleId);
                    contentNeedsRefetch = true;
                }
            }
            
            // 如果需要重新抓取内容
            if (contentNeedsRefetch) {
                try {
                    logger.info("开始重新抓取文章内容: {}", articleId);
                    // 同步抓取文章内容（在这种情况下，我们更希望同步等待结果）
                    fetchAndSaveArticleContent(metadata);
                    
                    // 重新获取内容
                    optionalContent = articleContentRepository.findByMysqlMetadataId(articleId);
                    content = optionalContent.orElse(null);
                    
                    if (content != null) {
                        logger.info("重新抓取文章内容成功: {}, HTML长度: {}", 
                            articleId, 
                            content.getFullHtmlContent() != null ? content.getFullHtmlContent().length() : 0);
                    } else {
                        logger.warn("重新抓取后文章内容仍为空: {}", articleId);
                    }
                } catch (Exception e) {
                    logger.error("重新抓取文章内容失败: {}, 错误: {}", articleId, e.getMessage(), e);
                }
                
                // 如果抓取原始链接失败，尝试直接从RSS源重新获取
                if (content == null || (content.getFullHtmlContent() == null && content.getPlainTextContent() == null)) {
                    try {
                        // 检查是否有RSS源信息
                        RssSource rssSource = metadata.getRssSource();
                        if (rssSource != null) {
                            logger.info("尝试从RSS源重新获取文章: {}, 源: {}", articleId, rssSource.getName());
                            // 重新触发RSS源抓取
                            fetchArticlesFromSource(rssSource);
                            
                            // 再次尝试获取内容
                            optionalContent = articleContentRepository.findByMysqlMetadataId(articleId);
                            content = optionalContent.orElse(null);
                        }
                    } catch (Exception e) {
                        logger.error("从RSS源重新获取文章失败: {}", e.getMessage(), e);
                    }
                }
            }
            
            // 转换为DTO
            ArticleDTO dto = ArticleDTO.fromEntity(metadata, content);
            
            if (dto != null) {
                // 再次确保DTO中有内容
                if ((dto.getHtmlContent() == null || dto.getHtmlContent().trim().isEmpty()) && 
                    (dto.getPlainTextContent() == null || dto.getPlainTextContent().trim().isEmpty())) {
                    
                    logger.warn("生成DTO后发现内容仍为空: {}", articleId);
                    
                    // 如果转换后的DTO中内容为空，但我们确实有内容数据，则手动设置
                    if (content != null) {
                        if (content.getFullHtmlContent() != null && !content.getFullHtmlContent().trim().isEmpty()) {
                            logger.info("手动设置HTML内容到DTO: {}, 长度: {}", 
                                   articleId, content.getFullHtmlContent().length());
                            dto.setHtmlContent(content.getFullHtmlContent());
                        }
                        
                        if (content.getPlainTextContent() != null && !content.getPlainTextContent().trim().isEmpty()) {
                            logger.info("手动设置纯文本内容到DTO: {}, 长度: {}", 
                                   articleId, content.getPlainTextContent().length());
                            dto.setPlainTextContent(content.getPlainTextContent());
                        }
                    }
                }
                
                logger.debug("成功转换为DTO: {}, 有内容: {}", articleId, content != null);
            } else {
                logger.warn("DTO转换失败: {}", articleId);
            }
            
            return dto;
            
        } catch (Exception e) {
            logger.error("获取文章详情失败: {}", articleId, e);
            return null;
        }
    }
    
    /**
     * 处理和验证RSS内容
     * 对提取的内容进行清理、编码处理和质量验证
     * 增强版本可处理多种RSS格式的内容
     * 
     * @param rawContent 原始内容
     * @return 处理后的内容，如果内容无效则返回null
     */
    private String processAndValidateContent(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 1. 处理可能的XML声明和DOCTYPE
            // 删除XML开头的注释，比如"This XML file does not appear to have any style information..."
            String processedContent = rawContent;
            if (processedContent.contains("<?xml") || processedContent.contains("<!DOCTYPE")) {
                // 检测是否为纯XML格式的RSS
                if (processedContent.contains("<rss") || processedContent.contains("<channel") || 
                    processedContent.contains("<item>") || processedContent.contains("<feed")) {
                    logger.info("检测到XML格式的RSS内容，提取实际文章内容");
                    
                    // 尝试解析XML并提取item/entry中的正文
                    try {
                        Document xmlDoc = Jsoup.parse(processedContent, "", org.jsoup.parser.Parser.xmlParser());
                        
                        // 查找关键内容节点 - 针对RSS 2.0格式
                        Elements descriptions = xmlDoc.select("item > description");
                        if (!descriptions.isEmpty()) {
                            for (Element desc : descriptions) {
                                String content = desc.text(); // 获取节点文本内容
                                if (content.length() > 100) {
                                    // 如果描述中有HTML，再次解析以保留格式
                                    if (content.contains("<")) {
                                        processedContent = content;
                                        logger.info("从XML的<description>提取到HTML内容，长度: {}", content.length());
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // 检查内容编码字段 - 针对RSS 1.0/2.0扩展
                        Elements contentEncoded = xmlDoc.select("item > encoded, item > content\\:encoded");
                        if (!contentEncoded.isEmpty()) {
                            String content = contentEncoded.first().text();
                            if (content.length() > 100) {
                                processedContent = content;
                                logger.info("从XML的<content:encoded>提取到内容，长度: {}", content.length());
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("XML解析失败，使用原始内容: {}", e.getMessage());
                    }
                }
            }
            
            // 2. 字符编码处理
            processedContent = TextEncodingUtil.processText(processedContent);
            
            // 3. 检测是否为HTML片段或完整HTML
            boolean isHtmlContent = processedContent.trim().startsWith("<") && 
                                   (processedContent.contains("<p>") || processedContent.contains("<div>"));
            
            // 4. 根据内容类型进行不同处理
            Document doc;
            if (isHtmlContent) {
                // HTML内容
                doc = Jsoup.parse(processedContent);
                
                // 移除明显的干扰元素但保留内容结构
                doc.select("script, style, noscript").remove();
                doc.select(".advertisement, .ads, .ad").remove();
                doc.select(".share, .sharing, .social-share").remove();
                
                // 处理可能带有注释的内容
                String htmlContent = doc.html();
                if (htmlContent.contains("<!--") && htmlContent.contains("-->")) {
                    htmlContent = htmlContent.replaceAll("<!--.*?-->", "");
                    doc = Jsoup.parse(htmlContent);
                }
            } else {
                // 可能是纯文本或简单格式，包装为HTML
                doc = Jsoup.parse("<div>" + processedContent + "</div>");
            }
            
            // 5. 内容质量验证
            String textContent = doc.text();
            if (textContent.length() < 50) {
                logger.debug("内容过短，可能不是有效的文章内容: {} 字符", textContent.length());
                // 对于短内容，如果确实有图片或其他媒体元素，依然保留
                Elements media = doc.select("img, video, iframe, object");
                if (media.isEmpty()) {
                    return null; // 既短又没有媒体元素，可能不是有效内容
                }
            }
            
            // 6. 检查是否包含有意义的内容
            boolean hasStructure = doc.select("p, div, li, h1, h2, h3").size() > 0;
            boolean hasMedia = doc.select("img, video, iframe").size() > 0;
            
            // 如果既没有结构也没有媒体且文本较少，可能不是有效内容
            if (!hasStructure && !hasMedia && textContent.length() < 200) {
                logger.debug("内容缺少结构和媒体且较短，可能不是完整文章");
                return null;
            }
            
            // 7. 格式化内容（确保图片URL是绝对路径）
            Elements images = doc.select("img[src]");
            for (Element img : images) {
                String src = img.attr("src");
                if (!src.startsWith("http")) {
                    // 尝试将相对路径转换为绝对路径
                    img.attr("src", src); // 保持原样，但标记问题
                    logger.debug("图片使用相对路径: {}", src);
                }
            }
            
            // 8. 处理内联样式和属性
            Elements allElements = doc.select("*");
            for (Element el : allElements) {
                // 保留宽高属性，清除其他样式属性
                String width = el.attr("width");
                String height = el.attr("height");
                el.clearAttributes();
                if (!width.isEmpty()) el.attr("width", width);
                if (!height.isEmpty()) el.attr("height", height);
            }
            
            String finalContent = doc.body().html();
            logger.debug("内容处理完成，原始长度: {}, 处理后长度: {}, 文本长度: {}", 
                rawContent.length(), finalContent.length(), textContent.length());
            
            return finalContent;
            
        } catch (Exception e) {
            logger.warn("处理RSS内容时出错，返回原始内容: {}", e.getMessage());
            return rawContent;
        }
    }
}
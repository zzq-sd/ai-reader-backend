package com.aireader.backend.controller;

import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.service.ArticleFetchService;
import com.aireader.backend.service.RssFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 文章控制器
 * 处理文章相关的请求
 */
@RestController
@RequestMapping("/articles")
@Tag(name = "文章管理", description = "文章查询和内容获取")
public class ArticleController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(ArticleController.class);
    
    @Autowired
    private RssFeedService rssFeedService;
    
    @Autowired
    private ArticleFetchService articleFetchService;
    
    /**
     * 获取最新文章列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 文章列表
     */
    @GetMapping("/latest")
    @Operation(summary = "获取最新文章", description = "获取用户订阅的所有RSS源的最新文章")
    public ResponseEntity<ApiResponse> getLatestArticles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        // 暂时移除认证要求，用于测试
        String userId = null; // getCurrentUserId();
        List<ArticleDTO> articles = rssFeedService.getLatestArticlesForUser(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }
    
    /**
     * 获取文章内容
     * 
     * @param articleId 文章ID
     * @return 文章内容
     */
    @GetMapping("/{articleId}/content")
    @Operation(summary = "获取文章内容", description = "获取文章的全文内容")
    public ResponseEntity<ApiResponse> getArticleContent(
            @Parameter(description = "文章ID") @PathVariable String articleId,
            HttpServletResponse response) {
        
        log.info("获取文章内容请求: articleId={}", articleId);
        
        try {
            // 设置响应头为UTF-8编码
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=UTF-8");
            
            // 首先获取文章详情（包含内容）
            ArticleDTO article = articleFetchService.getArticleById(articleId);
            
            if (article == null) {
                log.warn("文章不存在: {}", articleId);
                return ResponseEntity.ok(ApiResponse.error("文章不存在"));
            }
            
            // 检查是否有HTML内容
            String content = article.getHtmlContent();
            if (content == null || content.trim().isEmpty()) {
                // 如果没有HTML内容，尝试获取纯文本内容
                content = article.getPlainTextContent();
                if (content == null || content.trim().isEmpty()) {
                    log.warn("文章内容为空: {}", articleId);
                    return ResponseEntity.ok(ApiResponse.error("文章内容为空"));
                }
            }
            
            // 确保字符串是正确的UTF-8编码
            if (content != null) {
                try {
                    // 将字符串转换为UTF-8字节数组，然后重新创建字符串
                    byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
                    content = new String(contentBytes, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    log.warn("字符编码处理异常: {}", e.getMessage());
                }
            }
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("articleId", articleId);
            responseData.put("content", content);
            responseData.put("title", article.getTitle());
            responseData.put("author", article.getAuthor());
            responseData.put("publicationDate", article.getPublicationDate());
            responseData.put("originalUrl", article.getOriginalUrl());
            responseData.put("wordCount", article.getWordCount());
            responseData.put("readingTimeMinutes", article.getReadingTimeMinutes());
            
            log.info("成功获取文章内容: articleId={}, contentLength={}", 
                    articleId, content != null ? content.length() : 0);
            
            // 创建ResponseEntity时设置正确的Content-Type
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Content-Type", "application/json; charset=UTF-8");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(ApiResponse.success(responseData));
            
        } catch (Exception e) {
            log.error("获取文章内容失败: articleId={}, error={}", articleId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("获取文章内容失败: " + e.getMessage()));
        }
    }
    
    /**
     * 手动解析文章内容
     * 
     * @param articleId 文章ID
     * @return 解析结果
     */
    @PostMapping("/{articleId}/parse")
    @Operation(summary = "解析文章内容", description = "手动触发文章内容解析")
    public ResponseEntity<ApiResponse> parseArticleContent(
            @Parameter(description = "文章ID") @PathVariable String articleId) {
        boolean success = articleFetchService.parseArticleContent(articleId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("articleId", articleId);
        
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("文章内容解析成功", response));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("文章内容解析失败"));
        }
    }
    
    /**
     * 将文章加入处理队列
     * 
     * @param articleId 文章ID
     * @return 处理结果
     */
    @PostMapping("/{articleId}/process")
    @Operation(summary = "处理文章", description = "将文章加入处理队列")
    public ResponseEntity<ApiResponse> processArticle(
            @Parameter(description = "文章ID") @PathVariable String articleId) {
        articleFetchService.queueArticleForProcessing(articleId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("articleId", articleId);
        return ResponseEntity.ok(ApiResponse.success("文章已加入处理队列", response));
    }
    
    /**
     * 获取文章详情
     * 
     * @param articleId 文章ID
     * @return 文章详情
     */
    @GetMapping("/{articleId}")
    @Operation(summary = "获取文章详情", description = "获取指定文章的详细信息")
    public ResponseEntity<ApiResponse> getArticleById(
            @Parameter(description = "文章ID") @PathVariable String articleId) {
        try {
            ArticleDTO article = articleFetchService.getArticleById(articleId);
            
            if (article == null) {
                return ResponseEntity.ok(ApiResponse.error("文章不存在"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(article));
        } catch (Exception e) {
            log.error("获取文章详情失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("获取文章详情失败"));
        }
    }
    
    /**
     * 手动触发RSS源抓取（测试用）
     * 
     * @param sourceId RSS源ID
     * @return 抓取结果
     */
    @PostMapping("/fetch-rss/{sourceId}")
    @Operation(summary = "手动触发RSS抓取", description = "手动触发指定RSS源的文章抓取")
    public ResponseEntity<ApiResponse> fetchRssSource(
            @Parameter(description = "RSS源ID") @PathVariable String sourceId) {
        try {
            log.info("手动触发RSS源抓取: sourceId={}", sourceId);
            
            int fetchedCount = articleFetchService.fetchArticlesFromSourceById(sourceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sourceId", sourceId);
            response.put("fetchedCount", fetchedCount);
            response.put("message", String.format("成功抓取 %d 篇文章", fetchedCount));
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("手动触发RSS源抓取失败: sourceId={}, error={}", sourceId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("RSS抓取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有文章列表（测试用）
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 文章列表
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有文章", description = "获取系统中的所有文章（测试用）")
    public ResponseEntity<ApiResponse> getAllArticles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("获取所有文章列表: page={}, size={}", page, size);
            
            // 这里可以调用一个获取所有文章的方法
            List<ArticleDTO> articles = rssFeedService.getLatestArticlesForUser(null, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("articles", articles);
            response.put("page", page);
            response.put("size", size);
            response.put("total", articles.size());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("获取所有文章列表失败: error={}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("获取文章列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建测试数据（测试用）
     * 
     * @return 创建结果
     */
    @PostMapping("/create-test-data")
    @Operation(summary = "创建测试数据", description = "创建测试RSS源和文章数据")
    public ResponseEntity<ApiResponse> createTestData() {
        try {
            log.info("开始创建测试数据");
            
            // 这里可以创建一些测试用的RSS源和文章
            // 由于这是测试接口，我们返回一个简单的响应
            Map<String, Object> response = new HashMap<>();
            response.put("message", "测试数据创建功能需要在RssSourceController中实现");
            response.put("suggestion", "请先通过 /feeds 接口添加RSS源，然后通过 /articles/fetch-rss/{sourceId} 抓取文章");
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("创建测试数据失败: error={}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("创建测试数据失败: " + e.getMessage()));
        }
    }
} 
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
import org.springframework.util.StringUtils;
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
        String userId = getCurrentUserId();
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
            @Parameter(description = "文章ID") @PathVariable String articleId) {
        
        log.info("获取文章内容请求: articleId={}", articleId);
        
        try {
            // 新的简化逻辑：直接调用具备按需抓取能力的服务方法
            String content = articleFetchService.getArticleFullContent(articleId);
            
            // 检查内容是否成功获取
            if (!StringUtils.hasText(content)) {
                log.error("无法获取文章内容，所有尝试均失败: {}", articleId);
                return ResponseEntity.ok(ApiResponse.error("文章内容为空"));
            }
            
            // 获取文章元数据用于响应
            ArticleDTO article = articleFetchService.getArticleById(articleId);
            if (article == null) {
                // 这本不应该发生，因为能获取内容就说明元数据存在
                log.warn("文章 {} 内容存在但元数据不存在", articleId);
                return ResponseEntity.ok(ApiResponse.error("文章数据不一致"));
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
                    articleId, content.length());
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
            
        } catch (Exception e) {
            log.error("获取文章内容时发生未知异常: articleId={}, error={}", articleId, e.getMessage(), e);
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
} 
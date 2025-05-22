package com.aireader.backend.controller;

import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.service.ArticleFetchService;
import com.aireader.backend.service.RssFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章控制器
 * 处理文章相关的请求
 */
@RestController
@RequestMapping("/articles")
@Tag(name = "文章管理", description = "文章查询和内容获取")
public class ArticleController {
    
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
    public ResponseEntity<List<ArticleDTO>> getLatestArticles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        String userId = getCurrentUserId();
        List<ArticleDTO> articles = rssFeedService.getLatestArticlesForUser(userId, page, size);
        return ResponseEntity.ok(articles);
    }
    
    /**
     * 获取文章内容
     * 
     * @param articleId 文章ID
     * @return 文章内容
     */
    @GetMapping("/{articleId}/content")
    @Operation(summary = "获取文章内容", description = "获取文章的全文内容")
    public ResponseEntity<Map<String, Object>> getArticleContent(
            @Parameter(description = "文章ID") @PathVariable String articleId) {
        String content = articleFetchService.getArticleFullContent(articleId);
        
        if (content == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("articleId", articleId);
        response.put("content", content);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 手动解析文章内容
     * 
     * @param articleId 文章ID
     * @return 解析结果
     */
    @PostMapping("/{articleId}/parse")
    @Operation(summary = "解析文章内容", description = "手动触发文章内容解析")
    public ResponseEntity<Map<String, Object>> parseArticleContent(
            @Parameter(description = "文章ID") @PathVariable String articleId) {
        boolean success = articleFetchService.parseArticleContent(articleId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("articleId", articleId);
        
        if (success) {
            response.put("message", "文章内容解析成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "文章内容解析失败");
            return ResponseEntity.badRequest().body(response);
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
    public ResponseEntity<Map<String, Object>> processArticle(
            @Parameter(description = "文章ID") @PathVariable String articleId) {
        articleFetchService.queueArticleForProcessing(articleId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("articleId", articleId);
        response.put("message", "文章已加入处理队列");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取当前登录用户ID
     * 
     * @return 用户ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("未找到认证信息");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }
} 
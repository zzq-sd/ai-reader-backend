package com.aireader.backend.controller;

import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.dto.RssSourceDTO;
import com.aireader.backend.service.RssFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RSS源控制器
 * 处理RSS源的增删改查和文章获取
 */
@RestController
@RequestMapping("/feeds")
@Tag(name = "RSS源管理", description = "RSS源的增删改查和文章获取")
public class RssSourceController {
    
    @Autowired
    private RssFeedService rssFeedService;
    
    /**
     * 添加新的RSS源
     * 
     * @param rssSourceDTO RSS源信息
     * @return 保存后的RSS源信息
     */
    @PostMapping
    @Operation(summary = "添加RSS源", description = "添加新的RSS源")
    public ResponseEntity<RssSourceDTO> addRssSource(@Valid @RequestBody RssSourceDTO rssSourceDTO) {
        String userId = getCurrentUserId();
        RssSourceDTO savedSource = rssFeedService.addRssSource(rssSourceDTO, userId);
        return new ResponseEntity<>(savedSource, HttpStatus.CREATED);
    }
    
    /**
     * 获取用户的所有RSS源
     * 
     * @return RSS源列表
     */
    @GetMapping
    @Operation(summary = "获取用户的RSS源", description = "获取当前用户的所有RSS源")
    public ResponseEntity<List<RssSourceDTO>> getUserRssSources() {
        String userId = getCurrentUserId();
        List<RssSourceDTO> sources = rssFeedService.getUserRssSources(userId);
        return ResponseEntity.ok(sources);
    }
    
    /**
     * 获取所有公共RSS源
     * 
     * @return 公共RSS源列表
     */
    @GetMapping("/public")
    @Operation(summary = "获取公共RSS源", description = "获取所有公共RSS源")
    public ResponseEntity<List<RssSourceDTO>> getPublicRssSources() {
        List<RssSourceDTO> sources = rssFeedService.getPublicRssSources();
        return ResponseEntity.ok(sources);
    }
    
    /**
     * 根据ID获取RSS源
     * 
     * @param sourceId RSS源ID
     * @return RSS源信息
     */
    @GetMapping("/{sourceId}")
    @Operation(summary = "获取RSS源详情", description = "根据ID获取RSS源详细信息")
    public ResponseEntity<RssSourceDTO> getRssSourceById(
            @Parameter(description = "RSS源ID") @PathVariable String sourceId) {
        Optional<RssSourceDTO> source = rssFeedService.getRssSourceById(sourceId);
        return source.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 更新RSS源信息
     * 
     * @param sourceId RSS源ID
     * @param rssSourceDTO 更新的RSS源信息
     * @return 更新后的RSS源信息
     */
    @PutMapping("/{sourceId}")
    @Operation(summary = "更新RSS源", description = "更新RSS源信息")
    public ResponseEntity<RssSourceDTO> updateRssSource(
            @Parameter(description = "RSS源ID") @PathVariable String sourceId,
            @Valid @RequestBody RssSourceDTO rssSourceDTO) {
        String userId = getCurrentUserId();
        RssSourceDTO updatedSource = rssFeedService.updateRssSource(sourceId, rssSourceDTO, userId);
        return ResponseEntity.ok(updatedSource);
    }
    
    /**
     * 删除RSS源
     * 
     * @param sourceId RSS源ID
     * @return 删除结果
     */
    @DeleteMapping("/{sourceId}")
    @Operation(summary = "删除RSS源", description = "删除指定的RSS源")
    public ResponseEntity<Map<String, Boolean>> deleteRssSource(
            @Parameter(description = "RSS源ID") @PathVariable String sourceId) {
        String userId = getCurrentUserId();
        boolean deleted = rssFeedService.deleteRssSource(sourceId, userId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", deleted);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取RSS源的文章列表
     * 
     * @param sourceId RSS源ID
     * @param page 页码
     * @param size 每页大小
     * @return 文章列表
     */
    @GetMapping("/{sourceId}/articles")
    @Operation(summary = "获取RSS源文章", description = "获取指定RSS源的文章列表")
    public ResponseEntity<List<ArticleDTO>> getArticlesByRssSource(
            @Parameter(description = "RSS源ID") @PathVariable String sourceId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        List<ArticleDTO> articles = rssFeedService.getArticlesByRssSource(sourceId, page, size);
        return ResponseEntity.ok(articles);
    }
    
    /**
     * 获取用户订阅的所有RSS源的最新文章
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 文章列表
     */
    @GetMapping("/latest-articles")
    @Operation(summary = "获取最新文章", description = "获取用户订阅的所有RSS源的最新文章")
    public ResponseEntity<List<ArticleDTO>> getLatestArticlesForUser(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        String userId = getCurrentUserId();
        List<ArticleDTO> articles = rssFeedService.getLatestArticlesForUser(userId, page, size);
        return ResponseEntity.ok(articles);
    }
    
    /**
     * 手动触发RSS源的抓取
     * 
     * @param sourceId RSS源ID
     * @return 抓取结果
     */
    @PostMapping("/{sourceId}/fetch")
    @Operation(summary = "抓取RSS源", description = "手动触发指定RSS源的抓取")
    public ResponseEntity<Map<String, Object>> fetchRssSource(
            @Parameter(description = "RSS源ID") @PathVariable String sourceId) {
        int count = rssFeedService.fetchRssSource(sourceId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", count);
        response.put("message", "成功抓取" + count + "篇文章");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 检查RSS源URL是否有效
     * 
     * @param url RSS源URL
     * @return 检查结果
     */
    @GetMapping("/validate")
    @Operation(summary = "验证RSS源URL", description = "检查RSS源URL是否有效")
    public ResponseEntity<Map<String, Boolean>> validateRssUrl(
            @Parameter(description = "RSS源URL") @RequestParam String url) {
        boolean valid = rssFeedService.validateRssUrl(url);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", valid);
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
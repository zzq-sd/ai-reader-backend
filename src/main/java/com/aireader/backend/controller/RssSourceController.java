package com.aireader.backend.controller;

import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.dto.RssSourceDTO;
import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.service.RssFeedService;
import com.aireader.backend.util.RsshubUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
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
public class RssSourceController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(RssSourceController.class);
    
    @Autowired
    private RssFeedService rssFeedService;
    
    @Value("${rsshub.instances:http://localhost:1200}")
    private String rsshubInstances;
    
    @Value("${rsshub.timeout:15000}")
    private int rsshubTimeout;
    
    /**
     * 添加新的RSS源
     * 
     * @param rssSourceDTO RSS源信息
     * @return 保存后的RSS源信息
     */
    @PostMapping
    @Operation(summary = "添加RSS源", description = "添加新的RSS源")
    public ResponseEntity<ApiResponse<RssSourceDTO>> addRssSource(@Valid @RequestBody RssSourceDTO rssSourceDTO) {
        String userId = getCurrentUserId();
        RssSourceDTO savedSource = rssFeedService.addRssSource(rssSourceDTO, userId);
        return new ResponseEntity<>(ApiResponse.success(savedSource), HttpStatus.CREATED);
    }
    
    /**
     * 获取用户的所有RSS源
     * 
     * @return RSS源列表
     */
    @GetMapping
    @Operation(summary = "获取用户的RSS源", description = "获取当前用户的所有RSS源")
    public ResponseEntity<ApiResponse<List<RssSourceDTO>>> getUserRssSources() {
        String userId = getCurrentUserId();
        List<RssSourceDTO> sources = rssFeedService.getUserRssSources(userId);
        return ResponseEntity.ok(ApiResponse.success(sources));
    }
    
    /**
     * 获取所有公共RSS源
     * 
     * @return 公共RSS源列表
     */
    @GetMapping("/public")
    @Operation(summary = "获取公共RSS源", description = "获取所有公共RSS源")
    public ResponseEntity<ApiResponse<List<RssSourceDTO>>> getPublicRssSources() {
        List<RssSourceDTO> sources = rssFeedService.getPublicRssSources();
        return ResponseEntity.ok(ApiResponse.success(sources));
    }
    
    /**
     * 根据ID获取RSS源
     * 
     * @param sourceId RSS源ID
     * @return RSS源信息
     */
    @GetMapping("/{sourceId}")
    @Operation(summary = "获取RSS源详情", description = "根据ID获取RSS源详细信息")
    public ResponseEntity<ApiResponse<RssSourceDTO>> getRssSourceById(
            @Parameter(description = "RSS源ID") @PathVariable String sourceId) {
        Optional<RssSourceDTO> source = rssFeedService.getRssSourceById(sourceId);
        return source.map(s -> ResponseEntity.ok(ApiResponse.success(s)))
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
    public ResponseEntity<ApiResponse<RssSourceDTO>> updateRssSource(
            @Parameter(description = "RSS源ID") @PathVariable String sourceId,
            @Valid @RequestBody RssSourceDTO rssSourceDTO) {
        String userId = getCurrentUserId();
        RssSourceDTO updatedSource = rssFeedService.updateRssSource(sourceId, rssSourceDTO, userId);
        return ResponseEntity.ok(ApiResponse.success(updatedSource));
    }
    
    /**
     * 删除RSS源
     * 
     * @param sourceId RSS源ID
     * @return 删除结果
     */
    @DeleteMapping("/{sourceId}")
    @Operation(summary = "删除RSS源", description = "删除指定的RSS源")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> deleteRssSource(
            @Parameter(description = "RSS源ID") @PathVariable String sourceId) {
        String userId = getCurrentUserId();
        boolean deleted = rssFeedService.deleteRssSource(sourceId, userId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", deleted);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 获取RSS源的文章列表
     * 
     * @param sourceId RSS源ID
     * @param page 页码
     * @param size 每页大小
     * @return 文章列表及分页信息
     */
    @GetMapping("/{sourceId}/articles")
    @Operation(summary = "获取RSS源文章", description = "获取指定RSS源的文章列表")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getArticlesByRssSource(
            @Parameter(description = "RSS源ID") @PathVariable String sourceId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        String userId = getCurrentUserId();
        Page<ArticleDTO> articlesPage = rssFeedService.getArticlesByRssSource(sourceId, page, size, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", articlesPage.getContent());
        response.put("totalElements", articlesPage.getTotalElements());
        response.put("totalPages", articlesPage.getTotalPages());
        response.put("size", articlesPage.getSize());
        response.put("number", articlesPage.getNumber());
        response.put("first", articlesPage.isFirst());
        response.put("last", articlesPage.isLast());
        
        return ResponseEntity.ok(ApiResponse.success(response));
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
    public ResponseEntity<ApiResponse<List<ArticleDTO>>> getLatestArticlesForUser(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        String userId = getCurrentUserId();
        List<ArticleDTO> articles = rssFeedService.getLatestArticlesForUser(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }
    
    /**
     * 手动触发RSS源的抓取
     * 
     * @param sourceId RSS源ID
     * @return 抓取结果
     */
    @PostMapping("/{sourceId}/fetch")
    @Operation(summary = "抓取RSS源", description = "手动触发指定RSS源的抓取")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fetchRssSource(
            @Parameter(description = "RSS源ID") @PathVariable String sourceId) {
        int count = rssFeedService.fetchRssSource(sourceId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", count);
        return ResponseEntity.ok(ApiResponse.success("成功抓取" + count + "篇文章", response));
    }
    
    /**
     * 检查RSS源URL是否有效
     * 
     * @param url RSS源URL
     * @return 检查结果
     */
    @GetMapping("/validate")
    @Operation(summary = "验证RSS源URL", description = "检查RSS源URL是否有效")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> validateRssUrl(
            @Parameter(description = "RSS源URL") @RequestParam String url) {
        boolean valid = rssFeedService.validateRssUrl(url);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", valid);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 从RSSHub添加RSS源
     * 
     * @param requestBody 请求体，包含route、name和category
     * @return 保存的RSS源信息
     */
    @PostMapping("/rsshub")
    @Operation(summary = "添加RSSHub源", description = "通过RSSHub路由添加RSS源")
    public ResponseEntity<ApiResponse<RssSourceDTO>> addRsshubSource(@RequestBody Map<String, Object> requestBody) {
        String route = (String) requestBody.get("route");
        String name = (String) requestBody.get("name");
        String category = (String) requestBody.get("category");
        
        if (route == null || route.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("RSSHub路由不能为空"));
        }
        
        // 验证路由格式
        if (!RsshubUtil.validateRouteFormat(route)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("RSSHub路由格式无效"));
        }
        
        try {
            String userId = getCurrentUserId();
            
            // 获取基础URL (使用第一个可用实例)
            String baseUrl = getFirstAvailableRsshubInstance();
            
            // 构建完整的RSS URL
            String fullUrl = RsshubUtil.buildRsshubUrl(baseUrl, route, null);
            
            // 创建RssSourceDTO
            RssSourceDTO sourceDTO = new RssSourceDTO();
            sourceDTO.setUrl(fullUrl);
            sourceDTO.setName(name != null && !name.isEmpty() ? name : "RSSHub: " + route);
            sourceDTO.setCategory(category != null && !category.isEmpty() ? category : "other");
            sourceDTO.setDescription("RSSHub源: " + route);
            sourceDTO.setActive(true);
            sourceDTO.setPublic(false);
            
            // 设置RSSHub特定字段
            sourceDTO.setRsshub(true);
            sourceDTO.setRsshubRoute(route);
            sourceDTO.setRsshubInstance(baseUrl);
            
            // 添加源
            RssSourceDTO savedSource = rssFeedService.addRssSource(sourceDTO, userId);
            return ResponseEntity.ok(ApiResponse.success("RSSHub源添加成功", savedSource));
        } catch (Exception e) {
            log.error("添加RSSHub源失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 添加带参数的RSSHub源
     * 
     * @param requestBody 请求体，包含route、name、category、routeParams和queryParams
     * @return 保存的RSS源信息
     */
    @PostMapping("/rsshub/advanced")
    @Operation(summary = "添加高级RSSHub源", description = "通过RSSHub路由及参数添加RSS源")
    public ResponseEntity<ApiResponse<RssSourceDTO>> addAdvancedRsshubSource(@RequestBody Map<String, Object> requestBody) {
        String route = (String) requestBody.get("route");
        String name = (String) requestBody.get("name");
        String category = (String) requestBody.get("category");
        
        @SuppressWarnings("unchecked")
        Map<String, String> routeParams = (Map<String, String>) requestBody.get("routeParams");
        @SuppressWarnings("unchecked")
        Map<String, String> queryParams = (Map<String, String>) requestBody.get("queryParams");
        
        if (route == null || route.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("RSSHub路由不能为空"));
        }
        
        try {
            String userId = getCurrentUserId();
            
            // 获取基础URL (使用第一个可用实例)
            String baseUrl = getFirstAvailableRsshubInstance();
            
            // 构建完整的RSS URL，替换路由参数和添加查询参数
            String fullUrl = RsshubUtil.buildEnhancedRsshubUrl(baseUrl, route, routeParams, queryParams);
            
            // 创建RssSourceDTO
            RssSourceDTO sourceDTO = new RssSourceDTO();
            sourceDTO.setUrl(fullUrl);
            sourceDTO.setName(name != null && !name.isEmpty() ? name : "RSSHub: " + route);
            sourceDTO.setCategory(category != null && !category.isEmpty() ? category : "other");
            sourceDTO.setDescription("RSSHub源: " + route + 
                (queryParams != null && !queryParams.isEmpty() ? " (高级参数)" : ""));
            sourceDTO.setActive(true);
            sourceDTO.setPublic(false);
            
            // 设置RSSHub特定字段
            sourceDTO.setRsshub(true);
            sourceDTO.setRsshubRoute(route);
            sourceDTO.setRsshubInstance(baseUrl);
            
            // 添加源
            RssSourceDTO savedSource = rssFeedService.addRssSource(sourceDTO, userId);
            return ResponseEntity.ok(ApiResponse.success("高级RSSHub源添加成功", savedSource));
        } catch (Exception e) {
            log.error("添加高级RSSHub源失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 验证RSSHub路由
     * 
     * @param route RSSHub路由
     * @return 验证结果
     */
    @GetMapping("/rsshub/validate")
    @Operation(summary = "验证RSSHub路由", description = "检查RSSHub路由格式是否正确")
    public ResponseEntity<ApiResponse<Boolean>> validateRsshubRoute(@RequestParam String route) {
        if (route == null || route.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("RSSHub路由不能为空"));
        }
        
        // 简化验证逻辑，主要检查格式
        try {
            boolean isValid = RsshubUtil.validateRouteFormat(route);
            return ResponseEntity.ok(ApiResponse.success(isValid));
        } catch (Exception e) {
            log.error("验证RSSHub路由失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 检查RSSHub实例健康状态
     * 
     * @param instanceUrl 实例URL，可选，默认使用配置的第一个实例
     * @return 健康状态
     */
    @GetMapping("/rsshub/health")
    @Operation(summary = "检查RSSHub实例健康状态", description = "检查指定RSSHub实例是否可访问")
    public ResponseEntity<ApiResponse<Boolean>> checkInstanceHealth(
            @RequestParam(required = false) String instanceUrl) {
        
        // 如果未提供实例URL，使用默认实例
        if (instanceUrl == null || instanceUrl.isEmpty()) {
            instanceUrl = getFirstAvailableRsshubInstance();
        }
        
        try {
            boolean isHealthy = RsshubUtil.checkInstanceHealth(instanceUrl, rsshubTimeout);
            return ResponseEntity.ok(ApiResponse.success(isHealthy));
        } catch (Exception e) {
            log.error("检查RSSHub实例健康状态失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取第一个可用的RSSHub实例
     * 
     * @return RSSHub实例URL
     */
    private String getFirstAvailableRsshubInstance() {
        // 使用配置的第一个实例，或默认本地实例
        String[] instances = rsshubInstances.split(",");
        return instances.length > 0 ? instances[0].trim() : "http://localhost:1200";
    }
} 
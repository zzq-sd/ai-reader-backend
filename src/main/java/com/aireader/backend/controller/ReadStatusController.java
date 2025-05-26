package com.aireader.backend.controller;

import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.service.ReadStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阅读状态管理控制器
 * 处理文章已读/未读状态的管理
 */
@RestController
@RequestMapping("/articles")
@Tag(name = "阅读状态管理", description = "文章已读/未读状态的管理")
public class ReadStatusController extends BaseController {

    @Autowired
    private ReadStatusService readStatusService;

    /**
     * 标记文章为已读
     * 
     * @param articleId 文章ID
     * @return 操作结果
     */
    @PatchMapping("/{articleId}/status")
    @Operation(summary = "更新文章状态", description = "标记文章为已读或未读")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateArticleStatus(
            @Parameter(description = "文章ID") @PathVariable String articleId,
            @RequestBody Map<String, String> request) {
        
        String userId = getCurrentUserId();
        String status = request.get("status");
        
        if ("read".equals(status)) {
            readStatusService.markAsRead(articleId, userId);
        } else if ("unread".equals(status)) {
            readStatusService.markAsUnread(articleId, userId);
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无效的状态值，只支持 'read' 或 'unread'"));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("articleId", articleId);
        response.put("status", status);
        response.put("success", true);
        
        return ResponseEntity.ok(ApiResponse.success("文章状态更新成功", response));
    }

    /**
     * 批量更新文章状态
     * 
     * @param request 批量更新请求
     * @return 操作结果
     */
    @PatchMapping("/batch/status")
    @Operation(summary = "批量更新文章状态", description = "批量标记文章为已读或未读")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchUpdateArticleStatus(
            @RequestBody Map<String, Object> request) {
        
        String userId = getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<String> articleIds = (List<String>) request.get("articleIds");
        String status = (String) request.get("status");
        
        if (articleIds == null || articleIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文章ID列表不能为空"));
        }
        
        if ("read".equals(status)) {
            readStatusService.markMultipleAsRead(articleIds, userId);
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("批量操作只支持标记为已读"));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("articleIds", articleIds);
        response.put("status", status);
        response.put("count", articleIds.size());
        response.put("success", true);
        
        return ResponseEntity.ok(ApiResponse.success("批量更新成功", response));
    }

    /**
     * 标记所有文章为已读
     * 
     * @return 操作结果
     */
    @PostMapping("/mark-all-read")
    @Operation(summary = "标记所有文章为已读", description = "将用户订阅的所有未读文章标记为已读")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAsRead() {
        String userId = getCurrentUserId();
        
        long unreadCountBefore = readStatusService.getUnreadCount(userId);
        readStatusService.markAllAsRead(userId);
        long unreadCountAfter = readStatusService.getUnreadCount(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("markedCount", unreadCountBefore - unreadCountAfter);
        response.put("success", true);
        
        return ResponseEntity.ok(ApiResponse.success("所有文章已标记为已读", response));
    }

    /**
     * 检查文章阅读状态
     * 
     * @param articleId 文章ID
     * @return 阅读状态
     */
    @GetMapping("/{articleId}/read-status")
    @Operation(summary = "检查文章阅读状态", description = "检查指定文章是否已被用户阅读")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkReadStatus(
            @Parameter(description = "文章ID") @PathVariable String articleId) {
        
        String userId = getCurrentUserId();
        boolean isRead = readStatusService.isRead(articleId, userId);
        
        Map<String, Boolean> response = Collections.singletonMap("isRead", isRead);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取已读文章列表
     * 
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 已读文章列表
     */
    @GetMapping("/read")
    @Operation(summary = "获取已读文章", description = "分页获取用户已读的文章列表")
    public ResponseEntity<ApiResponse<Page<ArticleDTO>>> getReadArticles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        
        String userId = getCurrentUserId();
        Page<ArticleDTO> readArticles = readStatusService.getReadArticles(userId, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(readArticles));
    }

    /**
     * 获取未读文章列表
     * 
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 未读文章列表
     */
    @GetMapping("/unread")
    @Operation(summary = "获取未读文章", description = "分页获取用户未读的文章列表")
    public ResponseEntity<ApiResponse<Page<ArticleDTO>>> getUnreadArticles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        
        String userId = getCurrentUserId();
        Page<ArticleDTO> unreadArticles = readStatusService.getUnreadArticles(userId, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(unreadArticles));
    }

    /**
     * 获取阅读统计
     * 
     * @return 阅读统计信息
     */
    @GetMapping("/read-stats")
    @Operation(summary = "获取阅读统计", description = "获取用户的阅读统计信息")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getReadStats() {
        String userId = getCurrentUserId();
        
        long readCount = readStatusService.getReadCount(userId);
        long unreadCount = readStatusService.getUnreadCount(userId);
        long totalCount = readCount + unreadCount;
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("readCount", readCount);
        stats.put("unreadCount", unreadCount);
        stats.put("totalCount", totalCount);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
} 
package com.aireader.backend.controller;

import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏管理控制器
 */
@RestController
@RequestMapping("/favorites")
@Tag(name = "收藏管理", description = "文章收藏的添加、删除和查询")
public class FavoriteController extends BaseController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 添加文章到收藏
     * 
     * @param articleId 文章ID
     * @return 添加结果
     */
    @PostMapping("/{articleId}")
    @Operation(summary = "添加收藏", description = "将文章添加到收藏")
    public ResponseEntity<ApiResponse> addFavorite(@PathVariable String articleId) {
        String userId = getCurrentUserId();
        favoriteService.addFavorite(articleId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("文章已添加到收藏", null));
    }

    /**
     * 从收藏中移除文章
     * 
     * @param articleId 文章ID
     * @return 移除结果
     */
    @DeleteMapping("/{articleId}")
    @Operation(summary = "移除收藏", description = "从收藏中移除文章")
    public ResponseEntity<ApiResponse> removeFavorite(@PathVariable String articleId) {
        String userId = getCurrentUserId();
        favoriteService.removeFavorite(articleId, userId);
        return ResponseEntity.ok(ApiResponse.success("文章已从收藏中移除", null));
    }

    /**
     * 检查文章是否已收藏
     * 
     * @param articleId 文章ID
     * @return 是否已收藏
     */
    @GetMapping("/{articleId}/status")
    @Operation(summary = "检查收藏状态", description = "检查文章是否已收藏")
    public ResponseEntity<ApiResponse> checkFavoriteStatus(@PathVariable String articleId) {
        String userId = getCurrentUserId();
        boolean isFavorited = favoriteService.isFavorite(articleId, userId);
        Map<String, Boolean> data = Collections.singletonMap("isFavorited", isFavorited);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 获取收藏列表（分页）
     * 
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 收藏的文章列表
     */
    @GetMapping
    @Operation(summary = "获取收藏列表", description = "分页获取收藏的文章列表")
    public ResponseEntity<ApiResponse> getFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = getCurrentUserId();
        Page<ArticleDTO> favorites = favoriteService.getFavorites(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }

    /**
     * 获取最近收藏的文章
     * 
     * @param limit 限制数量
     * @return 最近收藏的文章列表
     */
    @GetMapping("/recent")
    @Operation(summary = "获取最近收藏", description = "获取最近收藏的文章")
    public ResponseEntity<ApiResponse> getRecentFavorites(
            @RequestParam(defaultValue = "5") int limit) {
        String userId = getCurrentUserId();
        List<ArticleDTO> recentFavorites = favoriteService.getRecentFavorites(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(recentFavorites));
    }

    /**
     * 根据标签查询收藏的文章
     * 
     * @param tagId 标签ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 带有指定标签的收藏文章列表
     */
    @GetMapping("/tags/{tagId}")
    @Operation(summary = "按标签查询收藏", description = "查询带有指定标签的收藏文章")
    public ResponseEntity<ApiResponse> getFavoritesByTag(
            @PathVariable String tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = getCurrentUserId();
        Page<ArticleDTO> favorites = favoriteService.getFavoritesByTag(userId, tagId, page, size);
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }

    /**
     * 搜索收藏的文章
     * 
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 符合搜索条件的收藏文章列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索收藏", description = "搜索收藏的文章")
    public ResponseEntity<ApiResponse> searchFavorites(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = getCurrentUserId();
        Page<ArticleDTO> searchResults = favoriteService.searchFavorites(userId, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(searchResults));
    }

    /**
     * 获取收藏文章数量
     * 
     * @return 收藏文章数量
     */
    @GetMapping("/count")
    @Operation(summary = "获取收藏数量", description = "获取收藏文章的总数")
    public ResponseEntity<ApiResponse> getFavoritesCount() {
        String userId = getCurrentUserId();
        long count = favoriteService.getFavoritesCount(userId);
        Map<String, Long> data = Collections.singletonMap("count", count);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
} 
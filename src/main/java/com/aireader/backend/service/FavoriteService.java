package com.aireader.backend.service;

import com.aireader.backend.dto.ArticleDTO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 收藏管理服务接口
 * 提供文章收藏、取消收藏和收藏列表查询功能
 */
public interface FavoriteService {
    
    /**
     * 添加文章到收藏
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    void addFavorite(String articleId, String userId);
    
    /**
     * 从收藏中移除文章
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    void removeFavorite(String articleId, String userId);
    
    /**
     * 检查文章是否已被用户收藏
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 如果已收藏返回true，否则返回false
     */
    boolean isFavorite(String articleId, String userId);
    
    /**
     * 获取用户收藏的文章列表（分页）
     * 
     * @param userId 用户ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 收藏的文章分页列表
     */
    Page<ArticleDTO> getFavorites(String userId, int page, int size);
    
    /**
     * 获取用户最近收藏的文章列表
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 最近收藏的文章列表
     */
    List<ArticleDTO> getRecentFavorites(String userId, int limit);
    
    /**
     * 根据标签查询收藏的文章
     * 
     * @param userId 用户ID
     * @param tagId 标签ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 带有指定标签的收藏文章分页列表
     */
    Page<ArticleDTO> getFavoritesByTag(String userId, String tagId, int page, int size);
    
    /**
     * 搜索收藏的文章
     * 
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 符合搜索条件的收藏文章分页列表
     */
    Page<ArticleDTO> searchFavorites(String userId, String keyword, int page, int size);
    
    /**
     * 获取用户收藏的文章数量
     * 
     * @param userId 用户ID
     * @return 收藏文章数量
     */
    long getFavoritesCount(String userId);
} 
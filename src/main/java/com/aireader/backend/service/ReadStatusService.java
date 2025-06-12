package com.aireader.backend.service;

import com.aireader.backend.dto.ArticleDTO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 阅读状态管理服务接口
 * 提供文章已读/未读状态的管理功能
 */
public interface ReadStatusService {
    
    /**
     * 标记文章为已读
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    void markAsRead(String articleId, String userId);
    
    /**
     * 标记文章为未读
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    void markAsUnread(String articleId, String userId);
    
    /**
     * 批量标记文章为已读
     * 
     * @param articleIds 文章ID列表
     * @param userId 用户ID
     */
    void markMultipleAsRead(List<String> articleIds, String userId);
    
    /**
     * 标记所有文章为已读
     * 
     * @param userId 用户ID
     */
    void markAllAsRead(String userId);
    
    /**
     * 检查文章是否已被用户阅读
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 如果已阅读返回true，否则返回false
     */
    boolean isRead(String articleId, String userId);
    
    /**
     * 获取用户已读的文章列表（分页）
     * 
     * @param userId 用户ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 已读的文章分页列表
     */
    Page<ArticleDTO> getReadArticles(String userId, int page, int size);
    
    /**
     * 获取用户未读的文章列表（分页）
     * 
     * @param userId 用户ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 未读的文章分页列表
     */
    Page<ArticleDTO> getUnreadArticles(String userId, int page, int size);
    
    /**
     * 获取用户已读文章数量
     * 
     * @param userId 用户ID
     * @return 已读文章数量
     */
    long getReadCount(String userId);
    
    /**
     * 获取用户未读文章数量
     * 
     * @param userId 用户ID
     * @return 未读文章数量
     */
    long getUnreadCount(String userId);
} 
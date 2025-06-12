package com.aireader.backend.service;

import com.aireader.backend.dto.RssSourceDTO;
import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.model.jpa.RssSource;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

/**
 * RSS源管理服务接口
 * 提供RSS源的增删改查和文章获取功能
 */
public interface RssFeedService {
    
    /**
     * 添加新的RSS源
     * 
     * @param rssSourceDTO RSS源信息
     * @param userId 用户ID
     * @return 保存后的RSS源信息
     */
    RssSourceDTO addRssSource(RssSourceDTO rssSourceDTO, String userId);
    
    /**
     * 获取用户的所有RSS源
     * 
     * @param userId 用户ID
     * @return RSS源列表
     */
    List<RssSourceDTO> getUserRssSources(String userId);
    
    /**
     * 获取所有公共RSS源
     * 
     * @return 公共RSS源列表
     */
    List<RssSourceDTO> getPublicRssSources();
    
    /**
     * 根据ID获取RSS源
     * 
     * @param sourceId RSS源ID
     * @return 可选的RSS源
     */
    Optional<RssSourceDTO> getRssSourceById(String sourceId);
    
    /**
     * 更新RSS源信息
     * 
     * @param sourceId RSS源ID
     * @param rssSourceDTO 更新的RSS源信息
     * @param userId 用户ID
     * @return 更新后的RSS源信息
     */
    RssSourceDTO updateRssSource(String sourceId, RssSourceDTO rssSourceDTO, String userId);
    
    /**
     * 删除RSS源
     * 
     * @param sourceId RSS源ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteRssSource(String sourceId, String userId);
    
    /**
     * 删除RSS源（管理员权限或内部调用）
     * 
     * @param sourceId RSS源ID
     * @return 是否删除成功
     */
    boolean deleteRssSource(String sourceId);
    
    /**
     * 获取RSS源的文章列表
     * 
     * @param sourceId RSS源ID
     * @param page 页码
     * @param size 每页大小
     * @param userId 用户ID，用于获取已读状态
     * @return 文章列表
     */
    Page<ArticleDTO> getArticlesByRssSource(String sourceId, int page, int size, String userId);
    
    /**
     * 获取用户订阅的所有RSS源的最新文章
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 文章列表
     */
    List<ArticleDTO> getLatestArticlesForUser(String userId, int page, int size);
    
    /**
     * 手动触发RSS源的抓取
     * 
     * @param sourceId RSS源ID
     * @return 抓取的文章数量
     */
    int fetchRssSource(String sourceId);
    
    /**
     * 检查RSS源URL是否有效
     * 
     * @param url RSS源URL
     * @return 是否有效
     */
    boolean validateRssUrl(String url);
    
    /**
     * 将实体转换为DTO
     * 
     * @param rssSource RSS源实体
     * @return RSS源DTO
     */
    RssSourceDTO convertToDto(RssSource rssSource);
    
    /**
     * 将DTO转换为实体
     * 
     * @param rssSourceDTO RSS源DTO
     * @return RSS源实体
     */
    RssSource convertToEntity(RssSourceDTO rssSourceDTO);
    
    /**
     * 将RSS源添加到抓取队列中，由消息消费者异步处理
     *
     * @param sourceId RSS源ID
     */
    void scheduleRssFetch(String sourceId);
} 
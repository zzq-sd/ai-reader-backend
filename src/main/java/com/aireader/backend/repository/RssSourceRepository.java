package com.aireader.backend.repository;

import com.aireader.backend.model.jpa.RssSource;
import com.aireader.backend.model.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RSS源信息仓库接口
 */
@Repository
public interface RssSourceRepository extends JpaRepository<RssSource, UUID> {
    
    /**
     * 根据URL查找RSS源
     * 
     * @param url RSS源URL
     * @return RSS源对象
     */
    Optional<RssSource> findByUrl(String url);
    
    /**
     * 根据URL和用户查找RSS源
     * 
     * @param url  RSS源URL
     * @param user 用户对象
     * @return RSS源对象
     */
    Optional<RssSource> findByUrlAndUser(String url, User user);
    
    /**
     * 根据用户查找所有RSS源
     * 
     * @param user 用户对象
     * @return RSS源列表
     */
    List<RssSource> findByUser(User user);
    
    /**
     * 根据用户ID查找所有RSS源
     * 
     * @param userId 用户ID
     * @return RSS源列表
     */
    List<RssSource> findByUserId(UUID userId);
    
    /**
     * 查找所有公共RSS源
     * 
     * @return RSS源列表
     */
    List<RssSource> findByIsPublicTrue();
    
    /**
     * 根据用户查找所有RSS源（分页）
     * 
     * @param user     用户对象
     * @param pageable 分页参数
     * @return RSS源分页对象
     */
    Page<RssSource> findByUser(User user, Pageable pageable);
    
    /**
     * 根据分类和用户查找RSS源
     * 
     * @param category 分类
     * @param user     用户对象
     * @return RSS源列表
     */
    List<RssSource> findByCategoryAndUser(String category, User user);
    
    /**
     * 查找所有需要更新的RSS源
     * 
     * @param lastFetchedBefore 上次抓取时间早于该时间的RSS源需要更新
     * @return 需要更新的RSS源列表
     */
    List<RssSource> findByLastFetchedAtBeforeOrLastFetchedAtIsNull(LocalDateTime lastFetchedBefore);
} 
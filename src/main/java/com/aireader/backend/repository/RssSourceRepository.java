package com.aireader.backend.repository;

import com.aireader.backend.model.jpa.RssSource;
import com.aireader.backend.model.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * RSS源信息仓库接口
 */
@Repository
public interface RssSourceRepository extends JpaRepository<RssSource, String> {
    
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
    @Query("SELECT r FROM RssSource r WHERE r.user.id = :userId")
    List<RssSource> findByUserId(@Param("userId") String userId);
    
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
    
    /**
     * 更新RSS源的RSSHub相关字段
     * 
     * @param id RSS源ID
     * @param isRsshub 是否为RSSHub源
     * @param rsshubRoute RSSHub路由
     * @param rsshubInstance RSSHub实例URL
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE RssSource r SET r.isRsshub = :isRsshub, r.rsshubRoute = :rsshubRoute, r.rsshubInstance = :rsshubInstance WHERE r.id = :id")
    int updateRsshubFields(@Param("id") String id, 
                          @Param("isRsshub") boolean isRsshub,
                          @Param("rsshubRoute") String rsshubRoute, 
                          @Param("rsshubInstance") String rsshubInstance);
    
    /**
     * 查找所有RSSHub源
     * 
     * @return RSSHub源列表
     */
    List<RssSource> findByIsRsshubTrue();
    
    /**
     * 根据用户查找所有RSSHub源
     * 
     * @param user 用户对象
     * @return RSSHub源列表
     */
    List<RssSource> findByIsRsshubTrueAndUser(User user);
} 
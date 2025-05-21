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

/**
 * RSS源数据访问接口
 */
@Repository
public interface RssSourceRepository extends JpaRepository<RssSource, String> {

    /**
     * 查找用户的所有RSS源
     *
     * @param user 用户实体
     * @return RSS源列表
     */
    List<RssSource> findByUser(User user);

    /**
     * 分页查找用户的所有RSS源
     *
     * @param user 用户实体
     * @param pageable 分页参数
     * @return RSS源分页结果
     */
    Page<RssSource> findByUser(User user, Pageable pageable);

    /**
     * 查找用户的所有RSS源，按创建时间倒序排列
     *
     * @param user 用户实体
     * @return RSS源列表
     */
    List<RssSource> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 查找用户的指定URL的RSS源
     *
     * @param url RSS源URL
     * @param user 用户实体
     * @return RSS源的Optional包装
     */
    Optional<RssSource> findByUrlAndUser(String url, User user);

    /**
     * 检查用户是否已添加指定URL的RSS源
     *
     * @param url RSS源URL
     * @param user 用户实体
     * @return 是否存在
     */
    boolean existsByUrlAndUser(String url, User user);

    /**
     * 查找需要更新的RSS源（活跃且上次抓取时间早于指定时间或者从未抓取）
     *
     * @param lastFetchedBefore 上次抓取时间早于此时间
     * @param pageable 分页参数
     * @return 需要更新的RSS源
     */
    Page<RssSource> findByActiveIsTrueAndLastFetchedAtBeforeOrLastFetchedAtIsNull(
            LocalDateTime lastFetchedBefore, Pageable pageable);
} 
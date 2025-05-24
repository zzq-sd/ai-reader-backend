package com.aireader.backend.repository;

import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.Note;
import com.aireader.backend.model.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 笔记仓库接口
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, String> {
    
    /**
     * 根据用户查找笔记（分页）
     * 
     * @param user     用户对象
     * @param pageable 分页参数
     * @return 笔记分页对象
     */
    Page<Note> findByUser(User user, Pageable pageable);
    
    /**
     * 根据文章元数据查找笔记
     * 
     * @param articleMetadata 文章元数据对象
     * @return 笔记列表
     */
    List<Note> findByArticleMetadata(ArticleMetadata articleMetadata);
    
    /**
     * 根据文章元数据查找笔记（分页）
     * 
     * @param articleMetadata 文章元数据对象
     * @param pageable 分页参数
     * @return 笔记分页对象
     */
    Page<Note> findByArticleMetadata(ArticleMetadata articleMetadata, Pageable pageable);
    
    /**
     * 根据用户和文章元数据查找笔记
     * 
     * @param user            用户对象
     * @param articleMetadata 文章元数据对象
     * @return 笔记对象
     */
    Optional<Note> findByUserAndArticleMetadata(User user, ArticleMetadata articleMetadata);
    
    /**
     * 根据用户和文章元数据查找笔记（分页）
     * 
     * @param user            用户对象
     * @param articleMetadata 文章元数据对象
     * @param pageable        分页参数
     * @return 笔记分页对象
     */
    Page<Note> findByUserAndArticleMetadata(User user, ArticleMetadata articleMetadata, Pageable pageable);
    
    /**
     * 查找需要AI处理的笔记
     * 
     * @param status   AI处理状态
     * @param pageable 分页参数
     * @return 笔记分页对象
     */
    Page<Note> findByAiProcessingStatus(String status, Pageable pageable);
    
    /**
     * 全文搜索用户笔记
     * 
     * @param user     用户对象
     * @param query    搜索关键词
     * @param pageable 分页参数
     * @return 笔记分页对象
     */
    @Query("SELECT n FROM Note n WHERE n.user = :user AND " +
            "(LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(n.contentRichText) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Note> searchByUserAndKeyword(@Param("user") User user, @Param("query") String query, Pageable pageable);
    
    /**
     * 根据标签名称查找用户笔记
     * 
     * @param user     用户对象
     * @param tagName  标签名称
     * @param pageable 分页参数
     * @return 笔记分页对象
     */
    @Query("SELECT n FROM Note n JOIN n.tags t WHERE n.user = :user AND LOWER(t.name) = LOWER(:tagName)")
    Page<Note> findByUserAndTagName(@Param("user") User user, @Param("tagName") String tagName, Pageable pageable);
    
    /**
     * 按照标题搜索用户笔记
     * 
     * @param user 用户
     * @param titleKeyword 标题关键词
     * @param pageable 分页参数
     * @return 分页笔记列表
     */
    Page<Note> findByUserAndTitleContainingIgnoreCase(User user, String titleKeyword, Pageable pageable);
    
    /**
     * 查找最近更新的笔记
     * 
     * @param user 用户
     * @param limit 限制条数
     * @return 笔记列表
     */
    @Query("SELECT n FROM Note n WHERE n.user = :user ORDER BY n.updatedAt DESC")
    List<Note> findRecentlyUpdatedNotes(@Param("user") User user, Pageable pageable);
    
    /**
     * 检查笔记是否属于特定用户
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 笔记Optional包装
     */
    @Query("SELECT n FROM Note n WHERE n.id = :noteId AND n.user.id = :userId")
    Optional<Note> findByIdAndUserId(@Param("noteId") String noteId, @Param("userId") String userId);
    
    /**
     * 查询特定用户特定时间之后创建的笔记
     * 
     * @param user 用户
     * @param createdAfter 创建时间下限
     * @param pageable 分页参数
     * @return 分页笔记列表
     */
    Page<Note> findByUserAndCreatedAtAfter(User user, LocalDateTime createdAfter, Pageable pageable);
} 
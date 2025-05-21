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
 * 笔记资源库接口
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, String> {
    
    /**
     * 查找用户的所有笔记
     * 
     * @param user 用户
     * @param pageable 分页参数
     * @return 分页笔记列表
     */
    Page<Note> findByUser(User user, Pageable pageable);
    
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
     * 查找与指定文章相关的笔记
     * 
     * @param user 用户
     * @param articleMetadata 文章元数据
     * @return 笔记列表
     */
    List<Note> findByUserAndArticleMetadata(User user, ArticleMetadata articleMetadata);
    
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
     * 查找待处理AI分析的笔记
     * 
     * @param status AI处理状态
     * @param limit 限制条数
     * @return 笔记列表
     */
    List<Note> findByAiProcessingStatus(Note.AiProcessingStatus status, Pageable pageable);
    
    /**
     * 检查笔记是否属于特定用户
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 笔记Optional包装
     */
    Optional<Note> findByIdAndUserId(String noteId, String userId);
    
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
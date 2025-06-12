package com.aireader.backend.repository;

import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.ArticleTag;
import com.aireader.backend.model.jpa.ArticleTag.ArticleTagId;
import com.aireader.backend.model.jpa.Tag;
import com.aireader.backend.model.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 文章标签关联仓库接口
 */
@Repository
public interface ArticleTagRepository extends JpaRepository<ArticleTag, ArticleTagId> {
    
    /**
     * 根据文章元数据查找所有标签关联
     * 
     * @param articleMetadata 文章元数据对象
     * @return 文章标签关联列表
     */
    List<ArticleTag> findByArticleMetadata(ArticleMetadata articleMetadata);
    
    /**
     * 根据文章元数据ID查找所有标签关联
     * 
     * @param articleMetadataId 文章元数据ID
     * @return 文章标签关联列表
     */
    @Query("SELECT at FROM ArticleTag at WHERE at.articleMetadata.id = :articleMetadataId")
    List<ArticleTag> findByArticleMetadataId(@Param("articleMetadataId") String articleMetadataId);
    
    /**
     * 根据标签查找所有文章关联
     * 
     * @param tag 标签对象
     * @return 文章标签关联列表
     */
    List<ArticleTag> findByTag(Tag tag);
    
    /**
     * 根据用户查找所有文章标签关联
     * 
     * @param user 用户对象
     * @return 文章标签关联列表
     */
    List<ArticleTag> findByUser(User user);
    
    /**
     * 根据用户和标签查找文章关联
     * 
     * @param user 用户对象
     * @param tag 标签对象
     * @return 文章标签关联列表
     */
    List<ArticleTag> findByUserAndTag(User user, Tag tag);
    
    /**
     * 根据用户、文章和标签查找关联
     * 
     * @param user 用户对象
     * @param articleMetadata 文章元数据对象
     * @param tag 标签对象
     * @return 文章标签关联对象
     */
    Optional<ArticleTag> findByUserAndArticleMetadataAndTag(User user, ArticleMetadata articleMetadata, Tag tag);
    
    /**
     * 根据文章元数据删除所有相关的标签关联
     * 
     * @param articleMetadata 文章元数据对象
     */
    void deleteByArticleMetadata(ArticleMetadata articleMetadata);
    
    /**
     * 根据文章元数据ID删除所有相关的标签关联
     * 
     * @param articleMetadataId 文章元数据ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ArticleTag at WHERE at.articleMetadata.id = :articleMetadataId")
    void deleteByArticleMetadataId(@Param("articleMetadataId") String articleMetadataId);
    
    /**
     * 根据文章元数据ID列表批量删除所有相关的标签关联
     * 
     * @param articleMetadataIds 文章元数据ID列表
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ArticleTag at WHERE at.articleMetadata.id IN :articleMetadataIds")
    void deleteByArticleMetadataIdIn(@Param("articleMetadataIds") List<String> articleMetadataIds);
    
    /**
     * 根据文章元数据对象列表批量删除所有相关的标签关联
     * 
     * @param articleMetadataList 文章元数据对象列表
     */
    void deleteByArticleMetadataIn(List<ArticleMetadata> articleMetadataList);
    
    /**
     * 根据标签删除所有相关的文章关联
     * 
     * @param tag 标签对象
     */
    void deleteByTag(Tag tag);
    
    /**
     * 根据用户删除所有相关的文章标签关联
     * 
     * @param user 用户对象
     */
    void deleteByUser(User user);
    
    /**
     * 统计文章的标签数量
     * 
     * @param articleMetadata 文章元数据对象
     * @return 标签数量
     */
    long countByArticleMetadata(ArticleMetadata articleMetadata);
    
    /**
     * 统计标签关联的文章数量
     * 
     * @param tag 标签对象
     * @return 文章数量
     */
    long countByTag(Tag tag);
    
    /**
     * 统计用户创建的文章标签关联数量
     * 
     * @param user 用户对象
     * @return 关联数量
     */
    long countByUser(User user);
} 
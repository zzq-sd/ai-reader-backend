package com.aireader.backend.service;

import com.aireader.backend.dto.NoteRequestDto;
import com.aireader.backend.dto.NoteResponseDto;
import com.aireader.backend.dto.NoteAnalysisResultDto;
import com.aireader.backend.dto.PageResponseDto;
import com.aireader.backend.dto.TagDto;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 笔记服务接口
 * 提供笔记的创建、查询、更新和删除功能
 */
public interface NoteService {
    
    /**
     * 创建笔记
     * 
     * @param noteRequest 笔记请求
     * @param userId 用户ID
     * @return 创建的笔记
     */
    NoteResponseDto createNote(NoteRequestDto noteRequest, String userId);
    
    /**
     * 更新笔记
     * 
     * @param noteId 笔记ID
     * @param noteRequest 笔记请求
     * @param userId 用户ID
     * @return 更新后的笔记
     */
    NoteResponseDto updateNote(String noteId, NoteRequestDto noteRequest, String userId);
    
    /**
     * 删除笔记
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteNote(String noteId, String userId);
    
    /**
     * 根据ID获取笔记
     * 
     * @param noteId 笔记ID
     * @return 笔记
     */
    Optional<NoteResponseDto> getNoteById(String noteId);
    
    /**
     * 获取用户的所有笔记
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页的笔记列表
     */
    PageResponseDto<NoteResponseDto> getUserNotes(String userId, int page, int size);
    
    /**
     * 获取文章的所有笔记
     * 
     * @param articleId 文章ID
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 笔记列表
     */
    List<NoteResponseDto> getArticleNotes(String articleId, String userId, int page, int size);
    
    /**
     * 获取标签下的所有笔记
     * 
     * @param tagId 标签ID
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 笔记列表
     */
    List<NoteResponseDto> getNotesByTag(String tagId, String userId, int page, int size);
    
    /**
     * 搜索笔记
     * 
     * @param keyword 关键词
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页的笔记列表
     */
    PageResponseDto<NoteResponseDto> searchNotes(String keyword, String userId, int page, int size);
    
    /**
     * 获取用户的所有标签
     * 
     * @param userId 用户ID
     * @return 标签列表
     */
    List<TagDto> getUserTags(String userId);
    
    /**
     * 创建标签
     * 
     * @param tagDto 标签DTO
     * @param userId 用户ID
     * @return 创建的标签
     */
    TagDto createTag(TagDto tagDto, String userId);
    
    /**
     * 更新标签
     * 
     * @param tagId 标签ID
     * @param tagDto 标签DTO
     * @param userId 用户ID
     * @return 更新后的标签
     */
    TagDto updateTag(String tagId, TagDto tagDto, String userId);
    
    /**
     * 删除标签
     * 
     * @param tagId 标签ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteTag(String tagId, String userId);
    
    /**
     * 为笔记添加标签
     * 
     * @param noteId 笔记ID
     * @param tagIds 标签ID集合
     * @param userId 用户ID
     * @return 更新后的笔记
     */
    NoteResponseDto addTagsToNote(String noteId, Set<String> tagIds, String userId);
    
    /**
     * 从笔记中移除标签
     * 
     * @param noteId 笔记ID
     * @param tagIds 标签ID集合
     * @param userId 用户ID
     * @return 更新后的笔记
     */
    NoteResponseDto removeTagsFromNote(String noteId, Set<String> tagIds, String userId);
    
    /**
     * 获取笔记的AI分析结果
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return AI分析结果
     */
    NoteAnalysisResultDto getNoteAnalysisResult(String noteId, String userId);
    
    /**
     * 手动触发笔记重新分析
     * 
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 是否成功提交分析任务
     */
    boolean reanalyzeNote(String noteId, String userId);
} 
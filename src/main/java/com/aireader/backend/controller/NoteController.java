package com.aireader.backend.controller;

import com.aireader.backend.controller.BaseController;
import com.aireader.backend.dto.NoteRequestDto;
import com.aireader.backend.dto.NoteResponseDto;
import com.aireader.backend.dto.PageResponseDto;
import com.aireader.backend.dto.TagDto;
import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 笔记控制器
 * 处理笔记相关的请求
 */
@RestController
@RequestMapping("/notes")
@Tag(name = "笔记管理", description = "笔记的创建、查询、更新和删除")
public class NoteController extends BaseController {
    
    @Autowired
    private NoteService noteService;
    
    /**
     * 创建笔记
     * 
     * @param noteRequest 笔记请求
     * @return 创建的笔记
     */
    @PostMapping
    @Operation(summary = "创建笔记", description = "创建新的笔记")
    public ResponseEntity<ApiResponse<NoteResponseDto>> createNote(@Valid @RequestBody NoteRequestDto noteRequest) {
        String userId = getCurrentUserId();
        NoteResponseDto createdNote = noteService.createNote(noteRequest, userId);
        return new ResponseEntity<>(ApiResponse.success(createdNote), HttpStatus.CREATED);
    }
    
    /**
     * 更新笔记
     * 
     * @param noteId 笔记ID
     * @param noteRequest 笔记请求
     * @return 更新后的笔记
     */
    @PutMapping("/{noteId}")
    @Operation(summary = "更新笔记", description = "更新指定的笔记")
    public ResponseEntity<ApiResponse<NoteResponseDto>> updateNote(
            @Parameter(description = "笔记ID") @PathVariable String noteId,
            @Valid @RequestBody NoteRequestDto noteRequest) {
        String userId = getCurrentUserId();
        NoteResponseDto updatedNote = noteService.updateNote(noteId, noteRequest, userId);
        return ResponseEntity.ok(ApiResponse.success(updatedNote));
    }
    
    /**
     * 删除笔记
     * 
     * @param noteId 笔记ID
     * @return 删除结果
     */
    @DeleteMapping("/{noteId}")
    @Operation(summary = "删除笔记", description = "删除指定的笔记")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> deleteNote(
            @Parameter(description = "笔记ID") @PathVariable String noteId) {
        String userId = getCurrentUserId();
        boolean deleted = noteService.deleteNote(noteId, userId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", deleted);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 根据ID获取笔记
     * 
     * @param noteId 笔记ID
     * @return 笔记
     */
    @GetMapping("/{noteId}")
    @Operation(summary = "获取笔记", description = "根据ID获取笔记")
    public ResponseEntity<ApiResponse<NoteResponseDto>> getNoteById(
            @Parameter(description = "笔记ID") @PathVariable String noteId) {
        return noteService.getNoteById(noteId)
                .map(note -> ResponseEntity.ok(ApiResponse.success(note)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 获取用户的所有笔记
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 笔记列表
     */
    @GetMapping
    @Operation(summary = "获取用户笔记", description = "获取当前用户的所有笔记")
    public ResponseEntity<ApiResponse<PageResponseDto<NoteResponseDto>>> getUserNotes(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        String userId = getCurrentUserId();
        PageResponseDto<NoteResponseDto> notes = noteService.getUserNotes(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }
    
    /**
     * 获取文章的所有笔记
     * 
     * @param articleId 文章ID
     * @param page 页码
     * @param size 每页大小
     * @return 笔记列表
     */
    @GetMapping("/article/{articleId}")
    @Operation(summary = "获取文章笔记", description = "获取指定文章的所有笔记")
    public ResponseEntity<ApiResponse<List<NoteResponseDto>>> getArticleNotes(
            @Parameter(description = "文章ID") @PathVariable String articleId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        String userId = getCurrentUserId();
        List<NoteResponseDto> notes = noteService.getArticleNotes(articleId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }
    
    /**
     * 获取标签下的所有笔记
     * 
     * @param tagId 标签ID
     * @param page 页码
     * @param size 每页大小
     * @return 笔记列表
     */
    @GetMapping("/tag/{tagId}")
    @Operation(summary = "获取标签笔记", description = "获取指定标签下的所有笔记")
    public ResponseEntity<ApiResponse<List<NoteResponseDto>>> getNotesByTag(
            @Parameter(description = "标签ID") @PathVariable String tagId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        String userId = getCurrentUserId();
        List<NoteResponseDto> notes = noteService.getNotesByTag(tagId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }
    
    /**
     * 搜索笔记
     * 
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 笔记列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索笔记", description = "根据关键词搜索笔记")
    public ResponseEntity<ApiResponse<PageResponseDto<NoteResponseDto>>> searchNotes(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        String userId = getCurrentUserId();
        PageResponseDto<NoteResponseDto> notes = noteService.searchNotes(keyword, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }
    
    /**
     * 获取用户的所有标签
     * 
     * @return 标签列表
     */
    @GetMapping("/tags")
    @Operation(summary = "获取用户标签", description = "获取当前用户的所有标签")
    public ResponseEntity<ApiResponse<List<TagDto>>> getUserTags() {
        String userId = getCurrentUserId();
        List<TagDto> tags = noteService.getUserTags(userId);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }
    
    /**
     * 创建标签
     * 
     * @param tagDto 标签DTO
     * @return 创建的标签
     */
    @PostMapping("/tags")
    @Operation(summary = "创建标签", description = "创建新的标签")
    public ResponseEntity<ApiResponse<TagDto>> createTag(@Valid @RequestBody TagDto tagDto) {
        String userId = getCurrentUserId();
        TagDto createdTag = noteService.createTag(tagDto, userId);
        return new ResponseEntity<>(ApiResponse.success(createdTag), HttpStatus.CREATED);
    }
    
    /**
     * 更新标签
     * 
     * @param tagId 标签ID
     * @param tagDto 标签DTO
     * @return 更新后的标签
     */
    @PutMapping("/tags/{tagId}")
    @Operation(summary = "更新标签", description = "更新指定的标签")
    public ResponseEntity<ApiResponse<TagDto>> updateTag(
            @Parameter(description = "标签ID") @PathVariable String tagId,
            @Valid @RequestBody TagDto tagDto) {
        String userId = getCurrentUserId();
        TagDto updatedTag = noteService.updateTag(tagId, tagDto, userId);
        return ResponseEntity.ok(ApiResponse.success(updatedTag));
    }
    
    /**
     * 删除标签
     * 
     * @param tagId 标签ID
     * @return 删除结果
     */
    @DeleteMapping("/tags/{tagId}")
    @Operation(summary = "删除标签", description = "删除指定的标签")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> deleteTag(
            @Parameter(description = "标签ID") @PathVariable String tagId) {
        String userId = getCurrentUserId();
        boolean deleted = noteService.deleteTag(tagId, userId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", deleted);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 为笔记添加标签
     * 
     * @param noteId 笔记ID
     * @param tagIds 标签ID集合
     * @return 更新后的笔记
     */
    @PostMapping("/{noteId}/tags")
    @Operation(summary = "添加标签", description = "为笔记添加标签")
    public ResponseEntity<ApiResponse<NoteResponseDto>> addTagsToNote(
            @Parameter(description = "笔记ID") @PathVariable String noteId,
            @RequestBody Set<String> tagIds) {
        String userId = getCurrentUserId();
        NoteResponseDto updatedNote = noteService.addTagsToNote(noteId, tagIds, userId);
        return ResponseEntity.ok(ApiResponse.success(updatedNote));
    }
    
    /**
     * 从笔记中移除标签
     * 
     * @param noteId 笔记ID
     * @param tagIds 标签ID集合
     * @return 更新后的笔记
     */
    @DeleteMapping("/{noteId}/tags")
    @Operation(summary = "移除标签", description = "从笔记中移除标签")
    public ResponseEntity<ApiResponse<NoteResponseDto>> removeTagsFromNote(
            @Parameter(description = "笔记ID") @PathVariable String noteId,
            @RequestBody Set<String> tagIds) {
        String userId = getCurrentUserId();
        NoteResponseDto updatedNote = noteService.removeTagsFromNote(noteId, tagIds, userId);
        return ResponseEntity.ok(ApiResponse.success(updatedNote));
    }
} 
package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 笔记响应DTO
 * 用于向前端返回笔记信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteResponseDto {
    
    /**
     * 笔记ID
     */
    private String id;
    
    /**
     * 笔记标题
     */
    private String title;
    
    /**
     * 笔记内容
     */
    private String content;
    
    /**
     * 笔记作者ID
     */
    private String userId;
    
    /**
     * 笔记作者用户名
     */
    private String username;
    
    /**
     * 关联的文章ID
     */
    private String articleId;
    
    /**
     * 关联的文章标题
     */
    private String articleTitle;
    
    /**
     * 笔记标签列表
     */
    private Set<String> tags;
    
    /**
     * 笔记是否公开
     */
    private boolean isPublic;
    
    /**
     * 笔记类型
     */
    private String type;
    
    /**
     * 笔记颜色
     */
    private String color;
    
    /**
     * 笔记位置
     */
    private String position;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
} 
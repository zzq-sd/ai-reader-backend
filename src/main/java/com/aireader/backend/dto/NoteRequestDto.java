package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * 笔记请求DTO
 * 用于创建和更新笔记
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRequestDto {
    
    /**
     * 笔记标题
     */
    @NotBlank(message = "笔记标题不能为空")
    @Size(max = 255, message = "笔记标题不能超过255个字符")
    private String title;
    
    /**
     * 笔记内容
     */
    @NotBlank(message = "笔记内容不能为空")
    private String content;
    
    /**
     * 关联的文章ID（可选）
     */
    private String articleId;
    
    /**
     * 笔记标签列表
     */
    private Set<String> tags;
    
    /**
     * 笔记是否公开
     */
    private boolean isPublic;
    
    /**
     * 笔记类型（普通笔记、摘抄、评论等）
     */
    private String type;
    
    /**
     * 笔记颜色（用于前端展示）
     */
    private String color;
    
    /**
     * 笔记位置（如果是文章中的笔记，表示在文章中的位置）
     */
    private String position;
} 
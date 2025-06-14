package com.aireader.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记分析请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteAnalysisRequest {
    
    /**
     * 笔记ID
     */
    private String noteId;
    
    /**
     * 笔记标题
     */
    private String title;
    
    /**
     * 笔记内容
     */
    private String content;
    
    /**
     * 分析选项（可选）
     * 可以指定需要分析的内容，如"summary,keywords,sentiment"
     */
    private String options;
} 
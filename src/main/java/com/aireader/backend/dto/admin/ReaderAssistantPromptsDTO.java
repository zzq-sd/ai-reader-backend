package com.aireader.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI阅读助手快捷提示词配置DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderAssistantPromptsDTO {
    
    /**
     * 快捷提示词列表
     */
    private List<String> quickPrompts;
} 
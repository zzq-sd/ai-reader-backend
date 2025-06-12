package com.aireader.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识关联配置DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeConfigDTO {
    private String extractPrompt;
    private String relationPrompt;
    private String summaryPrompt;
    private double similarityThreshold;
    private int maxRelatedNodes;
    private boolean enableAutoRelation;
} 
package com.aireader.backend.model.mongo;

import com.aireader.backend.ai.ArticleAnalysisResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 笔记分析结果MongoDB实体类
 * 存储笔记的AI分析结果，用于知识图谱构建
 */
@Data
@Document(collection = "notes_analysis")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteAnalysisResult {
    
    @Id
    private String id;
    
    @Indexed
    private String noteId;           // 对应MySQL notes表的ID
    
    @Indexed 
    private String userId;           // 用户ID
    
    private String enhancedSummary;  // AI摘要
    private String[] keyPoints;      // 关键点
    private String[] intelligentTags; // 智能标签
    private ArticleAnalysisResult.ConceptEntity[] extractedConcepts; // 提取的概念
    
    private String sentimentAnalysis; // 情感分析
    private String analysisVersion;   // 分析版本
    private LocalDateTime analyzedAt; // 分析时间
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
} 
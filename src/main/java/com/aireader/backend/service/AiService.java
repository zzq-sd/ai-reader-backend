package com.aireader.backend.service;

import com.aireader.backend.dto.ai.ArticleAnalysisResult;
import com.aireader.backend.dto.ai.NoteAnalysisResult;
import com.aireader.backend.dto.ai.ChatRequest;
import com.aireader.backend.dto.ai.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * AI服务统一接口
 * 提供文章和笔记的智能分析功能
 * 
 * 注意：此接口替换了原有的两个重复的AiService接口
 * （之前分别位于com.aireader.backend.service和com.aireader.backend.ai包中）
 */
public interface AiService {
    
    /**
     * 与AI助手进行聊天
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chatWithAi(ChatRequest request);

    /**
     * 与AI助手进行流式聊天
     * @param request 聊天请求
     * @return 聊天响应的Flux流
     */
    Flux<ChatResponse> streamChatWithAi(ChatRequest request);
    
    /**
     * 从文本中提取实体
     *
     * @param text 文本内容
     * @return 实体列表
     */
    List<Map<String, Object>> extractEntities(String text);
    
    /**
     * 从文本中提取关键词
     *
     * @param text 文本内容
     * @return 关键词列表
     */
    List<String> extractKeywords(String text);
    
    /**
     * 对文本进行分类
     *
     * @param text 文本内容
     * @return 分类结果
     */
    String classifyText(String text);
    
    /**
     * 分析文章
     *
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     * @return 文章分析结果
     */
    ArticleAnalysisResult analyzeArticle(String articleId, String title, String content);
    
    /**
     * 分析笔记
     *
     * @param noteId 笔记ID
     * @param title 笔记标题
     * @param content 笔记内容
     * @return 笔记分析结果
     */
    NoteAnalysisResult analyzeNote(String noteId, String title, String content);
    
    /**
     * 估算文本阅读时间
     *
     * @param content 文本内容
     * @return 阅读时间（分钟）
     */
    int estimateReadingTime(String content);
} 
package com.aireader.backend.ai;

import java.util.List;
import java.util.Map;

/**
 * AI服务接口
 * 提供文本分析、实体提取和关键词提取等功能
 */
public interface AiService {

    /**
     * 从文本中提取实体
     *
     * @param text 待分析的文本内容
     * @return 提取的实体列表，每个实体包含名称、类型等信息
     */
    List<Map<String, Object>> extractEntities(String text);

    /**
     * 从文本中提取关键词
     *
     * @param text 待分析的文本内容
     * @return 提取的关键词列表
     */
    List<String> extractKeywords(String text);

    /**
     * 对文本进行分类
     *
     * @param text 待分析的文本内容
     * @return 文本类别
     */
    String classifyText(String text);

    /**
     * 分析文章内容，提取关键信息
     *
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     * @return 文章分析结果
     */
    ArticleAnalysisResult analyzeArticle(String articleId, String title, String content);

    /**
     * 分析笔记内容，提取关键信息
     *
     * @param noteId 笔记ID
     * @param title 笔记标题
     * @param content 笔记内容
     * @return 笔记分析结果
     */
    NoteAnalysisResult analyzeNote(String noteId, String title, String content);

    /**
     * 估算阅读时间（分钟）
     *
     * @param content 内容
     * @return 估算的阅读时间（分钟）
     */
    int estimateReadingTime(String content);
} 
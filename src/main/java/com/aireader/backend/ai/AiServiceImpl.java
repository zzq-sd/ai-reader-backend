package com.aireader.backend.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * AI服务实现类 - 临时实现以便编译项目
 * 注：由于Spring AI版本兼容性问题，暂时使用空实现
 */
@Service
@Slf4j
public class AiServiceImpl implements AiService {

    @Value("${ai.reading-time.words-per-minute:200}")
    private int wordsPerMinute;

    @Override
    public List<Map<String, Object>> extractEntities(String text) {
        log.info("提取实体 (临时禁用)，文本长度: {}", text.length());
        return Collections.emptyList();
    }

    @Override
    public List<String> extractKeywords(String text) {
        log.info("提取关键词 (临时禁用)，文本长度: {}", text.length());
        return Collections.emptyList();
    }

    @Override
    public String classifyText(String text) {
        log.info("文本分类 (临时禁用)，文本长度: {}", text.length());
        return "未分类";
    }

    @Override
    public ArticleAnalysisResult analyzeArticle(String articleId, String title, String content) {
        log.info("分析文章 (临时禁用)，ID: {}, 标题: {}, 内容长度: {}", articleId, title, content.length());
        
        return ArticleAnalysisResult.builder()
                .articleId(articleId)
                .keywords(Collections.emptyList())
                .entities(Collections.emptyList())
                .category("未分类")
                .sentiment("NEUTRAL")
                .readingTimeMinutes(estimateReadingTime(content))
                .concepts(Collections.emptyList())
                .summary("AI分析服务临时禁用")
                .build();
    }
    
    @Override
    public NoteAnalysisResult analyzeNote(String noteId, String title, String content) {
        log.info("分析笔记 (临时禁用)，ID: {}, 标题: {}, 内容长度: {}", noteId, title, content.length());
        
        return NoteAnalysisResult.builder()
                .noteId(noteId)
                .keywords(Collections.emptyList())
                .entities(Collections.emptyList())
                .topic("未知主题")
                .keyPoints(Collections.emptyList())
                .relatedConcepts(Collections.emptyList())
                .build();
    }

    @Override
    public int estimateReadingTime(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        
        // 简单地按空格分词计算单词数量
        String[] words = content.split("\\s+");
        int wordCount = words.length;
        
        // 根据阅读速度估算阅读时间，至少为1分钟
        int readingTimeMinutes = Math.max(1, (int) Math.ceil((double) wordCount / wordsPerMinute));
        
        return readingTimeMinutes;
    }
}
package com.aireader.backend.ai;

import com.aireader.backend.dto.ai.ArticleAnalysisResult;
import com.aireader.backend.dto.ai.NoteAnalysisResult;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 增强的AI分析服务 - 基于Spring AI 1.0.0
 * 利用ChatClient API和结构化输出特性
 * 专注于知识图谱构建的AI分析功能
 */
@Service
@Slf4j
public class EnhancedAiAnalysisService {

    private final ChatModelProvider chatModelProvider;
    private final ObjectMapper objectMapper;

    @Autowired
    public EnhancedAiAnalysisService(ChatModelProvider chatModelProvider, ObjectMapper objectMapper) {
        this.chatModelProvider = chatModelProvider;
        this.objectMapper = objectMapper;
        log.info("✅ 增强AI分析服务初始化完成 - Spring AI 1.0.0. 使用 ChatModelProvider 动态创建 ChatClient。");
    }

    /**
     * 分析文章内容 - 使用结构化输出
     * 基于Spring AI 1.0.0的entity()方法实现结构化响应
     */
    public ArticleAnalysisResult analyzeArticleWithStructuredOutput(String articleId, String title, String content) {
        log.info("🧠 开始结构化AI分析文章: {}", articleId);
        
        // 动态获取默认模型（例如 deepseek）创建ChatClient
        ChatClient chatClient = chatModelProvider.getChatModel("deepseek")
                .map(ChatClient::create)
                .orElseThrow(() -> {
                    log.error("❌ 获取deepseek模型失败，无法分析文章");
                    return new IllegalStateException("默认的deepseek模型不可用");
                });
        
        try {
            // 使用结构化输出分析文章
            StructuredArticleAnalysis analysis = chatClient.prompt()
                .user(buildArticleAnalysisPrompt(title, content))
                .call()
                .entity(StructuredArticleAnalysis.class);
            
            // 转换为ArticleAnalysisResult
            ArticleAnalysisResult result = convertToArticleAnalysisResult(articleId, analysis);
            
            log.info("✅ 文章结构化分析完成: {} - 提取{}个概念", articleId, result.getConcepts().size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ 文章结构化分析失败: {}", articleId, e);
            throw new RuntimeException("文章结构化分析失败", e);
        }
    }

    /**
     * 提取概念 - 专门的概念提取功能
     */
    public ConceptExtractionResponse extractConceptsWithRelations(String content) {
        log.info("🔍 开始提取概念和关系，内容长度: {}", content.length());
        
        // 动态获取默认模型（例如 deepseek）创建ChatClient
        ChatClient chatClient = chatModelProvider.getChatModel("deepseek")
                .map(ChatClient::create)
                .orElseThrow(() -> {
                    log.error("❌ 获取deepseek模型失败，无法提取概念");
                    return new IllegalStateException("默认的deepseek模型不可用");
                });
        
        try {
            ConceptExtractionResponse response = chatClient.prompt()
                .user(buildConceptExtractionPrompt(content))
                .call()
                .entity(ConceptExtractionResponse.class);
            
            log.info("✅ 概念提取完成: {}个概念, {}个关系", 
                response.concepts().size(), response.relationships().size());
            return response;
            
        } catch (Exception e) {
            log.error("❌ 概念提取失败", e);
            throw new RuntimeException("概念提取失败", e);
        }
    }
    
    /**
     * 提取概念 - 使用自定义提示词
     * 
     * @param customPrompt 自定义提示词
     * @return 概念提取响应
     */
    public ConceptExtractionResponse extractConceptsWithRelationsCustomPrompt(String customPrompt) {
        log.info("🔍 开始使用自定义提示词提取概念和关系");
        
        // 动态获取默认模型（例如 deepseek）创建ChatClient
        ChatClient chatClient = chatModelProvider.getChatModel("deepseek")
                .map(ChatClient::create)
                .orElseThrow(() -> {
                    log.error("❌ 获取deepseek模型失败，无法使用自定义提示词提取概念");
                    return new IllegalStateException("默认的deepseek模型不可用");
                });
        
        try {
            // 确保自定义提示词包含必要的指导
            String finalPrompt = customPrompt + "\n\n请返回JSON格式，包含concepts和relationships列表。";
            
            ConceptExtractionResponse response = chatClient.prompt()
                .user(finalPrompt)
                .call()
                .entity(ConceptExtractionResponse.class);
            
            log.info("✅ 使用自定义提示词概念提取完成: {}个概念, {}个关系", 
                response.concepts().size(), response.relationships().size());
            return response;
            
        } catch (Exception e) {
            log.error("❌ 使用自定义提示词概念提取失败", e);
            throw new RuntimeException("使用自定义提示词概念提取失败", e);
        }
    }

    /**
     * RAG增强分析 - 基于已有知识的分析
     */
    public String analyzeWithKnowledgeContext(String content, String articleId) {
        log.info("🔗 开始RAG增强分析: {}", articleId);
        
        // RAG场景通常也需要特定的模型或配置，此处暂时也用deepseek，可根据需求调整
        ChatClient ragChatClient = chatModelProvider.getChatModel("deepseek")
                .map(ChatClient::create)
                .orElseThrow(() -> {
                    log.error("❌ 获取deepseek模型失败，无法进行RAG分析");
                    return new IllegalStateException("用于RAG的deepseek模型不可用");
                });
        
        try {
            String response = ragChatClient.prompt()
                .user(buildRagAnalysisPrompt(content))
                .call()
                .content();
            
            log.info("✅ RAG增强分析完成: {}", articleId);
            return response;
            
        } catch (Exception e) {
            log.error("❌ RAG增强分析失败: {}", articleId, e);
            throw new RuntimeException("RAG增强分析失败", e);
        }
    }

    /**
     * 分析笔记内容 - 结构化输出
     */
    public NoteAnalysisResult analyzeNoteWithStructuredOutput(String noteId, String title, String content) {
        log.info("📝 开始结构化AI分析笔记: {}", noteId);
        
        // 动态获取智谱（glm）模型创建ChatClient，专门用于笔记分析
        ChatClient chatClient = chatModelProvider.getChatModel("zhipuai")
                .map(ChatClient::create)
                .orElseThrow(() -> {
                    log.error("❌ 获取zhipuai模型失败，无法分析笔记");
                    return new IllegalStateException("笔记分析所需的zhipuai模型不可用");
                });

        try {
            StructuredNoteAnalysis analysis = chatClient.prompt()
                .user(buildNoteAnalysisPrompt(title, content))
                .call()
                .entity(StructuredNoteAnalysis.class);
            
            // 转换为NoteAnalysisResult
            NoteAnalysisResult result = convertToNoteAnalysisResult(noteId, analysis);
            
            log.info("✅ 笔记结构化分析完成: {} - 提取{}个概念", noteId, result.getExtractedConcepts().size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ 笔记结构化分析失败: {}", noteId, e);
            throw new RuntimeException("笔记结构化分析失败", e);
        }
    }

    // ====================== 私有方法 ======================

    /**
     * 构建文章分析提示词
     */
    private String buildArticleAnalysisPrompt(String title, String content) {
        return String.format("""
            请分析以下文章内容，提取关键信息用于构建知识图谱：
            
            标题：%s
            
            内容：%s
            
            请按照JSON格式返回分析结果，包含：
            1. summary: 文章摘要（100-200字）
            2. keyPoints: 关键观点列表（3-5个要点）
            3. concepts: 核心概念列表，每个概念包含name、type、confidence、context
            4. tags: 智能标签（5-8个）
            5. sentiment: 情感倾向（POSITIVE/NEUTRAL/NEGATIVE）
            6. category: 文章分类
            7. readingTimeMinutes: 预估阅读时间
            
            概念类型包括：TECHNOLOGY、PERSON、ORGANIZATION、LOCATION、EVENT、CONCEPT等
            """, title, content);
    }

    /**
     * 构建概念提取提示词
     */
    private String buildConceptExtractionPrompt(String content) {
        return String.format("""
            请从以下文本中提取核心概念和它们之间的关系：
            
            内容：%s
            
            请返回JSON格式，包含：
            1. concepts: 概念列表，每个概念包含name、type、confidence、description
            2. relationships: 关系列表，每个关系包含source、target、type、strength
            
            关系类型包括：RELATED_TO、PART_OF、INSTANCE_OF、CAUSES、ENABLES等
            """, content);
    }

    /**
     * 构建RAG分析提示词
     */
    private String buildRagAnalysisPrompt(String content) {
        return String.format("""
            基于已有的知识图谱信息，分析以下内容：
            
            %s
            
            请：
            1. 识别与已有概念的关联
            2. 发现新的概念和关系
            3. 提供深度分析和见解
            4. 建议知识图谱的扩展方向
            """, content);
    }

    /**
     * 构建笔记分析提示词
     */
    private String buildNoteAnalysisPrompt(String title, String content) {
        return String.format("""
            请分析以下笔记内容，提取关键信息：
            
            标题：%s
            
            内容：%s
            
            请按照JSON格式返回分析结果，包含：
            1. summary: 笔记摘要
            2. keyPoints: 关键观点
            3. concepts: 核心概念
            4. tags: 智能标签
            5. topic: 主要话题
            6. insights: 深度见解
            """, title, content);
    }

    /**
     * 转换为ArticleAnalysisResult
     */
    private ArticleAnalysisResult convertToArticleAnalysisResult(String articleId, StructuredArticleAnalysis analysis) {
        List<ArticleAnalysisResult.ConceptEntity> concepts = analysis.concepts().stream()
            .map(c -> ArticleAnalysisResult.ConceptEntity.builder()
                .name(c.name())
                .type(c.type())
                .confidence(c.confidence())
                .context(c.context())
                .frequency(1)
                .build())
            .collect(Collectors.toList());

        return ArticleAnalysisResult.builder()
            .articleId(articleId)
            .summary(analysis.summary())
            .keyPoints(analysis.keyPoints())
            .concepts(concepts)
            .intelligentTags(analysis.tags())
            .sentiment(analysis.sentiment())
            .category(analysis.category())
            .readingTimeMinutes(analysis.readingTimeMinutes())
            .keywords(concepts.stream().map(ArticleAnalysisResult.ConceptEntity::getName).collect(Collectors.toList()))
            .build();
    }

    /**
     * 转换为NoteAnalysisResult
     */
    private NoteAnalysisResult convertToNoteAnalysisResult(String noteId, StructuredNoteAnalysis analysis) {
        List<ArticleAnalysisResult.ConceptEntity> concepts = analysis.concepts().stream()
                .map(c -> ArticleAnalysisResult.ConceptEntity.builder()
                        .name(c.name())
                        .type(c.type())
                        .confidence(c.confidence())
                        .context(c.context())
                        .build())
                .collect(Collectors.toList());

        return NoteAnalysisResult.builder()
                .noteId(noteId)
                .enhancedSummary(analysis.summary())
                .keyPoints(analysis.keyPoints())
                .extractedConcepts(concepts)
                .intelligentTags(analysis.tags())
                .topic(analysis.topic())
                .keywords(concepts.stream().map(ArticleAnalysisResult.ConceptEntity::getName).collect(Collectors.toList()))
                .build();
    }

    // ====================== 结构化输出模型 ======================

    /**
     * 结构化文章分析响应
     */
    @JsonClassDescription("文章分析结果")
    public record StructuredArticleAnalysis(
        @JsonProperty("summary") String summary,
        @JsonProperty("keyPoints") List<String> keyPoints,
        @JsonProperty("concepts") List<ConceptInfo> concepts,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("sentiment") String sentiment,
        @JsonProperty("category") String category,
        @JsonProperty("readingTimeMinutes") Integer readingTimeMinutes
    ) {}

    /**
     * 概念提取响应
     */
    @JsonClassDescription("概念提取响应")
    public record ConceptExtractionResponse(
        @JsonProperty("concepts") List<ConceptInfo> concepts,
        @JsonProperty("relationships") List<ConceptRelation> relationships
    ) {}

    /**
     * 结构化笔记分析响应
     */
    @JsonClassDescription("笔记分析结果")
    public record StructuredNoteAnalysis(
        @JsonProperty("summary") String summary,
        @JsonProperty("keyPoints") List<String> keyPoints,
        @JsonProperty("concepts") List<ConceptInfo> concepts,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("topic") String topic,
        @JsonProperty("insights") List<String> insights
    ) {}

    /**
     * 概念信息
     */
    @JsonClassDescription("概念信息")
    public record ConceptInfo(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("confidence") Double confidence,
        @JsonProperty("context") String context
    ) {}

    /**
     * 概念关系
     */
    @JsonClassDescription("概念关系")
    public record ConceptRelation(
        @JsonProperty("source") String source,
        @JsonProperty("target") String target,
        @JsonProperty("type") String type,
        @JsonProperty("strength") Double strength
    ) {}
} 
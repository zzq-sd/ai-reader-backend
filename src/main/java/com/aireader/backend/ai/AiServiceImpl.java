package com.aireader.backend.ai;

import com.aireader.backend.dto.ai.ArticleAnalysisRequest;
import com.aireader.backend.dto.ai.ArticleAnalysisResult;
import com.aireader.backend.dto.ai.ArticleSummaryRequest;
import com.aireader.backend.dto.ai.ArticleSummaryResult;
import com.aireader.backend.dto.ai.ChatRequest;
import com.aireader.backend.dto.ai.ChatResponse;
import com.aireader.backend.dto.ai.NoteAnalysisRequest;
import com.aireader.backend.dto.ai.NoteAnalysisResult;
import com.aireader.backend.service.AiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI服务实现类 - 基于Spring AI 1.0.0
 * 提供文章和笔记的智能分析功能，支持知识图谱构建
 * 
 * 注意：此类实现了com.aireader.backend.service.AiService接口
 * 替代了原有的两个重复的AiService实现
 */
@Service
@Slf4j
public class AiServiceImpl implements AiService {

    private final ObjectMapper objectMapper;
    private final ChatModelProvider chatModelProvider;
    private final AiConfigService aiConfigService;

    // 为特定任务指定模型名称
    @Value("${spring.ai.model.chat.task.note-analysis:zhipuai}")
    private String noteAnalysisModel;
    
    @Value("${ai.reading-time.words-per-minute:200}")
    private int wordsPerMinute;

    @Autowired
    public AiServiceImpl(
            ObjectMapper objectMapper,
            ChatModelProvider chatModelProvider,
            AiConfigService aiConfigService) {
        this.objectMapper = objectMapper;
        this.chatModelProvider = chatModelProvider;
        this.aiConfigService = aiConfigService;
    }
    
    /**
     * 初始化方法，在所有依赖注入完成后执行
     */
    @PostConstruct
    public void init() {
        log.info("初始化AI服务...");
        testAndLogConnection();
    }
    
    /**
     * 处理AI配置变更事件
     * 当配置发生变化时，重新加载服务设置
     */
    @EventListener
    public void handleAiConfigChangedEvent(AiConfigService.AiConfigChangedEvent event) {
        log.info("接收到AI配置变更事件，重新加载AI服务设置");
        testAndLogConnection();
    }
    
    /**
     * 服务初始化时，测试并记录所有ChatClient的可用状态
     */
    private void testAndLogConnection() {
        log.info("🔍 开始测试AI服务连接...");
        String activeChatModelName = aiConfigService.getActiveChatModelName();
        log.info("当前数据库配置的激活聊天模型为: '{}'", activeChatModelName);

        chatModelProvider.getChatModel(activeChatModelName)
                .ifPresentOrElse(
                        model -> log.info("✅ 激活的聊天模型 '{}' 已成功加载.", activeChatModelName),
                        () -> log.error("❌ 激活的聊天模型 '{}' 未找到或加载失败!", activeChatModelName)
                );
        
        log.info("笔记分析模型固定为: '{}'", noteAnalysisModel);
        chatModelProvider.getChatModel(noteAnalysisModel)
                .ifPresentOrElse(
                        model -> log.info("✅ 笔记分析模型 '{}' 已成功加载.", noteAnalysisModel),
                        () -> log.error("❌ 笔记分析模型 '{}' 未找到或加载失败!", noteAnalysisModel)
                );

        log.info("✅ AI服务连接测试完成");
    }

    @Override
    public List<Map<String, Object>> extractEntities(String text) {
        log.info("提取实体，文本长度: {}", text.length());
        try {
            List<ArticleAnalysisResult.ConceptEntity> concepts = extractConcepts(text);
            return concepts.stream()
                    .map(concept -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", concept.getName());
                        map.put("type", concept.getType());
                        map.put("confidence", concept.getConfidence());
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("提取实体失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> extractKeywords(String text) {
        log.info("提取关键词，文本长度: {}", text.length());
        try {
            List<ArticleAnalysisResult.ConceptEntity> concepts = extractConcepts(text);
            return concepts.stream()
                    .map(ArticleAnalysisResult.ConceptEntity::getName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("提取关键词失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String classifyText(String text) {
        log.info("文本分类，文本长度: {}", text.length());
        try {
            return generateCategory(text);
        } catch (Exception e) {
            log.error("文本分类失败", e);
            return "未分类";
        }
    }

    @Override
    public ArticleAnalysisResult analyzeArticle(String articleId, String title, String content) {
        log.info("开始AI分析文章: {}", articleId);
        
        // AI分析功能统一使用笔记分析的配置
        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));
        
        try {
            // 获取当前AI配置
            int maxTokens = aiConfigService.getMaxTokens();
            log.debug("使用配置: maxTokens={}", maxTokens);
            
            // 1. 概念提取
            List<ArticleAnalysisResult.ConceptEntity> concepts = extractConcepts(content);
            
            // 2. 生成摘要
            String summary = generateSummary(content);
            
            // 3. 提取关键点
            List<String> keyPoints = extractKeyPoints(content);
            
            // 4. 生成智能标签
            List<String> tags = generateIntelligentTags(content, title);
            
            // 5. 情感分析
            String sentiment = analyzeSentiment(content);
            
            // 6. 生成分类
            String category = generateCategory(content);
            
            return ArticleAnalysisResult.builder()
                    .articleId(articleId)
                    .summary(summary)
                    .keyPoints(keyPoints)
                    .keywords(concepts.stream().map(ArticleAnalysisResult.ConceptEntity::getName).collect(Collectors.toList()))
                    .concepts(concepts)
                    .intelligentTags(tags)
                    .sentiment(sentiment)
                    .category(category)
                    .readingTimeMinutes(estimateReadingTime(content))
                    .entities(concepts.stream()
                            .map(concept -> {
                                Map<String, Object> map = new HashMap<>();
                                map.put("name", concept.getName());
                                map.put("type", concept.getType());
                                map.put("confidence", concept.getConfidence());
                                return map;
                            })
                            .collect(Collectors.toList()))
                    .build();
                    
        } catch (Exception e) {
            log.error("AI分析失败: {}", articleId, e);
            throw new RuntimeException("AI分析失败", e);
        }
    }
    
    @Override
    public NoteAnalysisResult analyzeNote(String noteId, String title, String content) {
        log.info("开始AI分析笔记: {}", noteId);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        try {
            // 1. 概念提取
            List<ArticleAnalysisResult.ConceptEntity> concepts = extractConceptsForNote(content);
            String summary = generateSummaryForNote(content);
            List<String> keyPoints = extractKeyPointsForNote(content);
            List<String> tags = generateIntelligentTagsForNote(content, title);
            String sentiment = analyzeSentimentForNote(content);
            String category = generateCategoryForNote(content);
            
            // 构建笔记分析结果
            NoteAnalysisResult result = NoteAnalysisResult.builder()
                    .noteId(noteId)
                    .keywords(concepts.stream().map(ArticleAnalysisResult.ConceptEntity::getName).collect(Collectors.toList()))
                    .enhancedSummary(summary)
                    .keyPoints(keyPoints)
                    .intelligentTags(tags)
                    .extractedConcepts(concepts)
                    .sentimentAnalysis(sentiment)
                    .topic(category)
                    .build();
            
            log.info("笔记分析完成: ID={}, 标题={}, 概念数量={}, 标签数量={}", 
                    noteId, title, concepts.size(), tags.size());
            
            return result;
        } catch (Exception e) {
            log.error("笔记分析失败: {}", e.getMessage());
            return createFallbackNoteAnalysis(noteId, title, content);
        }
    }
    
    /**
     * 创建降级的笔记分析结果
     */
    private NoteAnalysisResult createFallbackNoteAnalysis(String noteId, String title, String content) {
        log.info("创建笔记降级分析结果: {}", noteId);
        
        // 基础的关键词提取（简单的词频分析）
        List<String> basicKeywords = extractBasicKeywords(content);
        
        // 基础的主题分类（基于标题）
        String basicTopic = classifyBasicTopic(title, content);
        
        return NoteAnalysisResult.builder()
                .noteId(noteId)
                .keywords(basicKeywords)
                .entities(Collections.emptyList())
                .topic(basicTopic)
                .keyPoints(Collections.singletonList("AI服务暂时不可用，无法提取关键点"))
                .extractedConcepts(Collections.emptyList())
                .intelligentTags(Collections.singletonList("基础分析"))
                .enhancedSummary("AI服务暂时不可用，无法生成摘要")
                .sentimentAnalysis("NEUTRAL")
                .build();
    }
    
    /**
     * 基础关键词提取（不依赖AI）
     */
    private List<String> extractBasicKeywords(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // 简单的词频分析
        String[] words = content.toLowerCase()
                .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", " ")
                .split("\\s+");
        
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            if (word.length() > 1) { // 过滤单字符
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }
        
        return wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * 基础主题分类（不依赖AI）
     */
    private String classifyBasicTopic(String title, String content) {
        String text = (title + " " + content).toLowerCase();
        
        if (text.contains("技术") || text.contains("科技") || text.contains("编程") || text.contains("开发")) {
            return "科技";
        } else if (text.contains("商业") || text.contains("经济") || text.contains("市场") || text.contains("投资")) {
            return "商业";
        } else if (text.contains("健康") || text.contains("医疗") || text.contains("养生")) {
            return "健康";
        } else if (text.contains("教育") || text.contains("学习") || text.contains("知识")) {
            return "教育";
        } else {
            return "其他";
        }
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
    
    // ====================== 私有AI分析方法 ======================
    
    /**
     * 从文本中提取概念实体
     */
    private List<ArticleAnalysisResult.ConceptEntity> extractConcepts(String content) {
        log.info("开始提取概念...");
        String truncatedContent = truncateContent(content, 16000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        String prompt = """
            作为知识图谱专家，从以下内容中提取5-10个核心概念。
            
            要求：
            1. 提取最重要的概念实体
            2. 为每个概念分类：TECHNOLOGY, PERSON, ORGANIZATION, LOCATION, EVENT, CONCEPT
            3. 计算置信度(0.0-1.0)
            4. 统计出现频次
            5. 提供30字以内的上下文描述
            
            严格按JSON格式返回：
            {
              "concepts": [
                {
                  "name": "概念名称", 
                  "type": "TECHNOLOGY",
                  "confidence": 0.95,
                  "frequency": 3,
                  "context": "概念在文中的具体使用场景",
                  "synonyms": ["同义词1", "同义词2"]
                }
              ]
            }
            
            内容：
            %s
            """.formatted(truncatedContent);
            
        try {
            log.debug("🤖 开始调用AI进行概念提取，内容长度: {}", truncatedContent.length());
            
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
                    
            log.debug("✅ AI响应成功，响应长度: {}", response.length());
            log.debug("📄 AI响应内容预览: {}", response.substring(0, Math.min(200, response.length())));
            
            List<ArticleAnalysisResult.ConceptEntity> concepts = parseConceptResponse(response);
            log.info("🎯 成功提取概念数量: {}", concepts.size());
            
            return concepts;
        } catch (Exception e) {
            log.error("❌ 概念提取失败，详细错误信息:", e);
            log.error("🔧 错误类型: {}", e.getClass().getSimpleName());
            log.error("📝 错误消息: {}", e.getMessage());
            log.error("📋 输入内容长度: {}", truncatedContent.length());
            
            // 根据错误类型提供具体的解决建议
            if (e.getMessage() != null) {
                if (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized")) {
                    log.error("🔑 API认证失败 - 请检查API密钥是否正确");
                } else if (e.getMessage().contains("429") || e.getMessage().contains("rate limit")) {
                    log.error("⏰ API调用频率限制 - 请稍后重试");
                } else if (e.getMessage().contains("timeout")) {
                    log.error("⏱️ 请求超时 - 可能是网络问题或内容过长");
                } else if (e.getMessage().contains("model")) {
                    log.error("🤖 模型相关错误 - 请检查模型配置");
                }
            }
            
            return Collections.emptyList();
        }
    }
    
    /**
     * 生成文章摘要
     */
    private String generateSummary(String content) {
        log.info("开始生成摘要...");
        String truncatedContent = truncateContent(content, 8000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        String prompt = """
            请为以下内容生成一个150-200字的精准摘要：
            
            要求：
            1. 突出核心观点和关键信息
            2. 保持客观中性的语调
            3. 包含关键数据和结论
            4. 语言简洁明了
            
            内容：
            %s
            """.formatted(truncatedContent);
            
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("摘要生成失败", e);
            return "摘要生成失败";
        }
    }
    
    /**
     * 提取关键点
     */
    private List<String> extractKeyPoints(String content) {
        log.info("开始提取关键观点...");
        String truncatedContent = truncateContent(content, 8000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        String prompt = """
            从以下内容中提取3-6个核心要点：
            
            要求：
            1. 每个要点20-40字
            2. 突出重要信息和关键结论
            3. 按重要性排序
            4. 避免重复内容
            
            以JSON数组格式返回：["要点1", "要点2", "要点3"]
            
            内容：
            %s
            """.formatted(truncatedContent);
            
        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return parseStringArrayResponse(response);
        } catch (Exception e) {
            log.error("关键点提取失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 生成智能标签
     */
    private List<String> generateIntelligentTags(String content, String title) {
        log.info("开始生成智能标签...");
        String truncatedContent = truncateContent(content, 4000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        String prompt = """
            为以下文章生成4-8个智能标签：
            
            标题：%s
            
            要求：
            1. 包含主题分类标签
            2. 包含技术/领域标签  
            3. 包含内容特征标签
            4. 标签长度2-6个字
            5. 避免过于宽泛的标签
            
            以JSON数组格式返回：["标签1", "标签2", "标签3"]
            
            内容：
            %s
            """.formatted(title, truncatedContent);
            
        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return parseStringArrayResponse(response);
        } catch (Exception e) {
            log.error("标签生成失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 情感分析
     */
    private String analyzeSentiment(String content) {
        log.info("开始进行情感分析...");
        String truncatedContent = truncateContent(content, 2000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        String prompt = """
            对以下内容进行情感分析，返回以下之一：
            - POSITIVE: 积极正面
            - NEGATIVE: 消极负面  
            - NEUTRAL: 中性客观
            
            只返回单词，不要其他解释。
            
            内容：
            %s
            """.formatted(truncatedContent);
            
        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content().trim().toUpperCase();
                    
            if (response.contains("POSITIVE")) return "POSITIVE";
            if (response.contains("NEGATIVE")) return "NEGATIVE";
            return "NEUTRAL";
        } catch (Exception e) {
            log.error("情感分析失败", e);
            return "NEUTRAL";
        }
    }
    
    /**
     * 生成分类
     */
    private String generateCategory(String content) {
        log.info("开始生成分类...");
        String truncatedContent = truncateContent(content, 4000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        String prompt = """
            对以下内容进行主题分类，从以下类别中选择最合适的一个：
            - 科技
            - 商业
            - 健康
            - 教育
            - 娱乐
            - 体育
            - 政治
            - 社会
            - 文化
            - 其他
            
            只返回类别名称，不要其他解释。
            
            内容：
            %s
            """.formatted(truncatedContent);
            
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content().trim();
        } catch (Exception e) {
            log.error("分类生成失败", e);
            return "其他";
        }
    }
    
    /**
     * 解析概念提取的JSON响应
     */
    private List<ArticleAnalysisResult.ConceptEntity> parseConceptResponse(String response) {
        // 前置日志：记录完整的原始响应，用于调试
        log.debug("📄 开始解析概念提取响应，原始AI响应: {}", response);
        
        try {
            // 提取JSON部分
            String jsonPart = extractJsonFromResponse(response);
            JsonNode rootNode = objectMapper.readTree(jsonPart);
            JsonNode conceptsNode = rootNode.get("concepts");

            if (conceptsNode == null || !conceptsNode.isArray()) {
                // 丰富警告信息，包含原始响应内容
                log.warn("🚨 概念提取响应格式不正确，响应内容: {}", response);
                return Collections.emptyList();
            }

            List<ArticleAnalysisResult.ConceptEntity> concepts = new ArrayList<>();
            for (JsonNode conceptNode : conceptsNode) {
                ArticleAnalysisResult.ConceptEntity concept = ArticleAnalysisResult.ConceptEntity.builder()
                        .name(conceptNode.get("name").asText())
                        .type(conceptNode.get("type").asText())
                        .confidence(conceptNode.get("confidence").asDouble())
                        .frequency(conceptNode.get("frequency").asInt())
                        .context(conceptNode.has("context") ? conceptNode.get("context").asText() : "")
                        .synonyms(parseSynonyms(conceptNode.get("synonyms")))
                        .build();
                concepts.add(concept);
            }
            
            return concepts;
        } catch (JsonProcessingException e) {
            // 强化异常日志，包含导致解析失败的原始响应
            log.error("❌ 解析概念响应失败，发生JSON处理异常。原始响应: {}", response, e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("❌ 解析概念响应时发生意外错误。原始响应: {}", response, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 解析字符串数组响应
     */
    private List<String> parseStringArrayResponse(String response) {
        try {
            String jsonPart = extractJsonFromResponse(response);
            JsonNode arrayNode = objectMapper.readTree(jsonPart);
            
            if (!arrayNode.isArray()) {
                log.warn("字符串数组响应格式不正确");
                return Collections.emptyList();
            }
            
            List<String> result = new ArrayList<>();
            for (JsonNode item : arrayNode) {
                result.add(item.asText());
            }
            
            return result;
        } catch (Exception e) {
            log.error("解析字符串数组响应失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 从响应中提取JSON部分
     */
    private String extractJsonFromResponse(String response) {
        // 查找JSON开始和结束位置
        int startIndex = response.indexOf('[');
        int startObjectIndex = response.indexOf('{');
        
        if (startIndex == -1 && startObjectIndex == -1) {
            return "[]";
        }
        
        int actualStart = (startIndex == -1) ? startObjectIndex : 
                         (startObjectIndex == -1) ? startIndex : 
                         Math.min(startIndex, startObjectIndex);
        
        int endIndex = response.lastIndexOf(response.charAt(actualStart) == '[' ? ']' : '}');
        
        if (endIndex == -1) {
            return "[]";
        }
        
        return response.substring(actualStart, endIndex + 1);
    }
    
    /**
     * 解析同义词数组
     */
    private String[] parseSynonyms(JsonNode synonymsNode) {
        if (synonymsNode == null || !synonymsNode.isArray()) {
            return new String[0];
        }
        
        List<String> synonyms = new ArrayList<>();
        for (JsonNode synonym : synonymsNode) {
            synonyms.add(synonym.asText());
        }
        
        return synonyms.toArray(new String[0]);
    }
    
    /**
     * 从富文本内容中提取纯文本
     * 支持Quill Delta JSON格式
     */
    private String extractPlainTextFromRichText(String richTextContent) {
        if (richTextContent == null || richTextContent.trim().isEmpty()) {
            return "";
        }
        
        try {
            // 尝试解析为JSON（Quill Delta格式）
            JsonNode rootNode = objectMapper.readTree(richTextContent);
            
            if (rootNode.has("ops") && rootNode.get("ops").isArray()) {
                // Quill Delta格式
                StringBuilder plainText = new StringBuilder();
                JsonNode ops = rootNode.get("ops");
                
                for (JsonNode op : ops) {
                    if (op.has("insert")) {
                        JsonNode insertNode = op.get("insert");
                        if (insertNode.isTextual()) {
                            String text = insertNode.asText();
                            // 清理换行符和特殊字符
                            text = text.replaceAll("\\n+", " ").trim();
                            if (!text.isEmpty()) {
                                plainText.append(text).append(" ");
                            }
                        }
                    }
                }
                
                return plainText.toString().trim();
            } else {
                // 可能是其他JSON格式，尝试提取文本内容
                return extractTextFromJson(rootNode);
            }
        } catch (Exception e) {
            // 如果不是JSON格式，直接返回原文本
            log.debug("富文本解析失败，使用原文本: {}", e.getMessage());
            return richTextContent;
        }
    }
    
    /**
     * 从JSON节点中递归提取文本内容
     */
    private String extractTextFromJson(JsonNode node) {
        StringBuilder text = new StringBuilder();
        
        if (node.isTextual()) {
            text.append(node.asText()).append(" ");
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                text.append(extractTextFromJson(item));
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                text.append(extractTextFromJson(entry.getValue()));
            });
        }
        
        return text.toString();
    }

    // 以下是笔记分析专用的方法，使用独立的笔记分析ChatClient
    
    /**
     * 为笔记提取概念实体
     */
    private List<ArticleAnalysisResult.ConceptEntity> extractConceptsForNote(String content) {
        log.info("开始为笔记提取概念...");
        String truncatedContent = truncateContent(content, 16000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        // 构建提示词
        String prompt = String.format(
                "请识别以下文本中的关键概念和实体，并按照JSON格式返回结果：\n\n%s\n\n" +
                "返回格式为：\n" +
                "[\n" +
                "  {\"name\": \"概念名称\", \"type\": \"概念类型\", \"confidence\": 0.9},\n" +
                "  ...\n" +
                "]",
                truncatedContent);
        
        try {
            log.debug("🤖 开始调用笔记分析AI进行概念提取，内容长度: {}", content.length());
            
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            return parseConceptResponse(response);
        } catch (Exception e) {
            log.error("笔记概念提取失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 为笔记生成摘要
     */
    private String generateSummaryForNote(String content) {
        log.info("开始为笔记生成摘要...");
        String truncatedContent = truncateContent(content, 8000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        SystemMessage systemMessage = new SystemMessage(
                "你是一个文本摘要专家。请根据以下笔记内容，生成一段简洁的摘要。");
        
        Prompt prompt = new Prompt(Arrays.asList(systemMessage, new UserMessage(truncatedContent)));

        try {
            return chatClient.prompt(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("笔记摘要生成失败", e);
            return "摘要生成失败";
        }
    }
    
    /**
     * 为笔记提取关键点
     */
    private List<String> extractKeyPointsForNote(String content) {
        log.info("开始为笔记提取关键观点...");
        String truncatedContent = truncateContent(content, 8000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        SystemMessage systemMessage = new SystemMessage(
                "你是一个信息分析师。请从以下笔记中，提取出核心观点，并以无序列表的格式返回。");
        
        Prompt prompt = new Prompt(Arrays.asList(systemMessage, new UserMessage(truncatedContent)));

        try {
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();
            
            return parseStringArrayResponse(response);
        } catch (Exception e) {
            log.error("笔记关键点提取失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 为笔记生成智能标签
     */
    private List<String> generateIntelligentTagsForNote(String content, String title) {
        log.info("开始为笔记生成智能标签...");
        String truncatedContent = truncateContent(content, 4000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        SystemMessage systemMessage = new SystemMessage(
                "你是一个信息架构师。请为以下笔记生成3-5个最相关的标签（Tags），用于分类和检索。返回一个JSON字符串数组。");
        
        Prompt prompt = new Prompt(Arrays.asList(systemMessage, new UserMessage(title), new UserMessage(truncatedContent)));

        try {
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();
            
            return parseStringArrayResponse(response);
        } catch (Exception e) {
            log.error("笔记标签生成失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 为笔记分析情感
     */
    private String analyzeSentimentForNote(String content) {
        log.info("开始为笔记进行情感分析...");
        String truncatedContent = truncateContent(content, 2000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        SystemMessage systemMessage = new SystemMessage(
                "你是一个情感分析引擎。请分析以下笔记的情感倾向，只返回'正面'、'负面'或'中性'中的一个词。");
        
        Prompt prompt = new Prompt(Arrays.asList(systemMessage, new UserMessage(truncatedContent)));

        try {
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();
            
            // 简单处理情感分析结果
            String sentiment = response.toUpperCase();
            if (sentiment.contains("POSITIVE")) {
                return "POSITIVE";
            } else if (sentiment.contains("NEGATIVE")) {
                return "NEGATIVE";
            } else {
                return "NEUTRAL";
            }
        } catch (Exception e) {
            log.error("笔记情感分析失败", e);
            return "NEUTRAL";
        }
    }
    
    /**
     * 为笔记生成分类
     */
    private String generateCategoryForNote(String content) {
        log.info("开始为笔记生成分类...");
        String truncatedContent = truncateContent(content, 4000);

        ChatClient chatClient = chatModelProvider.getChatModel(noteAnalysisModel)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("笔记分析所需的AI模型 '" + noteAnalysisModel + "' 不可用。"));

        SystemMessage systemMessage = new SystemMessage(
                "你是一个图书管理员。请根据以下笔记内容，为其分配一个最合适的分类。只返回分类名称。");
        
        Prompt prompt = new Prompt(Arrays.asList(systemMessage, new UserMessage(truncatedContent)));

        try {
            return chatClient.prompt(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("笔记分类生成失败", e);
            return "其他";
        }
    }
    
    /**
     * 截断内容以符合令牌限制
     */
    private String truncateContent(String content, int maxChars) {
        if (content == null || content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars) + "...（内容已截断）";
    }

    private ArticleAnalysisResult createFallbackArticleAnalysis(String articleId, String title, String content) {
        log.warn("创建文章分析的回退结果: {}", articleId);
        return ArticleAnalysisResult.builder()
                .articleId(articleId)
                .summary("AI服务当前不可用，无法生成摘要。")
                .keyPoints(Collections.singletonList("AI服务当前不可用"))
                .keywords(Collections.emptyList())
                .intelligentTags(Collections.singletonList("无法分析"))
                .sentiment("未知")
                .category("未分类")
                .build();
    }

    @Override
    public ChatResponse chatWithAi(ChatRequest request) {
        log.info("在 AiService 中处理非流式聊天, 会话ID: {}", request.getSessionId());

        String activeModelName = aiConfigService.getActiveChatModelName();
        ChatClient chatClient = chatModelProvider.getChatModel(activeModelName)
                .map(ChatClient::create)
                .orElseThrow(() -> new IllegalStateException("数据库配置的AI模型 '" + activeModelName + "' 不可用。"));

        String response = chatClient.prompt()
                .user(request.getMessage())
                .call()
                .content();

        return ChatResponse.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(request.getSessionId())
                .content(response)
                .role("assistant")
                .timestamp(LocalDateTime.now())
                .done(true)
                .build();
    }

    @Override
    public Flux<ChatResponse> streamChatWithAi(ChatRequest request) {
        log.info("在 AiService 中处理流式聊天, 会话ID: {}, 消息: '{}'",
                request.getSessionId(), request.getMessage().substring(0, Math.min(request.getMessage().length(), 50)));

        // 1. 从数据库配置中获取当前激活的模型名称
        String activeModelName = aiConfigService.getActiveChatModelName();

        // 2. 获取模型并创建ChatClient
        ChatClient chatClient = chatModelProvider.getChatModel(activeModelName)
                .map(ChatClient::create)
                .orElseThrow(() -> {
                    log.error("无法创建ChatClient，因为数据库配置的激活模型 '{}' 不可用。", activeModelName);
                    return new IllegalStateException("当前配置的AI模型不可用，请联系管理员。");
                });

        log.info("动态选择了模型 '{}' 用于本次会话 {}", activeModelName, request.getSessionId());

        // 3. 历史消息处理 (如果需要)
        List<Message> history = new ArrayList<>();

        // 优化：为所有对话添加一个默认的系统指令，这是确保模型稳定响应的最佳实践
        history.add(new SystemMessage("你是一个乐于助人的AI助手。"));

        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            request.getHistory().forEach(msg -> {
                if (msg.containsKey("user")) {
                    history.add(new UserMessage(msg.get("user")));
                } else if (msg.containsKey("assistant")) {
                    history.add(new AssistantMessage(msg.get("assistant")));
                }
            });
        }
        
        // 4. 构建Prompt并返回流
        // 优化：直接使用.stream().content()获取最纯净的内容流(Flux<String>)
        // 这避免了处理复杂的ChatResponse对象和潜在的linter/dependency问题
        return chatClient.prompt()
                .messages(history)
                .user(request.getMessage())
                .stream()
                .content()
                // 增强诊断：添加更详细的流生命周期日志
                .doOnSubscribe(subscription -> log.info("🚀 订阅上游内容流成功. Session ID: {}", request.getSessionId()))
                .doOnNext(contentChunk -> log.info("📦 收到原始数据块(长度:{}). Session ID: {}", contentChunk.length(), request.getSessionId()))
                .map(contentChunk -> {
                    log.debug("🛠️ 映射数据块为DTO. Session ID: {}", request.getSessionId());
                    return ChatResponse.builder()
                            .id(UUID.randomUUID().toString()) // 直接生成新的ID
                            .sessionId(request.getSessionId())
                            .content(contentChunk) // contentChunk已经是纯净的String
                            .role("assistant")
                            .timestamp(LocalDateTime.now())
                            .done(false)
                            .build();
                })
                // 优化：在流末尾追加一个"完成"信号，这是前端正确处理流式响应的关键
                .concatWith(Mono.just(ChatResponse.builder()
                        .id(UUID.randomUUID().toString())
                        .sessionId(request.getSessionId())
                        .content("")
                        .role("assistant")
                        .timestamp(LocalDateTime.now())
                        .done(true)
                        .build()))
                .doOnEach(signal -> {
                    if (signal.isOnError()) {
                        log.error("🚨 流式聊天发生致命错误! Session ID: {}", request.getSessionId(), signal.getThrowable());
                    } else if (signal.isOnComplete()) {
                        log.info("✅ 流式聊天正常完成. Session ID: {}", request.getSessionId());
                    } else if (signal.isOnNext()) {
                        log.trace("➡️ 已发送数据块到下游. Session ID: {}", request.getSessionId());
                    }
                });
    }
}
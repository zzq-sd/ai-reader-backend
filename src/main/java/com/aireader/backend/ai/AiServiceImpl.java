package com.aireader.backend.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI服务实现类 - 基于Spring AI 1.0.0
 * 提供文章和笔记的智能分析功能，支持知识图谱构建
 */
@Service
@Slf4j
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.reading-time.words-per-minute:200}")
    private int wordsPerMinute;

    @Autowired
    public AiServiceImpl(@Qualifier("chatClient") @Autowired(required = false) ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        
        // 检查ChatClient是否可用
        if (chatClient == null) {
            log.warn("⚠️ ChatClient未配置，AI功能将不可用");
            log.warn("💡 请检查application.yml中的spring.ai配置");
        } else {
            log.info("✅ Spring AI 1.0.0 ChatClient已配置，AI功能可用");
            // 重新启用AI连接测试
            testAiConnection();
        }
    }
    
    /**
     * 测试AI服务连接
     */
    private void testAiConnection() {
        try {
            log.info("🔍 开始测试AI服务连接...");
            log.info("📋 ChatClient配置状态: {}", chatClient != null ? "已配置" : "未配置");
            
            if (chatClient == null) {
                log.error("❌ ChatClient为null，AI功能不可用");
                return;
            }
            
            // 简单的连接测试
            String testResponse = chatClient.prompt()
                    .user("Hello")
                    .call()
                    .content();
            
            log.info("✅ AI服务连接测试成功，响应: {}", testResponse.substring(0, Math.min(50, testResponse.length())));
            
        } catch (Exception e) {
            log.error("❌ AI服务连接测试失败，详细错误信息:", e);
            log.error("🔧 错误类型: {}", e.getClass().getSimpleName());
            log.error("📝 错误消息: {}", e.getMessage());
            
            // 检查是否是API密钥问题
            if (e.getMessage() != null) {
                if (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized") || e.getMessage().contains("Invalid API Key")) {
                    log.error("🔑 API密钥验证失败，请检查:");
                    log.error("   1. API密钥格式是否正确");
                    log.error("   2. API密钥是否有效且未过期");
                    log.error("   3. 账户是否有足够的额度");
                    log.error("   4. 环境变量ZHIPUAI_API_KEY是否已正确设置");
                } else if (e.getMessage().contains("timeout") || e.getMessage().contains("connect") || e.getMessage().contains("server authentication")) {
                    log.error("🌐 网络连接或身份验证问题，请检查:");
                    log.error("   1. 智谱AI服务是否可访问");
                    log.error("   2. base-url配置是否正确");
                    log.error("   3. 网络连接是否正常");
                    log.error("   4. 防火墙或代理设置是否正确");
                } else if (e.getMessage().contains("model") || e.getMessage().contains("Model")) {
                    log.error("🤖 模型配置问题，请检查:");
                    log.error("   1. GLM模型名称是否正确");
                    log.error("   2. 账户是否有权限使用该模型");
                }
            }
            
            log.warn("⚠️ AI服务连接失败，将使用降级策略处理分析请求");
        }
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
        
        try {
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
        
        try {
            // 检查AI服务是否可用
            if (chatClient == null) {
                log.warn("AI服务不可用，返回基础分析结果");
                return createFallbackNoteAnalysis(noteId, title, content);
            }
            
            // 解析富文本内容为纯文本
            String plainTextContent = extractPlainTextFromRichText(content);
            log.debug("解析后的纯文本内容: {}", plainTextContent.substring(0, Math.min(100, plainTextContent.length())));
            
            if (plainTextContent.trim().isEmpty()) {
                log.warn("笔记内容为空，返回基础分析结果");
                return createFallbackNoteAnalysis(noteId, title, "内容为空");
            }
            
            // 使用相同的AI分析流程
            List<ArticleAnalysisResult.ConceptEntity> concepts = extractConcepts(plainTextContent);
            String summary = generateSummary(plainTextContent);
            List<String> keyPoints = extractKeyPoints(plainTextContent);
            List<String> tags = generateIntelligentTags(plainTextContent, title);
            String sentiment = analyzeSentiment(plainTextContent);
            String topic = generateCategory(plainTextContent);
            
            return NoteAnalysisResult.builder()
                    .noteId(noteId)
                    .keywords(concepts.stream().map(ArticleAnalysisResult.ConceptEntity::getName).collect(Collectors.toList()))
                    .entities(concepts.stream()
                            .map(concept -> {
                                Map<String, Object> map = new HashMap<>();
                                map.put("name", concept.getName());
                                map.put("type", concept.getType());
                                map.put("confidence", concept.getConfidence());
                                return map;
                            })
                            .collect(Collectors.toList()))
                    .topic(topic)
                    .keyPoints(keyPoints)
                    .extractedConcepts(concepts)
                    .intelligentTags(tags)
                    .enhancedSummary(summary)
                    .sentimentAnalysis(sentiment)
                    .build();
        } catch (Exception e) {
            log.error("笔记AI分析失败，使用降级策略: {}", noteId, e);
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
        if (chatClient == null) {
            log.warn("ChatClient未配置，跳过概念提取");
            return Collections.emptyList();
        }
        
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
            """.formatted(content);
            
        try {
            log.debug("🤖 开始调用AI进行概念提取，内容长度: {}", content.length());
            
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
            log.error("📋 输入内容长度: {}", content.length());
            
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
        if (chatClient == null) {
            log.warn("ChatClient未配置，跳过摘要生成");
            return "AI服务未配置，无法生成摘要";
        }
        
        String prompt = """
            请为以下内容生成一个150-200字的精准摘要：
            
            要求：
            1. 突出核心观点和关键信息
            2. 保持客观中性的语调
            3. 包含关键数据和结论
            4. 语言简洁明了
            
            内容：
            %s
            """.formatted(content);
            
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
        if (chatClient == null) {
            log.warn("ChatClient未配置，跳过关键点提取");
            return Collections.emptyList();
        }
        
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
            """.formatted(content);
            
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
        if (chatClient == null) {
            log.warn("ChatClient未配置，跳过标签生成");
            return Collections.emptyList();
        }
        
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
            """.formatted(title, content);
            
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
        if (chatClient == null) {
            log.warn("ChatClient未配置，跳过情感分析");
            return "NEUTRAL";
        }
        
        String prompt = """
            对以下内容进行情感分析，返回以下之一：
            - POSITIVE: 积极正面
            - NEGATIVE: 消极负面  
            - NEUTRAL: 中性客观
            
            只返回单词，不要其他解释。
            
            内容：
            %s
            """.formatted(content);
            
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
        if (chatClient == null) {
            log.warn("ChatClient未配置，跳过分类生成");
            return "其他";
        }
        
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
            """.formatted(content);
            
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
        try {
            // 提取JSON部分
            String jsonPart = extractJsonFromResponse(response);
            JsonNode rootNode = objectMapper.readTree(jsonPart);
            JsonNode conceptsNode = rootNode.get("concepts");
            
            if (conceptsNode == null || !conceptsNode.isArray()) {
                log.warn("概念提取响应格式不正确");
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
        } catch (Exception e) {
            log.error("解析概念响应失败", e);
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
}
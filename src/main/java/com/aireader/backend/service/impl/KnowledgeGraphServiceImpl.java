package com.aireader.backend.service.impl;

import com.aireader.backend.ai.ArticleAnalysisResult;
import com.aireader.backend.ai.EnhancedAiAnalysisService;
import com.aireader.backend.ai.KnowledgeConfigService;
import com.aireader.backend.ai.NoteAnalysisResult;
import com.aireader.backend.dto.ConceptDetailDTO;
import com.aireader.backend.dto.ConceptSearchResultDTO;
import com.aireader.backend.dto.ConceptStatisticsDTO;
import com.aireader.backend.dto.GraphDataDTO;
import com.aireader.backend.model.neo4j.*;
import com.aireader.backend.repository.neo4j.ArticleNodeRepository;
import com.aireader.backend.repository.neo4j.ConceptNodeRepository;
import com.aireader.backend.repository.neo4j.NoteNodeRepository;
import com.aireader.backend.service.KnowledgeGraphService;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱服务实现类 - 简化版本
 * 基于Spring AI 1.0.0和Neo4j实现智能知识图谱构建
 */
@Service
@Slf4j
@Transactional
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final Neo4jTemplate neo4jTemplate;
    private final ConceptNodeRepository conceptNodeRepository;
    private final ArticleNodeRepository articleNodeRepository;
    private final NoteNodeRepository noteNodeRepository;
    private final EnhancedAiAnalysisService enhancedAiAnalysisService;
    
    @Autowired
    private KnowledgeConfigService knowledgeConfigService;

    @Autowired
    public KnowledgeGraphServiceImpl(
            Neo4jTemplate neo4jTemplate,
            ConceptNodeRepository conceptNodeRepository,
            ArticleNodeRepository articleNodeRepository,
            NoteNodeRepository noteNodeRepository,
            EnhancedAiAnalysisService enhancedAiAnalysisService) {
        this.neo4jTemplate = neo4jTemplate;
        this.conceptNodeRepository = conceptNodeRepository;
        this.articleNodeRepository = articleNodeRepository;
        this.noteNodeRepository = noteNodeRepository;
        this.enhancedAiAnalysisService = enhancedAiAnalysisService;
        
        log.info("✅ 知识图谱服务初始化完成 - 基于Spring AI 1.0.0");
    }
    
    /**
     * 处理知识关联配置变更事件
     * 当配置发生变化时，更新服务设置
     */
    @EventListener
    public void handleKnowledgeConfigChangedEvent(KnowledgeConfigService.KnowledgeConfigChangedEvent event) {
        log.info("接收到知识关联配置变更事件，更新知识图谱服务参数");
        
        // 记录当前配置
        double similarityThreshold = knowledgeConfigService.getSimilarityThreshold();
        int maxRelatedNodes = knowledgeConfigService.getMaxRelatedNodes();
        boolean autoRelation = knowledgeConfigService.isAutoRelationEnabled();
        
        log.info("当前知识关联配置: 相似度阈值={}, 最大关联节点数={}, 自动关联={}", 
                similarityThreshold, maxRelatedNodes, autoRelation);
    }

    @Override
    public void integrateArticleToGraph(String articleId, ArticleAnalysisResult analysisResult) {
        log.info("🔗 开始集成文章到知识图谱: {}", articleId);
        
        try {
            // 检查是否启用自动关联
            if (!knowledgeConfigService.isAutoRelationEnabled()) {
                log.info("⚠️ 自动关联功能已禁用，跳过知识图谱集成: {}", articleId);
                return;
            }
            
            // 1. 创建或更新文章节点
            ArticleNode articleNode = createOrUpdateArticleNode(articleId, analysisResult);
            
            // 2. 处理概念提取和关联
            for (ArticleAnalysisResult.ConceptEntity conceptEntity : analysisResult.getConcepts()) {
                ConceptNode conceptNode = createOrUpdateConceptNode(conceptEntity);
                // 创建文章-概念关系
                createArticleConceptRelation(articleNode, conceptNode, conceptEntity);
                log.debug("🔗 关联文章-概念: {} -> {}", articleId, conceptNode.getName());
            }
            
            // 3. 推理概念间关系
            inferConceptRelations(analysisResult.getConcepts());
            
            log.info("✅ 文章知识图谱集成完成: {}", articleId);
            
        } catch (Exception e) {
            log.error("❌ 知识图谱集成失败: {}", articleId, e);
            throw new RuntimeException("知识图谱集成失败", e);
        }
    }

    @Override
    public void integrateNoteToGraph(String noteId, NoteAnalysisResult analysisResult) {
        log.info("📝 开始集成笔记到知识图谱: {}", noteId);
        
        try {
            // 检查是否启用自动关联
            if (!knowledgeConfigService.isAutoRelationEnabled()) {
                log.info("⚠️ 自动关联功能已禁用，跳过知识图谱集成: {}", noteId);
                return;
            }
            
            // 1. 创建或更新笔记节点
            NoteNode noteNode = createOrUpdateNoteNode(noteId, analysisResult);
            
            // 2. 处理概念提取和关联
            for (ArticleAnalysisResult.ConceptEntity conceptEntity : analysisResult.getExtractedConcepts()) {
                ConceptNode conceptNode = createOrUpdateConceptNode(conceptEntity);
                // 创建笔记-概念关系
                createNoteConceptRelation(noteNode, conceptNode, conceptEntity);
                log.debug("🔗 关联笔记-概念: {} -> {}", noteId, conceptNode.getName());
            }
            
            // 3. 推理概念间关系
            inferConceptRelations(analysisResult.getExtractedConcepts());
            
            log.info("✅ 笔记知识图谱集成完成: {}", noteId);
            
        } catch (Exception e) {
            log.error("❌ 笔记知识图谱集成失败: {}", noteId, e);
            throw new RuntimeException("笔记知识图谱集成失败", e);
        }
    }

    @Override
    public ConceptNode createOrUpdateConceptNode(ArticleAnalysisResult.ConceptEntity conceptEntity) {
        log.debug("🔍 创建或更新概念节点: {}", conceptEntity.getName());
        
        try {
            // 1. 检查是否已存在相同概念
            Optional<ConceptNode> existingOpt = conceptNodeRepository.findByName(conceptEntity.getName());
            
            ConceptNode conceptNode;
            if (existingOpt.isPresent()) {
                // 更新现有概念
                conceptNode = existingOpt.get();
                conceptNode.setUpdatedAt(LocalDateTime.now());
                // 更新相关性评分（取最高值）
                if (conceptEntity.getConfidence() != null && conceptNode.getRelevanceScore() != null) {
                    if (conceptEntity.getConfidence() > conceptNode.getRelevanceScore()) {
                        conceptNode.setRelevanceScore(conceptEntity.getConfidence());
                    }
                } else if (conceptEntity.getConfidence() != null) {
                    conceptNode.setRelevanceScore(conceptEntity.getConfidence());
                }
                log.debug("📝 更新现有概念: {}", conceptEntity.getName());
            } else {
                // 创建新概念
                conceptNode = ConceptNode.builder()
                    .name(conceptEntity.getName())
                    .type(conceptEntity.getType())
                    .description(conceptEntity.getContext())
                    .relevanceScore(conceptEntity.getConfidence())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                log.debug("🆕 创建新概念: {}", conceptEntity.getName());
            }
            
            return neo4jTemplate.save(conceptNode);
            
        } catch (Exception e) {
            log.error("❌ 创建或更新概念节点失败: {}", conceptEntity.getName(), e);
            throw new RuntimeException("创建或更新概念节点失败", e);
        }
    }

    @Override
    public void inferConceptRelations(List<ArticleAnalysisResult.ConceptEntity> concepts) {
        log.debug("🕸️ 开始推理概念间关系，概念数量: {}", concepts.size());
        
        try {
            // 检查是否启用自动关联
            if (!knowledgeConfigService.isAutoRelationEnabled()) {
                log.info("⚠️ 自动关联功能已禁用，跳过概念关系推理");
                return;
            }
            
            // 使用AI分析概念间的关系
            if (concepts.size() >= 2) {
                String conceptsText = concepts.stream()
                    .map(c -> c.getName() + "(" + c.getType() + ")")
                    .collect(Collectors.joining(", "));
                
                try {
                    // 获取用户配置的关系构建提示词
                    String relationPrompt = knowledgeConfigService.getRelationPrompt();
                    log.debug("使用用户配置的关系构建提示词模板");
                    
                    // 替换提示词模板中的占位符
                    relationPrompt = relationPrompt.replace("{{entities}}", conceptsText);
                    
                    EnhancedAiAnalysisService.ConceptExtractionResponse response = 
                        enhancedAiAnalysisService.extractConceptsWithRelationsCustomPrompt(relationPrompt);
                    
                    // 处理AI识别的关系
                    if (response.relationships() != null) {
                        // 获取用户配置的最大关联节点数
                        int maxRelatedNodes = knowledgeConfigService.getMaxRelatedNodes();
                        
                        // 限制处理的关系数量
                        List<EnhancedAiAnalysisService.ConceptRelation> filteredRelations = 
                            response.relationships().stream()
                                .limit(maxRelatedNodes)
                                .collect(Collectors.toList());
                        
                        for (EnhancedAiAnalysisService.ConceptRelation relation : filteredRelations) {
                            // 保留原始的关系类型，包括ENABLES等特定领域关系
                            createConceptRelation(relation.source(), relation.target(), 
                                relation.type(), relation.strength());
                        }
                        
                        log.debug("✅ AI推理关系完成，识别{}个关系，处理{}个关系", 
                            response.relationships().size(), filteredRelations.size());
                    } else {
                        log.warn("⚠️ AI返回的关系列表为空，使用基础规则推理");
                        // 降级到基础规则推理
                        inferBasicConceptRelationsWithDomainKnowledge(concepts);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ AI关系推理失败，使用基础规则: {}", e.getMessage());
                    // 降级到基础规则推理
                    inferBasicConceptRelationsWithDomainKnowledge(concepts);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ 概念关系推理失败", e);
        }
    }
    
    /**
     * 基础概念关系推理（降级方案）- 增强版，添加领域知识
     */
    private void inferBasicConceptRelationsWithDomainKnowledge(List<ArticleAnalysisResult.ConceptEntity> concepts) {
        // 使用基础规则推理
        
        // 获取用户配置的最大关联节点数
        int maxRelatedNodes = knowledgeConfigService.getMaxRelatedNodes();
        int processedRelations = 0;
        
        for (int i = 0; i < concepts.size(); i++) {
            for (int j = i + 1; j < concepts.size(); j++) {
                if (processedRelations >= maxRelatedNodes) {
                    log.debug("已达到最大关联节点数: {}", maxRelatedNodes);
                    return;
                }
                
                ArticleAnalysisResult.ConceptEntity concept1 = concepts.get(i);
                ArticleAnalysisResult.ConceptEntity concept2 = concepts.get(j);
                
                // 应用领域知识规则
                if (shouldCreateEnablesRelation(concept1, concept2)) {
                    createConceptRelation(concept1.getName(), concept2.getName(), "ENABLES", 0.8);
                    processedRelations++;
                } else if (shouldCreateEnablesRelation(concept2, concept1)) {
                    createConceptRelation(concept2.getName(), concept1.getName(), "ENABLES", 0.8);
                    processedRelations++;
                } else if (shouldCreateBasicRelation(concept1, concept2)) {
                    // 应用基础关联规则
                    createConceptRelation(concept1.getName(), concept2.getName(), "RELATED_TO", 0.7);
                    processedRelations++;
                }
            }
        }
    }
    
    /**
     * 判断是否应该创建ENABLES关系
     */
    private boolean shouldCreateEnablesRelation(
            ArticleAnalysisResult.ConceptEntity concept1, 
            ArticleAnalysisResult.ConceptEntity concept2) {
        
        // 技术领域的一些启用关系规则
        
        // 1. AI模型启用应用场景
        if ("AI模型".equals(concept1.getType()) && "应用场景".equals(concept2.getType())) {
            return true;
        }
        
        // 2. 大语言模型启用内容分析
        if ("大语言模型".equals(concept1.getName()) && "内容分析".equals(concept2.getName())) {
            return true;
        }
        
        // 3. 大语言模型启用自然语言处理
        if ("大语言模型".equals(concept1.getName()) && "自然语言处理".equals(concept2.getName())) {
            return true;
        }
        
        // 4. 深度学习启用AI模型
        if ("深度学习".equals(concept1.getName()) && 
            concept2.getType() != null && concept2.getType().contains("AI")) {
            return true;
        }
        
        return false;
    }

    /**
     * 基础概念关系推理（降级方案）
     */
    private void inferBasicConceptRelations(List<ArticleAnalysisResult.ConceptEntity> concepts) {
        for (int i = 0; i < concepts.size(); i++) {
            for (int j = i + 1; j < concepts.size(); j++) {
                ArticleAnalysisResult.ConceptEntity concept1 = concepts.get(i);
                ArticleAnalysisResult.ConceptEntity concept2 = concepts.get(j);
                
                // 基于类型和置信度建立基础关系
                if (shouldCreateBasicRelation(concept1, concept2)) {
                    double strength = Math.min(concept1.getConfidence(), concept2.getConfidence());
                    createConceptRelation(concept1.getName(), concept2.getName(), "RELATED_TO", strength);
                }
            }
        }
    }

    /**
     * 判断是否应该创建基础关系
     */
    private boolean shouldCreateBasicRelation(ArticleAnalysisResult.ConceptEntity concept1, 
                                            ArticleAnalysisResult.ConceptEntity concept2) {
        // 相同类型的概念更容易相关
        if (concept1.getType() != null && concept1.getType().equals(concept2.getType())) {
            return concept1.getConfidence() > 0.7 && concept2.getConfidence() > 0.7;
        }
        
        // 不同类型但高置信度的概念
        return concept1.getConfidence() > 0.8 && concept2.getConfidence() > 0.8;
    }

    /**
     * 创建概念间关系
     */
    private void createConceptRelation(String sourceName, String targetName, String relationType, Double strength) {
        try {
            Optional<ConceptNode> sourceOpt = conceptNodeRepository.findByName(sourceName);
            Optional<ConceptNode> targetOpt = conceptNodeRepository.findByName(targetName);
            
            if (sourceOpt.isPresent() && targetOpt.isPresent()) {
                ConceptNode source = sourceOpt.get();
                ConceptNode target = targetOpt.get();
                
                // 检查关系是否已存在
                boolean relationExists = source.getRelatedConcepts().stream()
                    .anyMatch(rel -> rel.getTargetConcept().getName().equals(targetName) 
                             && rel.getRelationType().equals(relationType));
                
                if (!relationExists) {
                    // 使用传入的关系类型，不强制设置为RELATED_TO
                    source.addRelatedConcept(target, relationType, strength);
                    neo4jTemplate.save(source);
                    log.info("🔗 创建概念关系: {} -> {} ({})", sourceName, targetName, relationType);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 创建概念关系失败: {} -> {}", sourceName, targetName, e);
        }
    }

    @Override
    public ConceptNode deduplicateAndMergeConcept(String conceptName) {
        log.debug("🔄 概念去重合并: {}", conceptName);
        
        try {
            // 查找相似概念
            List<ConceptNode> similarConcepts = findSimilarConcepts(conceptName, 0.85);
            
            if (similarConcepts.isEmpty()) {
                return null;
            }
            
            // 选择最佳概念作为主概念
            ConceptNode primaryConcept = selectPrimaryConcept(similarConcepts);
            
            log.info("✅ 概念去重合并完成: {} -> {}", conceptName, primaryConcept.getName());
            return primaryConcept;
            
        } catch (Exception e) {
            log.error("❌ 概念去重合并失败: {}", conceptName, e);
            return null;
        }
    }

    /**
     * 查找相似概念
     */
    private List<ConceptNode> findSimilarConcepts(String conceptName, double threshold) {
        // 简化版本：基于名称相似度
        List<ConceptNode> allConcepts = conceptNodeRepository.findAll();
        return allConcepts.stream()
            .filter(concept -> calculateNameSimilarity(conceptName, concept.getName()) > threshold)
            .collect(Collectors.toList());
    }

    /**
     * 计算名称相似度
     */
    private double calculateNameSimilarity(String name1, String name2) {
        if (name1.equalsIgnoreCase(name2)) {
            return 1.0;
        }
        
        // 简单的编辑距离相似度
        int maxLen = Math.max(name1.length(), name2.length());
        int editDistance = calculateEditDistance(name1.toLowerCase(), name2.toLowerCase());
        return 1.0 - (double) editDistance / maxLen;
    }

    /**
     * 计算编辑距离
     */
    private int calculateEditDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i][j - 1], dp[i - 1][j]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    /**
     * 选择主概念
     */
    private ConceptNode selectPrimaryConcept(List<ConceptNode> concepts) {
        return concepts.stream()
            .max(Comparator.comparing(ConceptNode::getRelevanceScore))
            .orElse(concepts.get(0));
    }

    @Override
    public GraphDataDTO getGraphData(String nodeType, String search, int limit) {
        log.debug("📊 获取图谱数据: nodeType={}, search={}, limit={}", nodeType, search, limit);
        
        try {
            // 简化版本：返回基础图谱数据
            List<GraphDataDTO.NodeDTO> nodes = new ArrayList<>();
            List<GraphDataDTO.EdgeDTO> edges = new ArrayList<>();
            
            // 获取概念节点
            List<ConceptNode> concepts = conceptNodeRepository.findAll();
            for (ConceptNode concept : concepts) {
                GraphDataDTO.NodeDTO nodeDTO = GraphDataDTO.NodeDTO.builder()
                    .id(concept.getId().toString())
                    .label(concept.getName())
                    .type("CONCEPT")
                    .category(concept.getType())
                    .size(concept.getRelevanceScore() != null ? concept.getRelevanceScore() * 10 : 5.0)
                    .importance(concept.getRelevanceScore())
                    .build();
                nodes.add(nodeDTO);
            }
            
            return GraphDataDTO.builder()
                .nodes(nodes)
                .edges(edges)
                .statistics(GraphDataDTO.StatisticsDTO.builder()
                    .totalNodes(nodes.size())
                    .totalEdges(edges.size())
                    .conceptCount(nodes.size())
                    .build())
                .build();
            
        } catch (Exception e) {
            log.error("❌ 获取图谱数据失败", e);
            throw new RuntimeException("获取图谱数据失败", e);
        }
    }

    @Override
    public List<Object> findRelatedArticlesByConcept(String conceptName, int limit) {
        log.debug("🔍 查找概念相关文章: {}", conceptName);
        
        try {
            // 查找概念节点
            Optional<ConceptNode> conceptOpt = conceptNodeRepository.findByName(conceptName);
            if (conceptOpt.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 简化版本：返回空列表
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("❌ 查找概念相关文章失败: {}", conceptName, e);
            return Collections.emptyList();
        }
    }

    @Override
    public ConceptStatisticsDTO getConceptStatistics(String conceptName) {
        // 简化版本：返回null
        return null;
    }

    @Override
    public List<ConceptSearchResultDTO> searchConcepts(String query, int limit) {
        // 简化版本：返回空列表
        return Collections.emptyList();
    }

    @Override
    public ConceptDetailDTO getConceptDetail(String conceptName) {
        // 简化版本：返回null
        return null;
    }

    @Override
    public ArticleNode createOrUpdateArticleNode(String articleId, ArticleAnalysisResult analysisResult) {
        log.debug("📄 创建或更新文章节点: {}", articleId);
        
        try {
            Optional<ArticleNode> existingOpt = articleNodeRepository.findByMysqlId(articleId);
            
            ArticleNode articleNode;
            if (existingOpt.isPresent()) {
                articleNode = existingOpt.get();
                articleNode.setUpdatedAt(LocalDateTime.now());
            } else {
                articleNode = ArticleNode.builder()
                    .mysqlId(articleId)
                    .summary(analysisResult.getSummary())
                    .sentimentScore(convertSentimentToScore(analysisResult.getSentiment()))
                    .readingTimeMinutes(analysisResult.getReadingTimeMinutes())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            }
            
            return neo4jTemplate.save(articleNode);
            
        } catch (Exception e) {
            log.error("❌ 创建或更新文章节点失败: {}", articleId, e);
            throw new RuntimeException("创建或更新文章节点失败", e);
        }
    }

    @Override
    public NoteNode createOrUpdateNoteNode(String noteId, NoteAnalysisResult analysisResult) {
        log.debug("📝 创建或更新笔记节点: {}", noteId);
        
        try {
            Optional<NoteNode> existingOpt = noteNodeRepository.findByMysqlId(noteId);
            
            NoteNode noteNode;
            if (existingOpt.isPresent()) {
                noteNode = existingOpt.get();
                noteNode.setUpdatedAt(LocalDateTime.now());
            } else {
                                 noteNode = NoteNode.builder()
                     .mysqlId(noteId)
                     .contentSummary(analysisResult.getEnhancedSummary())
                     .createdAt(LocalDateTime.now())
                     .updatedAt(LocalDateTime.now())
                     .build();
            }
            
            return neo4jTemplate.save(noteNode);
            
        } catch (Exception e) {
            log.error("❌ 创建或更新笔记节点失败: {}", noteId, e);
            throw new RuntimeException("创建或更新笔记节点失败", e);
        }
    }

    @Override
    public void createArticleConceptRelation(ArticleNode articleNode, ConceptNode conceptNode, 
                                           ArticleAnalysisResult.ConceptEntity conceptEntity) {
        log.debug("🔗 创建文章-概念关系: {} -> {}", articleNode.getMysqlId(), conceptNode.getName());
        
        try {
            // 创建文章-概念关系
            ArticleConceptRelationship relationship = new ArticleConceptRelationship();
            relationship.setConcept(conceptNode);
            relationship.setArticle(articleNode);
            relationship.setRelevance(conceptEntity.getConfidence());
            relationship.setRelationType("ABOUT");
            relationship.setEntityType(conceptEntity.getType());
            relationship.setCreatedAt(LocalDateTime.now());
            relationship.setOccurrences(1);
            
            // 保存关系
            neo4jTemplate.save(relationship);
            
            log.info("✅ 创建文章-概念关系成功: {} -> {}", articleNode.getMysqlId(), conceptNode.getName());
        } catch (Exception e) {
            log.error("❌ 创建文章-概念关系失败: {} -> {}", articleNode.getMysqlId(), conceptNode.getName(), e);
        }
    }

    @Override
    public void createNoteConceptRelation(NoteNode noteNode, ConceptNode conceptNode, 
                                        ArticleAnalysisResult.ConceptEntity conceptEntity) {
        log.debug("🔗 创建笔记-概念关系: {} -> {}", noteNode.getMysqlId(), conceptNode.getName());
        
        try {
            // 使用NoteNode的addConcept方法创建关系
            noteNode.addConcept(conceptNode, conceptEntity.getConfidence(), "DISCUSSES");
            
            // 保存更新后的笔记节点（会自动保存关系）
            neo4jTemplate.save(noteNode);
            
            log.info("✅ 创建笔记-概念关系成功: {} -> {}", noteNode.getMysqlId(), conceptNode.getName());
        } catch (Exception e) {
            log.error("❌ 创建笔记-概念关系失败: {} -> {}", noteNode.getMysqlId(), conceptNode.getName(), e);
        }
    }

    @Override
    public Double calculateConceptImportance(Long conceptId) {
        // 简化版本：返回默认值
        return 0.0;
    }

    @Override
    public List<List<ConceptNode>> discoverConceptClusters(int minClusterSize) {
        // 简化版本：返回空列表
        return Collections.emptyList();
    }

    @Override
    public List<ConceptNode> recommendConcepts(String userId, int limit) {
        // 简化版本：返回空列表
        return Collections.emptyList();
    }

    /**
     * 将情感字符串转换为分数
     */
    private Double convertSentimentToScore(String sentiment) {
        if (sentiment == null) return 0.0;
        
        switch (sentiment.toUpperCase()) {
            case "POSITIVE":
                return 1.0;
            case "NEGATIVE":
                return -1.0;
            case "NEUTRAL":
            default:
                return 0.0;
        }
    }
} 
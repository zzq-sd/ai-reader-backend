package com.aireader.backend.service;

import com.aireader.backend.ai.ArticleAnalysisResult;
import com.aireader.backend.ai.NoteAnalysisResult;
import com.aireader.backend.dto.GraphDataDTO;
import com.aireader.backend.dto.ConceptStatisticsDTO;
import com.aireader.backend.dto.ConceptSearchResultDTO;
import com.aireader.backend.dto.ConceptDetailDTO;
import com.aireader.backend.model.neo4j.ArticleNode;
import com.aireader.backend.model.neo4j.ConceptNode;
import com.aireader.backend.model.neo4j.NoteNode;

import java.util.List;

/**
 * 知识图谱核心服务接口
 * 基于Spring AI 1.0.0，提供智能知识图谱构建和查询功能
 */
public interface KnowledgeGraphService {
    
    // ====================== 图谱构建 ======================
    
    /**
     * 处理文章的知识图谱集成
     * 基于AI分析结果构建或更新知识图谱
     * 
     * @param articleId 文章ID
     * @param analysisResult AI分析结果
     */
    void integrateArticleToGraph(String articleId, ArticleAnalysisResult analysisResult);
    
    /**
     * 处理笔记的知识图谱集成
     * 基于AI分析结果构建或更新知识图谱
     * 
     * @param noteId 笔记ID
     * @param analysisResult AI分析结果
     */
    void integrateNoteToGraph(String noteId, NoteAnalysisResult analysisResult);
    
    // ====================== 概念管理 ======================
    
    /**
     * 创建或更新概念节点
     * 包含智能去重和合并逻辑
     * 
     * @param conceptEntity 概念实体信息
     * @return 保存后的概念节点
     */
    ConceptNode createOrUpdateConceptNode(ArticleAnalysisResult.ConceptEntity conceptEntity);
    
    /**
     * 推理概念间关系
     * 基于AI分析结果自动建立概念间的关联
     * 
     * @param concepts 概念列表
     */
    void inferConceptRelations(List<ArticleAnalysisResult.ConceptEntity> concepts);
    
    /**
     * 概念去重合并
     * 检测相似概念并合并
     * 
     * @param conceptName 概念名称
     * @return 合并后的概念
     */
    ConceptNode deduplicateAndMergeConcept(String conceptName);
    
    // ====================== 图谱查询 ======================
    
    /**
     * 获取知识图谱数据
     * 用于前端可视化
     * 
     * @param nodeType 节点类型过滤
     * @param search 搜索关键词
     * @param limit 结果限制
     * @return 图谱数据
     */
    GraphDataDTO getGraphData(String nodeType, String search, int limit);
    
    /**
     * 查询概念相关的文章
     * 
     * @param conceptName 概念名称
     * @param limit 限制数量
     * @return 相关文章列表
     */
    List<Object> findRelatedArticlesByConcept(String conceptName, int limit);
    
    /**
     * 获取概念统计信息
     * 
     * @param conceptName 概念名称
     * @return 概念统计数据
     */
    ConceptStatisticsDTO getConceptStatistics(String conceptName);
    
    /**
     * 搜索概念
     * 
     * @param query 搜索查询
     * @param limit 结果限制
     * @return 搜索结果
     */
    List<ConceptSearchResultDTO> searchConcepts(String query, int limit);
    
    /**
     * 获取概念详情
     * 
     * @param conceptName 概念名称
     * @return 概念详情
     */
    ConceptDetailDTO getConceptDetail(String conceptName);
    
    // ====================== 节点操作 ======================
    
    /**
     * 创建或更新文章节点
     * 
     * @param articleId 文章ID
     * @param analysisResult AI分析结果
     * @return 文章节点
     */
    ArticleNode createOrUpdateArticleNode(String articleId, ArticleAnalysisResult analysisResult);
    
    /**
     * 创建或更新笔记节点
     * 
     * @param noteId 笔记ID
     * @param analysisResult AI分析结果
     * @return 笔记节点
     */
    NoteNode createOrUpdateNoteNode(String noteId, NoteAnalysisResult analysisResult);
    
    /**
     * 建立文章-概念关系
     * 
     * @param articleNode 文章节点
     * @param conceptNode 概念节点
     * @param conceptEntity 概念实体信息
     */
    void createArticleConceptRelation(ArticleNode articleNode, ConceptNode conceptNode, 
                                    ArticleAnalysisResult.ConceptEntity conceptEntity);
    
    /**
     * 建立笔记-概念关系
     * 
     * @param noteNode 笔记节点
     * @param conceptNode 概念节点
     * @param conceptEntity 概念实体信息
     */
    void createNoteConceptRelation(NoteNode noteNode, ConceptNode conceptNode, 
                                 ArticleAnalysisResult.ConceptEntity conceptEntity);
    
    // ====================== 图谱分析 ======================
    
    /**
     * 分析概念重要性
     * 基于图谱结构计算概念的重要性分数
     * 
     * @param conceptId 概念ID
     * @return 重要性分数
     */
    Double calculateConceptImportance(Long conceptId);
    
    /**
     * 发现概念集群
     * 基于图谱结构发现相关概念的集群
     * 
     * @param minClusterSize 最小集群大小
     * @return 概念集群列表
     */
    List<List<ConceptNode>> discoverConceptClusters(int minClusterSize);
    
    /**
     * 推荐相关概念
     * 基于用户兴趣和图谱结构推荐相关概念
     * 
     * @param userId 用户ID
     * @param limit 推荐数量
     * @return 推荐概念列表
     */
    List<ConceptNode> recommendConcepts(String userId, int limit);
}

/**
 * 文章图谱DTO
 */
interface ArticleGraphDTO {
    String getId();
    String getTitle();
    String getSummary();
    Double getRelevanceScore();
    String getPublishDate();
    List<String> getTags();
} 
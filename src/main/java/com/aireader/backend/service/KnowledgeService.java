package com.aireader.backend.service;

import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.dto.ConceptDetailDTO;
import com.aireader.backend.dto.ConceptStatisticsDTO;
import com.aireader.backend.dto.GraphDataDTO;
import com.aireader.backend.dto.GraphVisualizationDto;
import com.aireader.backend.dto.RelatedItemDto;
import com.aireader.backend.model.neo4j.ConceptNode;

import java.util.List;
import java.util.Map;

/**
 * 知识关联服务接口
 * 提供知识图谱构建、查询和推荐功能
 */
public interface KnowledgeService {

    /**
     * 根据文章ID构建知识图谱
     *
     * @param articleId 文章ID
     * @return 是否构建成功
     */
    boolean buildKnowledgeGraphFromArticle(String articleId);

    /**
     * 根据笔记ID构建知识图谱
     *
     * @param noteId 笔记ID
     * @return 是否构建成功
     */
    boolean buildKnowledgeGraphFromNote(String noteId);

    /**
     * 获取与指定文章相关的内容项
     *
     * @param articleId 文章ID
     * @param limit 返回条数限制
     * @return 相关内容项列表
     */
    List<RelatedItemDto> getRelatedItemsForArticle(String articleId, int limit);

    /**
     * 获取与指定笔记相关的内容项
     *
     * @param noteId 笔记ID
     * @param limit 返回条数限制
     * @return 相关内容项列表
     */
    List<RelatedItemDto> getRelatedItemsForNote(String noteId, int limit);

    /**
     * 获取与指定概念相关的内容项
     *
     * @param conceptId 概念ID
     * @param limit 返回条数限制
     * @return 相关内容项列表
     */
    List<RelatedItemDto> getRelatedItemsForConcept(String conceptId, int limit);

    /**
     * 获取以指定节点为中心的图谱可视化数据
     *
     * @param centerNodeId 中心节点ID
     * @param nodeType 节点类型（"article", "note", "concept"）
     * @param depth 深度（默认为2）
     * @return 图谱可视化数据
     */
    GraphVisualizationDto getGraphVisualizationData(String centerNodeId, String nodeType, int depth);

    /**
     * 根据名称搜索概念
     *
     * @param conceptName 概念名称（支持模糊匹配）
     * @param limit 返回条数限制
     * @return 概念列表
     */
    List<ConceptNode> searchConcepts(String conceptName, int limit);

    /**
     * 保存或更新概念节点
     *
     * @param name 概念名称
     * @param description 概念描述
     * @return 保存的概念节点
     */
    ConceptNode saveOrUpdateConcept(String name, String description);

    /**
     * 创建文章与概念的关联关系
     *
     * @param articleId 文章ID
     * @param conceptNames 概念名称列表
     * @return 关联的概念数量
     */
    int createArticleConceptRelationships(String articleId, List<String> conceptNames);

    /**
     * 创建笔记与概念的关联关系
     *
     * @param noteId 笔记ID
     * @param conceptNames 概念名称列表
     * @return 关联的概念数量
     */
    int createNoteConceptRelationships(String noteId, List<String> conceptNames);

    /**
     * 获取知识图谱统计信息
     *
     * @return 统计信息，包括节点数量、关系数量等
     */
    Map<String, Object> getKnowledgeGraphStatistics();

    /**
     * 获取完整知识图谱数据（用于前端D3.js可视化）
     *
     * @param nodeType 节点类型过滤（ALL, CONCEPT, ARTICLE, NOTE）
     * @param search 搜索关键词
     * @param limit 节点数量限制
     * @return 图谱数据
     */
    GraphDataDTO getGraphData(String nodeType, String search, int limit);

    /**
     * 获取概念相关的文章
     *
     * @param conceptName 概念名称
     * @param limit 返回条数限制
     * @return 相关文章列表
     */
    List<ArticleDTO> findRelatedArticlesByConcept(String conceptName, int limit);

    /**
     * 获取概念统计信息
     *
     * @param conceptName 概念名称
     * @return 概念统计信息
     */
    ConceptStatisticsDTO getConceptStatistics(String conceptName);

    /**
     * 获取概念详情
     *
     * @param conceptName 概念名称
     * @return 概念详情
     */
    ConceptDetailDTO getConceptDetail(String conceptName);
} 
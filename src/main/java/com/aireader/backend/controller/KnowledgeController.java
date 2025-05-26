package com.aireader.backend.controller;

import com.aireader.backend.dto.GraphVisualizationDto;
import com.aireader.backend.dto.RelatedItemDto;
import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.model.neo4j.ConceptNode;
import com.aireader.backend.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱控制器
 */
@RestController
@RequestMapping("/api/v1/knowledge")
@Tag(name = "Knowledge Controller", description = "知识图谱相关接口")
@Slf4j
public class KnowledgeController extends BaseController {

    @Autowired
    private KnowledgeService knowledgeService;

    /**
     * 获取与文章相关的内容项
     *
     * @param articleId 文章ID
     * @param limit 返回条数限制（可选，默认为10）
     * @return 相关内容项列表
     */
    @Operation(summary = "获取与文章相关的内容项")
    @GetMapping("/related-items/article/{articleId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<RelatedItemDto>>> getRelatedItemsForArticle(
            @Parameter(description = "文章ID") @PathVariable String articleId,
            @Parameter(description = "返回条数限制，默认10") @RequestParam(defaultValue = "10") int limit) {
        
        log.info("获取文章相关项，文章ID: {}，限制数量: {}", articleId, limit);
        List<RelatedItemDto> relatedItems = knowledgeService.getRelatedItemsForArticle(articleId, limit);
        return ResponseEntity.ok(ApiResponse.success(relatedItems));
    }

    /**
     * 获取与笔记相关的内容项
     *
     * @param noteId 笔记ID
     * @param limit 返回条数限制（可选，默认为10）
     * @return 相关内容项列表
     */
    @Operation(summary = "获取与笔记相关的内容项")
    @GetMapping("/related-items/note/{noteId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<RelatedItemDto>>> getRelatedItemsForNote(
            @Parameter(description = "笔记ID") @PathVariable String noteId,
            @Parameter(description = "返回条数限制，默认10") @RequestParam(defaultValue = "10") int limit) {
        
        log.info("获取笔记相关项，笔记ID: {}，限制数量: {}", noteId, limit);
        List<RelatedItemDto> relatedItems = knowledgeService.getRelatedItemsForNote(noteId, limit);
        return ResponseEntity.ok(ApiResponse.success(relatedItems));
    }

    /**
     * 获取与概念相关的内容项
     *
     * @param conceptId 概念ID
     * @param limit 返回条数限制（可选，默认为10）
     * @return 相关内容项列表
     */
    @Operation(summary = "获取与概念相关的内容项")
    @GetMapping("/related-items/concept/{conceptId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<RelatedItemDto>>> getRelatedItemsForConcept(
            @Parameter(description = "概念ID") @PathVariable String conceptId,
            @Parameter(description = "返回条数限制，默认10") @RequestParam(defaultValue = "10") int limit) {
        
        log.info("获取概念相关项，概念ID: {}，限制数量: {}", conceptId, limit);
        List<RelatedItemDto> relatedItems = knowledgeService.getRelatedItemsForConcept(conceptId, limit);
        return ResponseEntity.ok(ApiResponse.success(relatedItems));
    }

    /**
     * 获取知识图谱可视化数据
     *
     * @param centerNodeId 中心节点ID
     * @param nodeType 节点类型（"article", "note", "concept"）
     * @param depth 深度（可选，默认为2）
     * @return 图谱可视化数据
     */
    @Operation(summary = "获取知识图谱可视化数据")
    @GetMapping("/graph")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<GraphVisualizationDto>> getGraphVisualizationData(
            @Parameter(description = "中心节点ID") @RequestParam String centerNodeId,
            @Parameter(description = "节点类型（article、note、concept）") @RequestParam String nodeType,
            @Parameter(description = "图谱深度，默认2") @RequestParam(defaultValue = "2") int depth) {
        
        log.info("获取知识图谱可视化数据，中心节点ID: {}，节点类型: {}，深度: {}", centerNodeId, nodeType, depth);
        GraphVisualizationDto graphData = knowledgeService.getGraphVisualizationData(centerNodeId, nodeType, depth);
        return ResponseEntity.ok(ApiResponse.success(graphData));
    }

    /**
     * 搜索概念
     *
     * @param query 搜索关键词
     * @param limit 返回条数限制（可选，默认为10）
     * @return 概念列表
     */
    @Operation(summary = "搜索概念")
    @GetMapping("/concepts/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<ConceptNode>>> searchConcepts(
            @Parameter(description = "搜索关键词") @RequestParam String query,
            @Parameter(description = "返回条数限制，默认10") @RequestParam(defaultValue = "10") int limit) {
        
        log.info("搜索概念，关键词: {}，限制数量: {}", query, limit);
        List<ConceptNode> concepts = knowledgeService.searchConcepts(query, limit);
        return ResponseEntity.ok(ApiResponse.success(concepts));
    }

    /**
     * 为文章手动添加概念
     *
     * @param articleId 文章ID
     * @param conceptNames 概念名称列表
     * @return 添加结果
     */
    @Operation(summary = "为文章手动添加概念")
    @PostMapping("/article/{articleId}/concepts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Integer>> addConceptsToArticle(
            @Parameter(description = "文章ID") @PathVariable String articleId,
            @Parameter(description = "概念名称列表") @RequestBody List<String> conceptNames) {
        
        log.info("为文章添加概念，文章ID: {}，概念数量: {}", articleId, conceptNames.size());
        int count = knowledgeService.createArticleConceptRelationships(articleId, conceptNames);
        return ResponseEntity.ok(ApiResponse.success("成功添加 " + count + " 个概念关联", count));
    }

    /**
     * 为笔记手动添加概念
     *
     * @param noteId 笔记ID
     * @param conceptNames 概念名称列表
     * @return 添加结果
     */
    @Operation(summary = "为笔记手动添加概念")
    @PostMapping("/note/{noteId}/concepts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Integer>> addConceptsToNote(
            @Parameter(description = "笔记ID") @PathVariable String noteId,
            @Parameter(description = "概念名称列表") @RequestBody List<String> conceptNames) {
        
        log.info("为笔记添加概念，笔记ID: {}，概念数量: {}", noteId, conceptNames.size());
        int count = knowledgeService.createNoteConceptRelationships(noteId, conceptNames);
        return ResponseEntity.ok(ApiResponse.success("成功添加 " + count + " 个概念关联", count));
    }

    /**
     * 从文章构建知识图谱
     *
     * @param articleId 文章ID
     * @return 构建结果
     */
    @Operation(summary = "从文章构建知识图谱")
    @PostMapping("/build/article/{articleId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Boolean>> buildKnowledgeGraphFromArticle(
            @Parameter(description = "文章ID") @PathVariable String articleId) {
        
        log.info("从文章构建知识图谱，文章ID: {}", articleId);
        boolean success = knowledgeService.buildKnowledgeGraphFromArticle(articleId);
        return ResponseEntity.ok(ApiResponse.success(success ? "知识图谱构建成功" : "知识图谱构建失败", success));
    }

    /**
     * 从笔记构建知识图谱
     *
     * @param noteId 笔记ID
     * @return 构建结果
     */
    @Operation(summary = "从笔记构建知识图谱")
    @PostMapping("/build/note/{noteId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Boolean>> buildKnowledgeGraphFromNote(
            @Parameter(description = "笔记ID") @PathVariable String noteId) {
        
        log.info("从笔记构建知识图谱，笔记ID: {}", noteId);
        boolean success = knowledgeService.buildKnowledgeGraphFromNote(noteId);
        return ResponseEntity.ok(ApiResponse.success(success ? "知识图谱构建成功" : "知识图谱构建失败", success));
    }

    /**
     * 获取知识图谱统计信息
     *
     * @return 统计信息
     */
    @Operation(summary = "获取知识图谱统计信息")
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKnowledgeGraphStatistics() {
        
        log.info("获取知识图谱统计信息");
        Map<String, Object> statistics = knowledgeService.getKnowledgeGraphStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
} 
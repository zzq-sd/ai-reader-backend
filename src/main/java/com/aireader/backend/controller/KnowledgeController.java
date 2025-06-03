package com.aireader.backend.controller;

import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.dto.ConceptDetailDTO;
import com.aireader.backend.dto.ConceptStatisticsDTO;
import com.aireader.backend.dto.GraphDataDTO;
import com.aireader.backend.dto.GraphVisualizationDto;
import com.aireader.backend.dto.RelatedItemDto;
import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.model.neo4j.ConceptNode;
import com.aireader.backend.service.KnowledgeService;
import com.aireader.backend.util.KnowledgeGraphPerformanceMonitor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.neo4j.core.Neo4jClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private KnowledgeGraphPerformanceMonitor performanceMonitor;
    
    @Autowired
    private Neo4jClient neo4jClient;

    /**
     * 获取与文章相关的内容项
     *
     * @param articleId 文章ID
     * @param limit 返回条数限制（可选，默认为10）
     * @return 相关内容项列表
     */
    @Operation(summary = "获取与文章相关的内容项")
    @GetMapping("/related-items/article/{articleId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'PREMIUM', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('ROLE_USER', 'PREMIUM', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('ROLE_USER', 'PREMIUM', 'ADMIN')")
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
    @PreAuthorize("hasAuthority('ROLE_USER') or hasRole('PREMIUM') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GraphVisualizationDto>> getGraphVisualizationData(
            @Parameter(description = "中心节点ID") @RequestParam String centerNodeId,
            @Parameter(description = "节点类型（article、note、concept）") @RequestParam String nodeType,
            @Parameter(description = "图谱深度，默认2") @RequestParam(defaultValue = "2") int depth) {
        
        log.info("获取知识图谱可视化数据，中心节点ID: {}，节点类型: {}，深度: {}", centerNodeId, nodeType, depth);
        GraphVisualizationDto graphData = knowledgeService.getGraphVisualizationData(centerNodeId, nodeType, depth);
        return ResponseEntity.ok(ApiResponse.success(graphData));
    }

    /**
     * 获取完整知识图谱数据（用于前端D3.js可视化）
     *
     * @param nodeType 节点类型过滤（ALL, CONCEPT, ARTICLE, NOTE）
     * @param search 搜索关键词
     * @param limit 节点数量限制
     * @return 图谱数据
     */
    @Operation(summary = "获取完整知识图谱数据")
    @GetMapping("/graph-data")
    public ResponseEntity<ApiResponse<GraphDataDTO>> getGraphData(
            @Parameter(description = "节点类型过滤") @RequestParam(defaultValue = "ALL") String nodeType,
            @Parameter(description = "搜索关键词") @RequestParam(defaultValue = "") String search,
            @Parameter(description = "节点数量限制") @RequestParam(defaultValue = "100") int limit) {
        
        log.info("【知识图谱】控制器收到请求: nodeType={}, search={}, limit={}", nodeType, search, limit);
        
        try {
            // 添加Neo4j连接测试
            try {
                log.info("【知识图谱】控制器执行Neo4j连接测试");
                String testQuery = "MATCH (n) RETURN labels(n) AS labels, count(*) AS count";
                Collection<Map<String, Object>> testResults = neo4jClient.query(testQuery)
                    .fetch().all();
                
                if (testResults.isEmpty()) {
                    log.warn("【知识图谱】Neo4j连接正常但数据库可能为空");
                } else {
                    log.info("【知识图谱】Neo4j连接正常，数据库中有节点，示例:");
                    for (Map<String, Object> row : testResults) {
                        log.info("【知识图谱】- 标签: {}, 数量: {}", 
                                row.get("labels"), row.get("count"));
                    }
                }
            } catch (Exception e) {
                log.error("【知识图谱】Neo4j连接测试失败: {}", e.getMessage(), e);
                throw new RuntimeException("Neo4j连接失败", e);
            }
            
            // 调用服务层方法获取真实数据
            long startTime = System.currentTimeMillis();
            GraphDataDTO graphData = knowledgeService.getGraphData(nodeType, search, limit);
            long endTime = System.currentTimeMillis();
            
            log.info("【知识图谱】控制器获取数据完成，耗时: {}ms, 节点数: {}, 边数: {}", 
                    (endTime - startTime), graphData.getNodes().size(), graphData.getEdges().size());
            
            return ResponseEntity.ok(ApiResponse.success(graphData));
            
        } catch (Exception e) {
            log.error("【知识图谱】获取数据失败: {}", e.getMessage(), e);
            log.error("【知识图谱】异常详情", e);
            throw new RuntimeException("获取知识图谱数据失败", e);
        }
    }
    
    // 创建测试图谱数据
    private GraphDataDTO createTestGraphData() {
        List<GraphDataDTO.NodeDTO> nodes = new ArrayList<>();
        List<GraphDataDTO.EdgeDTO> edges = new ArrayList<>();
        
        // 添加概念节点
        for (int i = 1; i <= 10; i++) {
            GraphDataDTO.NodeDTO conceptNode = GraphDataDTO.NodeDTO.builder()
                    .id("concept_测试概念" + i)
                    .label("测试概念" + i)
                    .type("CONCEPT")
                    .size(10.0 + i)
                    .color("#9c27b0")
                    .importance(0.6)
                    .build();
            nodes.add(conceptNode);
        }
        
        // 添加文章节点
        for (int i = 1; i <= 5; i++) {
            GraphDataDTO.NodeDTO articleNode = GraphDataDTO.NodeDTO.builder()
                    .id("article_" + i)
                    .label("测试文章" + i)
                    .type("ARTICLE")
                    .size(15.0)
                    .color("#2196f3")
                    .importance(0.7)
                    .build();
            nodes.add(articleNode);
        }
        
        // 添加笔记节点
        for (int i = 1; i <= 3; i++) {
            GraphDataDTO.NodeDTO noteNode = GraphDataDTO.NodeDTO.builder()
                    .id("note_" + i)
                    .label("测试笔记" + i)
                    .type("NOTE")
                    .size(12.0)
                    .color("#ff9800")
                    .importance(0.5)
                    .build();
            nodes.add(noteNode);
        }
        
        // 添加边 - 概念和文章之间
        for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= 3; j++) {
                if (Math.random() > 0.3) {  // 随机添加部分关系
                    GraphDataDTO.EdgeDTO edge = GraphDataDTO.EdgeDTO.builder()
                            .id("edge_concept_article_" + i + "_" + j)
                            .source("concept_测试概念" + i)
                            .target("article_" + j)
                            .label("MENTIONS")
                            .type("MENTIONS")
                            .weight(0.8)
                            .color("#999999")
                            .build();
                    edges.add(edge);
                }
            }
        }
        
        // 添加边 - 概念和笔记之间
        for (int i = 6; i <= 10; i++) {
            for (int j = 1; j <= 3; j++) {
                if (Math.random() > 0.4) {  // 随机添加部分关系
                    GraphDataDTO.EdgeDTO edge = GraphDataDTO.EdgeDTO.builder()
                            .id("edge_concept_note_" + i + "_" + j)
                            .source("concept_测试概念" + i)
                            .target("note_" + j)
                            .label("CONTAINS")
                            .type("CONTAINS")
                            .weight(0.7)
                            .color("#999999")
                            .build();
                    edges.add(edge);
                }
            }
        }
        
        // 添加边 - 概念和概念之间
        for (int i = 1; i <= 5; i++) {
            for (int j = 6; j <= 10; j++) {
                if (Math.random() > 0.7) {  // 随机添加部分关系
                    GraphDataDTO.EdgeDTO edge = GraphDataDTO.EdgeDTO.builder()
                            .id("edge_concept_concept_" + i + "_" + j)
                            .source("concept_测试概念" + i)
                            .target("concept_测试概念" + j)
                            .label("RELATED_TO")
                            .type("RELATED_TO")
                            .weight(0.6)
                            .color("#999999")
                            .build();
                    edges.add(edge);
                }
            }
        }
        
        // 创建统计信息
        GraphDataDTO.StatisticsDTO statistics = GraphDataDTO.StatisticsDTO.builder()
                .totalNodes(nodes.size())
                .totalEdges(edges.size())
                .articleCount((int)nodes.stream().filter(n -> "ARTICLE".equals(n.getType())).count())
                .noteCount((int)nodes.stream().filter(n -> "NOTE".equals(n.getType())).count())
                .conceptCount((int)nodes.stream().filter(n -> "CONCEPT".equals(n.getType())).count())
                .avgConnectivity(nodes.isEmpty() ? 0.0 : (double) edges.size() / nodes.size())
                .build();
        
        return GraphDataDTO.builder()
                .nodes(nodes)
                .edges(edges)
                .statistics(statistics)
                .build();
    }

    /**
     * 简单测试端点
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.error("🔥🔥🔥 测试端点被调用");
        return ResponseEntity.ok("Knowledge API is working!");
    }

    /**
     * 临时诊断端点 - 获取原始Neo4j数据结构分析
     */
    @GetMapping("/diagnostic")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'PREMIUM', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> diagnosticQuery(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("【诊断】开始执行Neo4j数据结构诊断，limit: {}", limit);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试1：简单节点查询
            log.info("【诊断】执行基础节点查询");
            String basicQuery = "MATCH (n) RETURN n, labels(n) LIMIT " + limit;
            Collection<Map<String, Object>> basicResults = neo4jClient.query(basicQuery).fetch().all();
            result.put("basicQueryResultCount", basicResults.size());
            
            if (!basicResults.isEmpty()) {
                Map<String, Object> firstBasic = basicResults.iterator().next();
                result.put("sampleBasicNode", firstBasic);
                log.info("【诊断】基础查询示例节点: {}", firstBasic.keySet());
            }
            
            // 测试2：优化的节点和关系查询
            log.info("【诊断】执行优化的节点关系查询");
            String optimizedQuery = 
                "MATCH (n) " +
                "OPTIONAL MATCH (n)-[r]->(m) " +
                "RETURN n as node, labels(n) as nodeLabels, " +
                "collect({type: type(r), targetId: id(m), targetLabels: labels(m)}) as relationships " +
                "LIMIT " + limit;
                
            Collection<Map<String, Object>> optimizedResults = neo4jClient.query(optimizedQuery).fetch().all();
            result.put("optimizedQueryResultCount", optimizedResults.size());
            
            if (!optimizedResults.isEmpty()) {
                Map<String, Object> sample = optimizedResults.iterator().next();
                result.put("sampleOptimizedKeys", sample.keySet());
                result.put("nodeLabels", sample.get("nodeLabels"));
                
                // 检查节点数据结构
                Object nodeObj = sample.get("node");
                if (nodeObj instanceof Map) {
                    Map<String, Object> nodeMap = (Map<String, Object>) nodeObj;
                    result.put("nodeProperties", nodeMap.keySet());
                    result.put("sampleNodeData", nodeMap);
                }
                
                // 检查关系数据结构
                List<Map<String, Object>> rels = (List<Map<String, Object>>) sample.get("relationships");
                if (rels != null && !rels.isEmpty()) {
                    Map<String, Object> firstRel = rels.get(0);
                    if (firstRel != null && !firstRel.isEmpty()) {
                        result.put("sampleRelationship", firstRel);
                        log.info("【诊断】关系示例: {}", firstRel);
                    }
                }
            }
            
            // 测试3：原始复杂查询（用于对比）
            log.info("【诊断】执行原始复杂查询");
            String originalQuery = 
                "MATCH (n) " +
                "OPTIONAL MATCH (n)-[r]->(m) " +
                "RETURN n, labels(n) AS nodeLabels, " +
                "collect({rel: r, target: m, targetLabels: labels(m)}) AS outRelationships " +
                "LIMIT " + limit;
                
            Collection<Map<String, Object>> originalResults = neo4jClient.query(originalQuery).fetch().all();
            result.put("originalQueryResultCount", originalResults.size());
            
            if (!originalResults.isEmpty()) {
                Map<String, Object> originalSample = originalResults.iterator().next();
                result.put("originalSampleKeys", originalSample.keySet());
                
                // 检查原始关系结构
                List<Map<String, Object>> originalRels = (List<Map<String, Object>>) originalSample.get("outRelationships");
                if (originalRels != null && !originalRels.isEmpty()) {
                    Map<String, Object> firstOriginalRel = originalRels.get(0);
                    if (firstOriginalRel != null) {
                        result.put("originalRelationshipKeys", firstOriginalRel.keySet());
                        log.info("【诊断】原始关系键: {}", firstOriginalRel.keySet());
                    }
                }
            }
            
            log.info("【诊断】诊断查询完成");
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("【诊断】诊断查询失败: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getName());
            result.put("stackTrace", e.getStackTrace());
            return ResponseEntity.status(500).body(ApiResponse.error("诊断查询失败: " + e.getMessage()));
        }
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
    @PreAuthorize("hasAuthority('ROLE_USER') or hasRole('PREMIUM') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ConceptNode>>> searchConcepts(
            @Parameter(description = "搜索关键词") @RequestParam String query,
            @Parameter(description = "返回条数限制，默认10") @RequestParam(defaultValue = "10") int limit) {
        
        log.info("搜索概念，关键词: {}，限制数量: {}", query, limit);
        List<ConceptNode> concepts = knowledgeService.searchConcepts(query, limit);
        return ResponseEntity.ok(ApiResponse.success(concepts));
    }

    /**
     * 获取概念相关的文章
     *
     * @param conceptName 概念名称
     * @param limit 返回条数限制
     * @return 相关文章列表
     */
    @Operation(summary = "获取概念相关的文章")
    @GetMapping("/concepts/{conceptName}/articles")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasRole('PREMIUM') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ArticleDTO>>> getRelatedArticles(
            @Parameter(description = "概念名称") @PathVariable String conceptName,
            @Parameter(description = "返回条数限制") @RequestParam(defaultValue = "20") int limit) {
        
        log.info("获取概念相关文章: concept={}, limit={}", conceptName, limit);
        
        try {
            List<ArticleDTO> articles = knowledgeService.findRelatedArticlesByConcept(conceptName, limit);
            return ResponseEntity.ok(ApiResponse.success(articles));
        } catch (Exception e) {
            log.error("获取概念相关文章失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取概念相关文章失败: " + e.getMessage()));
        }
    }

    /**
     * 获取概念统计信息
     *
     * @param conceptName 概念名称
     * @return 概念统计信息
     */
    @Operation(summary = "获取概念统计信息")
    @GetMapping("/concepts/{conceptName}/statistics")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasRole('PREMIUM') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConceptStatisticsDTO>> getConceptStatistics(
            @Parameter(description = "概念名称") @PathVariable String conceptName) {
        
        log.info("获取概念统计信息: {}", conceptName);
        
        try {
            ConceptStatisticsDTO stats = knowledgeService.getConceptStatistics(conceptName);
            if (stats == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("获取概念统计信息失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取概念统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取概念详情
     *
     * @param conceptName 概念名称
     * @return 概念详情
     */
    @Operation(summary = "获取概念详情")
    @GetMapping("/concepts/{conceptName}")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasRole('PREMIUM') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConceptDetailDTO>> getConceptDetail(
            @Parameter(description = "概念名称") @PathVariable String conceptName) {
        
        log.info("获取概念详情: {}", conceptName);
        
        try {
            ConceptDetailDTO detail = knowledgeService.getConceptDetail(conceptName);
            if (detail == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(detail));
        } catch (Exception e) {
            log.error("获取概念详情失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取概念详情失败: " + e.getMessage()));
        }
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
    @PreAuthorize("hasAnyRole('ROLE_USER', 'PREMIUM', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('ROLE_USER', 'PREMIUM', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('ROLE_USER', 'PREMIUM', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('ROLE_USER', 'PREMIUM', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('ROLE_USER', 'PREMIUM', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKnowledgeGraphStatistics() {
        
        log.info("获取知识图谱统计信息");
        Map<String, Object> statistics = knowledgeService.getKnowledgeGraphStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 手动触发文章重新分析
     *
     * @param articleId 文章ID
     * @return 重新分析结果
     */
    @Operation(summary = "手动触发文章重新分析")
    @PostMapping("/articles/{articleId}/reanalyze")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'PREMIUM', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> reanalyzeArticle(
            @Parameter(description = "文章ID") @PathVariable String articleId) {
        
        log.info("手动触发文章重新分析: {}", articleId);
        
        try {
            // 发送到AI分析队列
            Map<String, Object> message = Map.of(
                "articleId", articleId,
                "forceReanalyze", true,
                "timestamp", System.currentTimeMillis()
            );
            
            rabbitTemplate.convertAndSend(
                "article.analysis.exchange",
                "article.analysis",
                message
            );
            
            return ResponseEntity.ok(ApiResponse.success("重新分析任务已提交"));
        } catch (Exception e) {
            log.error("提交重新分析任务失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("重新分析任务提交失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发笔记重新分析
     *
     * @param noteId 笔记ID
     * @return 重新分析结果
     */
    @Operation(summary = "手动触发笔记重新分析")
    @PostMapping("/notes/{noteId}/reanalyze")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'PREMIUM', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> reanalyzeNote(
            @Parameter(description = "笔记ID") @PathVariable String noteId) {
        
        log.info("手动触发笔记重新分析: {}", noteId);
        
        try {
            // 发送到笔记分析队列
            Map<String, Object> message = Map.of(
                "noteId", noteId,
                "forceReanalyze", true,
                "timestamp", System.currentTimeMillis()
            );
            
            rabbitTemplate.convertAndSend(
                "note.analysis.exchange",
                "note.analysis",
                message
            );
            
            return ResponseEntity.ok(ApiResponse.success("笔记重新分析任务已提交"));
        } catch (Exception e) {
            log.error("提交笔记重新分析任务失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("笔记重新分析任务提交失败: " + e.getMessage()));
        }
    }

    /**
     * 批量重建知识图谱
     *
     * @return 重建结果
     */
    @Operation(summary = "批量重建知识图谱")
    @PostMapping("/rebuild-graph")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> rebuildKnowledgeGraph() {
        
        log.info("开始批量重建知识图谱");
        
        try {
            // 发送批量重建消息
            Map<String, Object> message = Map.of(
                "action", "REBUILD_ALL",
                "timestamp", System.currentTimeMillis()
            );
            
            rabbitTemplate.convertAndSend(
                "knowledge.graph.exchange",
                "knowledge.graph.rebuild",
                message
            );
            
            return ResponseEntity.ok(ApiResponse.success("知识图谱重建任务已提交"));
        } catch (Exception e) {
            log.error("提交知识图谱重建任务失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("知识图谱重建任务提交失败: " + e.getMessage()));
        }
    }

    /**
     * 获取知识图谱性能监控信息
     *
     * @return 性能统计信息
     */
    @Operation(summary = "获取知识图谱性能监控信息")
    @GetMapping("/performance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<KnowledgeGraphPerformanceMonitor.PerformanceStats>> getPerformanceStats() {
        
        log.info("获取知识图谱性能监控信息");
        
        try {
            KnowledgeGraphPerformanceMonitor.PerformanceStats stats = performanceMonitor.getPerformanceStats();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("获取性能监控信息失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取性能监控信息失败: " + e.getMessage()));
        }
    }

    /**
     * 重置知识图谱性能监控统计
     *
     * @return 重置结果
     */
    @Operation(summary = "重置知识图谱性能监控统计")
    @PostMapping("/performance/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> resetPerformanceStats() {
        
        log.info("重置知识图谱性能监控统计");
        
        try {
            performanceMonitor.resetStats();
            return ResponseEntity.ok(ApiResponse.success("性能监控统计已重置"));
        } catch (Exception e) {
            log.error("重置性能监控统计失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("重置性能监控统计失败: " + e.getMessage()));
        }
    }

    /**
     * 健康检查接口
     *
     * @return 健康状态
     */
    @Operation(summary = "知识图谱服务健康检查")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        
        try {
            Map<String, Object> healthInfo = new HashMap<>();
            
            // 检查Neo4j连接
            try {
                Map<String, Object> stats = knowledgeService.getKnowledgeGraphStatistics();
                healthInfo.put("neo4j", "UP");
                healthInfo.put("totalNodes", stats.get("totalNodes"));
                healthInfo.put("totalRelationships", stats.get("totalRelationships"));
            } catch (Exception e) {
                healthInfo.put("neo4j", "DOWN");
                healthInfo.put("neo4jError", e.getMessage());
            }
            
            // 检查性能指标
            KnowledgeGraphPerformanceMonitor.PerformanceStats perfStats = performanceMonitor.getPerformanceStats();
            healthInfo.put("performanceStats", perfStats.getMethodStats().size());
            
            healthInfo.put("timestamp", System.currentTimeMillis());
            healthInfo.put("status", healthInfo.get("neo4j").equals("UP") ? "HEALTHY" : "DEGRADED");
            
            return ResponseEntity.ok(ApiResponse.success(healthInfo));
            
        } catch (Exception e) {
            log.error("健康检查失败", e);
            Map<String, Object> errorInfo = Map.of(
                "status", "UNHEALTHY",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("健康检查失败"));
        }
    }

    /**
     * 测试Neo4j连接和查询结果
     */
    @GetMapping("/test-neo4j")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testNeo4jConnection() {
        log.info("测试Neo4j连接");
        
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 测试1: 检查Neo4j连接 - 最简单的测试
            try {
                String testQuery1 = "RETURN 1 AS result";
                Collection<Map<String, Object>> testResult1 = neo4jClient.query(testQuery1).fetch().all();
                result.put("connectionTest", "成功: " + testResult1.iterator().next().get("result"));
                log.info("Neo4j连接测试成功");
            } catch (Exception e) {
                log.error("Neo4j连接测试失败: {}", e.getMessage(), e);
                result.put("connectionError", e.getMessage());
                // 如果连接测试失败，直接返回错误
                return ResponseEntity.ok(ApiResponse.success(result));
            }
            
            // 测试2: 获取节点标签和数量
            try {
                String testQuery2 = "MATCH (n) RETURN labels(n) AS labels, count(*) AS count";
                Collection<Map<String, Object>> testResult2 = neo4jClient.query(testQuery2).fetch().all();
                
                if (testResult2.isEmpty()) {
                    result.put("nodeCount", "数据库中没有节点");
                } else {
                    result.put("nodeInfo", testResult2);
                    log.info("节点统计查询成功: {}", testResult2);
                }
            } catch (Exception e) {
                log.error("节点统计查询失败: {}", e.getMessage(), e);
                result.put("nodeQueryError", e.getMessage());
            }
            
            // 测试3: 获取所有独特的节点标签
            try {
                String testQuery3 = "MATCH (n) RETURN DISTINCT labels(n) AS distinctLabels";
                Collection<Map<String, Object>> testResult3 = neo4jClient.query(testQuery3).fetch().all();
                result.put("distinctLabels", testResult3);
                log.info("独特节点标签查询成功: {}", testResult3);
            } catch (Exception e) {
                log.error("独特节点标签查询失败: {}", e.getMessage(), e);
                result.put("distinctLabelsError", e.getMessage());
            }
            
            // 测试4: 查询各类型节点的示例
            try {
                // 查询概念节点示例
                String conceptQuery = "MATCH (c:Concept) RETURN c LIMIT 1";
                Collection<Map<String, Object>> conceptResult = neo4jClient.query(conceptQuery).fetch().all();
                if (!conceptResult.isEmpty()) {
                    result.put("conceptExample", conceptResult.iterator().next());
                    log.info("概念节点查询成功");
                }
                
                // 查询文章节点示例
                String articleQuery = "MATCH (a:Article) RETURN a LIMIT 1";
                Collection<Map<String, Object>> articleResult = neo4jClient.query(articleQuery).fetch().all();
                if (!articleResult.isEmpty()) {
                    result.put("articleExample", articleResult.iterator().next());
                    log.info("文章节点查询成功");
                }
                
                // 查询笔记节点示例
                String noteQuery = "MATCH (n:Note) RETURN n LIMIT 1";
                Collection<Map<String, Object>> noteResult = neo4jClient.query(noteQuery).fetch().all();
                if (!noteResult.isEmpty()) {
                    result.put("noteExample", noteResult.iterator().next());
                    log.info("笔记节点查询成功");
                }
            } catch (Exception e) {
                log.error("节点示例查询失败: {}", e.getMessage(), e);
                result.put("nodeExampleError", e.getMessage());
            }
            
            // 测试5: 查询知识图谱使用的实际查询
            try {
                String graphQuery = "MATCH (n) " +
                                  "OPTIONAL MATCH (n)-[r]->(m) " +
                                  "RETURN n, labels(n) AS nodeLabels, " +
                                  "collect({rel: r, target: m, targetLabels: labels(m)}) AS outRelationships " +
                                  "LIMIT 5";
                
                Collection<Map<String, Object>> graphResult = neo4jClient.query(graphQuery).fetch().all();
                result.put("graphQueryResultCount", graphResult.size());
                
                if (!graphResult.isEmpty()) {
                    Map<String, Object> firstRow = graphResult.iterator().next();
                    result.put("firstRowKeys", firstRow.keySet());
                    
                    if (firstRow.containsKey("n")) {
                        Object nodeObj = firstRow.get("n");
                        if (nodeObj instanceof Map) {
                            result.put("nodeExample", ((Map<String, Object>)nodeObj).keySet());
                        } else {
                            result.put("nodeType", nodeObj != null ? nodeObj.getClass().getName() : "null");
                        }
                    }
                    
                    if (firstRow.containsKey("nodeLabels")) {
                        result.put("nodeLabelsExample", firstRow.get("nodeLabels"));
                    }
                    
                    if (firstRow.containsKey("outRelationships")) {
                        Object relsObj = firstRow.get("outRelationships");
                        if (relsObj instanceof List && !((List<?>)relsObj).isEmpty()) {
                            result.put("relationshipCount", ((List<?>)relsObj).size());
                            result.put("firstRelationship", ((List<?>)relsObj).get(0));
                        } else {
                            result.put("relationshipInfo", "No relationships or empty list");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("图谱查询失败: {}", e.getMessage(), e);
                result.put("graphQueryError", e.getMessage());
            }
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("测试Neo4j连接失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Neo4j连接测试失败: " + e.getMessage()));
        }
    }
} 
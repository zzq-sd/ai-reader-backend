package com.aireader.backend.controller;

import com.aireader.backend.dto.GraphDataDTO;
import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge-graph")
@Tag(name = "Knowledge Graph Controller", description = "知识图谱数据接口")
@Slf4j
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    @Operation(summary = "获取知识图谱数据", description = "获取用于D3.js渲染的全量或过滤后的知识图谱节点和关系数据")
    @GetMapping("/graph-data")
    public ResponseEntity<ApiResponse<GraphDataDTO>> getGraphData(
            @Parameter(description = "节点类型过滤 (e.g., 'Concept', 'Article')") @RequestParam(required = false) String nodeType,
            @Parameter(description = "搜索关键词，匹配节点名称") @RequestParam(required = false, defaultValue = "") String search,
            @Parameter(description = "返回的节点和关系的最大数量") @RequestParam(defaultValue = "500") int limit) {
        
        log.info("Request for knowledge graph data with filters: nodeType='{}', search='{}', limit={}", nodeType, search, limit);
        try {
            GraphDataDTO graphData = knowledgeGraphService.getGraphData(nodeType, search, limit);
            log.info("Successfully retrieved {} nodes and {} edges.", graphData.getNodes().size(), graphData.getEdges().size());
            return ResponseEntity.ok(ApiResponse.success(graphData));
        } catch (Exception e) {
            log.error("Error fetching knowledge graph data", e);
            return ResponseEntity.status(500).body(ApiResponse.error("获取知识图谱数据失败: " + e.getMessage()));
        }
    }
} 
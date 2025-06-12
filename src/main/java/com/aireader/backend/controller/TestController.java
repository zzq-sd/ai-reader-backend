package com.aireader.backend.controller;

import com.aireader.backend.dto.GraphDataDTO;
import com.aireader.backend.dto.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 测试控制器
 */
@RestController
@RequestMapping("/api/v1/test")
@Slf4j
public class TestController {

    /**
     * 获取测试图谱数据
     */
    @GetMapping("/graph-data")
    public ResponseEntity<ApiResponse<GraphDataDTO>> getTestGraphData() {
        log.info("【测试】提供测试图谱数据");
        return ResponseEntity.ok(ApiResponse.success(createTestGraphData()));
    }

    /**
     * 简单测试端点
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Test API is working!");
    }

    /**
     * 处理直接访问/ai/chat/health的情况
     * 这是为了解决前端可能直接访问此路径而不是/api/v1/ai/chat/health的问题
     */
    @GetMapping("/ai/chat/health")
    public ResponseEntity<Map<String, Object>> redirectAiHealthCheck() {
        log.info("接收到直接访问/ai/chat/health的请求，重定向到正确的API端点");
        Map<String, Object> response = Map.of(
            "status", "healthy",
            "message", "AI服务正常运行(重定向处理)",
            "redirect", "true"
        );
        return ResponseEntity.ok(response);
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
} 
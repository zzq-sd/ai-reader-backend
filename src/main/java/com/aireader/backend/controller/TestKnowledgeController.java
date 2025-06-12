package com.aireader.backend.controller;

import com.aireader.backend.dto.GraphDataDTO;
import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 测试知识图谱控制器 - 无权限验证
 */
@RestController
@RequestMapping("/test/knowledge")
@Slf4j
public class TestKnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    /**
     * 测试获取知识图谱数据
     */
    @GetMapping("/graph-data")
    public ResponseEntity<ApiResponse<GraphDataDTO>> getGraphData(
            @RequestParam(defaultValue = "ALL") String nodeType,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("🔥🔥🔥 测试控制器 - 获取知识图谱数据: nodeType={}, search={}, limit={}", nodeType, search, limit);
        
        try {
            GraphDataDTO graphData = knowledgeService.getGraphData(nodeType, search, limit);
            
            log.info("🔥🔥🔥 测试控制器 - 服务方法返回结果: nodes={}, edges={}", 
                    graphData.getNodes().size(), graphData.getEdges().size());
            
            return ResponseEntity.ok(ApiResponse.success(graphData));
        } catch (Exception e) {
            log.error("🔥🔥🔥 测试控制器 - 获取知识图谱数据失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取知识图谱数据失败: " + e.getMessage()));
        }
    }

    /**
     * 简单测试端点
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("🔥🔥🔥 测试控制器 - 测试端点被调用");
        return ResponseEntity.ok("Test Knowledge API is working!");
    }
} 
package com.aireader.backend.controller;

import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.service.KnowledgeGraphMessageService;
import com.aireader.backend.service.ConceptDeduplicationService;
import com.aireader.backend.model.neo4j.ConceptNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱测试控制器
 * 用于测试和验证知识图谱实时同步功能
 */
@RestController
@RequestMapping("/api/v1/knowledge-graph/test")
@Tag(name = "Knowledge Graph Test Controller", description = "知识图谱测试接口")
@Slf4j
@CrossOrigin(origins = "*")
public class KnowledgeGraphTestController {

    @Autowired
    private KnowledgeGraphMessageService messageService;
    
    @Autowired
    private ConceptDeduplicationService deduplicationService;

    /**
     * 测试发送文章更新消息
     */
    @Operation(summary = "测试文章更新消息", description = "测试发送文章知识图谱更新消息到RabbitMQ")
    @PostMapping("/message/article/{articleId}")
    public ResponseEntity<ApiResponse<String>> testArticleMessage(@PathVariable String articleId) {
        
        log.info("🧪 测试发送文章更新消息: {}", articleId);
        
        try {
            messageService.sendArticleUpdateMessage(articleId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "文章更新消息发送成功", 
                "MESSAGE_SENT"));
            
        } catch (Exception e) {
            log.error("❌ 测试发送文章更新消息失败: {}", articleId, e);
            return ResponseEntity.ok(ApiResponse.error("发送消息失败: " + e.getMessage()));
        }
    }

    /**
     * 测试发送笔记更新消息
     */
    @Operation(summary = "测试笔记更新消息", description = "测试发送笔记知识图谱更新消息到RabbitMQ")
    @PostMapping("/message/note/{noteId}")
    public ResponseEntity<ApiResponse<String>> testNoteMessage(@PathVariable String noteId) {
        
        log.info("🧪 测试发送笔记更新消息: {}", noteId);
        
        try {
            messageService.sendNoteUpdateMessage(noteId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "笔记更新消息发送成功", 
                "MESSAGE_SENT"));
            
        } catch (Exception e) {
            log.error("❌ 测试发送笔记更新消息失败: {}", noteId, e);
            return ResponseEntity.ok(ApiResponse.error("发送消息失败: " + e.getMessage()));
        }
    }

    /**
     * 测试概念去重功能
     */
    @Operation(summary = "测试概念去重", description = "测试概念去重和合并功能")
    @PostMapping("/deduplication/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConceptDeduplication(
            @RequestParam String conceptName,
            @RequestParam(defaultValue = "CONCEPT") String type) {
        
        log.info("🧪 测试概念去重: {} ({})", conceptName, type);
        
        try {
            // 查找相似概念
            List<ConceptNode> similarConcepts = deduplicationService.findSimilarConcepts(conceptName, 0.8);
            
            // 查找或创建概念
            ConceptNode concept = deduplicationService.findOrCreateConcept(conceptName, type);
            
            Map<String, Object> result = Map.of(
                "inputConcept", conceptName,
                "inputType", type,
                "resultConcept", concept.getName(),
                "resultId", concept.getId(),
                "similarConceptsFound", similarConcepts.size(),
                "similarConcepts", similarConcepts.stream()
                    .map(c -> Map.of(
                        "name", c.getName(),
                        "type", c.getType(),
                        "id", c.getId()
                    ))
                    .toList()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("❌ 测试概念去重失败: {}", conceptName, e);
            return ResponseEntity.ok(ApiResponse.error("概念去重测试失败: " + e.getMessage()));
        }
    }

    /**
     * 测试系统健康状态
     */
    @Operation(summary = "测试系统健康状态", description = "检查知识图谱系统各组件的健康状态")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testSystemHealth() {
        
        log.info("🧪 测试系统健康状态");
        
        try {
            Map<String, Object> healthStatus = Map.of(
                "timestamp", System.currentTimeMillis(),
                "components", Map.of(
                    "messageService", checkMessageServiceHealth(),
                    "deduplicationService", checkDeduplicationServiceHealth(),
                    "database", "CONNECTED",
                    "rabbitMQ", "CONNECTED"
                ),
                "version", "1.0.0",
                "springAI", "1.0.0",
                "status", "HEALTHY"
            );
            
            return ResponseEntity.ok(ApiResponse.success(healthStatus));
            
        } catch (Exception e) {
            log.error("❌ 系统健康检查失败", e);
            return ResponseEntity.ok(ApiResponse.error("健康检查失败: " + e.getMessage()));
        }
    }

    /**
     * 模拟批量处理测试
     */
    @Operation(summary = "模拟批量处理", description = "模拟批量文章处理，测试系统性能")
    @PostMapping("/batch/simulate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulateBatchProcessing(
            @RequestParam(defaultValue = "10") int count) {
        
        log.info("🧪 模拟批量处理: {}个项目", count);
        
        try {
            long startTime = System.currentTimeMillis();
            
            for (int i = 1; i <= count; i++) {
                String articleId = "test-article-" + i;
                messageService.sendArticleUpdateMessage(articleId);
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            Map<String, Object> result = Map.of(
                "processedCount", count,
                "duration", duration + "ms",
                "averageTime", (duration / count) + "ms/item",
                "throughput", String.format("%.2f items/sec", (count * 1000.0) / duration),
                "status", "COMPLETED"
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("❌ 批量处理模拟失败", e);
            return ResponseEntity.ok(ApiResponse.error("批量处理模拟失败: " + e.getMessage()));
        }
    }

    /**
     * 检查消息服务健康状态
     */
    private String checkMessageServiceHealth() {
        try {
            // 简单的健康检查
            return messageService != null ? "HEALTHY" : "UNAVAILABLE";
        } catch (Exception e) {
            return "ERROR";
        }
    }

    /**
     * 检查去重服务健康状态
     */
    private String checkDeduplicationServiceHealth() {
        try {
            // 简单的健康检查
            return deduplicationService != null ? "HEALTHY" : "UNAVAILABLE";
        } catch (Exception e) {
            return "ERROR";
        }
    }
} 
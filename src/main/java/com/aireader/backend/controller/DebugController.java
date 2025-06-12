package com.aireader.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug")
@CrossOrigin(origins = "*")
public class DebugController {

    @Autowired
    private Neo4jClient neo4jClient;

    @GetMapping("/neo4j/simple")
    public ResponseEntity<Object> testSimpleNeo4jQuery() {
        try {
            Collection<Map<String, Object>> result = neo4jClient.query("RETURN 1 as test")
                    .fetch()
                    .all();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Neo4j连接成功",
                "result", result
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Neo4j连接失败: " + e.getMessage(),
                "error", e.getClass().getName()
            ));
        }
    }

    @GetMapping("/neo4j/nodes")
    public ResponseEntity<Object> testNodeQuery() {
        try {
            Collection<Map<String, Object>> result = neo4jClient.query("MATCH (n) RETURN count(n) as nodeCount")
                    .fetch()
                    .all();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "节点查询成功",
                "result", result
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "节点查询失败: " + e.getMessage(),
                "error", e.getClass().getName()
            ));
        }
    }

    @GetMapping("/neo4j/first-node")
    public ResponseEntity<Object> testFirstNode() {
        try {
            Collection<Map<String, Object>> result = neo4jClient.query("MATCH (n) RETURN n, labels(n) as nodeLabels, elementId(n) as nodeId LIMIT 1")
                    .fetch()
                    .all();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "首个节点查询成功",
                "result", result
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "首个节点查询失败: " + e.getMessage(),
                "error", e.getClass().getName()
            ));
        }
    }
} 
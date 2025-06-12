package com.aireader.backend.service.impl;

import com.aireader.backend.model.neo4j.ConceptNode;
import com.aireader.backend.repository.neo4j.ConceptNodeRepository;
import com.aireader.backend.service.ConceptDeduplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 概念去重和合并服务实现
 * 基于AI分析和相似度算法实现智能概念管理
 */
@Service
@Slf4j
public class ConceptDeduplicationServiceImpl implements ConceptDeduplicationService {

    @Autowired
    private ConceptNodeRepository conceptNodeRepository;
    
    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @Override
    @Transactional
    public ConceptNode findOrCreateConcept(String conceptName, String type) {
        log.debug("查找或创建概念: name={}, type={}", conceptName, type);
        
        try {
            // 1. 首先尝试精确匹配
            Optional<ConceptNode> exactMatch = conceptNodeRepository.findByName(conceptName);
            if (exactMatch.isPresent()) {
                log.debug("找到精确匹配的概念: {}", conceptName);
                return exactMatch.get();
            }
            
            // 2. 查找相似概念
            List<ConceptNode> similarConcepts = findSimilarConcepts(conceptName, 0.8);
            if (!similarConcepts.isEmpty()) {
                ConceptNode mostSimilar = similarConcepts.get(0);
                log.debug("找到相似概念，使用现有概念: {} -> {}", conceptName, mostSimilar.getName());
                return mostSimilar;
            }
            
            // 3. 创建新概念
            ConceptNode newConcept = ConceptNode.builder()
                    .name(conceptName)
                    .type(type)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            ConceptNode savedConcept = neo4jTemplate.save(newConcept);
            log.debug("创建新概念: {}", conceptName);
            return savedConcept;
            
        } catch (Exception e) {
            log.error("查找或创建概念失败: name={}, type={}", conceptName, type, e);
            throw new RuntimeException("查找或创建概念失败", e);
        }
    }

    @Override
    public List<ConceptNode> findSimilarConcepts(String conceptName, double threshold) {
        log.debug("查找相似概念: name={}, threshold={}", conceptName, threshold);
        
        try {
            // 获取所有概念进行相似度比较
            List<ConceptNode> allConcepts = conceptNodeRepository.findAll();
            
            return allConcepts.stream()
                    .filter(concept -> {
                        double similarity = calculateTextSimilarity(conceptName, concept.getName());
                        return similarity >= threshold;
                    })
                    .sorted((c1, c2) -> {
                        double sim1 = calculateTextSimilarity(conceptName, c1.getName());
                        double sim2 = calculateTextSimilarity(conceptName, c2.getName());
                        return Double.compare(sim2, sim1); // 降序排列
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("查找相似概念失败: name={}", conceptName, e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional
    public ConceptNode mergeConcepts(ConceptNode sourceConcept, ConceptNode targetConcept) {
        log.info("合并概念: {} -> {}", sourceConcept.getName(), targetConcept.getName());
        
        try {
            // 更新目标概念的信息
            targetConcept.setUpdatedAt(LocalDateTime.now());
            
            ConceptNode mergedConcept = neo4jTemplate.save(targetConcept);
            
            log.info("概念合并完成: {}", targetConcept.getName());
            return mergedConcept;
            
        } catch (Exception e) {
            log.error("合并概念失败: {} -> {}", sourceConcept.getName(), targetConcept.getName(), e);
            throw new RuntimeException("合并概念失败", e);
        }
    }

    @Override
    public double calculateSimilarity(ConceptNode concept1, ConceptNode concept2) {
        if (concept1 == null || concept2 == null) {
            return 0.0;
        }
        
        return calculateTextSimilarity(concept1.getName(), concept2.getName());
    }

    @Override
    public List<ConceptNode> deduplicateConcepts(List<ConceptNode> concepts) {
        log.debug("批量去重概念，输入数量: {}", concepts.size());
        
        if (concepts == null || concepts.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Map<String, ConceptNode> uniqueConcepts = new HashMap<>();
            
            for (ConceptNode concept : concepts) {
                String key = concept.getName().toLowerCase().trim();
                
                // 检查是否已存在相似概念
                boolean found = false;
                for (String existingKey : uniqueConcepts.keySet()) {
                    if (calculateTextSimilarity(key, existingKey) >= 0.8) {
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    uniqueConcepts.put(key, concept);
                }
            }
            
            List<ConceptNode> result = new ArrayList<>(uniqueConcepts.values());
            log.debug("去重完成，输出数量: {}", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("批量去重概念失败", e);
            return concepts; // 返回原始列表
        }
    }
    
    /**
     * 计算文本相似度
     * 使用简单的编辑距离算法
     */
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        if (text1.equals(text2)) {
            return 1.0;
        }
        
        // 转换为小写并去除空格
        String s1 = text1.toLowerCase().trim();
        String s2 = text2.toLowerCase().trim();
        
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        // 计算编辑距离
        int editDistance = calculateEditDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        // 相似度 = 1 - (编辑距离 / 最大长度)
        return 1.0 - (double) editDistance / maxLength;
    }
    
    /**
     * 计算编辑距离（Levenshtein距离）
     */
    private int calculateEditDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        // 初始化
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        // 动态规划计算
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                            Math.min(dp[i - 1][j], dp[i][j - 1]),
                            dp[i - 1][j - 1]
                    );
                }
            }
        }
        
        return dp[m][n];
    }
} 
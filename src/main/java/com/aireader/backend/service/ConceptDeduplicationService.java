package com.aireader.backend.service;

import com.aireader.backend.model.neo4j.ConceptNode;

import java.util.List;

/**
 * 概念去重和合并服务接口
 * 基于AI分析和相似度算法实现智能概念管理
 */
public interface ConceptDeduplicationService {
    
    /**
     * 查找或创建概念
     * 包含智能去重逻辑
     * 
     * @param conceptName 概念名称
     * @param type 概念类型
     * @return 概念节点
     */
    ConceptNode findOrCreateConcept(String conceptName, String type);
    
    /**
     * 查找相似概念
     * 
     * @param conceptName 概念名称
     * @param threshold 相似度阈值
     * @return 相似概念列表
     */
    List<ConceptNode> findSimilarConcepts(String conceptName, double threshold);
    
    /**
     * 合并概念
     * 
     * @param sourceConcept 源概念
     * @param targetConcept 目标概念
     * @return 合并后的概念
     */
    ConceptNode mergeConcepts(ConceptNode sourceConcept, ConceptNode targetConcept);
    
    /**
     * 计算概念相似度
     * 
     * @param concept1 概念1
     * @param concept2 概念2
     * @return 相似度分数 (0-1)
     */
    double calculateSimilarity(ConceptNode concept1, ConceptNode concept2);
    
    /**
     * 批量去重概念
     * 
     * @param concepts 概念列表
     * @return 去重后的概念列表
     */
    List<ConceptNode> deduplicateConcepts(List<ConceptNode> concepts);
} 
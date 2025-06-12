package com.aireader.backend.repository.mongo;

import com.aireader.backend.model.mongo.NoteAnalysisResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 笔记分析结果MongoDB Repository
 */
@Repository
public interface NoteAnalysisResultRepository extends MongoRepository<NoteAnalysisResult, String> {
    
    /**
     * 根据笔记ID查找分析结果
     * @param noteId 笔记ID
     * @return 分析结果
     */
    Optional<NoteAnalysisResult> findByNoteId(String noteId);
    
    /**
     * 根据用户ID查找所有分析结果
     * @param userId 用户ID
     * @return 分析结果列表
     */
    List<NoteAnalysisResult> findByUserId(String userId);
    
    /**
     * 根据用户ID和笔记ID查找分析结果
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @return 分析结果
     */
    Optional<NoteAnalysisResult> findByUserIdAndNoteId(String userId, String noteId);
    
    /**
     * 删除指定笔记的分析结果
     * @param noteId 笔记ID
     */
    void deleteByNoteId(String noteId);
    
    /**
     * 查询包含特定概念的笔记分析结果
     * @param conceptName 概念名称
     * @return 分析结果列表
     */
    @Query("{ 'extractedConcepts.name': ?0 }")
    List<NoteAnalysisResult> findByConceptName(String conceptName);
    
    /**
     * 查询特定情感分析结果的笔记
     * @param sentiment 情感分析结果
     * @return 分析结果列表
     */
    List<NoteAnalysisResult> findBySentimentAnalysis(String sentiment);
} 
package com.aireader.backend.service.impl;

import com.aireader.backend.dto.ai.ArticleAnalysisResult;
import com.aireader.backend.model.constant.Neo4jRelationshipTypes;
import com.aireader.backend.model.jpa.Note;
import com.aireader.backend.model.mongo.NoteAnalysisResult;
import com.aireader.backend.model.neo4j.ConceptNode;
import com.aireader.backend.model.neo4j.NoteNode;
import com.aireader.backend.model.neo4j.NoteConceptRelationship;
import com.aireader.backend.repository.NoteRepository;
import com.aireader.backend.repository.mongo.NoteAnalysisResultRepository;
import com.aireader.backend.repository.neo4j.ConceptNodeRepository;
import com.aireader.backend.repository.neo4j.NoteNodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 笔记知识图谱集成服务
 * 专门处理笔记与知识图谱的集成逻辑
 */
@Service
@Slf4j
public class NoteKnowledgeGraphService {

    @Autowired
    private NoteRepository noteRepository;
    
    @Autowired
    private NoteAnalysisResultRepository noteAnalysisResultRepository;
    
    @Autowired
    private NoteNodeRepository noteNodeRepository;
    
    @Autowired
    private ConceptNodeRepository conceptNodeRepository;
    
    @Autowired
    private Neo4jTemplate neo4jTemplate;

    /**
     * 将笔记集成到知识图谱
     * 
     * @param noteId 笔记ID
     * @return 是否成功
     */
    @Transactional
    public boolean integrateNoteToKnowledgeGraph(String noteId) {
        log.info("开始将笔记集成到知识图谱, noteId={}", noteId);
        
        try {
            // 1. 获取笔记基本信息
            Optional<Note> noteOpt = noteRepository.findById(noteId);
            if (!noteOpt.isPresent()) {
                log.error("笔记不存在, noteId={}", noteId);
                return false;
            }
            Note note = noteOpt.get();
            
            // 2. 获取笔记AI分析结果
            Optional<NoteAnalysisResult> analysisOpt = noteAnalysisResultRepository.findByNoteId(noteId);
            if (!analysisOpt.isPresent()) {
                log.warn("笔记AI分析结果不存在, noteId={}", noteId);
                // 仍然创建基本的笔记节点
                createBasicNoteNode(note);
                return true;
            }
            NoteAnalysisResult analysisResult = analysisOpt.get();
            
            // 3. 创建或更新笔记节点
            NoteNode noteNode = createOrUpdateNoteNode(note, analysisResult);
            
            // 4. 处理概念关系
            if (analysisResult.getExtractedConcepts() != null) {
                for (var conceptEntity : analysisResult.getExtractedConcepts()) {
                    processNoteConceptRelation(noteNode, conceptEntity);
                }
            }
            
            // 5. 处理智能标签作为概念
            if (analysisResult.getIntelligentTags() != null) {
                for (String tag : analysisResult.getIntelligentTags()) {
                    processNoteTagConcept(noteNode, tag);
                }
            }
            
            log.info("笔记知识图谱集成完成, noteId={}", noteId);
            return true;
            
        } catch (Exception e) {
            log.error("笔记知识图谱集成失败, noteId=" + noteId, e);
            return false;
        }
    }
    
    /**
     * 创建基本的笔记节点（无AI分析结果）
     * 
     * @param note 笔记对象
     * @return 笔记节点
     */
    private NoteNode createBasicNoteNode(Note note) {
        try {
            // 查找是否已存在
            Optional<NoteNode> existingOpt = noteNodeRepository.findByMysqlId(note.getId());
            
            NoteNode noteNode;
            if (existingOpt.isPresent()) {
                noteNode = existingOpt.get();
                // 更新基本信息
                noteNode.setTitle(note.getTitle());
                noteNode.setUpdatedAt(LocalDateTime.now());
            } else {
                // 创建新节点
                noteNode = NoteNode.builder()
                        .mysqlId(note.getId())
                        .title(note.getTitle())
                        .userId(note.getUserId())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
            }
            
            return neo4jTemplate.save(noteNode);
        } catch (Exception e) {
            log.error("创建基本笔记节点失败, noteId=" + note.getId(), e);
            throw e;
        }
    }
    
    /**
     * 创建或更新笔记节点
     * 
     * @param note 笔记对象
     * @param analysisResult AI分析结果
     * @return 笔记节点
     */
    private NoteNode createOrUpdateNoteNode(Note note, NoteAnalysisResult analysisResult) {
        try {
            // 查找是否已存在
            Optional<NoteNode> existingOpt = noteNodeRepository.findByMysqlId(note.getId());
            
            NoteNode noteNode;
            if (existingOpt.isPresent()) {
                noteNode = existingOpt.get();
                // 更新信息
                noteNode.setTitle(note.getTitle());
                noteNode.setContentSummary(analysisResult.getEnhancedSummary());
                noteNode.setUpdatedAt(LocalDateTime.now());
            } else {
                // 创建新节点
                noteNode = NoteNode.builder()
                        .mysqlId(note.getId())
                        .title(note.getTitle())
                        .userId(note.getUserId())
                        .contentSummary(analysisResult.getEnhancedSummary())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
            }
            
            return neo4jTemplate.save(noteNode);
        } catch (Exception e) {
            log.error("创建或更新笔记节点失败, noteId=" + note.getId(), e);
            throw e;
        }
    }
    
    /**
     * 处理笔记-概念关系
     * 
     * @param noteNode 笔记节点
     * @param conceptEntity 概念实体
     */
    private void processNoteConceptRelation(NoteNode noteNode, 
            ArticleAnalysisResult.ConceptEntity conceptEntity) {
        try {
            // 创建或获取概念节点
            ConceptNode conceptNode = createOrUpdateConceptNode(conceptEntity);
            
            // 创建笔记-概念关系
            createNoteConceptRelationship(noteNode, conceptNode, conceptEntity);
            
        } catch (Exception e) {
            log.error("处理笔记-概念关系失败, noteId={}, conceptName={}", 
                     noteNode.getMysqlId(), conceptEntity.getName(), e);
        }
    }
    
    /**
     * 处理笔记标签概念
     * 
     * @param noteNode 笔记节点
     * @param tag 标签
     */
    private void processNoteTagConcept(NoteNode noteNode, String tag) {
        try {
            // 将标签作为概念处理
            ConceptNode conceptNode = createOrUpdateTagConcept(tag);
            
            // 创建笔记-概念关系
            createNoteTagRelationship(noteNode, conceptNode, tag);
            
        } catch (Exception e) {
            log.error("处理笔记标签概念失败, noteId={}, tag={}", 
                     noteNode.getMysqlId(), tag, e);
        }
    }
    
    /**
     * 创建或更新概念节点
     * 
     * @param conceptEntity 概念实体
     * @return 概念节点
     */
    private ConceptNode createOrUpdateConceptNode(
            ArticleAnalysisResult.ConceptEntity conceptEntity) {
        
        // 查找是否已存在
        Optional<ConceptNode> existingOpt = conceptNodeRepository.findByName(conceptEntity.getName());
        
        ConceptNode conceptNode;
        if (existingOpt.isPresent()) {
            conceptNode = existingOpt.get();
            // 更新统计信息
            conceptNode.setUpdatedAt(LocalDateTime.now());
        } else {
            // 创建新概念
            conceptNode = ConceptNode.builder()
                    .name(conceptEntity.getName())
                    .type(conceptEntity.getType())
                    .description(conceptEntity.getContext())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        
        return neo4jTemplate.save(conceptNode);
    }
    
    /**
     * 创建标签概念节点
     * 
     * @param tag 标签名称
     * @return 概念节点
     */
    private ConceptNode createOrUpdateTagConcept(String tag) {
        // 查找是否已存在
        Optional<ConceptNode> existingOpt = conceptNodeRepository.findByName(tag);
        
        ConceptNode conceptNode;
        if (existingOpt.isPresent()) {
            conceptNode = existingOpt.get();
            conceptNode.setUpdatedAt(LocalDateTime.now());
        } else {
            // 创建新的标签概念
            conceptNode = ConceptNode.builder()
                    .name(tag)
                    .type("TAG")
                    .description("智能标签: " + tag)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        
        return neo4jTemplate.save(conceptNode);
    }
    
    /**
     * 创建笔记-概念关系
     * 
     * @param noteNode 笔记节点
     * @param conceptNode 概念节点
     * @param conceptEntity 概念实体
     */
    private void createNoteConceptRelationship(NoteNode noteNode, ConceptNode conceptNode,
            ArticleAnalysisResult.ConceptEntity conceptEntity) {
        
        try {
            // 通过NoteNode的addConcept方法添加关系，这样会正确建立双向绑定
            noteNode.addConcept(conceptNode, conceptEntity.getConfidence(), Neo4jRelationshipTypes.EXTRACTED_CONCEPT);
            
            // 保存更新后的笔记节点（会自动保存关系）
            neo4jTemplate.save(noteNode);
            
            log.debug("创建笔记-概念关系成功, noteId={}, conceptName={}, confidence={}", 
                     noteNode.getMysqlId(), conceptNode.getName(), conceptEntity.getConfidence());
        } catch (Exception e) {
            log.error("创建笔记-概念关系失败", e);
        }
    }
    
    /**
     * 创建笔记-标签关系
     * 
     * @param noteNode 笔记节点
     * @param conceptNode 概念节点
     * @param tag 标签
     */
    private void createNoteTagRelationship(NoteNode noteNode, ConceptNode conceptNode, String tag) {
        try {
            // 通过NoteNode的addConcept方法添加关系
            noteNode.addConcept(conceptNode, 0.8, Neo4jRelationshipTypes.INTELLIGENT_TAG);
            
            // 保存更新后的笔记节点（会自动保存关系）
            neo4jTemplate.save(noteNode);
            
            log.debug("创建笔记-标签关系成功, noteId={}, tag={}", 
                     noteNode.getMysqlId(), tag);
        } catch (Exception e) {
            log.error("创建笔记-标签关系失败", e);
        }
    }
    
    /**
     * 删除笔记的知识图谱数据
     * 
     * @param noteId 笔记ID
     * @return 是否成功
     */
    @Transactional
    public boolean removeNoteFromKnowledgeGraph(String noteId) {
        log.info("开始从知识图谱中删除笔记, noteId={}", noteId);
        
        try {
            // 删除笔记节点（会级联删除关系）
            noteNodeRepository.deleteByMysqlId(noteId);
            
            // 删除MongoDB中的分析结果
            noteAnalysisResultRepository.deleteByNoteId(noteId);
            
            log.info("从知识图谱中删除笔记成功, noteId={}", noteId);
            return true;
        } catch (Exception e) {
            log.error("从知识图谱中删除笔记失败, noteId=" + noteId, e);
            return false;
        }
    }
} 
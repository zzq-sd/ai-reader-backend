package com.aireader.backend.consumer;

import com.aireader.backend.service.AiService;
import com.aireader.backend.dto.ai.ArticleAnalysisResult;
import com.aireader.backend.dto.ai.NoteAnalysisResult;
import com.aireader.backend.config.RabbitMQConfig;
import com.aireader.backend.config.KnowledgeGraphRabbitConfig;
import com.aireader.backend.model.jpa.Note;
import com.aireader.backend.repository.NoteRepository;
import com.aireader.backend.repository.mongo.NoteAnalysisResultRepository;
import com.aireader.backend.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 笔记分析消费者
 * 消费笔记分析队列中的消息，执行笔记的AI分析
 */
@Component
@Slf4j
public class NoteAnalysisConsumer {

    @Autowired
    private NoteRepository noteRepository;
    
    @Autowired
    private NoteAnalysisResultRepository noteAnalysisResultRepository;
    
    @Autowired
    private AiService aiService;
    
    @Autowired
    private KnowledgeService knowledgeService;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 监听笔记分析队列，处理消息
     *
     * @param message 消息内容
     */
    @SuppressWarnings("unchecked")
    @RabbitListener(queues = RabbitMQConfig.NOTE_ANALYSIS_QUEUE)
    @Transactional
    public void consumeNoteAnalysisMessage(Map<String, Object> message) {
        String noteId = null;
        try {
            log.info("接收到笔记分析消息: {}", message);
            
            // 解析消息中的笔记ID
            String noteIdStr = (String) message.get("noteId");
            if (noteIdStr == null || noteIdStr.isEmpty()) {
                log.error("笔记分析消息中缺少noteId字段");
                return;
            }
            
            noteId = noteIdStr;
            boolean forceReanalyze = Boolean.TRUE.equals(message.get("forceReanalyze"));
            
            log.info("开始分析笔记, noteId={}, 强制重新分析={}", noteId, forceReanalyze);
            
            // 获取笔记数据
            Optional<Note> noteOpt = noteRepository.findById(noteId);
            if (!noteOpt.isPresent()) {
                log.error("找不到笔记, noteId={}", noteId);
                return;
            }
            Note note = noteOpt.get();
            
            // 验证笔记内容
            if (note.getContentRichText() == null || note.getContentRichText().trim().isEmpty()) {
                log.error("笔记内容为空, noteId={}", noteId);
                updateNoteProcessingStatus(note, "FAILED", "笔记内容为空");
                return;
            }
            
            // 检查是否已经分析过（除非强制重新分析）
            if (!forceReanalyze && isAlreadyAnalyzed(noteId)) {
                log.info("笔记已分析过，跳过重复分析, noteId={}", noteId);
                return;
            }
            
            // 更新处理状态为进行中
            updateNoteProcessingStatus(note, "PROCESSING", "AI分析进行中");
            
            // 调用AI服务进行分析
            NoteAnalysisResult analysisResult = aiService.analyzeNote(
                noteId,
                note.getTitle(),
                note.getContentRichText()
            );
            
            // 设置用户ID
            analysisResult.setUserId(note.getUserId());
            
            // 保存分析结果到MongoDB
            saveNoteAnalysisResult(noteId, analysisResult);
            
            // 更新笔记处理状态为完成
            updateNoteProcessingStatus(note, "COMPLETED", "AI分析完成");
            
            // 将分析结果发送到知识图谱更新队列
            sendToKnowledgeGraphUpdateQueue(noteId, "note");
            
            log.info("笔记分析完成, noteId={}", noteId);
            
        } catch (DataAccessResourceFailureException e) {
            log.error("数据库连接失败, noteId={}", noteId, e);
            if (noteId != null) {
                updateNoteProcessingStatusSafely(noteId, "FAILED", "数据库连接失败");
            }
            // 重试逻辑应该由Spring Retry等机制处理
            throw e;
        } catch (Exception e) {
            log.error("处理笔记分析消息失败, noteId={}", noteId, e);
            if (noteId != null) {
                updateNoteProcessingStatusSafely(noteId, "FAILED", "AI分析失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 检查笔记是否已经分析过
     *
     * @param noteId 笔记ID
     * @return 是否已分析
     */
    private boolean isAlreadyAnalyzed(String noteId) {
        try {
            Optional<com.aireader.backend.model.mongo.NoteAnalysisResult> existingResult = 
                    noteAnalysisResultRepository.findByNoteId(noteId);
            return existingResult.isPresent();
        } catch (Exception e) {
            log.warn("检查笔记分析状态失败, noteId={}", noteId, e);
            return false;
        }
    }
    
    /**
     * 更新笔记处理状态
     *
     * @param note 笔记对象
     * @param status 状态
     * @param message 状态消息
     */
    private void updateNoteProcessingStatus(Note note, String status, String message) {
        try {
            note.setAiProcessingStatus(status);
            noteRepository.save(note);
            log.info("更新笔记处理状态成功, noteId={}, status={}, message={}", 
                    note.getId(), status, message);
        } catch (Exception e) {
            log.error("更新笔记处理状态失败, noteId={}", note.getId(), e);
        }
    }
    
    /**
     * 安全地更新笔记处理状态（通过ID查找）
     *
     * @param noteId 笔记ID
     * @param status 状态
     * @param message 状态消息
     */
    private void updateNoteProcessingStatusSafely(String noteId, String status, String message) {
        try {
            Optional<Note> noteOpt = noteRepository.findById(noteId);
            if (noteOpt.isPresent()) {
                updateNoteProcessingStatus(noteOpt.get(), status, message);
            }
        } catch (Exception e) {
            log.error("安全更新笔记处理状态失败, noteId={}", noteId, e);
        }
    }

    /**
     * 保存笔记分析结果到MongoDB
     *
     * @param noteId 笔记ID
     * @param analysisResult AI分析结果
     */
    private void saveNoteAnalysisResult(String noteId, NoteAnalysisResult analysisResult) {
        try {
            // 删除旧的分析结果（如果存在）
            noteAnalysisResultRepository.deleteByNoteId(noteId);
            
            // 创建新的MongoDB实体
            com.aireader.backend.model.mongo.NoteAnalysisResult mongoResult = 
                com.aireader.backend.model.mongo.NoteAnalysisResult.builder()
                    .noteId(noteId)
                    .userId(analysisResult.getUserId())
                    .enhancedSummary(analysisResult.getEnhancedSummary())
                    .keyPoints(analysisResult.getKeyPoints() != null ? 
                              analysisResult.getKeyPoints().toArray(new String[0]) : new String[0])
                    .intelligentTags(analysisResult.getIntelligentTags() != null ? 
                                   analysisResult.getIntelligentTags().toArray(new String[0]) : new String[0])
                    .sentimentAnalysis(analysisResult.getSentimentAnalysis())
                    .analysisVersion("1.0.0")
                    .analyzedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            // 转换概念实体
            if (analysisResult.getExtractedConcepts() != null && !analysisResult.getExtractedConcepts().isEmpty()) {
                com.aireader.backend.dto.ai.ArticleAnalysisResult.ConceptEntity[] conceptEntities = 
                    analysisResult.getExtractedConcepts().stream()
                        .toArray(com.aireader.backend.dto.ai.ArticleAnalysisResult.ConceptEntity[]::new);
                mongoResult.setExtractedConcepts(conceptEntities);
            }
            
            // 保存到MongoDB
            noteAnalysisResultRepository.save(mongoResult);
            
            log.info("保存笔记分析结果成功, noteId={}, 概念数量={}, 关键点数量={}", 
                    noteId, 
                    analysisResult.getExtractedConcepts() != null ? analysisResult.getExtractedConcepts().size() : 0,
                    analysisResult.getKeyPoints() != null ? analysisResult.getKeyPoints().size() : 0);
        } catch (Exception e) {
            log.error("保存笔记分析结果失败, noteId=" + noteId, e);
            throw new RuntimeException("保存笔记分析结果失败", e);
        }
    }
    
    /**
     * 发送消息到知识图谱更新队列
     *
     * @param entityId 实体ID
     * @param type 实体类型
     */
    private void sendToKnowledgeGraphUpdateQueue(String entityId, String type) {
        try {
            Map<String, Object> message = Map.of(
                "entityId", entityId,
                "type", type,
                "timestamp", System.currentTimeMillis(),
                "source", "note-analysis"
            );
            
            rabbitTemplate.convertAndSend(
                KnowledgeGraphRabbitConfig.KNOWLEDGE_GRAPH_EXCHANGE,
                KnowledgeGraphRabbitConfig.KNOWLEDGE_GRAPH_UPDATE_ROUTING_KEY,
                message
            );
            
            log.info("已发送到知识图谱更新队列, entityId={}, type={}", entityId, type);
        } catch (Exception e) {
            log.error("发送到知识图谱更新队列失败, entityId={}, type={}", entityId, type, e);
            // 不抛出异常，避免影响主流程
        }
    }
} 
package com.aireader.backend.consumer;

import com.aireader.backend.ai.EnhancedAiAnalysisService;
import com.aireader.backend.dto.ai.ArticleAnalysisResult;
import com.aireader.backend.dto.ai.NoteAnalysisResult;
import com.aireader.backend.config.KnowledgeGraphRabbitConfig;
import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.Note;
import com.aireader.backend.model.mongo.ArticleContent;
import com.aireader.backend.mq.KnowledgeGraphUpdateMessage;
import com.aireader.backend.mq.GraphUpdateMessage;
import com.aireader.backend.repository.ArticleMetadataRepository;
import com.aireader.backend.repository.NoteRepository;
import com.aireader.backend.repository.mongo.ArticleContentRepository;
import com.aireader.backend.service.KnowledgeGraphService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 增强的知识图谱更新消费者
 * 基于Spring AI 1.0.0实现实时知识图谱同步
 */
@Component
@Slf4j
public class EnhancedKnowledgeGraphUpdateConsumer {
    
    @Autowired
    private EnhancedAiAnalysisService enhancedAiAnalysisService;
    
    @Autowired
    private KnowledgeGraphService knowledgeGraphService;
    
    @Autowired
    private ArticleMetadataRepository articleMetadataRepository;
    
    @Autowired
    private ArticleContentRepository articleContentRepository;
    
    @Autowired
    private NoteRepository noteRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final int MAX_RETRY_COUNT = 3;
    
    /**
     * 处理知识图谱更新消息
     */
    @RabbitListener(queues = KnowledgeGraphRabbitConfig.KNOWLEDGE_GRAPH_UPDATE_QUEUE)
    @Transactional
    public void handleGraphUpdate(Map<String, Object> messageMap) {
        log.info("收到知识图谱更新消息: {}", messageMap);
        
        try {
            // 从Map中提取基本信息
            String entityId = (String) messageMap.get("entityId");
            String type = (String) messageMap.get("type");
            String source = (String) messageMap.get("source");
            Long timestamp = (Long) messageMap.get("timestamp");
            
            if (entityId == null || entityId.isEmpty()) {
                log.error("知识图谱更新消息中缺少entityId字段");
                return;
            }
            
            if (type == null || type.isEmpty()) {
                log.error("知识图谱更新消息中缺少type字段");
                return;
            }
            
            // 创建KnowledgeGraphUpdateMessage对象
            KnowledgeGraphUpdateMessage message = KnowledgeGraphUpdateMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .entityId(entityId)
                .entityType(type)
                .updateType(KnowledgeGraphUpdateMessage.UpdateType.CREATE)
                .createdAt(java.time.LocalDateTime.now())
                .retryCount(0)
                .build();
            
            switch (type.toLowerCase()) {
                case "article" -> handleArticleUpdate(message);
                case "note" -> handleNoteUpdate(message);
                default -> log.warn("不支持的实体类型: {}", type);
            }
            
        } catch (Exception e) {
            log.error("处理知识图谱更新消息失败: {}", messageMap, e);
            handleProcessingError(messageMap.toString(), e);
        }
    }
    
    /**
     * 处理文章更新
     */
    private void handleArticleUpdate(KnowledgeGraphUpdateMessage message) {
        log.info("处理文章知识图谱更新: entityId={}, updateType={}", 
                message.getEntityId(), message.getUpdateType());
        
        try {
            // 1. 获取文章数据
            Optional<ArticleMetadata> metadataOpt = articleMetadataRepository
                .findById(message.getEntityId());
            if (!metadataOpt.isPresent()) {
                log.error("文章不存在: {}", message.getEntityId());
                return;
            }
            
            Optional<ArticleContent> contentOpt = articleContentRepository
                .findByMysqlMetadataId(message.getEntityId());
            if (!contentOpt.isPresent() || contentOpt.get().getPlainTextContent() == null) {
                log.error("文章内容不存在: {}", message.getEntityId());
                return;
            }
            
            ArticleMetadata metadata = metadataOpt.get();
            ArticleContent content = contentOpt.get();
            
            // 2. 使用增强AI分析服务进行结构化分析
            ArticleAnalysisResult analysisResult = enhancedAiAnalysisService
                .analyzeArticleWithStructuredOutput(
                    message.getEntityId(),
                    metadata.getTitle(),
                    content.getPlainTextContent()
                );
            
            // 3. 集成到知识图谱
            knowledgeGraphService.integrateArticleToGraph(
                message.getEntityId(), analysisResult);
            
            // 记录图谱更新信息
            log.info("文章知识图谱更新完成: {}, 标题: {}", message.getEntityId(), metadata.getTitle());
            
        } catch (Exception e) {
            log.error("处理文章知识图谱更新失败: {}", message.getEntityId(), e);
            handleRetryOrDLQ(message, e);
        }
    }
    
    /**
     * 处理笔记更新
     */
    private void handleNoteUpdate(KnowledgeGraphUpdateMessage message) {
        log.info("处理笔记知识图谱更新: entityId={}, updateType={}", 
                message.getEntityId(), message.getUpdateType());
        
        try {
            // 1. 获取笔记数据
            Optional<Note> noteOpt = noteRepository.findById(message.getEntityId());
            if (!noteOpt.isPresent()) {
                log.error("笔记不存在: {}", message.getEntityId());
                return;
            }
            
            Note note = noteOpt.get();
            
            // 2. 使用增强AI分析服务进行结构化分析
            NoteAnalysisResult analysisResult = enhancedAiAnalysisService
                .analyzeNoteWithStructuredOutput(
                    message.getEntityId(),
                    note.getTitle(),
                    note.getContentRichText()
                );
            
            // 3. 集成到知识图谱
            knowledgeGraphService.integrateNoteToGraph(
                message.getEntityId(), analysisResult);
            
            // 记录图谱更新信息
            log.info("笔记知识图谱更新完成: {}, 标题: {}", message.getEntityId(), note.getTitle());
            
        } catch (Exception e) {
            log.error("处理笔记知识图谱更新失败: {}", message.getEntityId(), e);
            handleRetryOrDLQ(message, e);
        }
    }
    
    /**
     * 记录图谱更新信息
     */
    private void logGraphUpdate(GraphUpdateMessage updateMessage) {
        log.info("图谱更新: 类型={}, 详情={}", updateMessage.getType(), updateMessage);
    }
    
    /**
     * 创建节点更新数据
     */
    private Object createNodeUpdateData(String type, String id, String title) {
        return new Object() {
            public String getType() { return type; }
            public String getId() { return id; }
            public String getTitle() { return title; }
        };
    }
    
    /**
     * 处理重试或发送到死信队列
     */
    private void handleRetryOrDLQ(KnowledgeGraphUpdateMessage message, Exception error) {
        message.incrementRetryCount();
        
        if (message.isMaxRetryExceeded(MAX_RETRY_COUNT)) {
            log.error("消息处理失败，超过最大重试次数，发送到死信队列: {}", message.getEntityId());
            sendToDLQ(message, error);
        } else {
            log.warn("消息处理失败，准备重试 ({}/{}): {}", 
                    message.getRetryCount(), MAX_RETRY_COUNT, message.getEntityId());
            retryMessage(message);
        }
    }
    
    /**
     * 重试消息
     */
    private void retryMessage(KnowledgeGraphUpdateMessage message) {
        try {
            // 延迟重试（指数退避）
            long delay = (long) Math.pow(2, message.getRetryCount()) * 1000;
            
            rabbitTemplate.convertAndSend(
                KnowledgeGraphRabbitConfig.KNOWLEDGE_GRAPH_RETRY_QUEUE, 
                objectMapper.writeValueAsString(message),
                msg -> {
                    msg.getMessageProperties().setDelay((int) delay);
                    return msg;
                }
            );
        } catch (Exception e) {
            log.error("重试消息发送失败", e);
        }
    }
    
    /**
     * 发送到死信队列
     */
    private void sendToDLQ(KnowledgeGraphUpdateMessage message, Exception error) {
        try {
            rabbitTemplate.convertAndSend(
                KnowledgeGraphRabbitConfig.KNOWLEDGE_GRAPH_DLQ_EXCHANGE, 
                KnowledgeGraphRabbitConfig.KNOWLEDGE_GRAPH_DLQ_ROUTING_KEY,
                objectMapper.writeValueAsString(message)
            );
        } catch (Exception e) {
            log.error("发送到死信队列失败", e);
        }
    }
    
    /**
     * 处理处理错误
     */
    private void handleProcessingError(String messageJson, Exception error) {
        log.error("消息格式错误或处理异常: {}", messageJson, error);
        // 可以发送到专门的错误处理队列
    }
} 
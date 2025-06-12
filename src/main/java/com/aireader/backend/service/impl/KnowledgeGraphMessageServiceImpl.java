package com.aireader.backend.service.impl;

import com.aireader.backend.config.KnowledgeGraphRabbitConfig;
import com.aireader.backend.mq.KnowledgeGraphUpdateMessage;
import com.aireader.backend.service.KnowledgeGraphMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 知识图谱消息服务实现
 * 负责发送知识图谱更新消息到RabbitMQ
 */
@Service
@Slf4j
public class KnowledgeGraphMessageServiceImpl implements KnowledgeGraphMessageService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public void sendArticleUpdateMessage(String articleId) {
        KnowledgeGraphUpdateMessage message = KnowledgeGraphUpdateMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .entityId(articleId)
                .entityType("article")
                .updateType(KnowledgeGraphUpdateMessage.UpdateType.CREATE)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();
        
        sendUpdateMessage(message);
    }
    
    @Override
    public void sendNoteUpdateMessage(String noteId) {
        KnowledgeGraphUpdateMessage message = KnowledgeGraphUpdateMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .entityId(noteId)
                .entityType("note")
                .updateType(KnowledgeGraphUpdateMessage.UpdateType.CREATE)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();
        
        sendUpdateMessage(message);
    }
    
    @Override
    public void sendUpdateMessage(KnowledgeGraphUpdateMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            
            rabbitTemplate.convertAndSend(
                KnowledgeGraphRabbitConfig.KNOWLEDGE_GRAPH_EXCHANGE,
                KnowledgeGraphRabbitConfig.KNOWLEDGE_GRAPH_UPDATE_ROUTING_KEY,
                messageJson
            );
            
            log.info("已发送知识图谱更新消息: entityId={}, entityType={}, updateType={}", 
                    message.getEntityId(), message.getEntityType(), message.getUpdateType());
            
        } catch (Exception e) {
            log.error("发送知识图谱更新消息失败: entityId={}, entityType={}", 
                    message.getEntityId(), message.getEntityType(), e);
        }
    }
    
    @Override
    public void sendArticleReanalyzeMessage(String articleId) {
        KnowledgeGraphUpdateMessage message = KnowledgeGraphUpdateMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .entityId(articleId)
                .entityType("article")
                .updateType(KnowledgeGraphUpdateMessage.UpdateType.REANALYZE)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();
        
        sendUpdateMessage(message);
    }
    
    @Override
    public void sendNoteReanalyzeMessage(String noteId) {
        KnowledgeGraphUpdateMessage message = KnowledgeGraphUpdateMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .entityId(noteId)
                .entityType("note")
                .updateType(KnowledgeGraphUpdateMessage.UpdateType.REANALYZE)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();
        
        sendUpdateMessage(message);
    }
} 
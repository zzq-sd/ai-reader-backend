package com.aireader.backend.consumer;

import com.aireader.backend.config.RabbitMQConfig;
import com.aireader.backend.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 知识图谱更新消费者
 * 消费知识图谱更新队列中的消息，执行知识图谱的更新
 */
@Component
@Slf4j
public class KnowledgeGraphConsumer {

    @Autowired
    private KnowledgeService knowledgeService;

    /**
     * 监听知识图谱更新队列，处理消息
     *
     * @param message 消息内容
     */
    @SuppressWarnings("unchecked")
    @RabbitListener(queues = RabbitMQConfig.KNOWLEDGE_GRAPH_UPDATE_QUEUE)
    public void consumeKnowledgeGraphUpdateMessage(Map<String, Object> message) {
        try {
            String entityId = (String) message.get("entityId");
            String type = (String) message.get("type");
            
            if (entityId == null || type == null) {
                log.error("无效的消息格式: {}", message);
                return;
            }
            
            log.info("接收到知识图谱更新消息: {}", message);
            
            log.info("开始更新知识图谱, type={}, entityId={}", type, entityId);
            
            // 根据不同的类型执行不同的知识图谱更新操作
            boolean success = false;
            switch (type) {
                case "article":
                    // 处理文章相关的知识图谱更新
                    log.info("处理文章知识图谱更新, articleId={}", entityId);
                    success = knowledgeService.buildKnowledgeGraphFromArticle(entityId);
                    break;
                case "note":
                    // 处理笔记相关的知识图谱更新
                    log.info("处理笔记知识图谱更新, noteId={}", entityId);
                    success = knowledgeService.buildKnowledgeGraphFromNote(entityId);
                    break;
                default:
                    log.warn("未知的知识图谱更新类型: {}", type);
                    break;
            }
            
            log.info("知识图谱更新{}, type={}, entityId={}", 
                    success ? "成功" : "失败", type, entityId);
            
        } catch (NumberFormatException e) {
            log.error("实体ID格式错误", e);
        } catch (DataAccessResourceFailureException e) {
            log.error("数据库连接失败", e);
            // 重试逻辑应该由Spring Retry等机制处理
            throw e;
        } catch (Exception e) {
            log.error("处理知识图谱更新消息失败", e);
        }
    }
} 
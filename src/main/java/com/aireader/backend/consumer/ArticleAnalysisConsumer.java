package com.aireader.backend.consumer;

import com.aireader.backend.service.AiService;
import com.aireader.backend.dto.ai.ArticleAnalysisResult;
import com.aireader.backend.config.RabbitMQConfig;
import com.aireader.backend.config.KnowledgeGraphRabbitConfig;
import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.mongo.ArticleContent;
import com.aireader.backend.repository.ArticleMetadataRepository;
import com.aireader.backend.repository.mongo.ArticleContentRepository;
import com.aireader.backend.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 文章分析消费者
 * 消费文章分析队列中的消息，执行文章的AI分析
 */
@Component
@Slf4j
public class ArticleAnalysisConsumer {

    @Autowired
    private ArticleMetadataRepository articleMetadataRepository;
    
    @Autowired
    private ArticleContentRepository articleContentRepository;
    
    @Autowired
    private AiService aiService;
    
    @Autowired
    private KnowledgeService knowledgeService;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 监听文章分析队列，处理消息
     *
     * @param message 消息内容
     */
    @SuppressWarnings("unchecked")
    @RabbitListener(queues = RabbitMQConfig.ARTICLE_ANALYSIS_QUEUE)
    public void consumeArticleAnalysisMessage(Map<String, Object> message) {
        try {
            log.info("接收到文章分析消息: {}", message);
            
            // 解析消息中的文章ID
            String articleIdStr = (String) message.get("articleId");
            if (articleIdStr == null || articleIdStr.isEmpty()) {
                log.error("文章分析消息中缺少articleId字段");
                return;
            }
            
            Long articleIdLong = Long.parseLong(articleIdStr);
            String articleId = articleIdStr; // 使用原始字符串作为ID
            log.info("开始分析文章, articleId={}", articleId);
            
            // 获取文章元数据和内容
            Optional<ArticleMetadata> metadataOpt = articleMetadataRepository.findById(articleId);
            if (!metadataOpt.isPresent()) {
                log.error("找不到文章元数据, articleId={}", articleId);
                return;
            }
            ArticleMetadata metadata = metadataOpt.get();
            
            // 获取文章内容
            Optional<ArticleContent> contentOpt = articleContentRepository.findByMysqlMetadataId(articleId);
            if (!contentOpt.isPresent() || contentOpt.get().getPlainTextContent() == null) {
                log.error("找不到文章内容或内容为空, articleId={}", articleId);
                return;
            }
            ArticleContent content = contentOpt.get();
            
            // 调用AI服务进行分析
            ArticleAnalysisResult analysisResult = aiService.analyzeArticle(
                articleId,
                metadata.getTitle(),
                content.getPlainTextContent()
            );
            
            // 更新文章元数据中的AI分析结果
            updateArticleWithAnalysisResult(metadata, analysisResult);
            
            // 更新MongoDB中的文章内容处理信息
            updateArticleContentProcessingInfo(content, analysisResult);
            
            // 将分析结果发送到知识图谱更新队列
            sendToKnowledgeGraphUpdateQueue(articleId, "article");
            
            log.info("文章分析完成, articleId={}", articleId);
            
        } catch (NumberFormatException e) {
            log.error("文章ID格式错误", e);
        } catch (DataAccessResourceFailureException e) {
            log.error("数据库连接失败", e);
            // 重试逻辑应该由Spring Retry等机制处理
            throw e;
        } catch (Exception e) {
            log.error("处理文章分析消息失败", e);
        }
    }
    
    /**
     * 根据分析结果更新文章元数据
     *
     * @param metadata 文章元数据
     * @param result 分析结果
     */
    private void updateArticleWithAnalysisResult(ArticleMetadata metadata, ArticleAnalysisResult result) {
        try {
            // 更新估计阅读时间
            if (result.getReadingTimeMinutes() != null) {
                metadata.setReadingTimeMinutes(result.getReadingTimeMinutes());
            }
            
            // 更新摘要
            if (result.getSummary() != null && !result.getSummary().isEmpty()) {
                metadata.setSummary(result.getSummary());
            }
            
            // 更新主题分类
            if (result.getCategory() != null && !result.getCategory().isEmpty()) {
                metadata.setCategory(result.getCategory());
            }
            
            // 更新处理状态
            metadata.setAiProcessingStatus("COMPLETED");
            
            // 保存更新后的元数据
            articleMetadataRepository.save(metadata);
            log.info("更新文章元数据成功, articleId={}", metadata.getId());
        } catch (Exception e) {
            log.error("更新文章元数据失败, articleId=" + metadata.getId(), e);
        }
    }
    
    /**
     * 更新MongoDB中的文章内容处理信息
     *
     * @param content 文章内容
     * @param result AI分析结果
     */
    private void updateArticleContentProcessingInfo(ArticleContent content, ArticleAnalysisResult result) {
        try {
            // 创建或更新处理信息
            ArticleContent.ArticleProcessingInfo processingInfo = content.getProcessingInfo();
            if (processingInfo == null) {
                processingInfo = ArticleContent.ArticleProcessingInfo.builder().build();
            }
            
            // 更新AI分析结果
            processingInfo.setEnhancedSummary(result.getSummary());
            processingInfo.setKeyPoints(result.getKeyPoints().toArray(new String[0]));
            processingInfo.setIntelligentTags(result.getIntelligentTags().toArray(new String[0]));
            processingInfo.setSentimentAnalysis(result.getSentiment());
            processingInfo.setAnalysisVersion("1.0.0");
            processingInfo.setLastAnalyzedAt(java.time.LocalDateTime.now());
            
                    // 转换概念实体
        ArticleAnalysisResult.ConceptEntity[] conceptEntities =
                result.getConcepts().stream()
                        .toArray(ArticleAnalysisResult.ConceptEntity[]::new);
            processingInfo.setExtractedConcepts(conceptEntities);
            
            // 更新传统字段以保持兼容性
            processingInfo.setExtractedKeywords(result.getKeywords().toArray(new String[0]));
            processingInfo.setReadingTimeMinutes(result.getReadingTimeMinutes());
            
            // 设置处理信息并保存
            content.setProcessingInfo(processingInfo);
            articleContentRepository.save(content);
            
            log.info("更新文章内容处理信息成功, articleId={}", content.getMysqlMetadataId());
        } catch (Exception e) {
            log.error("更新文章内容处理信息失败, articleId=" + content.getMysqlMetadataId(), e);
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
                "type", type
            );
            
            rabbitTemplate.convertAndSend(
                KnowledgeGraphRabbitConfig.KNOWLEDGE_GRAPH_EXCHANGE,
                KnowledgeGraphRabbitConfig.KNOWLEDGE_GRAPH_UPDATE_ROUTING_KEY,
                message
            );
            
            log.info("已发送到知识图谱更新队列, entityId={}, type={}", entityId, type);
        } catch (Exception e) {
            log.error("发送到知识图谱更新队列失败", e);
        }
    }
} 
package com.aireader.backend.consumer;

import com.aireader.backend.config.RabbitMQConfig;
import com.aireader.backend.service.ArticleFetchService;
import com.aireader.backend.service.RssFeedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RSS抓取消费者
 * 消费RSS抓取队列中的消息并调用服务进行处理
 */
@Component
@Slf4j
public class RssFetchConsumer {

    private final ArticleFetchService articleFetchService;
    private final RssFeedService rssFeedService;
    private final ObjectMapper objectMapper;

    @Autowired
    public RssFetchConsumer(ArticleFetchService articleFetchService, 
                           RssFeedService rssFeedService,
                           ObjectMapper objectMapper) {
        this.articleFetchService = articleFetchService;
        this.rssFeedService = rssFeedService;
        this.objectMapper = objectMapper;
    }

    /**
     * 监听RSS抓取队列，处理消息
     *
     * @param message 消息内容
     */
    @SuppressWarnings("unchecked")
    @RabbitListener(queues = RabbitMQConfig.RSS_FETCH_QUEUE)
    public void consumeRssFetchMessage(Map<String, Object> message) {
        try {
            log.info("接收到RSS抓取消息: {}", message);
            
            // 解析消息中的RSS源ID
            String sourceIdStr = (String) message.get("sourceId");
            if (sourceIdStr == null || sourceIdStr.isEmpty()) {
                log.error("RSS抓取消息中缺少sourceId字段");
                return;
            }
            
            log.info("开始处理RSS源抓取, sourceId={}", sourceIdStr);
            
            // 调用文章抓取服务执行抓取
            int fetchedCount = articleFetchService.fetchArticlesFromSourceById(sourceIdStr);
            log.info("RSS源抓取完成, sourceId={}, 抓取文章数量={}", sourceIdStr, fetchedCount);
            
        } catch (Exception e) {
            log.error("处理RSS抓取消息失败", e);
        }
    }
} 
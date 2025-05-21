package com.aireader.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMQConfig {

    // 交换机名称
    public static final String RSS_PROCESSING_EXCHANGE = "ex.direct.rss_processing";
    public static final String AI_PROCESSING_EXCHANGE = "ex.direct.ai_processing";
    public static final String KG_UPDATES_EXCHANGE = "ex.direct.kg_updates";
    public static final String NOTIFICATIONS_EXCHANGE = "ex.fanout.notifications";

    // 队列名称
    public static final String RSS_FETCH_PARSE_QUEUE = "q.rss.fetch_parse";
    public static final String ARTICLE_AI_ANALYZE_QUEUE = "q.article.ai_analyze";
    public static final String NOTE_AI_ANALYZE_QUEUE = "q.note.ai_analyze";
    public static final String KG_UPDATE_FROM_AI_QUEUE = "q.kg.update_from_ai";
    
    // 路由键
    public static final String RSS_FETCH_PARSE_ROUTING_KEY = "rk.rss.fetch_parse";
    public static final String ARTICLE_AI_ANALYZE_ROUTING_KEY = "rk.article.ai_analyze";
    public static final String NOTE_AI_ANALYZE_ROUTING_KEY = "rk.note.ai_analyze";
    public static final String KG_UPDATE_FROM_AI_ROUTING_KEY = "rk.kg.update_from_ai";

    /**
     * RabbitMQ消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置RabbitTemplate以使用JSON消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    // 交换机
    @Bean
    public DirectExchange rssProcessingExchange() {
        return new DirectExchange(RSS_PROCESSING_EXCHANGE);
    }

    @Bean
    public DirectExchange aiProcessingExchange() {
        return new DirectExchange(AI_PROCESSING_EXCHANGE);
    }

    @Bean
    public DirectExchange kgUpdatesExchange() {
        return new DirectExchange(KG_UPDATES_EXCHANGE);
    }

    @Bean
    public FanoutExchange notificationsExchange() {
        return new FanoutExchange(NOTIFICATIONS_EXCHANGE);
    }

    // 队列
    @Bean
    public Queue rssFetchParseQueue() {
        return QueueBuilder.durable(RSS_FETCH_PARSE_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.direct")
                .withArgument("x-dead-letter-routing-key", "dlrk.rss.fetch_parse")
                .build();
    }

    @Bean
    public Queue articleAiAnalyzeQueue() {
        return QueueBuilder.durable(ARTICLE_AI_ANALYZE_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.direct")
                .withArgument("x-dead-letter-routing-key", "dlrk.article.ai_analyze")
                .build();
    }

    @Bean
    public Queue noteAiAnalyzeQueue() {
        return QueueBuilder.durable(NOTE_AI_ANALYZE_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.direct")
                .withArgument("x-dead-letter-routing-key", "dlrk.note.ai_analyze")
                .build();
    }

    @Bean
    public Queue kgUpdateFromAiQueue() {
        return QueueBuilder.durable(KG_UPDATE_FROM_AI_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.direct")
                .withArgument("x-dead-letter-routing-key", "dlrk.kg.update_from_ai")
                .build();
    }

    // 死信交换机和队列
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dlx.direct");
    }

    @Bean
    public Queue deadLetterRssQueue() {
        return QueueBuilder.durable("dlq.rss.fetch_parse").build();
    }

    @Bean
    public Queue deadLetterArticleAiQueue() {
        return QueueBuilder.durable("dlq.article.ai_analyze").build();
    }

    @Bean
    public Queue deadLetterNoteAiQueue() {
        return QueueBuilder.durable("dlq.note.ai_analyze").build();
    }

    @Bean
    public Queue deadLetterKgQueue() {
        return QueueBuilder.durable("dlq.kg.update_from_ai").build();
    }

    // 绑定关系
    @Bean
    public Binding rssFetchParseBinding() {
        return BindingBuilder.bind(rssFetchParseQueue()).to(rssProcessingExchange()).with(RSS_FETCH_PARSE_ROUTING_KEY);
    }

    @Bean
    public Binding articleAiAnalyzeBinding() {
        return BindingBuilder.bind(articleAiAnalyzeQueue()).to(aiProcessingExchange()).with(ARTICLE_AI_ANALYZE_ROUTING_KEY);
    }

    @Bean
    public Binding noteAiAnalyzeBinding() {
        return BindingBuilder.bind(noteAiAnalyzeQueue()).to(aiProcessingExchange()).with(NOTE_AI_ANALYZE_ROUTING_KEY);
    }

    @Bean
    public Binding kgUpdateFromAiBinding() {
        return BindingBuilder.bind(kgUpdateFromAiQueue()).to(kgUpdatesExchange()).with(KG_UPDATE_FROM_AI_ROUTING_KEY);
    }

    // 死信队列绑定
    @Bean
    public Binding deadLetterRssBinding() {
        return BindingBuilder.bind(deadLetterRssQueue()).to(deadLetterExchange()).with("dlrk.rss.fetch_parse");
    }

    @Bean
    public Binding deadLetterArticleAiBinding() {
        return BindingBuilder.bind(deadLetterArticleAiQueue()).to(deadLetterExchange()).with("dlrk.article.ai_analyze");
    }

    @Bean
    public Binding deadLetterNoteAiBinding() {
        return BindingBuilder.bind(deadLetterNoteAiQueue()).to(deadLetterExchange()).with("dlrk.note.ai_analyze");
    }

    @Bean
    public Binding deadLetterKgBinding() {
        return BindingBuilder.bind(deadLetterKgQueue()).to(deadLetterExchange()).with("dlrk.kg.update_from_ai");
    }
} 
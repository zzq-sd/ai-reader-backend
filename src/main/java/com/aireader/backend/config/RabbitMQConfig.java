package com.aireader.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMQConfig {

    // 队列名称
    public static final String RSS_FETCH_QUEUE = "rss.fetch.queue";
    public static final String ARTICLE_ANALYSIS_QUEUE = "article.analysis.queue";
    public static final String KNOWLEDGE_GRAPH_UPDATE_QUEUE = "knowledge.graph.update.queue";
    
    // 交换机名称
    public static final String RSS_EXCHANGE = "rss.exchange";
    public static final String ARTICLE_EXCHANGE = "article.exchange";
    public static final String KNOWLEDGE_EXCHANGE = "knowledge.exchange";
    
    // 路由键
    public static final String RSS_FETCH_ROUTING_KEY = "rss.fetch";
    public static final String ARTICLE_ANALYSIS_ROUTING_KEY = "article.analysis";
    public static final String KNOWLEDGE_GRAPH_UPDATE_ROUTING_KEY = "knowledge.graph.update";

    // RSS抓取队列
    @Bean
    Queue rssFetchQueue() {
        return QueueBuilder.durable(RSS_FETCH_QUEUE)
                .withArgument("x-message-ttl", 60000) // 消息过期时间：60秒
                .build();
    }
    
    // 文章分析队列
    @Bean
    Queue articleAnalysisQueue() {
        return QueueBuilder.durable(ARTICLE_ANALYSIS_QUEUE)
                .withArgument("x-message-ttl", 3600000) // 消息过期时间：1小时
                .build();
    }
    
    // 知识图谱更新队列
    @Bean
    Queue knowledgeGraphUpdateQueue() {
        return QueueBuilder.durable(KNOWLEDGE_GRAPH_UPDATE_QUEUE)
                .withArgument("x-message-ttl", 7200000) // 消息过期时间：2小时
                .build();
    }

    // RSS交换机
    @Bean
    DirectExchange rssExchange() {
        return new DirectExchange(RSS_EXCHANGE);
    }
    
    // 文章交换机
    @Bean
    DirectExchange articleExchange() {
        return new DirectExchange(ARTICLE_EXCHANGE);
    }
    
    // 知识图谱交换机
    @Bean
    DirectExchange knowledgeExchange() {
        return new DirectExchange(KNOWLEDGE_EXCHANGE);
    }

    // RSS抓取绑定
    @Bean
    Binding rssBinding(Queue rssFetchQueue, DirectExchange rssExchange) {
        return BindingBuilder.bind(rssFetchQueue)
                .to(rssExchange)
                .with(RSS_FETCH_ROUTING_KEY);
    }
    
    // 文章分析绑定
    @Bean
    Binding articleBinding(Queue articleAnalysisQueue, DirectExchange articleExchange) {
        return BindingBuilder.bind(articleAnalysisQueue)
                .to(articleExchange)
                .with(ARTICLE_ANALYSIS_ROUTING_KEY);
    }
    
    // 知识图谱更新绑定
    @Bean
    Binding knowledgeBinding(Queue knowledgeGraphUpdateQueue, DirectExchange knowledgeExchange) {
        return BindingBuilder.bind(knowledgeGraphUpdateQueue)
                .to(knowledgeExchange)
                .with(KNOWLEDGE_GRAPH_UPDATE_ROUTING_KEY);
    }
    
    // 消息转换器
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    // 配置RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
} 
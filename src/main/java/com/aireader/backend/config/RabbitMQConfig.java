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
    public static final String NOTE_ANALYSIS_QUEUE = "note.analysis.queue";
    
    // 交换机名称
    public static final String RSS_EXCHANGE = "rss.exchange";
    public static final String ARTICLE_EXCHANGE = "article.exchange";
    public static final String NOTE_EXCHANGE = "note.exchange";
    
    // 路由键
    public static final String RSS_FETCH_ROUTING_KEY = "rss.fetch";
    public static final String ARTICLE_ANALYSIS_ROUTING_KEY = "article.analysis";
    public static final String NOTE_ANALYSIS_ROUTING_KEY = "note.analysis";

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
    
    // 笔记分析队列
    @Bean
    Queue noteAnalysisQueue() {
        return QueueBuilder.durable(NOTE_ANALYSIS_QUEUE)
                .withArgument("x-message-ttl", 3600000) // 消息过期时间：1小时
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
    
    // 笔记交换机
    @Bean
    DirectExchange noteExchange() {
        return new DirectExchange(NOTE_EXCHANGE);
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
    
    // 笔记分析绑定
    @Bean
    Binding noteBinding(Queue noteAnalysisQueue, DirectExchange noteExchange) {
        return BindingBuilder.bind(noteAnalysisQueue)
                .to(noteExchange)
                .with(NOTE_ANALYSIS_ROUTING_KEY);
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
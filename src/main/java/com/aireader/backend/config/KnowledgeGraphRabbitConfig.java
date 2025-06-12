package com.aireader.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 知识图谱相关的RabbitMQ配置
 * 配置知识图谱更新、重试和死信队列
 */
@Configuration
public class KnowledgeGraphRabbitConfig {
    
    // 队列名称常量
    public static final String KNOWLEDGE_GRAPH_UPDATE_QUEUE = "knowledge.graph.update";
    public static final String KNOWLEDGE_GRAPH_RETRY_QUEUE = "knowledge.graph.retry";
    public static final String KNOWLEDGE_GRAPH_DLQ = "knowledge.graph.dlq";
    
    // 交换机名称常量
    public static final String KNOWLEDGE_GRAPH_EXCHANGE = "knowledge.graph.exchange";
    public static final String KNOWLEDGE_GRAPH_DLQ_EXCHANGE = "knowledge.graph.dlq.exchange";
    
    // 路由键常量
    public static final String KNOWLEDGE_GRAPH_UPDATE_ROUTING_KEY = "knowledge.graph.update";
    public static final String KNOWLEDGE_GRAPH_RETRY_ROUTING_KEY = "knowledge.graph.retry";
    public static final String KNOWLEDGE_GRAPH_DLQ_ROUTING_KEY = "knowledge.graph.failed";
    
    /**
     * 知识图谱更新队列
     */
    @Bean
    public Queue knowledgeGraphUpdateQueue() {
        return QueueBuilder.durable(KNOWLEDGE_GRAPH_UPDATE_QUEUE)
                .withArgument("x-dead-letter-exchange", KNOWLEDGE_GRAPH_DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", KNOWLEDGE_GRAPH_DLQ_ROUTING_KEY)
                .build();
    }
    
    /**
     * 知识图谱重试队列
     */
    @Bean
    public Queue knowledgeGraphRetryQueue() {
        return QueueBuilder.durable(KNOWLEDGE_GRAPH_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", KNOWLEDGE_GRAPH_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", KNOWLEDGE_GRAPH_UPDATE_ROUTING_KEY)
                .withArgument("x-message-ttl", 60000) // 1分钟TTL
                .build();
    }
    
    /**
     * 知识图谱死信队列
     */
    @Bean
    public Queue knowledgeGraphDLQ() {
        return QueueBuilder.durable(KNOWLEDGE_GRAPH_DLQ).build();
    }
    
    /**
     * 知识图谱主交换机
     */
    @Bean
    public DirectExchange knowledgeGraphExchange() {
        return new DirectExchange(KNOWLEDGE_GRAPH_EXCHANGE);
    }
    
    /**
     * 知识图谱死信交换机
     */
    @Bean
    public DirectExchange knowledgeGraphDLQExchange() {
        return new DirectExchange(KNOWLEDGE_GRAPH_DLQ_EXCHANGE);
    }
    
    /**
     * 绑定知识图谱更新队列到交换机
     */
    @Bean
    public Binding knowledgeGraphUpdateBinding() {
        return BindingBuilder
                .bind(knowledgeGraphUpdateQueue())
                .to(knowledgeGraphExchange())
                .with(KNOWLEDGE_GRAPH_UPDATE_ROUTING_KEY);
    }
    
    /**
     * 绑定知识图谱重试队列到交换机
     */
    @Bean
    public Binding knowledgeGraphRetryBinding() {
        return BindingBuilder
                .bind(knowledgeGraphRetryQueue())
                .to(knowledgeGraphExchange())
                .with(KNOWLEDGE_GRAPH_RETRY_ROUTING_KEY);
    }
    
    /**
     * 绑定知识图谱死信队列到死信交换机
     */
    @Bean
    public Binding knowledgeGraphDLQBinding() {
        return BindingBuilder
                .bind(knowledgeGraphDLQ())
                .to(knowledgeGraphDLQExchange())
                .with(KNOWLEDGE_GRAPH_DLQ_ROUTING_KEY);
    }
} 
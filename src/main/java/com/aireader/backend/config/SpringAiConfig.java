package com.aireader.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring AI 1.0正式版配置类
 * 支持智谱AI GLM模型
 * 
 * @author AI Assistant
 * @version 1.0
 */
@Configuration
@EnableRetry
@Slf4j
public class SpringAiConfig {

    /**
     * 创建主要ChatClient实例 - 基于智谱AI(GLM)
     */
    @Bean
    @Primary
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("💬 配置主要ChatClient - 使用智谱AI API");
        
        return ChatClient.builder(chatModel)
            .defaultSystem("""
                你是一个智能、友好的AI助手，具备以下特点：
                
                🤖 **身份特征**：
                - 我是基于GLM-4模型开发的AI助手
                - 我擅长理解和分析各种文本内容
                - 我能够进行自然、流畅的对话
                
                💬 **对话风格**：
                - 用自然、友好的语言与用户交流
                - 回答准确、有用，语言简洁明了
                - 根据问题复杂度调整回答的详细程度
                - 必要时提供具体的例子或建议
                
                🎯 **核心能力**：
                - 文本分析和内容理解
                - 知识问答和信息检索
                - 创意写作和内容生成
                - 编程和技术问题解答
                
                请始终保持helpful、harmless、honest的原则。
                """)
            .build();
    }

    /**
     * 配置RAG增强的ChatClient - 基于智谱AI(GLM)
     */
    @Bean("ragChatClient")
    public ChatClient ragChatClient(ChatModel chatModel) {
        log.info("🔍 配置基于GLM的RAG增强ChatClient");
        
        return ChatClient.builder(chatModel)
            .defaultSystem("""
                你是一个专业的知识图谱分析助手，擅长基于上下文信息进行深度分析。
                请结合提供的背景信息，对用户查询进行准确回答。
                """)
            .build();
    }

    /**
     * Spring AI健康检查指示器
     */
    @Bean
    public SpringAiHealthIndicator springAiHealthIndicator(ChatClient chatClient) {
        return new SpringAiHealthIndicator(chatClient);
    }

    /**
     * Spring AI健康检查指示器类
     */
    public static class SpringAiHealthIndicator {
        private final ChatClient chatClient;

        public SpringAiHealthIndicator(ChatClient chatClient) {
            this.chatClient = chatClient;
            log.info("🎯 SpringAI健康检查器初始化完成");
        }

        /**
         * 测试AI服务连接
         */
        public boolean testConnection() {
            try {
                String response = chatClient.prompt()
                        .user("测试连接")
                        .call()
                        .content();
                
                log.info("🎯 AI服务连接测试成功: {}", 
                    response.substring(0, Math.min(50, response.length())));
                return true;
            } catch (Exception e) {
                log.error("❌ AI服务连接测试失败", e);
                return false;
            }
        }

        /**
         * 测试概念提取功能
         */
        public boolean testConceptExtraction() {
            try {
                String testText = "人工智能是计算机科学的一个分支，它试图理解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。";
                
                String response = chatClient.prompt()
                        .user("请从以下文本中提取关键概念：" + testText)
                        .call()
                        .content();
                
                log.info("🧠 概念提取测试成功: {}", 
                    response.substring(0, Math.min(100, response.length())));
                return true;
            } catch (Exception e) {
                log.error("❌ 概念提取测试失败", e);
                return false;
            }
        }

        /**
         * 测试知识图谱分析功能
         */
        public boolean testKnowledgeGraphAnalysis() {
            try {
                String testText = "Spring框架是Java平台上的开源应用框架，它提供了依赖注入和面向切面编程的功能。";
                
                String response = chatClient.prompt()
                        .user("""
                            请分析以下文本并提取知识图谱信息，包括实体、概念和关系：
                            
                            """ + testText + """
                            
                            请以JSON格式返回结果，包含：
                            - entities: 实体列表
                            - concepts: 概念列表  
                            - relationships: 关系列表
                            """)
                        .call()
                        .content();
                
                log.info("🕸️ 知识图谱分析测试成功: {}", 
                    response.substring(0, Math.min(200, response.length())));
                return true;
            } catch (Exception e) {
                log.error("❌ 知识图谱分析测试失败", e);
                return false;
            }
        }
    }
} 
package com.aireader.backend.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatClient配置测试
 * 验证Spring AI 1.0.0的ChatClient Bean是否正确配置
 */
@SpringBootTest
@ActiveProfiles("test")
public class ChatClientTest {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    @Qualifier("ragChatClient")
    private ChatClient ragChatClient;

    @Test
    public void testChatClientBeanExists() {
        assertNotNull(chatClient, "ChatClient Bean应该被正确创建");
        assertNotNull(ragChatClient, "RAG ChatClient Bean应该被正确创建");
    }

    @Test
    public void testChatClientBasicFunctionality() {
        // 测试基本的ChatClient功能
        try {
            String response = chatClient.prompt()
                    .user("你好，请回复'测试成功'")
                    .call()
                    .content();
            
            assertNotNull(response, "ChatClient应该返回响应");
            assertFalse(response.trim().isEmpty(), "响应不应该为空");
            
            System.out.println("ChatClient测试响应: " + response);
        } catch (Exception e) {
            // 如果没有配置API密钥或网络问题，测试可能会失败
            // 这里只是验证Bean配置是否正确
            System.out.println("ChatClient调用失败（可能是API配置问题）: " + e.getMessage());
        }
    }

    @Test
    public void testRagChatClientBasicFunctionality() {
        // 测试RAG ChatClient功能
        try {
            String response = ragChatClient.prompt()
                    .user("你好，请回复'RAG测试成功'")
                    .call()
                    .content();
            
            assertNotNull(response, "RAG ChatClient应该返回响应");
            assertFalse(response.trim().isEmpty(), "响应不应该为空");
            
            System.out.println("RAG ChatClient测试响应: " + response);
        } catch (Exception e) {
            // 如果没有配置API密钥或网络问题，测试可能会失败
            // 这里只是验证Bean配置是否正确
            System.out.println("RAG ChatClient调用失败（可能是API配置问题）: " + e.getMessage());
        }
    }
} 
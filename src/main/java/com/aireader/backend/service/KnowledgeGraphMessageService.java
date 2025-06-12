package com.aireader.backend.service;

import com.aireader.backend.mq.KnowledgeGraphUpdateMessage;

/**
 * 知识图谱消息服务接口
 * 用于发送知识图谱更新消息到RabbitMQ
 */
public interface KnowledgeGraphMessageService {
    
    /**
     * 发送文章知识图谱更新消息
     * 
     * @param articleId 文章ID
     */
    void sendArticleUpdateMessage(String articleId);
    
    /**
     * 发送笔记知识图谱更新消息
     * 
     * @param noteId 笔记ID
     */
    void sendNoteUpdateMessage(String noteId);
    
    /**
     * 发送知识图谱更新消息
     * 
     * @param message 更新消息
     */
    void sendUpdateMessage(KnowledgeGraphUpdateMessage message);
    
    /**
     * 发送文章重新分析消息
     * 
     * @param articleId 文章ID
     */
    void sendArticleReanalyzeMessage(String articleId);
    
    /**
     * 发送笔记重新分析消息
     * 
     * @param noteId 笔记ID
     */
    void sendNoteReanalyzeMessage(String noteId);
}
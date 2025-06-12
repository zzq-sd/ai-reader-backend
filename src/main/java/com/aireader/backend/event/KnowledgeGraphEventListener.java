package com.aireader.backend.event;

import com.aireader.backend.service.KnowledgeGraphMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 知识图谱事件监听器
 * 监听文章和笔记的保存事件，自动触发知识图谱更新
 */
@Component
@Slf4j
public class KnowledgeGraphEventListener {
    
    @Autowired
    private KnowledgeGraphMessageService messageService;
    
    /**
     * 监听文章保存事件
     */
    @EventListener
    @Async
    public void handleArticleSavedEvent(ArticleSavedEvent event) {
        log.info("🔗 监听到文章保存事件，触发知识图谱更新: {}", event.getArticleId());
        
        try {
            // 发送知识图谱更新消息
            messageService.sendArticleUpdateMessage(event.getArticleId());
            
            log.info("✅ 文章知识图谱更新消息已发送: {}", event.getArticleId());
            
        } catch (Exception e) {
            log.error("❌ 处理文章保存事件失败: {}", event.getArticleId(), e);
        }
    }
    
    /**
     * 监听笔记保存事件
     */
    @EventListener
    @Async
    public void handleNoteSavedEvent(NoteSavedEvent event) {
        log.info("📝 监听到笔记保存事件，触发知识图谱更新: {}", event.getNoteId());
        
        try {
            // 发送知识图谱更新消息
            messageService.sendNoteUpdateMessage(event.getNoteId());
            
            log.info("✅ 笔记知识图谱更新消息已发送: {}", event.getNoteId());
            
        } catch (Exception e) {
            log.error("❌ 处理笔记保存事件失败: {}", event.getNoteId(), e);
        }
    }
    
    /**
     * 文章保存事件
     */
    public static class ArticleSavedEvent {
        private final String articleId;
        private final String userId;
        
        public ArticleSavedEvent(String articleId, String userId) {
            this.articleId = articleId;
            this.userId = userId;
        }
        
        public String getArticleId() {
            return articleId;
        }
        
        public String getUserId() {
            return userId;
        }
    }
    
    /**
     * 笔记保存事件
     */
    public static class NoteSavedEvent {
        private final String noteId;
        private final String userId;
        
        public NoteSavedEvent(String noteId, String userId) {
            this.noteId = noteId;
            this.userId = userId;
        }
        
        public String getNoteId() {
            return noteId;
        }
        
        public String getUserId() {
            return userId;
        }
    }
} 
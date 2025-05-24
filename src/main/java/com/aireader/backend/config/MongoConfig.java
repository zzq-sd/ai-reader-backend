package com.aireader.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.bson.Document;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB配置类
 * 用于配置MongoDB的索引等设置
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    /**
     * 应用启动后创建MongoDB索引
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initIndicesAfterStartup() {
        IndexOperations indexOps = mongoTemplate.indexOps("articles_content");
        
        // 创建内容全文索引
        TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("plainTextContent", 3.0f)  // 纯文本内容，权重为3
                .onField("titleFromContent", 5.0f)  // 标题，权重为5
                .build();
        indexOps.ensureIndex(textIndex);
        
        // 创建原始URL的唯一索引
        IndexDefinition urlIndex = new CompoundIndexDefinition(
                new Document("originalUrl", 1))
                .unique();
        indexOps.ensureIndex(urlIndex);
        
        // 创建内容哈希值索引（用于检测重复）
        IndexDefinition hashIndex = new CompoundIndexDefinition(
                new Document("contentHash", 1))
                .unique();
        indexOps.ensureIndex(hashIndex);
        
        // 创建抓取时间索引（用于清理旧数据）
        IndexDefinition fetchedAtIndex = new CompoundIndexDefinition(
                new Document("fetchedAt", -1));
        indexOps.ensureIndex(fetchedAtIndex);
    }
} 
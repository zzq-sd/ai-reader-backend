package com.aireader.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.data.neo4j.core.Neo4jClient;

/**
 * 数据库连接验证组件
 * 在应用启动时验证各数据库连接是否正常
 */
@Slf4j
@Component
public class DatabaseStartupValidator {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private Neo4jClient neo4jClient;
    
    /**
     * 应用启动后验证数据库连接
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateDatabaseConnections() {
        try {
            // 验证MySQL连接
            String mySqlVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            log.info("MySQL数据库连接成功，版本: {}", mySqlVersion);
            
            // 验证MongoDB连接
            Object mongoVersion = mongoTemplate.executeCommand("{ buildInfo: 1 }").get("version");
            log.info("MongoDB数据库连接成功，版本: {}", mongoVersion);
            
            // 验证Neo4j连接
            String neoVersion = neo4jClient.query("RETURN 'Neo4j连接测试通过' AS result")
                    .fetchAs(String.class)
                    .one()
                    .orElse("Neo4j连接成功，但未返回数据");
            log.info(neoVersion);
            
            log.info("所有数据库连接验证通过，应用程序就绪");
        } catch (Exception e) {
            log.error("数据库连接验证失败，错误信息: {}", e.getMessage(), e);
            throw new RuntimeException("数据库连接验证失败，请检查配置", e);
        }
    }
} 
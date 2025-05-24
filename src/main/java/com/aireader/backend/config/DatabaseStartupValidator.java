package com.aireader.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
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
    
    @Autowired
    private Driver neo4jDriver;
    
    /**
     * 应用启动后验证数据库连接
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateDatabaseConnections() {
        validateMySqlConnection();
        validateMongoDBConnection();
        validateNeo4jConnection();
    }
    
    private void validateMySqlConnection() {
        try {
            // 验证MySQL连接
            String mySqlVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            log.info("MySQL数据库连接成功，版本: {}", mySqlVersion);
        } catch (Exception e) {
            log.error("MySQL数据库连接验证失败，错误信息: {}", e.getMessage(), e);
            throw new RuntimeException("MySQL数据库连接验证失败，请检查配置", e);
        }
    }
    
    private void validateMongoDBConnection() {
        try {
            // 验证MongoDB连接
            Object mongoVersion = mongoTemplate.executeCommand("{ buildInfo: 1 }").get("version");
            log.info("MongoDB数据库连接成功，版本: {}", mongoVersion);
        } catch (Exception e) {
            log.error("MongoDB数据库连接验证失败，错误信息: {}", e.getMessage(), e);
            throw new RuntimeException("MongoDB数据库连接验证失败，请检查配置", e);
        }
    }
    
    private void validateNeo4jConnection() {
        try {
            // 使用配置的Neo4j驱动直接验证连接
            neo4jDriver.verifyConnectivity();
            
            // 使用Session执行查询获取版本信息
            try (Session session = neo4jDriver.session()) {
                String neoVersion = session.run("CALL dbms.components() YIELD name, versions, edition RETURN name, versions, edition")
                        .single().get("name") + " " + session.run("CALL dbms.components() YIELD name, versions, edition RETURN name, versions, edition")
                        .single().get("edition") + " 版本: " + session.run("CALL dbms.components() YIELD name, versions, edition RETURN name, versions, edition")
                        .single().get("versions").asList().get(0);
                log.info("Neo4j数据库连接成功: {}", neoVersion);
                
                // 检查数据库中的节点数量
                Long nodeCount = session.run("MATCH (n) RETURN count(n) AS nodeCount")
                        .single().get("nodeCount").asLong();
                log.info("Neo4j数据库当前包含 {} 个节点", nodeCount);
            }
            
            log.info("所有数据库连接验证通过，应用程序就绪");
        } catch (Exception e) {
            // 临时禁用Neo4j连接验证，仅记录警告日志而不抛出异常
            log.warn("Neo4j数据库连接验证失败，错误信息: {}", e.getMessage());
            log.warn("请确认Neo4j服务是否正在运行，检查应用程序配置中的Neo4j连接参数是否正确");
            log.warn("Neo4j功能将暂时不可用，但应用程序将继续运行");
            // 不再抛出异常
            // throw new RuntimeException("Neo4j数据库连接验证失败，请检查配置", e);
        }
    }
} 
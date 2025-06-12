package com.aireader.backend.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;

/**
 * Neo4j驱动配置类
 * 用于显式配置Neo4j驱动和认证方式
 * 适配Spring Boot 3.x的Neo4j配置
 */
@Configuration
public class Neo4jDriverConfig {

    @Value("${spring.data.neo4j.uri}")
    private String uri;

    @Value("${spring.data.neo4j.authentication.username}")
    private String username;

    @Value("${spring.data.neo4j.authentication.password}")
    private String password;

    /**
     * 配置Neo4j驱动
     * 显式指定认证方式为基本认证
     * 
     * @return Neo4j驱动实例
     */
    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }

    /**
     * 配置Neo4j事务管理器
     * 
     * @param driver Neo4j驱动
     * @return Neo4j事务管理器
     */
    @Bean
    public Neo4jTransactionManager neo4jTransactionManager(Driver driver) {
        return new Neo4jTransactionManager(driver);
    }
} 
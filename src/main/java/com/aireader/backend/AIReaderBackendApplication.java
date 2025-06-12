package com.aireader.backend;

import com.aireader.backend.config.Neo4jDateTimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * AI阅读器系统后端应用程序入口
 */
@SpringBootApplication
@EnableScheduling // 启用定时任务，用于RSS源的定期抓取
@EnableTransactionManagement // 启用事务管理
public class AIReaderBackendApplication {
    private static final Logger log = LoggerFactory.getLogger(AIReaderBackendApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AIReaderBackendApplication.class, args);
    }
    
    /**
     * 应用启动后验证Neo4j日期时间转换器是否正确加载
     */
    @Bean
    public ApplicationRunner checkNeo4jDateTimeConfig(Neo4jDateTimeConfig dateTimeConfig) {
        return args -> {
            log.info("==============================================");
            log.info("检查Neo4j日期时间转换器配置");
            Neo4jConversions conversions = dateTimeConfig.neo4jConversions();
            log.info("Neo4j自定义转换器已加载：{}", conversions != null);
            log.info("==============================================");
        };
    }
} 
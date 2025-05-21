package com.aireader.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * AI阅读器系统后端应用程序入口
 */
@SpringBootApplication
@EnableScheduling // 启用定时任务，用于RSS源的定期抓取
@EnableTransactionManagement // 启用事务管理
public class AIReaderBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIReaderBackendApplication.class, args);
    }
} 
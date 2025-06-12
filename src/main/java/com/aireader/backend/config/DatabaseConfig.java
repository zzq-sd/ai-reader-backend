package com.aireader.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Arrays;

/**
 * 数据库配置类
 * 用于配置MySQL、MongoDB和Neo4j的数据库连接及相关设置
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.aireader.backend.repository")
@EnableMongoRepositories(basePackages = "com.aireader.backend.repository")
@EnableNeo4jRepositories(basePackages = "com.aireader.backend.repository.neo4j")
@EnableJpaAuditing
@EnableMongoAuditing
@EnableNeo4jAuditing
@EnableTransactionManagement
public class DatabaseConfig {
    
    @Autowired
    private Environment environment;
    
    /**
     * 配置数据源初始化器，用于加载初始数据
     * 
     * @param dataSource 数据源
     * @return 数据源初始化器
     */
    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("data.sql"));
        
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        
        // 仅在开发环境下执行初始化SQL脚本
        boolean initialize = Arrays.asList(environment.getActiveProfiles()).contains("dev") || 
                             environment.getProperty("spring.jpa.hibernate.ddl-auto").equals("create") ||
                             environment.getProperty("spring.jpa.hibernate.ddl-auto").equals("create-drop");
        initializer.setEnabled(initialize);
        
        return initializer;
    }
    
    /**
     * 配置主要的JPA事务管理器
     * 用于MySQL数据库的事务管理，设置为主要事务管理器
     * 
     * @param entityManagerFactory JPA实体管理器工厂
     * @return JPA事务管理器
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
} 
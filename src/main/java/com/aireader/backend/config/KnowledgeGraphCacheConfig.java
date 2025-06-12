package com.aireader.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 知识图谱缓存配置
 */
@Configuration
@EnableCaching
public class KnowledgeGraphCacheConfig {
    
    @Bean("knowledgeGraphCacheManager")
    @Primary
    public CacheManager knowledgeGraphCacheManager(RedisConnectionFactory connectionFactory) {
        
        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))  // 默认1小时过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // 不同缓存区域的配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 图谱数据缓存 - 30分钟过期
        cacheConfigurations.put("graph-data", defaultConfig
                .entryTtl(Duration.ofMinutes(30)));
        
        // 概念统计缓存 - 2小时过期
        cacheConfigurations.put("concept-stats", defaultConfig
                .entryTtl(Duration.ofHours(2)));
        
        // 概念详情缓存 - 1小时过期
        cacheConfigurations.put("concept-details", defaultConfig
                .entryTtl(Duration.ofHours(1)));
        
        // 相关文章缓存 - 15分钟过期
        cacheConfigurations.put("related-articles", defaultConfig
                .entryTtl(Duration.ofMinutes(15)));
        
        // 概念搜索缓存 - 10分钟过期
        cacheConfigurations.put("concept-search", defaultConfig
                .entryTtl(Duration.ofMinutes(10)));
        
        // 知识图谱统计缓存 - 5分钟过期
        cacheConfigurations.put("graph-statistics", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));
        
        // 相关项目缓存 - 20分钟过期
        cacheConfigurations.put("related-items", defaultConfig
                .entryTtl(Duration.ofMinutes(20)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
    
    /**
     * 缓存键生成器
     */
    @Bean("knowledgeGraphKeyGenerator")
    public org.springframework.cache.interceptor.KeyGenerator knowledgeGraphKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName()).append(":");
            key.append(method.getName()).append(":");
            
            for (Object param : params) {
                if (param != null) {
                    key.append(param.toString()).append(":");
                }
            }
            
            // 移除最后一个冒号
            if (key.length() > 0 && key.charAt(key.length() - 1) == ':') {
                key.setLength(key.length() - 1);
            }
            
            return key.toString();
        };
    }
} 
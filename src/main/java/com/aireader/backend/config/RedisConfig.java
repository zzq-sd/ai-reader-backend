package com.aireader.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存配置类
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 默认缓存过期时间（30分钟）
     */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    /**
     * 自定义不同缓存空间的过期时间
     *
     * @return 包含不同缓存空间TTL配置的Map
     */
    private Map<String, RedisCacheConfiguration> getRedisCacheConfigMap() {
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        
        // 用户信息缓存 - 1小时
        configMap.put("userCache", createCacheConfig(Duration.ofHours(1)));
        
        // RSS源信息缓存 - 6小时
        configMap.put("rssFeedCache", createCacheConfig(Duration.ofHours(6)));
        
        // 文章元数据缓存 - 2小时
        configMap.put("articleMetadataCache", createCacheConfig(Duration.ofHours(2)));
        
        // 笔记内容缓存 - 30分钟
        configMap.put("noteCache", createCacheConfig(Duration.ofMinutes(30)));
        
        // 知识图谱查询结果缓存 - 1小时
        configMap.put("knowledgeGraphCache", createCacheConfig(Duration.ofHours(1)));
        
        // === 阶段六：知识图谱专用缓存配置 ===
        
        // 图谱数据缓存 - 30分钟
        configMap.put("graph-data", createCacheConfig(Duration.ofMinutes(30)));
        
        // 概念详情缓存 - 1小时
        configMap.put("concept-details", createCacheConfig(Duration.ofHours(1)));
        
        // 相关文章缓存 - 15分钟
        configMap.put("related-articles", createCacheConfig(Duration.ofMinutes(15)));
        
        // 概念统计缓存 - 2小时
        configMap.put("concept-stats", createCacheConfig(Duration.ofHours(2)));
        
        // 搜索结果缓存 - 10分钟
        configMap.put("concept-search", createCacheConfig(Duration.ofMinutes(10)));
        
        // AI分析结果缓存 - 6小时
        configMap.put("ai-analysis", createCacheConfig(Duration.ofHours(6)));
        
        // 用户兴趣图谱缓存 - 4小时
        configMap.put("user-interest-graph", createCacheConfig(Duration.ofHours(4)));
        
        // 热门概念缓存 - 1小时
        configMap.put("trending-concepts", createCacheConfig(Duration.ofHours(1)));
        
        return configMap;
    }

    /**
     * 创建指定TTL的缓存配置
     *
     * @param ttl 过期时间
     * @return Redis缓存配置
     */
    private RedisCacheConfiguration createCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();
    }

    /**
     * 配置缓存管理器
     *
     * @param connectionFactory Redis连接工厂
     * @return 缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认配置
        RedisCacheConfiguration defaultConfig = createCacheConfig(DEFAULT_TTL);
        
        // 构建缓存管理器
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(getRedisCacheConfigMap())
                .transactionAware()
                .build();
    }

    /**
     * 配置RedisTemplate
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置key的序列化方式
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 设置value的序列化方式
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
} 
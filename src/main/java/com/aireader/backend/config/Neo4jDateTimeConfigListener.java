package com.aireader.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Neo4j日期时间配置监听器
 * 在应用启动后验证Neo4j日期时间转换器是否正确加载
 */
@Component
public class Neo4jDateTimeConfigListener implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger log = LoggerFactory.getLogger(Neo4jDateTimeConfigListener.class);

    @Autowired
    private Neo4jDateTimeConfig neo4jDateTimeConfig;
    
    @Autowired(required = false)
    private ConversionService conversionService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("=========== Neo4j日期时间转换器验证 ===========");
        
        try {
            // 检查Neo4j自定义转换器是否加载
            Neo4jConversions conversions = neo4jDateTimeConfig.neo4jConversions();
            log.info("自定义Neo4j转换器已加载");
            
            // 检查转换服务是否可用
            if (conversionService != null) {
                // 检查特定转换器是否注册
                List<Class<?>[]> conversionsToCheck = Arrays.asList(
                    new Class<?>[] { ZonedDateTime.class, LocalDateTime.class },
                    new Class<?>[] { String.class, LocalDateTime.class },
                    new Class<?>[] { LocalDateTime.class, ZonedDateTime.class }
                );
                
                for (Class<?>[] conversionPair : conversionsToCheck) {
                    Class<?> sourceType = conversionPair[0];
                    Class<?> targetType = conversionPair[1];
                    boolean canConvert = conversionService.canConvert(sourceType, targetType);
                    log.info("转换器注册状态: {} -> {} = {}", 
                            sourceType.getSimpleName(),
                            targetType.getSimpleName(),
                            canConvert ? "已注册" : "未注册");
                }
            } else {
                log.warn("ConversionService不可用，无法验证转换器注册状态");
            }
            
        } catch (Exception e) {
            log.error("验证Neo4j日期时间转换器时出错", e);
        }
        
        log.info("===========================================");
    }
} 
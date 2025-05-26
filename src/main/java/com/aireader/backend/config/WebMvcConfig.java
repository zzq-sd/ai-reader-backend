package com.aireader.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    /**
     * 配置API版本控制和统一路径前缀
     * 所有控制器将以/api/v1/开头
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // configurer.addPathPrefix("/api/v1", c -> true); // 注释掉此行，因为 context-path 已经配置
    }
} 
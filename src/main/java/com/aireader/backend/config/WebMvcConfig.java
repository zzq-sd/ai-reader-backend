package com.aireader.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
    
    /**
     * 配置静态资源处理
     * 明确指定静态资源的位置，避免错误的资源路径解析
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 明确定义静态资源路径映射，禁止所有未明确指定的路径
        
        // 静态资源只从/static目录加载
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
                
        // Swagger UI资源
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
                
        // OpenAPI规范文档
        registry.addResourceHandler("/api-docs/**")
                .addResourceLocations("classpath:/META-INF/resources/");
                
        // 明确禁止API相关路径被视为静态资源
        String[] apiPaths = {"/ai/**", "/chat/**", "/api/**", "/articles/**", "/notes/**", "/rss/**", "/knowledge/**"};
        for (String path : apiPaths) {
            registry.addResourceHandler(path)
                    .addResourceLocations("classpath:/non-existent-path/")
                    .resourceChain(false);
        }
    }
} 
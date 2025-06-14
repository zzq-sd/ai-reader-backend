package com.aireader.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web MVC 异步处理配置
 * <p>
 * 解决 "SimpleAsyncTaskExecutor is not suitable for production use" 警告。
 * 通过配置一个专用的、生产级别的线程池来处理异步请求（如SSE），
 * 避免因默认配置在并发下无限创建线程导致的资源耗尽风险。
 *
 * @author AI Architect Reviewer
 * @version 1.1
 */
@Configuration
@Slf4j
public class WebMvcAsyncConfig implements WebMvcConfigurer {

    /**
     * 定义处理异步Web请求的线程池。
     *
     * @return 配置好的任务执行器
     */
    @Bean(name = "asyncTaskExecutor")
    public AsyncTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 根据服务器CPU核心数动态设置核心线程数
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(corePoolSize);
        // 最大线程数设置为核心线程数的两倍
        executor.setMaxPoolSize(corePoolSize * 2);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名前缀，方便日志追踪
        executor.setThreadNamePrefix("MvcAsync-");
        // 当池满时，由调用者线程执行任务
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("✅ 生产级Web异步任务执行器 (ThreadPoolTaskExecutor) 已配置完成. 核心线程数: {}, 最大线程数: {}", corePoolSize, corePoolSize * 2);
        return executor;
    }

    /**
     * 将我们自定义的线程池配置给Spring MVC用于异步请求处理。
     *
     * @param configurer 异步支持配置器
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(asyncTaskExecutor());
        // 可以设置默认的超时时间（例如30秒）
        configurer.setDefaultTimeout(30_000L);
        log.info("Configurer为Spring MVC异步请求配置了自定义的TaskExecutor。");
    }
} 
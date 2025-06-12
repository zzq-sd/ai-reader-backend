package com.aireader.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * API重定向过滤器
 * 拦截不带/api/v1前缀的直接API请求，并重定向到正确的API路径
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ApiRedirectFilter implements Filter {

    // 需要重定向的路径映射
    private final Map<String, String> redirectPaths = new HashMap<>();
    
    /**
     * 初始化过滤器
     * 配置需要重定向的路径
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 添加常见的错误路径映射
        redirectPaths.put("/ai/chat/health", "/direct/ai/chat/health");
        redirectPaths.put("/chat/stream", "/direct/chat/stream");
        redirectPaths.put("/ai/chat/stream", "/direct/ai/chat/stream");
        redirectPaths.put("/chat/message", "/api/v1/ai/chat");
        redirectPaths.put("/ai/chat/message", "/api/v1/ai/chat");
        redirectPaths.put("/ai/chat", "/api/v1/ai/chat");
        redirectPaths.put("/chat", "/api/v1/ai/chat");
        
        log.info("API重定向过滤器初始化完成，配置了{}个重定向路径", redirectPaths.size());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        
        // 检查是否需要重定向
        String redirectTo = redirectPaths.get(requestURI);
        if (redirectTo != null) {
            log.info("拦截到错误路径请求: {}, 重定向到: {}", requestURI, redirectTo);
            httpResponse.sendRedirect(contextPath + redirectTo);
            return;
        }
        
        chain.doFilter(request, response);
    }
} 
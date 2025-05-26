package com.aireader.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.StringJoiner;

/**
 * RSSHub工具类
 * 用于处理RSSHub相关的URL构建和验证
 */
public class RsshubUtil {
    
    private static final Logger log = LoggerFactory.getLogger(RsshubUtil.class);
    
    /**
     * 构建RSSHub URL（支持高级参数）
     * 
     * @param baseUrl RSSHub实例基础URL
     * @param route RSSHub路由
     * @param routeParams 路由参数
     * @param queryParams 查询参数
     * @return 构建好的完整URL
     */
    public static String buildEnhancedRsshubUrl(String baseUrl, String route, Map<String, String> routeParams, Map<String, String> queryParams) {
        // 确保路由以/开头
        if (!route.startsWith("/")) {
            route = "/" + route;
        }
        
        // 替换路由参数（如 :uid, :id 等）
        if (routeParams != null && !routeParams.isEmpty()) {
            for (Map.Entry<String, String> entry : routeParams.entrySet()) {
                route = route.replace(":" + entry.getKey(), entry.getValue());
            }
        }
        
        // 构建基础URL
        String url = baseUrl + route;
        
        // 添加查询参数（如 limit, mode=fulltext 等）
        if (queryParams != null && !queryParams.isEmpty()) {
            StringJoiner queryString = new StringJoiner("&");
            queryParams.forEach((key, value) -> {
                try {
                    queryString.add(key + "=" + java.net.URLEncoder.encode(value, "UTF-8"));
                } catch (Exception e) {
                    queryString.add(key + "=" + value);
                }
            });
            url += (url.contains("?") ? "&" : "?") + queryString.toString();
        }
        
        log.info("构建RSSHub URL: {}", url);
        return url;
    }
    
    /**
     * 构建基础RSSHub URL
     * 
     * @param baseUrl RSSHub实例基础URL
     * @param route RSSHub路由
     * @param params 查询参数
     * @return 构建好的完整URL
     */
    public static String buildRsshubUrl(String baseUrl, String route, Map<String, String> params) {
        return buildEnhancedRsshubUrl(baseUrl, route, null, params);
    }
    
    /**
     * 检查RSSHub实例健康状态
     * 
     * @param instanceUrl RSSHub实例URL
     * @param timeout 超时时间（毫秒）
     * @return 是否健康
     */
    public static boolean checkInstanceHealth(String instanceUrl, int timeout) {
        try {
            URL url = new URL(instanceUrl + "/healthz");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            return responseCode == HttpStatus.OK.value();
        } catch (Exception e) {
            log.warn("RSSHub实例健康检查失败: {}, 错误: {}", instanceUrl, e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证RSSHub路由基本格式
     * 
     * @param route RSSHub路由
     * @return 是否格式有效
     */
    public static boolean validateRouteFormat(String route) {
        // 基本格式验证
        if (route == null || route.trim().isEmpty()) {
            return false;
        }
        
        // 确保路由格式正确（以/开头）
        String normalizedRoute = route.trim();
        if (!normalizedRoute.startsWith("/")) {
            normalizedRoute = "/" + normalizedRoute;
        }
        
        // 简单格式检查通过
        return true;
    }
} 
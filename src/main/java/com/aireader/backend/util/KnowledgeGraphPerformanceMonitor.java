package com.aireader.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 知识图谱性能监控
 */
@Aspect
@Component
@Slf4j
public class KnowledgeGraphPerformanceMonitor {
    
    // 方法调用次数统计
    private final ConcurrentHashMap<String, AtomicLong> methodCallCounts = new ConcurrentHashMap<>();
    
    // 方法执行时间统计
    private final ConcurrentHashMap<String, AtomicLong> methodExecutionTimes = new ConcurrentHashMap<>();
    
    // 错误次数统计
    private final ConcurrentHashMap<String, AtomicLong> methodErrorCounts = new ConcurrentHashMap<>();
    
    /**
     * 监控KnowledgeService中的方法执行
     */
    @Around("execution(* com.aireader.backend.service.impl.KnowledgeServiceImpl.*(..))")
    public Object monitorKnowledgeService(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        
        // 增加调用次数
        methodCallCounts.computeIfAbsent(methodName, k -> new AtomicLong(0)).incrementAndGet();
        
        try {
            Object result = joinPoint.proceed();
            
            // 记录执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            methodExecutionTimes.computeIfAbsent(methodName, k -> new AtomicLong(0)).addAndGet(executionTime);
            
            // 记录性能日志
            if (executionTime > 1000) { // 超过1秒的慢查询
                log.warn("知识图谱慢查询: 方法={}, 执行时间={}ms, 参数={}", 
                        methodName, executionTime, joinPoint.getArgs());
            } else if (executionTime > 500) { // 超过500ms的查询
                log.info("知识图谱查询: 方法={}, 执行时间={}ms", methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            // 增加错误次数
            methodErrorCounts.computeIfAbsent(methodName, k -> new AtomicLong(0)).incrementAndGet();
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("知识图谱方法执行失败: 方法={}, 执行时间={}ms, 错误={}", 
                    methodName, executionTime, e.getMessage(), e);
            
            throw e;
        }
    }
    
    /**
     * 监控KnowledgeController中的方法执行
     */
    @Around("execution(* com.aireader.backend.controller.KnowledgeController.*(..))")
    public Object monitorKnowledgeController(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = "Controller." + joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录API调用日志
            if (executionTime > 2000) { // 超过2秒的慢API
                log.warn("知识图谱慢API: 接口={}, 执行时间={}ms", methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("知识图谱API执行失败: 接口={}, 执行时间={}ms, 错误={}", 
                    methodName, executionTime, e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * 获取性能统计信息
     */
    public PerformanceStats getPerformanceStats() {
        PerformanceStats stats = new PerformanceStats();
        
        methodCallCounts.forEach((method, count) -> {
            AtomicLong totalTime = methodExecutionTimes.get(method);
            AtomicLong errorCount = methodErrorCounts.get(method);
            
            MethodStats methodStats = new MethodStats();
            methodStats.setMethodName(method);
            methodStats.setCallCount(count.get());
            methodStats.setTotalExecutionTime(totalTime != null ? totalTime.get() : 0);
            methodStats.setAverageExecutionTime(
                count.get() > 0 && totalTime != null ? 
                totalTime.get() / count.get() : 0
            );
            methodStats.setErrorCount(errorCount != null ? errorCount.get() : 0);
            methodStats.setErrorRate(
                count.get() > 0 && errorCount != null ? 
                (double) errorCount.get() / count.get() * 100 : 0.0
            );
            
            stats.getMethodStats().put(method, methodStats);
        });
        
        return stats;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        methodCallCounts.clear();
        methodExecutionTimes.clear();
        methodErrorCounts.clear();
        log.info("知识图谱性能统计信息已重置");
    }
    
    /**
     * 性能统计数据结构
     */
    public static class PerformanceStats {
        private final ConcurrentHashMap<String, MethodStats> methodStats = new ConcurrentHashMap<>();
        
        public ConcurrentHashMap<String, MethodStats> getMethodStats() {
            return methodStats;
        }
    }
    
    /**
     * 方法统计数据结构
     */
    public static class MethodStats {
        private String methodName;
        private long callCount;
        private long totalExecutionTime;
        private long averageExecutionTime;
        private long errorCount;
        private double errorRate;
        
        // Getters and Setters
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        
        public long getCallCount() { return callCount; }
        public void setCallCount(long callCount) { this.callCount = callCount; }
        
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public void setTotalExecutionTime(long totalExecutionTime) { this.totalExecutionTime = totalExecutionTime; }
        
        public long getAverageExecutionTime() { return averageExecutionTime; }
        public void setAverageExecutionTime(long averageExecutionTime) { this.averageExecutionTime = averageExecutionTime; }
        
        public long getErrorCount() { return errorCount; }
        public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
        
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
    }
} 
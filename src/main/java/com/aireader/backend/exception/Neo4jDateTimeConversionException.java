package com.aireader.backend.exception;

/**
 * Neo4j日期时间转换异常
 * 用于处理Neo4j日期时间转换问题
 */
public class Neo4jDateTimeConversionException extends RuntimeException {

    /**
     * 创建一个Neo4j日期时间转换异常
     * 
     * @param message 异常消息
     */
    public Neo4jDateTimeConversionException(String message) {
        super(message);
    }

    /**
     * 创建一个Neo4j日期时间转换异常
     * 
     * @param message 异常消息
     * @param cause 原始异常
     */
    public Neo4jDateTimeConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建一个Neo4j日期时间转换异常，附带转换前的原始值信息
     * 
     * @param originalValue 原始值
     * @param targetType 目标类型
     * @param cause 原始异常
     */
    public Neo4jDateTimeConversionException(Object originalValue, Class<?> targetType, Throwable cause) {
        super(String.format("无法将值 [%s] (类型: %s) 转换为 %s: %s", 
                originalValue, 
                originalValue != null ? originalValue.getClass().getName() : "null", 
                targetType.getName(),
                cause.getMessage()), 
            cause);
    }
} 
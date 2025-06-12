package com.aireader.backend.exception;

import lombok.Getter;

/**
 * 业务异常类，用于表示业务逻辑中的错误情况
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误代码
     */
    private final String errorCode;
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param errorCode 错误代码
     */
    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param errorCode 错误代码
     * @param cause 原始异常
     */
    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 创建数据库异常
     * 
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException dbError(String message) {
        return new BusinessException(message, "DB_ERROR");
    }
    
    /**
     * 创建资源未找到异常
     * 
     * @param resource 资源类型
     * @param id 资源ID
     * @return 业务异常
     */
    public static BusinessException notFound(String resource, String id) {
        return new BusinessException(
            String.format("%s不存在，ID: %s", resource, id),
            "NOT_FOUND"
        );
    }
    
    /**
     * 创建数据验证异常
     * 
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException validationError(String message) {
        return new BusinessException(message, "VALIDATION_ERROR");
    }
    
    /**
     * 创建权限不足异常
     * 
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(message, "FORBIDDEN");
    }
} 
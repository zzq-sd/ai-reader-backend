package com.aireader.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * 自定义API异常
 */
public class APIException extends RuntimeException {
    
    private final HttpStatus status;
    
    /**
     * 创建一个API异常
     * 
     * @param message 异常消息
     * @param status HTTP状态码
     */
    public APIException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    /**
     * 创建一个API异常
     * 
     * @param message 异常消息
     * @param status HTTP状态码
     * @param cause 原始异常
     */
    public APIException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
    
    /**
     * 创建一个BAD_REQUEST状态的API异常
     * 
     * @param message 异常消息
     * @return API异常
     */
    public static APIException badRequest(String message) {
        return new APIException(message, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 创建一个NOT_FOUND状态的API异常
     * 
     * @param message 异常消息
     * @return API异常
     */
    public static APIException notFound(String message) {
        return new APIException(message, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 创建一个CONFLICT状态的API异常
     * 
     * @param message 异常消息
     * @return API异常
     */
    public static APIException conflict(String message) {
        return new APIException(message, HttpStatus.CONFLICT);
    }
    
    /**
     * 创建一个INTERNAL_SERVER_ERROR状态的API异常
     * 
     * @param message 异常消息
     * @return API异常
     */
    public static APIException internalError(String message) {
        return new APIException(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 创建一个INTERNAL_SERVER_ERROR状态的API异常
     * 
     * @param message 异常消息
     * @param cause 原始异常
     * @return API异常
     */
    public static APIException internalError(String message, Throwable cause) {
        return new APIException(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
    
    /**
     * 获取HTTP状态码
     * 
     * @return HTTP状态码
     */
    public HttpStatus getStatus() {
        return status;
    }
} 
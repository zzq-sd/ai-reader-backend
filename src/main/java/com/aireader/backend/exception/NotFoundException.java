package com.aireader.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 资源未找到异常
 * 当请求的资源（如文章、用户、标签等）不存在时抛出此异常
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    /**
     * 创建资源未找到异常
     * 
     * @param message 异常消息
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * 创建资源未找到异常
     * 
     * @param message 异常消息
     * @param cause 原始异常
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 
package com.aireader.backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API统一响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 设置响应数据
     * 
     * @param data 响应数据
     * @return 当前对象
     */
    public ApiResponse<T> setData(T data) {
        this.data = data;
        return this;
    }
    
    /**
     * 创建成功响应
     * 
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return API响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }
    
    /**
     * 创建成功响应
     * 
     * @param message 成功消息
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return API响应对象
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
    
    /**
     * 创建错误响应
     * 
     * @param message 错误消息
     * @param <T> 响应数据类型
     * @return API响应对象
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
} 
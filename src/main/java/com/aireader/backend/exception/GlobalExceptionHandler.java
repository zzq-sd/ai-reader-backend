package com.aireader.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理各类异常，返回一致的错误响应格式
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * 
     * @param ex 业务异常
     * @return 响应实体
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.error("业务异常: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getMessage(), ex.getErrorCode(), HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理数据库异常
     * 
     * @param ex 数据库异常
     * @return 响应实体
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseException(DataAccessException ex) {
        log.error("数据库访问异常: {}", ex.getMessage(), ex);
        return buildErrorResponse("数据库操作失败，请稍后重试", "DB_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 处理认证异常
     * 
     * @param ex 认证异常
     * @return 响应实体
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(BadCredentialsException ex) {
        log.error("认证异常: {}", ex.getMessage());
        return buildErrorResponse("用户名或密码错误", "AUTH_ERROR", HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * 处理授权异常
     * 
     * @param ex 授权异常
     * @return 响应实体
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("授权异常: {}", ex.getMessage());
        return buildErrorResponse("无权访问此资源", "ACCESS_DENIED", HttpStatus.FORBIDDEN);
    }
    
    /**
     * 处理参数验证异常
     * 
     * @param ex 参数验证异常
     * @return 响应实体
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("参数验证异常: {}", ex.getMessage());
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((error1, error2) -> error1 + "; " + error2)
                .orElse("参数验证失败");
        
        return buildErrorResponse(errorMessage, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理IO异常
     * 
     * @param ex IO异常
     * @return 响应实体
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex) {
        log.error("IO异常: {}", ex.getMessage(), ex);
        return buildErrorResponse("读写操作失败", "IO_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 处理资源访问异常（网络相关）
     * 
     * @param ex 资源访问异常
     * @return 响应实体
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAccessException(ResourceAccessException ex) {
        log.error("资源访问异常: {}", ex.getMessage(), ex);
        return buildErrorResponse("网络或外部资源访问失败", "RESOURCE_ACCESS_ERROR", HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * 处理静态资源未找到异常
     * 
     * @param ex 静态资源未找到异常
     * @return 响应实体
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(NoResourceFoundException ex) {
        // 日志级别调整为WARN，因为这可能是前端误请求导致的常见情况
        String path = ex.getMessage().replace("No static resource ", "");
        log.warn("静态资源未找到: {}", path);
        
        // 构建响应
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "请求的资源不存在");
        errorResponse.put("path", path);
        errorResponse.put("errorCode", "RESOURCE_NOT_FOUND");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("suggestion", "请检查URL是否正确，API路径应以/api/v1开头");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 处理所有未捕获的异常
     * 
     * @param ex 异常
     * @return 响应实体
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("未处理的异常: {}", ex.getMessage(), ex);
        return buildErrorResponse("服务器内部错误", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 构建统一的错误响应
     * 
     * @param message 错误消息
     * @param errorCode 错误代码
     * @param status HTTP状态码
     * @return 响应实体
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, String errorCode, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(errorResponse, status);
    }
}
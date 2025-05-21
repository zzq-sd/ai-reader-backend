package com.aireader.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理所有API异常
     */
    @ExceptionHandler(APIException.class)
    public ResponseEntity<ErrorResponse> handleAPIException(APIException ex, WebRequest request) {
        log.error("API异常: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                new Date(),
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false));
        
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception ex, WebRequest request) {
        log.error("认证异常: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                new Date(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false));
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 处理授权异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.error("授权异常: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                new Date(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false));
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("参数校验异常: {}", ex.getMessage(), ex);
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                new Date(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "请求参数验证失败",
                request.getDescription(false),
                errors);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("未处理的异常: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                new Date(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "服务器内部错误，请稍后再试",
                request.getDescription(false));
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 标准错误响应类
     */
    public static class ErrorResponse {
        private Date timestamp;
        private int status;
        private String error;
        private String message;
        private String path;

        public ErrorResponse(Date timestamp, int status, String error, String message, String path) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
        }

        // Getters
        public Date getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
    }

    /**
     * 参数校验错误响应类
     */
    public static class ValidationErrorResponse extends ErrorResponse {
        private Map<String, String> errors;

        public ValidationErrorResponse(Date timestamp, int status, String error, String message, String path, Map<String, String> errors) {
            super(timestamp, status, error, message, path);
            this.errors = errors;
        }

        public Map<String, String> getErrors() { return errors; }
    }
}
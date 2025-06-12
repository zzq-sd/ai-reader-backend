package com.aireader.backend.exception;

/**
 * 知识图谱相关异常
 */
public class KnowledgeGraphException extends RuntimeException {
    
    private final String errorCode;
    
    public KnowledgeGraphException(String message) {
        super(message);
        this.errorCode = "KNOWLEDGE_GRAPH_ERROR";
    }
    
    public KnowledgeGraphException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public KnowledgeGraphException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "KNOWLEDGE_GRAPH_ERROR";
    }
    
    public KnowledgeGraphException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    // 常见错误代码
    public static class ErrorCodes {
        public static final String CONCEPT_NOT_FOUND = "CONCEPT_NOT_FOUND";
        public static final String GRAPH_DATA_QUERY_FAILED = "GRAPH_DATA_QUERY_FAILED";
        public static final String ARTICLE_ANALYSIS_FAILED = "ARTICLE_ANALYSIS_FAILED";
        public static final String NOTE_ANALYSIS_FAILED = "NOTE_ANALYSIS_FAILED";
        public static final String RELATIONSHIP_CREATE_FAILED = "RELATIONSHIP_CREATE_FAILED";
        public static final String NEO4J_CONNECTION_ERROR = "NEO4J_CONNECTION_ERROR";
        public static final String AI_SERVICE_UNAVAILABLE = "AI_SERVICE_UNAVAILABLE";
    }
} 
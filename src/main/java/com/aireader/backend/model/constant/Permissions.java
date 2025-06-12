package com.aireader.backend.model.constant;

/**
 * 权限常量类
 * 定义系统中的权限常量，使用Spring Security的表达式格式
 */
public class Permissions {
    // 文章权限
    public static final String READ_ARTICLE = "hasAnyRole('USER', 'PREMIUM', 'ADMIN')";
    public static final String MANAGE_ARTICLE = "hasAnyRole('ADMIN')";
    
    // RSS源权限
    public static final String READ_RSS = "hasAnyRole('USER', 'PREMIUM', 'ADMIN')";
    public static final String CREATE_RSS = "hasAnyRole('USER', 'PREMIUM', 'ADMIN')";
    public static final String MANAGE_PUBLIC_RSS = "hasAnyRole('ADMIN')";
    
    // 笔记权限
    public static final String READ_OWN_NOTE = "hasAnyRole('USER', 'PREMIUM', 'ADMIN')";
    public static final String CREATE_NOTE = "hasAnyRole('USER', 'PREMIUM', 'ADMIN')";
    public static final String EDIT_OWN_NOTE = "hasAnyRole('USER', 'PREMIUM', 'ADMIN')";
    public static final String DELETE_OWN_NOTE = "hasAnyRole('USER', 'PREMIUM', 'ADMIN')";
    public static final String MANAGE_ALL_NOTES = "hasAnyRole('ADMIN')";
    
    // 收藏权限
    public static final String MANAGE_OWN_FAVORITE = "hasAnyRole('USER', 'PREMIUM', 'ADMIN')";
    
    // 用户权限
    public static final String READ_USER = "hasAnyRole('ADMIN')";
    public static final String MANAGE_USER = "hasAnyRole('ADMIN')";
    
    // AI功能权限
    public static final String USE_BASIC_AI = "hasAnyRole('USER', 'PREMIUM', 'ADMIN')";
    public static final String USE_ADVANCED_AI = "hasAnyRole('PREMIUM', 'ADMIN')";
    
    // 知识图谱权限
    public static final String VIEW_KNOWLEDGE_GRAPH = "hasAnyRole('PREMIUM', 'ADMIN')";
    public static final String MANAGE_KNOWLEDGE_GRAPH = "hasAnyRole('ADMIN')";
    
    // 系统管理权限
    public static final String MANAGE_SYSTEM = "hasRole('ADMIN')";
    
    private Permissions() {
        // 私有构造方法防止实例化
    }
} 
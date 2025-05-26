package com.aireader.backend.model.constant;

/**
 * 用户角色枚举
 */
public enum UserRole {
    /**
     * 管理员角色
     * 拥有系统的全部权限
     */
    ROLE_ADMIN,
    
    /**
     * 普通用户角色
     * 拥有基本的阅读、收藏、笔记等功能权限
     */
    ROLE_USER,
    
    /**
     * 高级用户角色
     * 在普通用户基础上增加了更多高级功能权限
     */
    ROLE_PREMIUM,
    
    /**
     * 游客角色
     * 只能访问公开内容，无法创建、修改数据
     */
    ROLE_GUEST
} 
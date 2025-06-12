package com.aireader.backend.model.constant;

/**
 * Neo4j关系类型常量定义
 * 确保整个系统中关系类型的一致性
 */
public final class Neo4jRelationshipTypes {
    
    // ====================== 笔记相关关系 ======================
    
    /**
     * 笔记包含概念的关系类型
     */
    public static final String NOTE_CONTAINS_CONCEPT = "CONTAINS_CONCEPT";
    
    /**
     * 笔记提及概念
     */
    public static final String NOTE_MENTIONS = "MENTIONS";
    
    /**
     * 笔记引用文章
     */
    public static final String NOTE_REFERS_TO = "REFERS_TO";
    
    /**
     * 笔记被用户创建
     */
    public static final String NOTE_CREATED_BY = "CREATED_BY";
    
    /**
     * 笔记被标记为某标签
     */
    public static final String NOTE_TAGGED = "TAGGED";
    
    // ====================== 概念关系类型 ======================
    
    /**
     * 提取的概念
     */
    public static final String EXTRACTED_CONCEPT = "EXTRACTED_CONCEPT";
    
    /**
     * 智能标签
     */
    public static final String INTELLIGENT_TAG = "INTELLIGENT_TAG";
    
    /**
     * 关键词
     */
    public static final String KEYWORD = "KEYWORD";
    
    /**
     * 实体
     */
    public static final String ENTITY = "ENTITY";
    
    /**
     * 主题
     */
    public static final String TOPIC = "TOPIC";
    
    /**
     * 关键观点
     */
    public static final String KEY_POINT = "KEY_POINT";
    
    // ====================== 文章相关关系 ======================
    
    /**
     * 文章包含概念
     */
    public static final String ARTICLE_CONTAINS_CONCEPT = "CONTAINS_CONCEPT";
    
    /**
     * 文章关于某概念
     */
    public static final String ARTICLE_ABOUT = "ABOUT";
    
    /**
     * 文章被用户收藏
     */
    public static final String ARTICLE_FAVORITED = "FAVORITED";
    
    // ====================== 概念间关系 ======================
    
    /**
     * 概念相关联
     */
    public static final String CONCEPT_RELATED_TO = "RELATED_TO";
    
    /**
     * 概念是某类型的子类
     */
    public static final String CONCEPT_IS_A = "IS_A";
    
    /**
     * 概念是某个的部分
     */
    public static final String CONCEPT_PART_OF = "PART_OF";
    
    // ====================== 用户相关关系 ======================
    
    /**
     * 用户对概念感兴趣
     */
    public static final String USER_INTERESTED_IN = "INTERESTED_IN";
    
    /**
     * 用户关注其他用户
     */
    public static final String USER_FOLLOWS = "FOLLOWS";
    
    // 私有构造函数，防止实例化
    private Neo4jRelationshipTypes() {
        throw new UnsupportedOperationException("常量类不能被实例化");
    }
} 
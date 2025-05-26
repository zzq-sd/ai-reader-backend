package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关联项数据传输对象
 * 用于封装知识关联推荐的条目信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatedItemDto {

    /**
     * 条目ID
     */
    private String itemId;

    /**
     * 条目标题
     */
    private String title;

    /**
     * 条目摘要/描述
     */
    private String summary;

    /**
     * 条目类型（ARTICLE-文章、NOTE-笔记、CONCEPT-概念）
     */
    private ItemType type;

    /**
     * 来源ID（如RSS源ID、用户ID等）
     */
    private String sourceId;

    /**
     * 关联分数（用于排序）
     */
    private Double relevanceScore;

    /**
     * 创建/发布时间的时间戳
     */
    private Long timestamp;

    /**
     * 标签列表（以逗号分隔）
     */
    private String tags;

    /**
     * 关联原因说明
     */
    private String reason;

    /**
     * 条目类型枚举
     */
    public enum ItemType {
        /**
         * 文章
         */
        ARTICLE,
        
        /**
         * 笔记
         */
        NOTE,
        
        /**
         * 概念
         */
        CONCEPT
    }
} 
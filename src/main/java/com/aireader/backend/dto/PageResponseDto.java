package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应DTO
 * 用于返回分页数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto<T> {
    
    /**
     * 当前页内容
     */
    private List<T> content;
    
    /**
     * 总元素数量
     */
    private long totalElements;
    
    /**
     * 总页数
     */
    private int totalPages;
    
    /**
     * 每页大小
     */
    private int size;
    
    /**
     * 当前页码
     */
    private int number;
    
    /**
     * 是否为第一页
     */
    private boolean first;
    
    /**
     * 是否为最后一页
     */
    private boolean last;
    
    /**
     * 是否为空页
     */
    private boolean empty;
    
    /**
     * 当前页元素数量
     */
    private int numberOfElements;
} 
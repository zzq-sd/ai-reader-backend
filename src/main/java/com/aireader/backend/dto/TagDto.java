package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 标签DTO
 * 用于标签的创建、更新和查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDto {
    
    /**
     * 标签ID
     */
    private String id;
    
    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称不能超过50个字符")
    private String name;
    
    /**
     * 标签颜色
     */
    private String color;
    
    /**
     * 标签描述
     */
    @Size(max = 255, message = "标签描述不能超过255个字符")
    private String description;
    
    /**
     * 标签使用计数
     */
    private Integer count;
    
    /**
     * 标签创建者ID
     */
    private String userId;
} 
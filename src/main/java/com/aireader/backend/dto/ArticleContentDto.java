package com.aireader.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 文章内容数据传输对象
 * 用于封装从MongoDB获取的文章完整内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleContentDto {

    /**
     * MongoDB中的文档ID
     */
    private String mongoId;

    /**
     * MySQL中的文章元数据ID
     */
    private Long mysqlMetadataId;

    /**
     * 文章标题
     */
    @NotBlank(message = "文章标题不能为空")
    private String title;

    /**
     * 文章全文HTML内容
     */
    private String fullHtmlContent;

    /**
     * 文章纯文本内容
     */
    private String plainTextContent;

    /**
     * 原始URL
     */
    private String originalUrl;

    /**
     * 发布时间
     */
    private LocalDateTime publishDate;

    /**
     * 作者
     */
    private String author;

    /**
     * 文章摘要
     */
    private String summary;
} 
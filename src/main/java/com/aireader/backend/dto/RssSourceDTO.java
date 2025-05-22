package com.aireader.backend.dto;

import com.aireader.backend.model.jpa.RssSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * RSS源数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssSourceDTO {

    private String id;

    @NotBlank(message = "RSS URL不能为空")
    @Pattern(regexp = "^https?://.*", message = "RSS URL必须以http://或https://开头")
    private String url;

    private String name;

    private String category;

    private String description;

    private String websiteUrl;

    private String iconUrl;

    private String fetchStatus;

    private String errorMessage;

    private LocalDateTime lastFetchedAt;

    private boolean active;

    private boolean isPublic;

    private String userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 将实体转换为DTO
     * 
     * @param rssSource RSS源实体
     * @return RSS源DTO
     */
    public static RssSourceDTO fromEntity(RssSource rssSource) {
        if (rssSource == null) {
            return null;
        }
        
        return RssSourceDTO.builder()
                .id(rssSource.getId().toString())
                .url(rssSource.getUrl())
                .name(rssSource.getName())
                .category(rssSource.getCategory())
                .description(rssSource.getDescription())
                .isPublic(rssSource.isPublic())
                .userId(rssSource.getUser() != null ? rssSource.getUser().getId().toString() : null)
                .fetchStatus(rssSource.getFetchStatus())
                .errorMessage(rssSource.getErrorMessage())
                .lastFetchedAt(rssSource.getLastFetchedAt())
                .createdAt(rssSource.getCreatedAt())
                .updatedAt(rssSource.getUpdatedAt())
                .build();
    }

    /**
     * 将DTO转换为实体
     * 
     * @return RSS源实体
     */
    public RssSource toEntity() {
        return RssSource.builder()
                .url(this.url)
                .name(this.name)
                .category(this.category)
                .description(this.description)
                .isPublic(this.isPublic)
                .build();
    }
}
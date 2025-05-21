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

    private RssSource.FetchStatus fetchStatus;

    private String fetchError;

    private LocalDateTime lastFetchedAt;

    private boolean active;

    private LocalDateTime createdAt;

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
                .id(rssSource.getId())
                .url(rssSource.getUrl())
                .name(rssSource.getName())
                .category(rssSource.getCategory())
                .description(rssSource.getDescription())
                .websiteUrl(rssSource.getWebsiteUrl())
                .iconUrl(rssSource.getIconUrl())
                .fetchStatus(rssSource.getFetchStatus())
                .fetchError(rssSource.getFetchError())
                .lastFetchedAt(rssSource.getLastFetchedAt())
                .active(rssSource.isActive())
                .createdAt(rssSource.getCreatedAt())
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
                .websiteUrl(this.websiteUrl)
                .iconUrl(this.iconUrl)
                .active(this.active)
                .build();
    }
}
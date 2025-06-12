package com.aireader.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RSS源状态DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssSourceStatusDTO {
    private String id;
    private String title;
    private String url;
    private boolean active;
    private int articleCount;
    private LocalDateTime lastUpdate;
    private String errorMessage;
} 
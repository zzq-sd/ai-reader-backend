package com.aireader.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统统计数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsDTO {
    private int userCount;
    private int rssCount;
    private int articleCount;
    private int conceptCount;
} 
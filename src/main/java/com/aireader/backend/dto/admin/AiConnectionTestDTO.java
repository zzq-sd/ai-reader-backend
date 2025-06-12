package com.aireader.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI连接测试结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConnectionTestDTO {
    private boolean success;
    private String message;
} 
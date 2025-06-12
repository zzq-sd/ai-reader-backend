package com.aireader.backend.controller;

import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.dto.admin.ReaderAssistantPromptsDTO;
import com.aireader.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * 公共配置控制器
 * 提供系统配置的公共访问接口，无需管理员权限
 */
@RestController
@RequestMapping("/api/config")
@Tag(name = "系统配置", description = "系统配置的公共API接口")
@Slf4j
public class ConfigController extends BaseController {
    
    @Autowired
    private AdminService adminService;
    
    /**
     * 获取AI阅读助手快捷提示词配置（公共访问）
     */
    @Operation(summary = "获取AI阅读助手快捷提示词配置", description = "获取当前的AI阅读助手快捷提示词配置，所有用户可访问")
    @GetMapping("/reader-assistant-prompts")
    public ResponseEntity<ApiResponse<ReaderAssistantPromptsDTO>> getReaderAssistantPrompts() {
        try {
            ReaderAssistantPromptsDTO config = adminService.getReaderAssistantPrompts();
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            log.error("获取AI阅读助手快捷提示词配置失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取AI阅读助手快捷提示词配置失败: " + e.getMessage()));
        }
    }
} 
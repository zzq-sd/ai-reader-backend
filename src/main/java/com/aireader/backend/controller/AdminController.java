package com.aireader.backend.controller;

import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.dto.admin.SystemStatsDTO;
import com.aireader.backend.dto.admin.RssSourceStatusDTO;
import com.aireader.backend.dto.admin.AiConfigDTO;
import com.aireader.backend.dto.admin.KnowledgeConfigDTO;
import com.aireader.backend.dto.admin.AiConnectionTestDTO;
import com.aireader.backend.dto.auth.UserResponseDto;
import com.aireader.backend.dto.admin.ReaderAssistantPromptsDTO;
import com.aireader.backend.service.AdminService;
import com.aireader.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 * 处理系统管理相关的请求
 * 所有端点都需要ADMIN角色权限
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "系统管理", description = "系统管理相关的API接口")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController extends BaseController {
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取系统统计数据
     */
    @Operation(summary = "获取系统统计数据", description = "获取系统各项统计数据，包括用户数、RSS源数、文章数和概念数")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SystemStatsDTO>> getSystemStats() {
        SystemStatsDTO stats = adminService.getSystemStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * 获取用户列表
     */
    @Operation(summary = "获取用户列表", description = "获取所有用户的列表，可以通过查询参数进行过滤")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserList(
            @RequestParam(required = false) String query) {
        List<UserResponseDto> users = userService.getAllUsers();
        
        // 如果有查询参数，进行过滤
        if (query != null && !query.trim().isEmpty()) {
            String lowercaseQuery = query.toLowerCase();
            users = users.stream()
                    .filter(user -> 
                        user.getUsername().toLowerCase().contains(lowercaseQuery) || 
                        user.getEmail().toLowerCase().contains(lowercaseQuery))
                    .toList();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        response.put("total", users.size());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 更新用户状态
     */
    @Operation(summary = "更新用户状态", description = "启用或禁用指定的用户")
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Boolean>> updateUserStatus(
            @PathVariable String userId,
            @RequestBody Map<String, Boolean> request) {
        Boolean enabled = request.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("缺少启用状态参数"));
        }
        
        boolean success = adminService.updateUserStatus(userId, enabled);
        return ResponseEntity.ok(ApiResponse.success(success));
    }
    
    /**
     * 获取所有RSS源状态
     */
    @Operation(summary = "获取RSS源状态", description = "获取所有RSS源的状态信息")
    @GetMapping("/rss/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRssSourcesStatus() {
        List<RssSourceStatusDTO> sources = adminService.getAllRssSourcesStatus();
        
        Map<String, Object> response = new HashMap<>();
        response.put("sources", sources);
        response.put("total", sources.size());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 获取AI配置
     */
    @Operation(summary = "获取AI配置", description = "获取当前的AI服务配置")
    @GetMapping("/config/ai")
    public ResponseEntity<ApiResponse<AiConfigDTO>> getAiConfig() {
        AiConfigDTO config = adminService.getAiConfig();
        return ResponseEntity.ok(ApiResponse.success(config));
    }
    
    /**
     * 更新AI配置
     */
    @Operation(summary = "更新AI配置", description = "更新AI服务的配置")
    @PutMapping("/config/ai")
    public ResponseEntity<ApiResponse<AiConfigDTO>> updateAiConfig(
            @RequestBody AiConfigDTO config) {
        AiConfigDTO updatedConfig = adminService.updateAiConfig(config);
        return ResponseEntity.ok(ApiResponse.success(updatedConfig));
    }
    
    /**
     * 测试AI配置连接
     */
    @Operation(summary = "测试AI连接", description = "测试AI服务连接是否正常")
    @PostMapping("/config/ai/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testAiConnection(
            @RequestBody AiConfigDTO config) {
        AiConnectionTestDTO result = adminService.testAiConnection(config);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 获取知识关联配置
     */
    @Operation(summary = "获取知识关联配置", description = "获取当前的知识关联算法配置")
    @GetMapping("/config/knowledge")
    public ResponseEntity<ApiResponse<KnowledgeConfigDTO>> getKnowledgeConfig() {
        KnowledgeConfigDTO config = adminService.getKnowledgeConfig();
        return ResponseEntity.ok(ApiResponse.success(config));
    }
    
    /**
     * 更新知识关联配置
     */
    @Operation(summary = "更新知识关联配置", description = "更新知识关联算法的配置")
    @PutMapping("/config/knowledge")
    public ResponseEntity<ApiResponse<KnowledgeConfigDTO>> updateKnowledgeConfig(
            @RequestBody KnowledgeConfigDTO config) {
        KnowledgeConfigDTO updatedConfig = adminService.updateKnowledgeConfig(config);
        return ResponseEntity.ok(ApiResponse.success(updatedConfig));
    }
    
    /**
     * 获取AI阅读助手快捷提示词配置
     */
    @GetMapping("/config/reader-assistant-prompts")
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
    
    /**
     * 更新AI阅读助手快捷提示词配置
     */
    @PutMapping("/config/reader-assistant-prompts")
    public ResponseEntity<ApiResponse<ReaderAssistantPromptsDTO>> updateReaderAssistantPrompts(
            @RequestBody ReaderAssistantPromptsDTO config) {
        try {
            ReaderAssistantPromptsDTO updatedConfig = adminService.updateReaderAssistantPrompts(config);
            return ResponseEntity.ok(ApiResponse.success(updatedConfig));
        } catch (Exception e) {
            log.error("更新AI阅读助手快捷提示词配置失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新AI阅读助手快捷提示词配置失败: " + e.getMessage()));
        }
    }
} 
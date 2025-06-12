package com.aireader.backend.controller;

import com.aireader.backend.dto.NoteAnalysisResultDto;
import com.aireader.backend.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 笔记AI分析控制器
 */
@RestController
@RequestMapping("/api/v1/notes")
@Slf4j
public class NoteAnalysisController {

    @Autowired
    private NoteService noteService;

    /**
     * 获取笔记的AI分析结果
     * 
     * @param noteId 笔记ID
     * @param authentication 认证信息
     * @return AI分析结果
     */
    @GetMapping("/{noteId}/analysis")
    public ResponseEntity<NoteAnalysisResultDto> getNoteAnalysisResult(
            @PathVariable String noteId,
            Authentication authentication) {
        
        log.info("获取笔记AI分析结果，笔记ID: {}, 用户: {}", noteId, authentication.getName());
        
        try {
            String userId = authentication.getName();
            NoteAnalysisResultDto result = noteService.getNoteAnalysisResult(noteId, userId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取笔记AI分析结果失败，笔记ID: " + noteId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 手动触发笔记重新分析
     * 
     * @param noteId 笔记ID
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PostMapping("/{noteId}/reanalyze")
    public ResponseEntity<String> reanalyzeNote(
            @PathVariable String noteId,
            Authentication authentication) {
        
        log.info("手动触发笔记重新分析，笔记ID: {}, 用户: {}", noteId, authentication.getName());
        
        try {
            String userId = authentication.getName();
            boolean success = noteService.reanalyzeNote(noteId, userId);
            
            if (success) {
                return ResponseEntity.ok("重新分析任务已提交，请稍后查看结果");
            } else {
                return ResponseEntity.internalServerError().body("提交重新分析任务失败");
            }
        } catch (Exception e) {
            log.error("手动触发笔记重新分析失败，笔记ID: " + noteId, e);
            return ResponseEntity.internalServerError().body("操作失败：" + e.getMessage());
        }
    }
} 
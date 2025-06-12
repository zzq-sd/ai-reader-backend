package com.aireader.backend.controller;

import com.aireader.backend.dto.GraphDataDTO;
import com.aireader.backend.service.KnowledgeService;
import com.aireader.backend.util.KnowledgeGraphPerformanceMonitor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * KnowledgeController 单元测试
 */
@WebMvcTest(KnowledgeController.class)
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeService knowledgeService;

    @MockBean
    private KnowledgeGraphPerformanceMonitor performanceMonitor;

    @MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "USER")
    void testGetGraphData() throws Exception {
        // 准备测试数据
        GraphDataDTO mockGraphData = GraphDataDTO.builder()
                .nodes(new ArrayList<>())
                .edges(new ArrayList<>())
                .build();

        when(knowledgeService.getGraphData(anyString(), anyString(), anyInt()))
                .thenReturn(mockGraphData);

        // 执行测试
        mockMvc.perform(get("/api/v1/knowledge/graph-data")
                        .param("nodeType", "ALL")
                        .param("search", "")
                        .param("limit", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testSearchConcepts() throws Exception {
        when(knowledgeService.searchConcepts(anyString(), anyInt()))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/knowledge/concepts/search")
                        .param("query", "AI")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetKnowledgeGraphStatistics() throws Exception {
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("totalNodes", 100);
        mockStats.put("totalRelationships", 200);

        when(knowledgeService.getKnowledgeGraphStatistics())
                .thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/knowledge/statistics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNodes").value(100));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetPerformanceStats() throws Exception {
        KnowledgeGraphPerformanceMonitor.PerformanceStats mockStats = 
                new KnowledgeGraphPerformanceMonitor.PerformanceStats();

        when(performanceMonitor.getPerformanceStats())
                .thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/knowledge/performance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testHealthCheck() throws Exception {
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("totalNodes", 100);
        mockStats.put("totalRelationships", 200);

        when(knowledgeService.getKnowledgeGraphStatistics())
                .thenReturn(mockStats);

        KnowledgeGraphPerformanceMonitor.PerformanceStats perfStats = 
                new KnowledgeGraphPerformanceMonitor.PerformanceStats();

        when(performanceMonitor.getPerformanceStats())
                .thenReturn(perfStats);

        mockMvc.perform(get("/api/v1/knowledge/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").exists());
    }
} 
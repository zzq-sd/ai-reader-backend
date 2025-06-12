package com.aireader.backend.controller;

import com.aireader.backend.dto.RssSourceDTO;
import com.aireader.backend.dto.common.ApiResponse;
import com.aireader.backend.service.RssFeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * RSS源控制器测试类
 */
class RssSourceControllerTest {
    
    @Mock
    private RssFeedService rssFeedService;
    
    @InjectMocks
    private RssSourceController rssSourceController;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 设置RSSHub实例和超时
        ReflectionTestUtils.setField(rssSourceController, "rsshubInstances", "http://localhost:1200");
        ReflectionTestUtils.setField(rssSourceController, "rsshubTimeout", 5000);
        
        // 模拟getCurrentUserId方法
        doReturn("test-user-id").when(rssFeedService).addRssSource(any(RssSourceDTO.class), anyString());
    }
    
    @Test
    void testAddRsshubSource() {
        // 准备测试数据
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("route", "/zhihu/daily");
        requestBody.put("name", "知乎日报");
        requestBody.put("category", "news");
        
        // 模拟服务返回
        RssSourceDTO mockResponse = new RssSourceDTO();
        mockResponse.setId("test-id");
        mockResponse.setName("知乎日报");
        mockResponse.setUrl("http://localhost:1200/zhihu/daily");
        mockResponse.setRsshubRoute("/zhihu/daily");
        mockResponse.setRsshubInstance("http://localhost:1200");
        mockResponse.setRsshub(true);
        
        when(rssFeedService.addRssSource(any(RssSourceDTO.class), anyString())).thenReturn(mockResponse);
        
        // 执行测试
        ResponseEntity<ApiResponse<RssSourceDTO>> response = rssSourceController.addRsshubSource(requestBody);
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("test-id", response.getBody().getData().getId());
        assertEquals("知乎日报", response.getBody().getData().getName());
        assertEquals("http://localhost:1200/zhihu/daily", response.getBody().getData().getUrl());
        assertTrue(response.getBody().getData().isRsshub());
    }
    
    @Test
    void testAddRsshubSourceWithEmptyRoute() {
        // 准备测试数据
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "测试源");
        
        // 执行测试
        ResponseEntity<ApiResponse<RssSourceDTO>> response = rssSourceController.addRsshubSource(requestBody);
        
        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("RSSHub路由不能为空", response.getBody().getMessage());
    }
    
    @Test
    void testValidateRsshubRoute() {
        // 执行测试
        ResponseEntity<ApiResponse<Boolean>> response = rssSourceController.validateRsshubRoute("/zhihu/daily");
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getData());
    }
    
    @Test
    void testValidateRsshubRouteWithInvalidRoute() {
        // 执行测试
        ResponseEntity<ApiResponse<Boolean>> response = rssSourceController.validateRsshubRoute("");
        
        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("RSSHub路由不能为空", response.getBody().getMessage());
    }
} 
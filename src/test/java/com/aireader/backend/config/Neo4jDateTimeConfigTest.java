package com.aireader.backend.config;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.value.StringValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Neo4j日期时间转换器测试类
 */
@SpringBootTest
class Neo4jDateTimeConfigTest {

    @Autowired
    private Neo4jDateTimeConfig neo4jDateTimeConfig;

    @Test
    void testNeo4jConversionsBean() {
        Neo4jConversions conversions = neo4jDateTimeConfig.neo4jConversions();
        assertNotNull(conversions, "Neo4jConversions Bean不应为空");
    }

    @Test
    void testDateTimeToLocalDateTimeConverter() {
        Neo4jDateTimeConfig.DateTimeToLocalDateTimeConverter converter = 
            new Neo4jDateTimeConfig.DateTimeToLocalDateTimeConverter();
        
        // 创建一个带有时区的日期时间字符串值
        String dateTimeStr = "2025-05-26T18:32:45.390Z";
        Value value = Values.value(dateTimeStr);
        
        // 执行转换
        LocalDateTime result = converter.convert(value);
        
        // 验证结果
        assertNotNull(result, "转换结果不应为空");
        assertEquals(2025, result.getYear(), "年份应为2025");
        assertEquals(5, result.getMonthValue(), "月份应为5");
        assertEquals(26, result.getDayOfMonth(), "日期应为26");
    }

    @Test
    void testStringToLocalDateTimeConverter() {
        Neo4jDateTimeConfig.StringToLocalDateTimeConverter converter = 
            new Neo4jDateTimeConfig.StringToLocalDateTimeConverter();
        
        // 测试不同格式的日期时间字符串
        String[] testStrings = {
            "2025-05-26T18:32:45.390Z",
            "2025-05-26T18:32:45.390+08:00",
            "2025-05-26T18:32:45",
            "2025-05-26T18:32:45.390"
        };
        
        for (String dateTimeStr : testStrings) {
            // 执行转换
            LocalDateTime result = converter.convert(dateTimeStr);
            
            // 验证结果
            assertNotNull(result, "转换 " + dateTimeStr + " 的结果不应为空");
            assertEquals(2025, result.getYear(), "年份应为2025");
            assertEquals(5, result.getMonthValue(), "月份应为5");
            assertEquals(26, result.getDayOfMonth(), "日期应为26");
        }
    }

    @Test
    void testZonedDateTimeToLocalDateTimeConverter() {
        Neo4jDateTimeConfig.ZonedDateTimeToLocalDateTimeConverter converter = 
            new Neo4jDateTimeConfig.ZonedDateTimeToLocalDateTimeConverter();
        
        // 创建一个ZonedDateTime
        ZonedDateTime zonedDateTime = ZonedDateTime.of(
            2025, 5, 26, 18, 32, 45, 390000000, ZoneId.of("UTC"));
        
        // 执行转换
        LocalDateTime result = converter.convert(zonedDateTime);
        
        // 验证结果
        assertNotNull(result, "转换结果不应为空");
        assertEquals(2025, result.getYear(), "年份应为2025");
        assertEquals(5, result.getMonthValue(), "月份应为5");
        assertEquals(26, result.getDayOfMonth(), "日期应为26");
        assertEquals(18, result.getHour(), "小时应为18");
    }

    @Test
    void testLocalDateTimeToZonedDateTimeConverter() {
        Neo4jDateTimeConfig.LocalDateTimeToZonedDateTimeConverter converter = 
            new Neo4jDateTimeConfig.LocalDateTimeToZonedDateTimeConverter();
        
        // 创建一个LocalDateTime
        LocalDateTime localDateTime = LocalDateTime.of(2025, 5, 26, 18, 32, 45, 390000000);
        
        // 执行转换
        ZonedDateTime result = converter.convert(localDateTime);
        
        // 验证结果
        assertNotNull(result, "转换结果不应为空");
        assertEquals(2025, result.getYear(), "年份应为2025");
        assertEquals(5, result.getMonthValue(), "月份应为5");
        assertEquals(26, result.getDayOfMonth(), "日期应为26");
        assertEquals(18, result.getHour(), "小时应为18");
        assertEquals(ZoneId.systemDefault(), result.getZone(), "时区应为系统默认时区");
    }

    @Test
    void testHandlingNullValues() {
        // 测试所有转换器处理null值的情况
        Neo4jDateTimeConfig.DateTimeToLocalDateTimeConverter converter1 = 
            new Neo4jDateTimeConfig.DateTimeToLocalDateTimeConverter();
        assertNull(converter1.convert(null), "DateTimeToLocalDateTimeConverter应返回null");
        
        Neo4jDateTimeConfig.StringToLocalDateTimeConverter converter2 = 
            new Neo4jDateTimeConfig.StringToLocalDateTimeConverter();
        assertNull(converter2.convert(null), "StringToLocalDateTimeConverter应返回null");
        
        Neo4jDateTimeConfig.ZonedDateTimeToLocalDateTimeConverter converter3 = 
            new Neo4jDateTimeConfig.ZonedDateTimeToLocalDateTimeConverter();
        assertNull(converter3.convert(null), "ZonedDateTimeToLocalDateTimeConverter应返回null");
        
        Neo4jDateTimeConfig.LocalDateTimeToZonedDateTimeConverter converter4 = 
            new Neo4jDateTimeConfig.LocalDateTimeToZonedDateTimeConverter();
        assertNull(converter4.convert(null), "LocalDateTimeToZonedDateTimeConverter应返回null");
    }
} 
package com.aireader.backend.config;

import com.aireader.backend.exception.Neo4jDateTimeConversionException;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.value.Uncoercible;
import org.neo4j.driver.internal.value.DateTimeValue;
import org.neo4j.driver.internal.value.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Neo4j日期时间转换器配置
 * 用于解决Neo4j的DATE_TIME类型与Java的LocalDateTime类型不兼容问题
 */
@Configuration
public class Neo4jDateTimeConfig {
    private static final Logger log = LoggerFactory.getLogger(Neo4jDateTimeConfig.class);

    /**
     * 创建自定义的Neo4j转换器
     * 
     * @return Neo4jConversions实例，包含自定义的日期时间转换器
     */
    @Bean
    public Neo4jConversions neo4jConversions() {
        List<Object> converters = new ArrayList<>();
        
        // 注册读取转换器
        converters.add(new DateTimeToLocalDateTimeConverter());
        converters.add(new StringToLocalDateTimeConverter());
        converters.add(new ZonedDateTimeToLocalDateTimeConverter());
        
        // 注册写入转换器
        converters.add(new LocalDateTimeToZonedDateTimeConverter());
        
        log.info("注册Neo4j日期时间自定义转换器: {} 个转换器", converters.size());
        return new Neo4jConversions(converters);
    }

    /**
     * Neo4j日期时间值到LocalDateTime的通用转换器
     * 解决错误：Cannot coerce DATE_TIME to LocalDateTime
     */
    @ReadingConverter
    public static class DateTimeToLocalDateTimeConverter implements Converter<Value, LocalDateTime> {
        @Override
        public LocalDateTime convert(Value source) {
            if (source == null) {
                return null;
            }
            
            try {
                // 尝试直接转换为LocalDateTime
                return source.asLocalDateTime();
            } catch (Uncoercible e) {
                try {
                    // 如果无法直接转换，尝试先转换为ZonedDateTime，再获取LocalDateTime部分
                    ZonedDateTime zonedDateTime = source.asZonedDateTime();
                    log.debug("从ZonedDateTime转换为LocalDateTime: {}", zonedDateTime);
                    return zonedDateTime.toLocalDateTime();
                } catch (Uncoercible e2) {
                    try {
                        // 尝试从ISO字符串解析 - Neo4j Value没有asInstant方法
                        String dateTimeStr = source.asString();
                        log.debug("尝试从字符串解析日期时间: {}", dateTimeStr);
                        
                        // 处理带有时区的ISO日期时间格式
                        if (dateTimeStr.contains("Z") || dateTimeStr.contains("+")) {
                            try {
                                ZonedDateTime zdt = ZonedDateTime.parse(dateTimeStr);
                                return zdt.toLocalDateTime();
                            } catch (DateTimeParseException e5) {
                                try {
                                    OffsetDateTime odt = OffsetDateTime.parse(dateTimeStr);
                                    return odt.toLocalDateTime();
                                } catch (DateTimeParseException e6) {
                                    try {
                                        Instant instant = Instant.parse(dateTimeStr);
                                        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                    } catch (DateTimeParseException e7) {
                                        log.warn("无法解析带时区的日期时间字符串: {}", dateTimeStr);
                                    }
                                }
                            }
                        } else {
                            // 处理不带时区的ISO日期时间格式
                            try {
                                return LocalDateTime.parse(dateTimeStr);
                            } catch (DateTimeParseException e5) {
                                log.warn("无法解析不带时区的日期时间字符串: {}", dateTimeStr);
                            }
                        }
                    } catch (Exception e3) {
                        log.error("无法将Neo4j日期时间值转换为LocalDateTime: {}", source, e3);
                    }
                    
                    // 所有尝试失败后，创建一个包含详细信息的异常但不抛出
                    // 而是返回当前时间作为兜底方案，避免整个程序崩溃
                    Neo4jDateTimeConversionException conversionException = 
                        new Neo4jDateTimeConversionException(source, LocalDateTime.class, e2);
                    log.error("日期时间转换失败，使用当前时间作为替代: {}", conversionException.getMessage());
                    return LocalDateTime.now();
                }
            }
        }
    }

    /**
     * 字符串到LocalDateTime的转换器
     */
    @ReadingConverter
    public static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        
        @Override
        public LocalDateTime convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            
            // 尝试多种格式解析
            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    // 优先尝试作为LocalDateTime解析
                    return LocalDateTime.parse(source, formatter);
                } catch (DateTimeParseException e) {
                    try {
                        // 尝试作为ZonedDateTime解析然后转换
                        return ZonedDateTime.parse(source, formatter).toLocalDateTime();
                    } catch (DateTimeParseException ignored) {
                        // 继续尝试下一个格式
                    }
                }
            }
            
            // 特殊处理带Z的UTC时间格式
            if (source.endsWith("Z")) {
                try {
                    return Instant.parse(source)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                } catch (DateTimeParseException e) {
                    log.warn("无法解析带Z的时间格式: {}", source);
                }
            }
            
            // 处理自定义格式
            try {
                // 尝试常见的日期时间格式
                DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return LocalDateTime.parse(source, customFormatter);
            } catch (DateTimeParseException e) {
                log.warn("无法解析自定义格式的日期时间: {}", source);
            }
            
            log.error("无法将字符串转换为LocalDateTime: {}", source);
            // 使用兜底方案
            return LocalDateTime.now();
        }
    }

    /**
     * ZonedDateTime到LocalDateTime的转换器
     */
    @ReadingConverter
    public static class ZonedDateTimeToLocalDateTimeConverter implements Converter<ZonedDateTime, LocalDateTime> {
        @Override
        public LocalDateTime convert(ZonedDateTime source) {
            if (source == null) {
                return null;
            }
            return source.toLocalDateTime();
        }
    }

    /**
     * LocalDateTime到ZonedDateTime的转换器
     * 在写入Neo4j时使用
     */
    @WritingConverter
    public static class LocalDateTimeToZonedDateTimeConverter implements Converter<LocalDateTime, ZonedDateTime> {
        @Override
        public ZonedDateTime convert(LocalDateTime source) {
            if (source == null) {
                return null;
            }
            
            // 将LocalDateTime转换为系统默认时区的ZonedDateTime
            return source.atZone(ZoneId.systemDefault());
        }
    }
} 
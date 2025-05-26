package com.aireader.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本编码工具类
 * 用于处理各种字符编码问题，特别是Unicode转义序列
 */
public class TextEncodingUtil {
    
    private static final Logger log = LoggerFactory.getLogger(TextEncodingUtil.class);
    
    // Unicode转义序列的正则表达式
    private static final Pattern UNICODE_ESCAPE_PATTERN = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    private static final Pattern HEX_ESCAPE_PATTERN = Pattern.compile("\\\\x([0-9a-fA-F]{2})");
    
    /**
     * 解码Unicode转义序列
     * @param input 包含Unicode转义序列的字符串
     * @return 解码后的字符串
     */
    public static String decodeUnicodeEscapes(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        try {
            String result = input;
            
            // 处理 \\uXXXX 格式的Unicode转义序列
            Matcher unicodeMatcher = UNICODE_ESCAPE_PATTERN.matcher(result);
            StringBuffer sb = new StringBuffer();
            while (unicodeMatcher.find()) {
                String hex = unicodeMatcher.group(1);
                int codePoint = Integer.parseInt(hex, 16);
                unicodeMatcher.appendReplacement(sb, String.valueOf((char) codePoint));
            }
            unicodeMatcher.appendTail(sb);
            result = sb.toString();
            
            // 处理 \\xXX 格式的十六进制转义序列
            Matcher hexMatcher = HEX_ESCAPE_PATTERN.matcher(result);
            StringBuffer sb2 = new StringBuffer();
            while (hexMatcher.find()) {
                String hex = hexMatcher.group(1);
                int codePoint = Integer.parseInt(hex, 16);
                hexMatcher.appendReplacement(sb2, String.valueOf((char) codePoint));
            }
            hexMatcher.appendTail(sb2);
            result = sb2.toString();
            
            return result;
        } catch (Exception e) {
            log.warn("Unicode解码失败: {}", e.getMessage());
            return input;
        }
    }
    
    /**
     * 确保字符串是正确的UTF-8编码
     * @param input 输入字符串
     * @return UTF-8编码的字符串
     */
    public static String ensureUtf8(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        try {
            // 将字符串编码为UTF-8字节数组，然后重新解码
            byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("UTF-8编码转换失败: {}", e.getMessage());
            return input;
        }
    }
    
    /**
     * 完整的文本处理流水线
     * @param input 原始文本
     * @return 处理后的文本
     */
    public static String processText(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String processed = input;
        
        // 1. 解码Unicode转义序列
        processed = decodeUnicodeEscapes(processed);
        
        // 2. 确保UTF-8编码
        processed = ensureUtf8(processed);
        
        // 3. 处理常见的HTML实体
        processed = decodeHtmlEntities(processed);
        
        return processed;
    }
    
    /**
     * 解码常见的HTML实体
     * @param input 包含HTML实体的字符串
     * @return 解码后的字符串
     */
    public static String decodeHtmlEntities(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        return input
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace("&#8217;", "\u2019")  // 右单引号
                .replace("&#8216;", "\u2018")  // 左单引号
                .replace("&#8220;", "\u201c")  // 左双引号
                .replace("&#8221;", "\u201d")  // 右双引号
                .replace("&#8212;", "\u2014")  // em破折号
                .replace("&#8211;", "\u2013"); // en破折号
    }
    
    /**
     * 检查字符串是否包含Unicode转义序列
     * @param input 输入字符串
     * @return 如果包含转义序列返回true
     */
    public static boolean containsUnicodeEscapes(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        return UNICODE_ESCAPE_PATTERN.matcher(input).find() || 
               HEX_ESCAPE_PATTERN.matcher(input).find();
    }
    
    /**
     * 安全地截断文本到指定长度，避免破坏Unicode字符
     * @param input 输入文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    public static String safeTruncate(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        
        String truncated = input.substring(0, maxLength);
        
        // 检查是否在Unicode代理对中间截断
        if (Character.isLowSurrogate(truncated.charAt(truncated.length() - 1))) {
            // 如果最后一个字符是低代理，移除它
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        
        return truncated;
    }
} 
#!/bin/bash

echo "🔍 测试KnowledgeServiceImpl日志配置..."
echo ""

# 检查当前的logback配置
echo "📋 当前logback配置文件内容（KnowledgeServiceImpl相关部分）："
echo "----------------------------------------"
grep -A 5 -B 2 "KnowledgeServiceImpl" src/main/resources/logback-spring.xml
echo "----------------------------------------"
echo ""

# 检查日志文件是否存在
echo "📁 检查日志文件："
if [ -d "logs" ]; then
    echo "✅ logs目录存在"
    ls -la logs/ | grep -E "(knowledge|ai-reader)" || echo "❌ 没有找到相关日志文件"
else
    echo "❌ logs目录不存在"
fi
echo ""

echo "💡 建议："
echo "1. 重启Spring Boot应用以使新的日志配置生效"
echo "2. 调用KnowledgeServiceImpl的任何方法来测试日志输出"
echo "3. 控制台应该只显示带🔍标识的KnowledgeServiceImpl日志"
echo "4. 其他所有日志都会写入文件但不在控制台显示"
echo ""

echo "🚀 如果要启动应用进行测试，请运行："
echo "   mvn spring-boot:run" 
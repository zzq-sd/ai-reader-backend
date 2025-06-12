#!/bin/bash

# 日志模式切换脚本
# 用法: ./switch-log-mode.sh [knowledge-only|full-debug]

RESOURCES_DIR="src/main/resources"
CURRENT_CONFIG="$RESOURCES_DIR/logback-spring.xml"
BACKUP_CONFIG="$RESOURCES_DIR/logback-spring-full-debug.xml.backup"
KNOWLEDGE_ONLY_CONFIG="$RESOURCES_DIR/logback-spring-knowledge-only.xml"

# 如果knowledge-only配置不存在，创建它
if [ ! -f "$KNOWLEDGE_ONLY_CONFIG" ]; then
    cp "$CURRENT_CONFIG" "$KNOWLEDGE_ONLY_CONFIG"
fi

case "$1" in
    "knowledge-only")
        echo "🔍 切换到KnowledgeServiceImpl专用日志模式..."
        cp "$KNOWLEDGE_ONLY_CONFIG" "$CURRENT_CONFIG"
        echo "✅ 已切换到KnowledgeServiceImpl专用日志模式"
        echo "   控制台将只显示KnowledgeServiceImpl的日志信息"
        echo "   其他日志仍会记录到文件中"
        ;;
    "full-debug")
        echo "🔧 切换到全调试模式..."
        if [ -f "$BACKUP_CONFIG" ]; then
            cp "$BACKUP_CONFIG" "$CURRENT_CONFIG"
            echo "✅ 已切换到全调试模式"
            echo "   控制台将显示所有组件的详细日志"
        else
            echo "❌ 备份配置文件不存在: $BACKUP_CONFIG"
            exit 1
        fi
        ;;
    *)
        echo "用法: $0 [knowledge-only|full-debug]"
        echo ""
        echo "模式说明:"
        echo "  knowledge-only  - 只在控制台显示KnowledgeServiceImpl的日志"
        echo "  full-debug      - 显示所有组件的详细调试日志"
        echo ""
        echo "当前配置文件: $CURRENT_CONFIG"
        exit 1
        ;;
esac

echo ""
echo "💡 提示: 重启应用后新的日志配置才会生效" 
# AI Reader 日志配置指南

## 默认配置（生产/开发环境）

默认情况下，系统使用优化后的日志配置，只显示重要的信息：

- 只输出 INFO 级别及以上的应用日志
- 只输出 WARN 级别及以上的框架日志
- 保留关键的启动信息和错误信息
- 使用彩色高亮显示日志级别，便于快速识别重要信息

这种配置适合日常开发和生产环境使用，可以减少日志输出量，提高系统性能和可读性。

## 调试模式（问题排查）

当需要详细日志进行问题排查时，有以下几种方式切换到调试模式：

### 方式一：使用调试配置文件启动

```bash
# 使用调试配置文件启动应用
java -jar your-app.jar --spring.profiles.active=debug
```

或者在IDE中设置活动配置文件为`debug`。

### 方式二：替换日志配置文件

将`logback-spring-debug.xml`重命名为`logback-spring.xml`，替换原有配置文件。

### 方式三：运行时调整日志级别

通过Spring Boot Actuator动态调整日志级别（需要开启对应端点）：

```bash
# 调整特定包的日志级别为DEBUG
curl -X POST http://localhost:8080/api/v1/actuator/loggers/com.aireader.backend \
  -H 'Content-Type: application/json' \
  -d '{"configuredLevel": "DEBUG"}'

# 调整根日志级别为DEBUG
curl -X POST http://localhost:8080/api/v1/actuator/loggers/ROOT \
  -H 'Content-Type: application/json' \
  -d '{"configuredLevel": "DEBUG"}'
```

## 日志文件位置

所有日志文件存放在应用根目录的`logs`文件夹下：

- `ai-reader.log` - 主日志文件
- `error.log` - 错误日志文件
- `neo4j.log` - Neo4j相关日志文件

## 最佳实践

1. 在开发和测试过程中，使用默认配置以保持日志简洁明了
2. 只在需要排查特定问题时临时切换到调试模式
3. 排查完成后，立即切换回默认配置
4. 生产环境建议使用默认配置或更低的日志级别（如将应用日志也设为WARN） 
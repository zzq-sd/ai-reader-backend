# AI 阅读器系统后端

基于Spring Boot 3.2.5和Spring AI 1.0.0构建的智能阅读系统后端。

## 技术栈

- **Spring Boot 3.2.5**：核心框架
- **Spring AI 1.0.0**：AI集成框架
- **多数据库架构**：
  - MySQL：结构化数据
  - MongoDB：非结构化内容
  - Neo4j：知识图谱
  - Redis：缓存
- **RabbitMQ**：异步消息处理
- **Spring Security + JWT**：安全认证

## DeepSeek API集成

本系统使用Spring AI 1.0.0正式版集成DeepSeek API，实现AI分析功能。

### 配置说明

1. **环境变量配置**（推荐）：
   ```bash
   export DEEPSEEK_API_KEY=sk-your-api-key
   ```

2. **application.yml配置**：
   ```yaml
   spring:
     ai:
       deepseek:
         api-key: ${DEEPSEEK_API_KEY:sk-...}
         base-url: https://api.deepseek.com
         chat:
           options:
             model: deepseek-chat
             temperature: 0.7
             max-tokens: 2000
   ```

### 启用方法

1. 获取DeepSeek API密钥
2. 配置环境变量或application.yml
3. 重启应用

### 故障排查

如果AI功能不可用，请检查：

1. API密钥是否正确配置
2. 网络连接是否正常
3. 日志中的详细错误信息

## 项目启动

```bash
# 克隆项目
git clone https://github.com/yourusername/ai-reader-system.git

# 进入项目目录
cd ai-reader-system/ai-reader-backend

# 配置环境变量
export DEEPSEEK_API_KEY=sk-your-api-key

# 构建项目
mvn clean package -DskipTests

# 运行项目
mvn spring-boot:run
```

## API文档

启动应用后，访问Swagger UI查看API文档：
http://localhost:8080/api/v1/swagger-ui.html 
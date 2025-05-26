# 文章内容获取修复指南

## 问题描述

之前文章页面能进入但内容无法正确显示的问题已经修复。主要修复了以下几个方面：

1. **后端RSS抓取逻辑**：改进了`fetchAndSaveArticleContent`方法，确保文章内容正确保存到MongoDB
2. **文章内容获取API**：修复了`getArticleContent`接口，提供更完整的文章数据
3. **前端API调用**：修复了`fetchArticleContent`函数，移除对模拟数据的依赖
4. **数据关联**：确保MySQL和MongoDB之间的ID关联正确

## 修复内容

### 后端修复

1. **ArticleFetchServiceImpl.fetchAndSaveArticleContent()**
   - 添加了重复检查，避免重复抓取
   - 使用Jsoup解析HTML并提取纯文本
   - 提取图片URLs
   - 计算内容哈希
   - 更新MySQL中的mongodb_content_id字段
   - 改进错误处理和日志

2. **ArticleController.getArticleContent()**
   - 返回更完整的文章数据
   - 支持HTML和纯文本内容
   - 添加详细的错误处理和日志

3. **新增测试接口**
   - `POST /articles/fetch-rss/{sourceId}` - 手动触发RSS抓取
   - `GET /articles/all` - 获取所有文章列表
   - `POST /articles/create-test-data` - 创建测试数据提示

### 前端修复

1. **reader.js fetchArticleContent()**
   - 修复API调用路径为 `/articles/{id}/content`
   - 移除对模拟数据的依赖
   - 改进错误处理
   - 正确转换后端数据格式

## 测试步骤

### 1. 启动后端服务

确保后端服务正常运行在 `http://localhost:8080`

### 2. 添加RSS源

```bash
# 添加一个测试RSS源
curl -X POST http://localhost:8080/api/v1/feeds \
  -H "Content-Type: application/json" \
  -d '{
    "title": "测试RSS源",
    "url": "https://feeds.feedburner.com/oreilly/radar",
    "description": "测试用RSS源",
    "category": "技术",
    "isPublic": true
  }'
```

### 3. 手动触发RSS抓取

```bash
# 使用返回的RSS源ID触发抓取
curl -X POST http://localhost:8080/api/v1/articles/fetch-rss/{RSS_SOURCE_ID}
```

### 4. 查看抓取的文章

```bash
# 获取所有文章列表
curl http://localhost:8080/api/v1/articles/all
```

### 5. 获取文章内容

```bash
# 使用文章ID获取内容
curl http://localhost:8080/api/v1/articles/{ARTICLE_ID}/content
```

### 6. 测试前端

1. 启动前端服务
2. 访问文章列表页面
3. 点击文章进入阅读页面
4. 验证文章内容是否正确显示

## 数据库检查

### MySQL检查

```sql
-- 检查文章元数据
SELECT id, title, link_to_original, mongodb_content_id, ai_processing_status 
FROM article_metadata 
ORDER BY created_at DESC 
LIMIT 10;
```

### MongoDB检查

```javascript
// 检查文章内容
db.articles_content.find({}).limit(10).pretty()

// 检查特定文章的内容
db.articles_content.findOne({"mysqlMetadataId": "YOUR_ARTICLE_ID"})
```

## 常见问题排查

### 1. 文章内容为空

**可能原因：**
- RSS源URL无法访问
- 网络连接问题
- HTML解析失败

**排查方法：**
- 检查后端日志
- 验证RSS源URL是否有效
- 检查MongoDB中是否有对应记录

### 2. 前端显示错误

**可能原因：**
- 后端API返回错误
- 前端API调用路径错误
- 数据格式转换问题

**排查方法：**
- 检查浏览器控制台错误
- 验证API调用是否成功
- 检查后端日志

### 3. 数据库关联问题

**可能原因：**
- MySQL和MongoDB ID关联错误
- 数据保存失败

**排查方法：**
- 检查article_metadata表的mongodb_content_id字段
- 验证MongoDB中的mysqlMetadataId字段
- 检查数据库连接配置

## 性能优化建议

1. **批量处理**：对于大量文章的RSS源，考虑批量处理
2. **缓存机制**：添加文章内容缓存，减少数据库查询
3. **异步处理**：使用消息队列异步处理文章内容抓取
4. **内容去重**：基于contentHash字段避免重复内容

## 后续改进

1. **内容质量检测**：添加文章内容质量检测
2. **多媒体支持**：支持图片、视频等多媒体内容
3. **全文搜索**：基于MongoDB的全文搜索功能
4. **AI增强**：使用Spring AI进行内容分析和摘要生成 
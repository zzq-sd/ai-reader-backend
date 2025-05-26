# AI阅读器系统后端 API 文档

## 项目概述

AI阅读器系统是一个知识关联AI阅读器系统，基于Spring Boot 3.2.5框架开发，集成了Spring AI、Spring Security、MySQL、MongoDB、Neo4j等技术栈。

### 技术栈
- **框架**: Spring Boot 3.2.5
- **AI集成**: Spring AI (OpenAI)
- **数据库**: MySQL (关系数据)、MongoDB (文档存储)、Neo4j (知识图谱)
- **缓存**: Redis
- **消息队列**: RabbitMQ
- **安全**: Spring Security + JWT
- **文档**: Swagger/OpenAPI 3

### 项目结构
```
com.aireader.backend/
├── controller/      # REST控制器
├── service/         # 业务逻辑层
├── repository/      # 数据访问层
├── model/          # 数据模型
├── dto/            # 数据传输对象
├── config/         # 配置类
├── security/       # 安全配置
├── ai/             # AI相关功能
├── mq/             # 消息队列
└── util/           # 工具类
```

## API 统一响应格式

所有API接口都使用统一的响应格式：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    // 具体的响应数据
  }
}
```

### 响应字段说明
- `success`: boolean，操作是否成功
- `message`: string，响应消息（可选）
- `data`: object，响应数据（可选）

## 认证与授权

系统使用JWT令牌进行认证，除了登录和注册接口外，其他接口都需要在请求头中携带JWT令牌：

```
Authorization: Bearer <JWT_TOKEN>
```

---

## 1. 认证接口 (AuthController)

### 1.1 用户注册
- **接口**: `POST /auth/register`
- **描述**: 创建新用户账号
- **请求参数**:
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```
- **响应**: 
```json
{
  "success": true,
  "data": {
    "id": "string",
    "username": "string",
    "email": "string",
    "createdAt": "timestamp"
  }
}
```

### 1.2 用户登录
- **接口**: `POST /auth/login`
- **描述**: 验证用户凭证并返回JWT令牌
- **请求参数**:
```json
{
  "username": "string",
  "password": "string"
}
```
- **响应**:
```json
{
  "success": true,
  "data": {
    "accessToken": "string",
    "refreshToken": "string",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

### 1.3 刷新令牌
- **接口**: `POST /auth/refresh`
- **描述**: 使用刷新令牌获取新的访问令牌
- **请求参数**: `refreshToken` (query parameter)
- **响应**: 与登录接口相同的JWT响应格式

### 1.4 获取当前用户信息
- **接口**: `GET /auth/me`
- **描述**: 返回当前登录用户的详细信息
- **权限**: 需要认证
- **响应**:
```json
{
  "success": true,
  "data": {
    "id": "string",
    "username": "string",
    "email": "string",
    "roles": ["USER"]
  }
}
```

### 1.5 检查用户名可用性
- **接口**: `GET /auth/check-username`
- **描述**: 检查指定的用户名是否已被注册
- **请求参数**: `username` (query parameter)
- **响应**:
```json
{
  "success": true,
  "data": {
    "available": true
  }
}
```

### 1.6 检查邮箱可用性
- **接口**: `GET /auth/check-email`
- **描述**: 检查指定的邮箱是否已被注册
- **请求参数**: `email` (query parameter)
- **响应**:
```json
{
  "success": true,
  "data": {
    "available": true
  }
}
```

---

## 2. 文章管理接口 (ArticleController)

### 2.1 获取最新文章
- **接口**: `GET /articles/latest`
- **描述**: 获取用户订阅的所有RSS源的最新文章
- **权限**: 需要认证
- **请求参数**:
  - `page`: int，页码（默认0）
  - `size`: int，每页大小（默认20）
- **响应**:
```json
{
  "success": true,
  "data": [
    {
      "id": "string",
      "title": "string",
      "content": "string",
      "url": "string",
      "publishDate": "timestamp",
      "source": "string"
    }
  ]
}
```

### 2.2 获取文章内容
- **接口**: `GET /articles/{articleId}/content`
- **描述**: 获取文章的全文内容
- **权限**: 需要认证
- **路径参数**: `articleId` - 文章ID
- **响应**:
```json
{
  "success": true,
  "data": {
    "articleId": "string",
    "content": "string"
  }
}
```

### 2.3 解析文章内容
- **接口**: `POST /articles/{articleId}/parse`
- **描述**: 手动触发文章内容解析
- **权限**: 需要认证
- **路径参数**: `articleId` - 文章ID
- **响应**:
```json
{
  "success": true,
  "message": "文章内容解析成功",
  "data": {
    "success": true,
    "articleId": "string"
  }
}
```

### 2.4 处理文章
- **接口**: `POST /articles/{articleId}/process`
- **描述**: 将文章加入处理队列
- **权限**: 需要认证
- **路径参数**: `articleId` - 文章ID
- **响应**:
```json
{
  "success": true,
  "message": "文章已加入处理队列",
  "data": {
    "success": true,
    "articleId": "string"
  }
}
```

---

## 3. RSS源管理接口 (RssSourceController)

### 3.1 添加RSS源
- **接口**: `POST /feeds`
- **描述**: 添加新的RSS源
- **权限**: 需要认证
- **请求参数**:
```json
{
  "title": "string",
  "url": "string",
  "description": "string",
  "category": "string",
  "isPublic": false
}
```
- **响应**:
```json
{
  "success": true,
  "data": {
    "id": "string",
    "title": "string",
    "url": "string",
    "description": "string",
    "category": "string",
    "isPublic": false,
    "createdAt": "timestamp"
  }
}
```

### 3.2 获取用户RSS源
- **接口**: `GET /feeds`
- **描述**: 获取当前用户的所有RSS源
- **权限**: 需要认证
- **响应**: RSS源列表

### 3.3 获取公共RSS源
- **接口**: `GET /feeds/public`
- **描述**: 获取所有公共RSS源
- **响应**: 公共RSS源列表

### 3.4 获取RSS源详情
- **接口**: `GET /feeds/{sourceId}`
- **描述**: 根据ID获取RSS源详细信息
- **路径参数**: `sourceId` - RSS源ID
- **响应**: RSS源详细信息

### 3.5 更新RSS源
- **接口**: `PUT /feeds/{sourceId}`
- **描述**: 更新RSS源信息
- **权限**: 需要认证
- **路径参数**: `sourceId` - RSS源ID
- **请求参数**: RSS源更新信息
- **响应**: 更新后的RSS源信息

### 3.6 删除RSS源
- **接口**: `DELETE /feeds/{sourceId}`
- **描述**: 删除指定的RSS源
- **权限**: 需要认证
- **路径参数**: `sourceId` - RSS源ID
- **响应**:
```json
{
  "success": true,
  "data": {
    "deleted": true
  }
}
```

### 3.7 获取RSS源文章
- **接口**: `GET /feeds/{sourceId}/articles`
- **描述**: 获取指定RSS源的文章列表
- **路径参数**: `sourceId` - RSS源ID
- **请求参数**:
  - `page`: int，页码（默认0）
  - `size`: int，每页大小（默认20）
- **响应**: 文章列表

### 3.8 抓取RSS源
- **接口**: `POST /feeds/{sourceId}/fetch`
- **描述**: 手动触发指定RSS源的抓取
- **权限**: 需要认证
- **路径参数**: `sourceId` - RSS源ID
- **响应**:
```json
{
  "success": true,
  "message": "成功抓取10篇文章",
  "data": {
    "success": true,
    "count": 10
  }
}
```

### 3.9 验证RSS源URL
- **接口**: `GET /feeds/validate`
- **描述**: 检查RSS源URL是否有效
- **请求参数**: `url` (query parameter)
- **响应**:
```json
{
  "success": true,
  "data": {
    "valid": true
  }
}
```

---

## 4. 收藏管理接口 (FavoriteController)

### 4.1 添加收藏
- **接口**: `POST /favorites/{articleId}`
- **描述**: 将文章添加到收藏
- **权限**: 需要认证
- **路径参数**: `articleId` - 文章ID
- **响应**:
```json
{
  "success": true,
  "message": "文章已添加到收藏"
}
```

### 4.2 移除收藏
- **接口**: `DELETE /favorites/{articleId}`
- **描述**: 从收藏中移除文章
- **权限**: 需要认证
- **路径参数**: `articleId` - 文章ID
- **响应**:
```json
{
  "success": true,
  "message": "文章已从收藏中移除"
}
```

### 4.3 检查收藏状态
- **接口**: `GET /favorites/{articleId}/status`
- **描述**: 检查文章是否已收藏
- **权限**: 需要认证
- **路径参数**: `articleId` - 文章ID
- **响应**:
```json
{
  "success": true,
  "data": {
    "isFavorited": true
  }
}
```

### 4.4 获取收藏列表
- **接口**: `GET /favorites`
- **描述**: 分页获取收藏的文章列表
- **权限**: 需要认证
- **请求参数**:
  - `page`: int，页码（默认0）
  - `size`: int，每页大小（默认10）
- **响应**: 分页的收藏文章列表

### 4.5 获取最近收藏
- **接口**: `GET /favorites/recent`
- **描述**: 获取最近收藏的文章
- **权限**: 需要认证
- **请求参数**: `limit` - 限制数量（默认5）
- **响应**: 最近收藏的文章列表

### 4.6 按标签查询收藏
- **接口**: `GET /favorites/tags/{tagId}`
- **描述**: 查询带有指定标签的收藏文章
- **权限**: 需要认证
- **路径参数**: `tagId` - 标签ID
- **请求参数**:
  - `page`: int，页码（默认0）
  - `size`: int，每页大小（默认10）
- **响应**: 带指定标签的收藏文章列表

### 4.7 搜索收藏
- **接口**: `GET /favorites/search`
- **描述**: 搜索收藏的文章
- **权限**: 需要认证
- **请求参数**:
  - `keyword`: string，搜索关键词
  - `page`: int，页码（默认0）
  - `size`: int，每页大小（默认10）
- **响应**: 符合搜索条件的收藏文章列表

### 4.8 获取收藏数量
- **接口**: `GET /favorites/count`
- **描述**: 获取收藏文章的总数
- **权限**: 需要认证
- **响应**:
```json
{
  "success": true,
  "data": {
    "count": 42
  }
}
```

---

## 5. 笔记管理接口 (NoteController)

### 5.1 创建笔记
- **接口**: `POST /notes`
- **描述**: 创建新的笔记
- **权限**: 需要认证
- **请求参数**:
```json
{
  "title": "string",
  "content": "string",
  "articleId": "string",
  "tags": ["tag1", "tag2"]
}
```
- **响应**:
```json
{
  "success": true,
  "data": {
    "id": "string",
    "title": "string",
    "content": "string",
    "articleId": "string",
    "tags": ["tag1", "tag2"],
    "createdAt": "timestamp",
    "updatedAt": "timestamp"
  }
}
```

### 5.2 更新笔记
- **接口**: `PUT /notes/{noteId}`
- **描述**: 更新指定的笔记
- **权限**: 需要认证
- **路径参数**: `noteId` - 笔记ID
- **请求参数**: 笔记更新信息
- **响应**: 更新后的笔记信息

### 5.3 删除笔记
- **接口**: `DELETE /notes/{noteId}`
- **描述**: 删除指定的笔记
- **权限**: 需要认证
- **路径参数**: `noteId` - 笔记ID
- **响应**:
```json
{
  "success": true,
  "data": {
    "deleted": true
  }
}
```

### 5.4 获取笔记详情
- **接口**: `GET /notes/{noteId}`
- **描述**: 根据ID获取笔记
- **权限**: 需要认证
- **路径参数**: `noteId` - 笔记ID
- **响应**: 笔记详细信息

### 5.5 获取用户笔记
- **接口**: `GET /notes`
- **描述**: 获取当前用户的所有笔记
- **权限**: 需要认证
- **请求参数**:
  - `page`: int，页码（默认0）
  - `size`: int，每页大小（默认20）
- **响应**: 用户笔记列表

### 5.6 获取文章笔记
- **接口**: `GET /notes/article/{articleId}`
- **描述**: 获取指定文章的所有笔记
- **权限**: 需要认证
- **路径参数**: `articleId` - 文章ID
- **请求参数**:
  - `page`: int，页码（默认0）
  - `size`: int，每页大小（默认20）
- **响应**: 文章相关的笔记列表

### 5.7 获取标签笔记
- **接口**: `GET /notes/tag/{tagId}`
- **描述**: 获取指定标签下的所有笔记
- **权限**: 需要认证
- **路径参数**: `tagId` - 标签ID
- **请求参数**:
  - `page`: int，页码（默认0）
  - `size`: int，每页大小（默认20）
- **响应**: 标签下的笔记列表

### 5.8 搜索笔记
- **接口**: `GET /notes/search`
- **描述**: 根据关键词搜索笔记
- **权限**: 需要认证
- **请求参数**:
  - `keyword`: string，搜索关键词
  - `page`: int，页码（默认0）
  - `size`: int，每页大小（默认20）
- **响应**: 符合搜索条件的笔记列表

### 5.9 获取用户标签
- **接口**: `GET /notes/tags`
- **描述**: 获取当前用户的所有标签
- **权限**: 需要认证
- **响应**: 用户标签列表

### 5.10 创建标签
- **接口**: `POST /notes/tags`
- **描述**: 创建新的标签
- **权限**: 需要认证
- **请求参数**:
```json
{
  "name": "string",
  "color": "string",
  "description": "string"
}
```
- **响应**: 创建的标签信息

### 5.11 更新标签
- **接口**: `PUT /notes/tags/{tagId}`
- **描述**: 更新指定的标签
- **权限**: 需要认证
- **路径参数**: `tagId` - 标签ID
- **请求参数**: 标签更新信息
- **响应**: 更新后的标签信息

### 5.12 删除标签
- **接口**: `DELETE /notes/tags/{tagId}`
- **描述**: 删除指定的标签
- **权限**: 需要认证
- **路径参数**: `tagId` - 标签ID
- **响应**:
```json
{
  "success": true,
  "data": {
    "deleted": true
  }
}
```

### 5.13 为笔记添加标签
- **接口**: `POST /notes/{noteId}/tags`
- **描述**: 为笔记添加标签
- **权限**: 需要认证
- **路径参数**: `noteId` - 笔记ID
- **请求参数**: 标签ID集合
- **响应**: 更新后的笔记信息

### 5.14 从笔记移除标签
- **接口**: `DELETE /notes/{noteId}/tags`
- **描述**: 从笔记中移除标签
- **权限**: 需要认证
- **路径参数**: `noteId` - 笔记ID
- **请求参数**: 标签ID集合
- **响应**: 更新后的笔记信息

---

## 6. 知识图谱接口 (KnowledgeController)

### 6.1 获取文章相关项
- **接口**: `GET /api/v1/knowledge/related-items/article/{articleId}`
- **描述**: 获取与文章相关的内容项
- **权限**: 需要USER角色
- **路径参数**: `articleId` - 文章ID
- **请求参数**: `limit` - 返回条数限制（默认10）
- **响应**: 相关内容项列表

### 6.2 获取笔记相关项
- **接口**: `GET /api/v1/knowledge/related-items/note/{noteId}`
- **描述**: 获取与笔记相关的内容项
- **权限**: 需要USER角色
- **路径参数**: `noteId` - 笔记ID
- **请求参数**: `limit` - 返回条数限制（默认10）
- **响应**: 相关内容项列表

### 6.3 获取概念相关项
- **接口**: `GET /api/v1/knowledge/related-items/concept/{conceptId}`
- **描述**: 获取与概念相关的内容项
- **权限**: 需要USER角色
- **路径参数**: `conceptId` - 概念ID
- **请求参数**: `limit` - 返回条数限制（默认10）
- **响应**: 相关内容项列表

### 6.4 获取知识图谱可视化数据
- **接口**: `GET /api/v1/knowledge/graph`
- **描述**: 获取知识图谱可视化数据
- **权限**: 需要USER角色
- **请求参数**:
  - `centerNodeId`: string，中心节点ID
  - `nodeType`: string，节点类型（article、note、concept）
  - `depth`: int，图谱深度（默认2）
- **响应**:
```json
{
  "success": true,
  "data": {
    "nodes": [
      {
        "id": "string",
        "type": "string",
        "label": "string",
        "properties": {}
      }
    ],
    "edges": [
      {
        "source": "string",
        "target": "string",
        "type": "string",
        "weight": 1.0
      }
    ]
  }
}
```

### 6.5 搜索概念
- **接口**: `GET /api/v1/knowledge/concepts/search`
- **描述**: 搜索概念
- **权限**: 需要USER角色
- **请求参数**:
  - `query`: string，搜索关键词
  - `limit`: int，返回条数限制（默认10）
- **响应**: 概念列表

### 6.6 为文章添加概念
- **接口**: `POST /api/v1/knowledge/article/{articleId}/concepts`
- **描述**: 为文章手动添加概念
- **权限**: 需要USER角色
- **路径参数**: `articleId` - 文章ID
- **请求参数**: 概念名称列表
- **响应**:
```json
{
  "success": true,
  "message": "成功添加 5 个概念关联",
  "data": 5
}
```

### 6.7 为笔记添加概念
- **接口**: `POST /api/v1/knowledge/note/{noteId}/concepts`
- **描述**: 为笔记手动添加概念
- **权限**: 需要USER角色
- **路径参数**: `noteId` - 笔记ID
- **请求参数**: 概念名称列表
- **响应**: 添加的概念关联数量

### 6.8 从文章构建知识图谱
- **接口**: `POST /api/v1/knowledge/build/article/{articleId}`
- **描述**: 从文章构建知识图谱
- **权限**: 需要USER角色
- **路径参数**: `articleId` - 文章ID
- **响应**:
```json
{
  "success": true,
  "message": "知识图谱构建成功",
  "data": true
}
```

### 6.9 从笔记构建知识图谱
- **接口**: `POST /api/v1/knowledge/build/note/{noteId}`
- **描述**: 从笔记构建知识图谱
- **权限**: 需要USER角色
- **路径参数**: `noteId` - 笔记ID
- **响应**: 构建结果

### 6.10 获取知识图谱统计信息
- **接口**: `GET /api/v1/knowledge/statistics`
- **描述**: 获取知识图谱统计信息
- **权限**: 需要USER角色
- **响应**:
```json
{
  "success": true,
  "data": {
    "totalNodes": 1000,
    "totalRelationships": 2500,
    "concepts": 300,
    "articles": 400,
    "notes": 300
  }
}
```

---

## 7. 测试接口 (TestController)

### 7.1 测试数据库连接
- **接口**: `GET /test/db`
- **描述**: 测试数据库连接和数据
- **响应**:
```json
{
  "status": "success",
  "mysqlConnection": "ok",
  "userCount": 10,
  "roleCount": 2,
  "adminUser": {
    "id": "string",
    "email": "admin@example.com",
    "roles": ["ADMIN"]
  }
}
```

### 7.2 测试安全配置
- **接口**: `GET /test/secured`
- **描述**: 测试需要管理员权限的资源访问
- **权限**: 需要ADMIN角色
- **响应**:
```json
{
  "status": "success",
  "message": "您已成功访问需要管理员权限的资源"
}
```

---

## 错误码说明

### HTTP状态码
- `200` - 请求成功
- `201` - 创建成功
- `400` - 请求参数错误
- `401` - 未认证
- `403` - 权限不足
- `404` - 资源未找到
- `500` - 服务器内部错误

### 业务错误响应格式
```json
{
  "success": false,
  "message": "具体的错误描述",
  "data": null
}
```

---

## 分页参数说明

大部分列表查询接口都支持分页，通用参数：
- `page`: 页码，从0开始（默认0）
- `size`: 每页大小（默认值因接口而异）

分页响应格式：
```json
{
  "success": true,
  "data": {
    "content": [], // 数据列表
    "totalElements": 100, // 总记录数
    "totalPages": 10, // 总页数
    "size": 10, // 每页大小
    "number": 0, // 当前页码
    "first": true, // 是否第一页
    "last": false // 是否最后一页
  }
}
```

---

## 安全说明

1. **JWT令牌**: 系统使用JWT进行身份认证，令牌有效期为1小时
2. **刷新令牌**: 访问令牌过期后可使用刷新令牌获取新的访问令牌
3. **角色权限**: 系统支持基于角色的访问控制（RBAC）
4. **数据隔离**: 用户只能访问自己的数据（笔记、收藏等）

---

## 部署说明

### 环境要求
- Java 17+
- MySQL 8.0+
- MongoDB 4.4+
- Neo4j 4.4+
- Redis 6.0+
- RabbitMQ 3.8+

### 配置文件
主要配置在 `application.yml` 中，包括：
- 数据库连接配置
- JWT密钥配置
- Spring AI配置
- Redis配置
- RabbitMQ配置

### 启动命令
```bash
# 开发环境
mvn spring-boot:run

# 生产环境
java -jar ai-reader-backend-0.0.1-SNAPSHOT.jar
```

---

## 更新日志

### v0.0.1-SNAPSHOT
- 初始版本
- 实现基础的用户认证功能
- 实现RSS源管理功能
- 实现文章管理功能
- 实现笔记管理功能
- 实现收藏功能
- 实现知识图谱功能
- 集成Spring AI支持

# 博客管理系统 API 文档

## 项目概述

这是一个基于Spring Boot的博客管理系统，提供用户认证、博客管理、图片上传、LLM集成等功能。

### 技术栈

- **框架**: Spring Boot 3.4.5
- **数据库**: MySQL + Spring Data JPA
- **安全**: Spring Security + JWT
- **文件上传**: MultipartFile
- **LLM集成**: 支持XModel和BigModel
- **Java版本**: 17

### 服务器配置

- **端口**: 5200
- **基础URL**: `http://localhost:5200`

---

## API 接口文档

### 1. 用户认证模块 (`/api/auth`)

#### 1.1 用户注册

- **URL**: `POST /api/auth/register`
- **描述**: 注册新用户
- **请求体**:

```json
{
  "username": "string", // 用户名
  "email": "string", // 邮箱
  "password": "string" // 密码
}
```

- **响应**:

```json
{
  "message": "用户注册成功!"
}
```

- **错误响应**:

```json
{
  "message": "错误: 用户名已被使用!"
}
```

#### 1.2 用户登录

- **URL**: `POST /api/auth/login`
- **描述**: 用户登录获取JWT令牌
- **请求体**:

```json
{
  "username": "string", // 用户名
  "password": "string" // 密码
}
```

- **响应**:

```json
{
  "accessToken": "jwt_token_string",
  "tokenType": "Bearer",
  "id": 1,
  "username": "用户名",
  "email": "邮箱地址"
}
```

---

### 2. 博客管理模块 (`/api/blogs`)

#### 2.1 获取博客列表

- **URL**: `GET /api/blogs/lists`
- **描述**: 获取当前用户的所有博客
- **响应**:

```json
{
  "status": 0,
  "data": [
    {
      "id": 1,
      "title": "博客标题",
      "filename": "文件名.md",
      "categories": "分类",
      "tags": ["标签1", "标签2"],
      "saying": "格言",
      "dateTime": "2025-06-09T10:30:00",
      "content": "博客内容"
    }
  ],
  "error": null
}
```

#### 2.2 根据ID获取博客

- **URL**: `GET /api/blogs/{id}`
- **描述**: 获取指定ID的博客详情
- **路径参数**:
  - `id`: 博客ID
- **响应**:

```json
{
  "status": 0,
  "data": {
    "id": 1,
    "title": "博客标题",
    "filename": "文件名.md",
    "categories": "分类",
    "tags": ["标签1", "标签2"],
    "saying": "格言",
    "dateTime": "2025-06-09T10:30:00",
    "content": "博客内容"
  },
  "error": null
}
```

#### 2.3 创建博客

- **URL**: `POST /api/blogs`
- **描述**: 创建新的博客
- **请求体**:

```json
{
  "title": "博客标题",
  "categories": "分类",
  "tags": ["标签1", "标签2"],
  "saying": "格言",
  "content": "博客内容"
}
```

- **响应**:

```json
{
  "status": 0,
  "data": null,
  "error": null
}
```

#### 2.4 更新博客

- **URL**: `PUT /api/blogs/{id}`
- **描述**: 更新指定ID的博客
- **路径参数**:
  - `id`: 博客ID
- **请求体**:

```json
{
  "title": "更新的标题",
  "categories": "更新的分类",
  "tags": ["更新的标签"],
  "saying": "更新的格言",
  "content": "更新的内容"
}
```

- **响应**:

```json
{
  "status": 0,
  "data": null,
  "error": null
}
```

#### 2.5 删除博客

- **URL**: `DELETE /api/blogs/{id}`
- **描述**: 删除指定ID的博客
- **路径参数**:
  - `id`: 博客ID
- **响应**:

```json
{
  "status": 0,
  "data": null,
  "error": null
}
```

#### 2.6 搜索博客

- **URL**: `GET /api/blogs/search`
- **描述**: 根据条件搜索博客
- **查询参数**:
  - `title`: 标题关键字（可选）
  - `categories`: 分类（可选）
  - `tags`: 标签（可选）
- **响应**:

```json
{
  "status": 0,
  "data": [
    {
      "id": 1,
      "title": "博客标题",
      "categories": "分类",
      "tags": ["标签1"],
      "dateTime": "2025-06-09T10:30:00"
    }
  ],
  "error": null
}
```

---

### 3. 图片管理模块 (`/image`)

#### 3.1 获取图片

- **URL**: `GET /image/{*relativePath}`
- **描述**: 根据相对路径获取图片资源
- **路径参数**:
  - `relativePath`: 图片的相对路径
- **示例**: `GET /image/folder/image.png`
- **响应**: 返回图片文件流

#### 3.2 上传图片

- **URL**: `POST /image/upload`
- **描述**: 上传图片到指定路径
- **请求参数**:
  - `file`: MultipartFile - 图片文件
  - `relativePath`: String - 保存的相对路径
- **响应**:

```json
{
  "success": true,
  "message": "图片上传成功",
  "path": "相对路径/文件名.jpg"
}
```

---

### 4. 配置管理模块 (`/config`)

#### 4.1 获取配置

- **URL**: `GET /config/get`
- **描述**: 获取系统配置信息
- **响应**:

```json
{
  "blogStoragePath": "博客存储路径",
  "imageStoragePath": "图片存储路径",
  "xmodelAPIKey": "XModel API密钥",
  "bigmodelAPIKey": "BigModel API密钥"
}
```

#### 4.2 更新配置

- **URL**: `POST /config/set`
- **描述**: 更新系统配置
- **请求体**:

```json
{
  "blogStoragePath": "博客存储路径",
  "imageStoragePath": "图片存储路径",
  "xmodelAPIKey": "XModel API密钥",
  "bigmodelAPIKey": "BigModel API密钥"
}
```

- **响应**: `boolean` - 是否更新成功

---

### 5. LLM集成模块 (`/llm`)

#### 5.1 设置LLM类型

- **URL**: `POST /llm/set`
- **描述**: 设置要使用的LLM类型
- **查询参数**:
  - `llmType`: LLM类型 ("XModel" 或 "BigModel")
- **响应**: 无返回值

#### 5.2 获取当前LLM

- **URL**: `GET /llm/get`
- **描述**: 获取当前设置的LLM类型
- **响应**: `string` - LLM类名或"null"

#### 5.3 获取建议

- **URL**: `GET /llm/getsuggestion`
- **描述**: 基于输入获取LLM建议
- **查询参数**:
  - `param`: 输入参数
- **响应**: `string` - LLM生成的建议内容

#### 5.4 聊天对话

- **URL**: `GET /llm/chat`
- **描述**: 与LLM进行聊天对话
- **查询参数**:
  - `param`: 对话内容
- **响应**: `string` - LLM的回复

---

## 数据模型

### User (用户)

```json
{
  "id": "Long - 用户ID",
  "username": "String - 用户名 (3-50字符)",
  "email": "String - 邮箱地址",
  "password": "String - 密码 (最少6位)",
  "createdAt": "LocalDateTime - 创建时间",
  "updatedAt": "LocalDateTime - 更新时间",
  "role": "Role - 用户角色 (USER/ADMIN)"
}
```

### Blog (博客)

```json
{
  "id": "Long - 博客ID",
  "filepath": "Path - 文件路径",
  "filename": "String - 文件名",
  "title": "String - 标题",
  "dateTime": "LocalDateTime - 日期时间",
  "categories": "String - 分类",
  "tags": "String[] - 标签数组",
  "saying": "String - 格言",
  "content": "String - 内容"
}
```

### Message (响应消息)

```json
{
  "status": "int - 状态码 (0成功, 1失败)",
  "data": "Object - 数据内容",
  "error": "String - 错误信息"
}
```

---

## 认证说明

### JWT认证

大部分API需要JWT认证，在请求头中添加：

```
Authorization: Bearer <jwt_token>
```

### 跨域配置

- 认证相关接口支持跨域访问
- 允许的源：`*`
- 最大缓存时间：3600秒

---

## 错误处理

### 状态码说明

- `0`: 操作成功
- `1`: 操作失败

### 常见错误

- **401 Unauthorized**: 未授权访问
- **400 Bad Request**: 请求参数错误
- **404 Not Found**: 资源不存在
- **500 Internal Server Error**: 服务器内部错误

---

## 部署配置

### 数据库配置

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/blog_manager
spring.datasource.username=root
spring.datasource.password=password
```

### 文件上传限制

- 最大文件大小：10MB
- 最大请求大小：10MB

### JWT配置

- 密钥：可在 `application.properties`中配置
- 过期时间：24小时（86400000毫秒）

---

## 使用示例

### 完整的API调用流程

1. **用户注册**

```bash
curl -X POST http://localhost:5200/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"123456"}'
```

2. **用户登录**

```bash
curl -X POST http://localhost:5200/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

3. **创建博客（需要JWT Token）**

```bash
curl -X POST http://localhost:5200/api/blogs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt_token>" \
  -d '{"title":"我的第一篇博客","categories":"技术","tags":["Java","Spring"],"saying":"学而时习之","content":"这是我的第一篇博客内容"}'
```

4. **上传图片**

```bash
curl -X POST http://localhost:5200/image/upload \
  -F "file=@image.jpg" \
  -F "relativePath=blog/images"
```

---

## 注意事项

1. **安全性**: 生产环境中请更改默认的JWT密钥
2. **文件路径**: 图片和博客存储路径需要在配置中正确设置
3. **数据库**: 确保MySQL服务运行并创建了 `blog_manager`数据库
4. **LLM API**: 使用LLM功能前需要配置相应的API密钥
5. **跨域**: 当前配置允许所有源的跨域访问，生产环境中建议限制具体域名

---

_最后更新时间：2025年6月9日_

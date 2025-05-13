# 博客管理系统API文档

本文档详细说明了博客管理系统的所有API端点、请求参数和返回数据格式。

## 通用说明

所有API返回的数据格式为JSON，结构如下：

```json
{
  "status": 0,    // 状态码，0表示成功，1表示失败
  "data": object, // 返回的数据对象，失败时为null
  "error": string // 错误信息，成功时为null
}
```

## API列表

### 1. 获取所有博客列表

获取系统中所有博客的基本信息列表。

- **URL**: `/api/blogs/lists`
- **方法**: GET
- **参数**: 无
- **成功响应**:
  ```json
  {
    "status": 0,
    "data": [
      {
        "title": "博客标题",
        "filename": "博客文件名.md",
        "categories": "分类",
        "tags": ["标签1", "标签2"],
        "saying": "摘要",
        "date": "2023-01-01 12:00:00"
      },
      // 更多博客...
    ],
    "error": null
  }
  ```
- **失败响应**:
  ```json
  {
    "status": 1,
    "data": null,
    "error": "获取博客文件名失败"
  }
  ```

### 2. 获取博客详情

根据日期和文件名获取特定博客的完整内容。

- **URL**: `/api/blogs/{year}/{month}/{day}/{filename}/`
- **方法**: GET
- **参数**:
  - `year`: 博客发布年份
  - `month`: 博客发布月份
  - `day`: 博客发布日期
  - `filename`: 博客文件名
- **成功响应**:
  ```json
  {
    "status": 0,
    "data": {
      "title": "博客标题",
      "filename": "博客文件名.md",
      "filepath": "博客文件路径",
      "categories": "分类",
      "tags": ["标签1", "标签2"],
      "saying": "摘要",
      "date": "2023-01-01 12:00:00",
      "content": "博客完整内容..."
    },
    "error": null
  }
  ```
- **失败响应**:
  ```json
  {
    "status": 1,
    "data": null,
    "error": "未找到博客"
  }
  ```

### 3. 更新博客信息

更新博客的基本信息（标题、分类、标签、摘要）。

- **URL**: `/api/blogs/{year}/{month}/{day}/{filename}/updateinfo`
- **方法**: GET
- **参数**:
  - `year`: 博客发布年份
  - `month`: 博客发布月份
  - `day`: 博客发布日期
  - `filename`: 博客文件名
  - `title`: 新的博客标题
  - `categories`: 新的博客分类
  - `tags`: 新的博客标签数组
  - `saying`: 新的博客摘要
- **成功响应**:
  ```json
  {
    "status": 0,
    "data": null,
    "error": "更新博客成功"
  }
  ```
- **失败响应**:
  ```json
  {
    "status": 1,
    "data": null,
    "error": "更新博客失败"
  }
  ```

### 4. 更新博客内容

更新特定博客的正文内容。

- **URL**: `/api/blogs/{year}/{month}/{day}/{filename}/updatecontent`
- **方法**: POST
- **参数**:
  - `year`: 博客发布年份
  - `month`: 博客发布月份
  - `day`: 博客发布日期
  - `filename`: 博客文件名
  - 请求体: 新的博客内容文本
- **成功响应**:
  ```json
  {
    "status": 0,
    "data": null,
    "error": "更新博客内容成功"
  }
  ```
- **失败响应**:
  ```json
  {
    "status": 1,
    "data": null,
    "error": "更新博客内容失败"
  }
  ```

### 5. 添加新博客

创建一个新的博客。

- **URL**: `/api/blogs/add`
- **方法**: GET
- **参数**:
  - `title`: 博客标题
  - `categories`: 博客分类
  - `tags`: 博客标签数组
  - `saying`: 博客摘要
- **成功响应**:
  ```json
  {
    "status": 0,
    "data": null,
    "error": "添加博客成功"
  }
  ```
- **失败响应**:
  ```json
  {
    "status": 1,
    "data": null,
    "error": "添加博客失败"
  }
  ```

### 6. 删除博客

删除特定的博客。

- **URL**: `/api/blogs/{year}/{month}/{day}/{filename}/delete/`
- **方法**: GET
- **参数**:
  - `year`: 博客发布年份
  - `month`: 博客发布月份
  - `day`: 博客发布日期
  - `filename`: 博客文件名
- **成功响应**:
  ```json
  {
    "status": 0,
    "data": null,
    "error": "删除博客成功"
  }
  ```
- **失败响应**:
  ```json
  {
    "status": 1,
    "data": null,
    "error": "删除博客失败"
  }
  ```

### 7. 获取图片资源

根据相对路径获取图片资源，直接返回图片文件而不是JSON。

- **URL**: `/image/{relativePath}`
- **方法**: GET
- **参数**:
  - `relativePath`: 图片的相对路径，例如 "image/计算机网络第五章--网络层_20250425_151005/1745565448467.png"
- **成功响应**:
  - 直接返回图片文件，Content-Type 会根据图片类型自动设置（如 image/png, image/jpeg 等）
- **失败响应**:
  - HTTP 404 Not Found: 如果图片不存在
  - HTTP 400 Bad Request: 如果请求处理过程中出现异常

### 8. 上传图片

将图片上传到服务器的指定相对路径中。

- **URL**: `/image/upload`
- **方法**: POST
- **Content-Type**: `multipart/form-data`
- **参数**:
  - `file`: 要上传的图片文件（表单文件字段）
  - `relativePath`: 保存图片的相对路径，如 "计算机网络第五章--网络层_20250425_151005"
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "图片上传成功",
    "path": "计算机网络第五章--网络层_20250425_151005/example.png"
  }
  ```
- **失败响应**:
  ```json
  {
    "success": false,
    "message": "上传图片失败: 错误信息"
  }
  ```

### 9. 获取系统配置

获取系统的当前配置信息。

- **URL**: `/config/get`
- **方法**: GET
- **参数**: 无
- **成功响应**:
  ```json
  {
    "blogStoragePath": "/path/to/blog/storage",
    "imageStoragePath": "/path/to/image/storage",
    "XModelAPIKey": "api-key-value"
  }
  ```
- **失败响应**: 
  - HTTP 500 Internal Server Error: 如果在获取配置过程中出现异常

### 10. 更新系统配置

更新系统的配置信息。

- **URL**: `/config/set`
- **方法**: POST
- **Content-Type**: `application/json`
- **请求体**:
  ```json
  {
    "blogStoragePath": "/new/path/to/blog/storage",
    "imageStoragePath": "/new/path/to/image/storage",
    "XModelAPIKey": "new-api-key-value"
  }
  ```
- **成功响应**: 
  - `true`: 配置更新成功
  - `false`: 配置更新失败
- **失败响应**: 
  - HTTP 400 Bad Request: 如果请求体格式不正确
  - HTTP 500 Internal Server Error: 如果在更新配置过程中出现异常

### 11. 设置LLM类型

根据提供的类型设置当前使用的LLM模型。

- **URL**: `/llm/set`
- **方法**: GET
- **参数**:
  - `llmType`: LLM的类型 (例如: "XModel", "BigModel")
- **成功响应**: 无特定响应内容，HTTP 200 OK
- **失败响应**:
  - HTTP 500 Internal Server Error: 如果LLM类型不受支持或设置失败。

### 12. 获取当前LLM类型

获取当前正在使用的LLM模型的名称。

- **URL**: `/llm/get`
- **方法**: GET
- **参数**: 无
- **成功响应**:
  ```json
  {
    "status": 0, // 假设成功时返回类似结构，或者直接返回字符串
    "data": "XModel", // 当前LLM的名称，例如 "XModel" 或 "BigModel"
    "error": null
  }
  ```
  或者直接返回: `XModel`
- **失败响应**:
  - `null` 或错误信息字符串，如果LLM未设置。

### 13. 获取LLM建议（非流式）

向LLM发送提示并获取非流式响应的建议。

- **URL**: `/llm/getsuggestion`
- **方法**: GET
- **参数**:
  - `param`: 用户输入的内容或提示
- **成功响应**: 字符串形式的LLM建议
- **失败响应**: 错误信息字符串，例如 "LLM 实例未设置" 或 "处理请求时发生错误: ..."


### 14. 与LLM聊天（非流式）

向LLM发送聊天内容并获取非流式响应。

- **URL**: `/llm/chat`
- **方法**: GET
- **参数**:
  - `param`: 用户输入的聊天内容
- **成功响应**: 字符串形式的LLM聊天回复
- **失败响应**: 错误信息字符串，例如 "LLM 实例未设置" 或 "处理请求时发生错误: ..."


### 15. 与LLM聊天（流式）

通过Server-Sent Events (SSE)与LLM进行流式聊天。

- **URL**: `/llm/stream-chat`
- **方法**: GET
- **参数**:
  - `param`: 用户输入的聊天内容
- **成功响应**: SSE事件流。事件类型包括:
  - `start`: 表示请求开始处理，数据为 "开始处理请求"
  - `chunk`: 表示LLM返回的数据块，数据为实际内容
  - `end`: 表示LLM响应结束，数据为最后一块内容
  - `error`: 表示发生错误，数据为错误信息
- **失败响应**: 如果LLM未设置，初始会返回错误。在流处理过程中也可能通过SSE发送错误事件。


### 16. 获取LLM建议（流式）

通过Server-Sent Events (SSE)获取LLM的流式建议。

- **URL**: `/llm/stream-suggestion`
- **方法**: GET
- **参数**:
  - `param`: 用户输入的内容或提示
- **成功响应**: SSE事件流。事件类型包括:
  - `start`: 表示请求开始处理，数据为 "开始处理请求"
  - `chunk`: 表示LLM返回的数据块，数据为实际内容
  - `end`: 表示LLM响应结束，数据为最后一块内容
  - `error`: 表示发生错误，数据为错误信息
- **失败响应**: 如果LLM未设置，初始会返回错误。在流处理过程中也可能通过SSE发送错误事件。

## 示例

### 获取博客列表

```
GET /api/blogs/lists
```

### 获取特定博客

```
GET /api/blogs/2023/12/25/我的博客_20231225_120000.md/
```

### 更新博客信息

```
GET /api/blogs/2023/12/25/我的博客_20231225_120000.md/updateinfo?title=新标题&categories=技术&tags=Java&tags=Spring&saying=这是一个摘要
```

### 添加新博客

```
GET /api/blogs/add?title=新博客&categories=技术分享&tags=Java&tags=编程&saying=这是一篇关于Java的博客
```

### 删除博客

```
GET /api/blogs/2023/12/25/我的博客_20231225_120000.md/delete/
```

### 获取图片资源

```
GET /api/计算机网络第五章--网络层_20250425_151005/1745565448467.png
```

### 上传图片

```
POST /image/upload
Content-Type: multipart/form-data

Form fields:
- file: [图片文件]
- relativePath: 计算机网络第五章--网络层_20250425_151005
```

### 获取系统配置

```
GET /config/get
```

### 更新系统配置

```
POST /config/set
Content-Type: application/json

{
  "blogStoragePath": "D:/blog_storage",
  "imageStoragePath": "D:/image_storage",
  "XModelAPIKey": "abc123def456"
}
```

### 设置LLM类型

```
GET /llm/set?llmType=XModel
```

### 获取当前LLM类型

```
GET /llm/get
```

### 获取LLM建议（非流式）

```
GET /llm/getsuggestion?param=请帮我润色一下这段文字
```

### 与LLM聊天（非流式）

```
GET /llm/chat?param=你好，你叫什么名字
```

### 与LLM聊天（流式）

```
GET /llm/stream-chat?param=讲一个笑话
```

### 获取LLM建议（流式）

```
GET /llm/stream-suggestion?param=针对这个主题给我一些写作建议
```

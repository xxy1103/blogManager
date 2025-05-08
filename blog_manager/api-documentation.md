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

- **URL**: `/api/{relativePath}`
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

- **URL**: `/api/upload`
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
POST /api/upload
Content-Type: multipart/form-data

Form fields:
- file: [图片文件]
- relativePath: 计算机网络第五章--网络层_20250425_151005
```

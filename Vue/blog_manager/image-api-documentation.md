# 图片 API 文档

## 概述

本部分 API 用于管理和访问图片资源。图片数据直接存储在数据库中。

## 端点

### 1. 上传图片

*   **HTTP 方法:** `POST`
*   **URL:** `/image/upload`
*   **描述:** 上传一张图片到服务器并将其存储在数据库中。
*   **请求格式:** `multipart/form-data`
*   **参数:**
    *   `file`: (必需) 要上传的图片文件。
*   **成功响应 (200 OK):**
    ```json
    {
        "success": true,
        "message": "图片上传成功",
        "imageId": 123, // 图片在数据库中的ID
        "fileName": "example.png" // 上传的图片文件名
    }
    ```
*   **失败响应 (500 Internal Server Error):**
    ```json
    {
        "success": false,
        "message": "上传图片失败: [错误详情]"
    }
    ```
*   **示例 (curl):**
    ```bash
    curl -X POST -F "file=@/path/to/your/image.png" http://localhost:8080/image/upload
    ```

### 2. 获取图片

*   **HTTP 方法:** `GET`
*   **URL:** `/image/{filename}`
*   **描述:** 根据文件名从数据库中检索图片。
*   **路径参数:**
    *   `filename`: (必需) 要检索的图片的名称 (例如: `example.png`)。
*   **成功响应 (200 OK):**
    *   **Headers:**
        *   `Content-Type`: 图片的 MIME 类型 (例如: `image/png`, `image/jpeg`)
        *   `Content-Disposition`: `attachment; filename="<filename>"`
    *   **Body:** 图片的原始二进制数据。
*   **失败响应 (404 Not Found):**
    *   当具有指定文件名的图片在数据库中不存在时返回。
*   **示例 (浏览器或 curl):**
    ```
    http://localhost:8080/image/example.png
    ```
    或者
    ```bash
    curl -o downloaded_image.png http://localhost:8080/image/example.png
    ```


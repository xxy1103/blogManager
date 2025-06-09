# LLM 流式 API 使用文档

本文档介绍了如何使用博客管理系统提供的 LLM 流式 API。这些 API 允许客户端通过 Server-Sent Events (SSE) 接收来自大语言模型的实时响应。

## 端点

系统提供以下两个流式 API 端点：

1.  **流式聊天 (Stream Chat)**
    *   **URL:** `/llm/stream-chat`
    *   **方法:** `GET`
    *   **描述:** 此端点用于与 LLM进行流式聊天。用户发送一个问题或提示，LLM 会以数据块的形式逐步返回响应。
    *   **查询参数:**
        *   `param` (string, required): 用户输入的聊天内容或问题。
    *   **响应类型:** `text/event-stream`
    *   **事件流:**
        *   `start`: 表示请求已开始处理。`data` 字段包含 "开始处理请求"。
        *   `chunk`: 表示 LLM 返回的一个数据块。`data` 字段包含实际的文本内容。
        *   `end`: 表示 LLM 已完成所有响应。`data` 字段包含最后一块文本内容。
        *   `error`: 表示在处理过程中发生错误。`data` 字段包含错误信息。

2.  **流式生成建议 (Stream Suggestion)**
    *   **URL:** `/llm/stream-suggestion`
    *   **方法:** `GET`
    *   **描述:** 此端点用于从 LLM 流式获取建议。用户提供一些上下文或输入，LLM 会以数据块的形式逐步返回相关的建议。
    *   **查询参数:**
        *   `param` (string, required): 用户输入的内容，用于 LLM 生成建议。
    *   **响应类型:** `text/event-stream`
    *   **事件流:**
        *   `start`: 表示请求已开始处理。`data` 字段包含 "开始处理请求"。
        *   `chunk`: 表示 LLM 返回的一个数据块。`data` 字段包含实际的文本内容。
        *   `end`: 表示 LLM 已完成所有响应。`data` 字段包含最后一块文本内容。
        *   `error`: 表示在处理过程中发生错误。`data` 字段包含错误信息。

## 使用前提

在使用这些流式 API 之前，需要先通过 `/llm/set` 端点设置要使用的 LLM 类型。例如，要使用 "XModel"，可以发送一个 GET 请求到 `/llm/set?llmType=XModel`。

## 如何连接

客户端可以使用支持 Server-Sent Events 的库或原生 JavaScript `EventSource` API 来连接这些端点。

### JavaScript 示例 (`EventSource`)

```javascript
const llmType = 'XModel'; // 或者 'BigModel'
const userInput = '你好，请介绍一下自己。'; // 用户输入

// 1. 设置 LLM 类型 (如果尚未设置)
fetch(`/llm/set?llmType=${encodeURIComponent(llmType)}`)
  .then(response => {
    if (!response.ok) {
      console.error('设置 LLM 类型失败:', response.statusText);
      return;
    }
    console.log(`LLM 类型已设置为: ${llmType}`);

    // 2. 连接到流式聊天端点
    const eventSource = new EventSource(`/llm/stream-chat?param=${encodeURIComponent(userInput)}`);

    eventSource.addEventListener('start', (event) => {
      console.log('连接开始:', event.data);
      // 可以在这里更新 UI，例如显示 "正在连接..."
    });

    eventSource.addEventListener('chunk', (event) => {
      console.log('收到数据块:', event.data);
      // 将 event.data 追加到聊天窗口
    });

    eventSource.addEventListener('end', (event) => {
      console.log('流结束:', event.data);
      // 将 event.data 追加到聊天窗口
      eventSource.close(); // 关闭连接
      // 可以在这里更新 UI，例如移除 "正在输入..." 指示
    });

    eventSource.addEventListener('error', (event) => {
      console.error('发生错误:', event);
      let errorMessage = '未知错误';
      if (event.data) {
        errorMessage = event.data;
      } else if (event.target && event.target.readyState === EventSource.CLOSED) {
        errorMessage = '连接已关闭。';
      }
      // 显示错误信息
      eventSource.close(); // 关闭连接
    });

  })
  .catch(error => {
    console.error('设置 LLM 类型时发生网络错误:', error);
  });

// 对于流式建议，只需更改 EventSource 的 URL:
// const eventSourceSuggestion = new EventSource(`/llm/stream-suggestion?param=${encodeURIComponent(userInput)}`);
// 其余事件处理逻辑类似。
```

## 注意事项

*   **LLM 实例:** 确保在调用流式 API 之前，已经通过 `/llm/set` 成功设置了 LLM 实例。如果 LLM 实例未设置，API 将返回错误。
*   **超时:** `SseEmitter` 的默认超时时间设置为 1 小时。如果 LLM 处理时间过长，连接可能会超时。
*   **错误处理:** 客户端应妥善处理 `error` 事件，并在发生错误或连接关闭时采取适当的措施。
*   **并发:** `LLMController` 使用 `Executors.newCachedThreadPool()` 来异步处理请求，这意味着它可以同时处理多个流式连接。

## 示例请求

假设 LLM 类型已设置为 "XModel"。

**请求流式聊天:**

`GET /llm/stream-chat?param=请给我讲一个关于太空旅行的短故事`

**可能的响应流 (简化):**

```
event: start
data: 开始处理请求

event: chunk
data: 从前，在遥远的未来，

event: chunk
data: 人类掌握了星际旅行的技术。

event: chunk
data: 一艘名为“探索者号”的飞船，

event: chunk
data: 正准备启程前往未知的星系...

event: end
data: 旅程充满了奇迹与挑战。
```

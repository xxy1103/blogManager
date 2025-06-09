import { useAuthStore } from '../stores/auth.js'

const API_BASE_URL = 'http://localhost:5200' // 根据 API 文档

interface StreamChatCallbacks {
  onStart?: (event: MessageEvent) => void
  onChunk?: (textChunk: string, event: MessageEvent) => void
  onEnd?: (event: MessageEvent) => void
  onErrorEvent?: (event: MessageEvent) => void // Renamed from onError to avoid conflict with EventSource.onerror
  onStreamError?: (error: Event) => void // For network or EventSource specific errors
  onComplete?: () => void // Called when the stream is properly closed by onEnd or an error handled by onErrorEvent/onStreamError
}

export const llmService = {
  streamChat(param: string, callbacks: StreamChatCallbacks): EventSource | null {
    const authStore = useAuthStore()
    const token = authStore.token
    if (!token) {
      console.error('用户未登录，无法发送聊天请求')
      callbacks.onStreamError?.(new Event('Authentication error: User not logged in'))
      callbacks.onComplete?.()
      return null
    }

    const queryParams = new URLSearchParams({ param })
    const url = `${API_BASE_URL}/llm/stream-chat?${queryParams.toString()}`

    // EventSource 不支持自定义 headers，因此我们需要通过 query 参数传递 token
    // 后端需要相应地修改以从 query 参数读取 token
    // 如果后端不支持从 query 参数读取 token，则需要寻找其他 SSE 客户端库或通过代理转发请求
    // 假设后端已修改为支持从 query 参数 'token' 读取 JWT
    const fullUrl = `${url}&token=${encodeURIComponent(token)}`

    // console.log(`Requesting stream chat with URL: ${fullUrl}`); // 调试信息
    const eventSource = new EventSource(fullUrl) // 直接传递 token，如果后端支持

    eventSource.onopen = () => {
      // console.log('SSE connection opened.') // 调试信息
    }

    eventSource.addEventListener('start', (event) => {
      // console.log('SSE event: start', event) // 调试信息
      callbacks.onStart?.(event as MessageEvent)
    })

    eventSource.addEventListener('chunk', (event) => {
      // console.log('SSE event: chunk', event) // 调试信息
      const messageEvent = event as MessageEvent
      try {
        const data = JSON.parse(messageEvent.data)
        if (data && typeof data.text === 'string') {
          callbacks.onChunk?.(data.text, messageEvent)
        } else {
          // console.warn('Received chunk with unexpected data format:', data) // 调试信息
        }
      } catch (parseError) {
        console.error('Error parsing chunk data:', parseError, messageEvent.data) // 调试信息
        // 如果解析失败，可能直接传递原始数据或标记为错误
        callbacks.onChunk?.(messageEvent.data, messageEvent) // 仍然尝试传递原始数据
      }
    })

    eventSource.addEventListener('end', (event) => {
      // console.log('SSE event: end', event) // 调试信息
      callbacks.onEnd?.(event as MessageEvent)
      eventSource.close()
      callbacks.onComplete?.()
    })

    eventSource.addEventListener('error_event', (event) => {
      // 对应文档中的 error event
      // console.log('SSE event: error_event', event) // 调试信息
      callbacks.onErrorEvent?.(event as MessageEvent)
      // 根据 API 文档，error_event 后流可能不会自动关闭，但通常表示一个可处理的错误，流可能继续或需要客户端决定是否关闭
      // 如果 API 文档指明 error_event 后流会关闭，则在此处 close
      // eventSource.close();
      // callbacks.onComplete?.();
    })

    eventSource.onerror = (error) => {
      // EventSource 本身的错误，如网络问题
      // console.error('SSE EventSource error:', error) // 调试信息
      callbacks.onStreamError?.(error)
      eventSource.close()
      callbacks.onComplete?.()
    }
    return eventSource
  },

  async getSuggestion(param: string): Promise<string> {
    const authStore = useAuthStore()
    const token = authStore.token
    if (!token) {
      throw new Error('用户未登录，无法获取建议')
    }

    const queryParams = new URLSearchParams({ param })
    try {
      const response = await fetch(`${API_BASE_URL}/llm/getsuggestion?${queryParams.toString()}`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      if (!response.ok) {
        const errorText = await response.text()
        console.error(`获取建议失败: ${response.status} ${response.statusText} - ${errorText}`)
        throw new Error(`获取建议失败: ${response.statusText}`)
      }
      return await response.text()
    } catch (error) {
      console.error('获取建议失败:', error)
      throw error
    }
  },

  async setLlmType(llmType: 'XModel' | 'BigModel'): Promise<void> {
    const authStore = useAuthStore()
    const token = authStore.token
    if (!token) {
      throw new Error('用户未登录，无法设置LLM类型')
    }
    const queryParams = new URLSearchParams({ llmType })
    try {
      const response = await fetch(`${API_BASE_URL}/llm/set?${queryParams.toString()}`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })
      if (!response.ok) {
        const errorText = await response.text()
        console.error(`设置LLM类型失败: ${response.status} ${response.statusText} - ${errorText}`)
        throw new Error(`设置LLM类型失败: ${response.statusText}`)
      }
    } catch (error) {
      console.error('设置LLM类型失败:', error)
      throw error
    }
  },

  async getCurrentLlmType(): Promise<string> {
    const authStore = useAuthStore()
    const token = authStore.token
    if (!token) {
      throw new Error('用户未登录，无法获取当前LLM类型')
    }
    try {
      const response = await fetch(`${API_BASE_URL}/llm/get`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })
      if (!response.ok) {
        const errorText = await response.text()
        console.error(
          `获取当前LLM类型失败: ${response.status} ${response.statusText} - ${errorText}`,
        )
        throw new Error(`获取当前LLM类型失败: ${response.statusText}`)
      }
      return await response.text()
    } catch (error) {
      console.error('获取当前LLM类型失败:', error)
      throw error
    }
  },
}

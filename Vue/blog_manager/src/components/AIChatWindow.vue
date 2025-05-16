<template>
  <div :class="['ai-chat-sidebar', { collapsed: isCollapsed }]">
    <!-- 添加一个始终可见的展开按钮 -->
    <button v-if="isCollapsed" @click="toggleCollapse" class="expand-button" title="展开AI助手">
      <svg
        width="16"
        height="16"
        viewBox="0 0 16 16"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M11 2L5 8L11 14"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
      <span>AI助手</span>
    </button>

    <div class="sidebar-header">
      <div class="header-title">
        <div class="copilot-icon">
          <svg
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M12 2L4 7V17L12 22L20 17V7L12 2Z"
              stroke="currentColor"
              stroke-width="2"
              stroke-linejoin="round"
            />
            <path
              d="M12 11C13.1046 11 14 10.1046 14 9C14 7.89543 13.1046 7 12 7C10.8954 7 10 7.89543 10 9C10 10.1046 10.8954 11 12 11Z"
              stroke="currentColor"
              stroke-width="2"
            />
            <path
              d="M12 11V15M9 18H15"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            />
          </svg>
        </div>
        <span>AI 助手</span>
      </div>
      <div class="header-controls">
        <button @click="resetChat" class="icon-button" :disabled="isLoading" title="重置对话">
          <svg
            width="16"
            height="16"
            viewBox="0 0 16 16"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M13.6 3.4C12.2 2.1 10.2 1.2 8 1.2C4.4 1.2 1.5 4.1 1.5 7.7C1.5 11.3 4.4 14.2 8 14.2C11 14.2 13.5 12.3 14.4 9.7"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
            />
            <path
              d="M10.5 3.5H13.5V6.5"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>
        <button @click="toggleCollapse" class="icon-button" title="折叠">
          <svg
            width="16"
            height="16"
            viewBox="0 0 16 16"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M5 2L11 8L5 14"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>
      </div>
    </div>

    <div v-if="!isCollapsed" class="sidebar-content">
      <div class="model-selector">
        <span>大模型:</span>
        <select v-model="selectedModel" @change="changeModel" :disabled="isLoading">
          <option value="XModel">XModel</option>
          <option value="BigModel">BigModel</option>
        </select>
      </div>

      <div class="messages-area" ref="messagesContainer">
        <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.sender]">
          <div class="message-header">{{ msg.sender === 'user' ? '我' : 'AI 助手' }}</div>

          <!-- 推理内容区域 -->
          <div v-if="msg.sender === 'ai' && msg.reasoning" class="reasoning-container">
            <div class="reasoning-header" @click="toggleReasoning(msg)">
              <span class="reasoning-title">推理过程</span>
              <span class="reasoning-toggle">
                {{ msg.showReasoning ? '收起' : '展开' }}
                <svg
                  width="12"
                  height="12"
                  viewBox="0 0 16 16"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    :d="msg.showReasoning ? 'M2 10L8 4L14 10' : 'M2 6L8 12L14 6'"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </span>
            </div>
            <div v-if="msg.showReasoning" class="reasoning-content">
              {{ msg.reasoning }}
            </div>
          </div>

          <div class="message-content" v-if="msg.sender === 'user'">
            {{ msg.text }}
          </div>
          <div
            class="message-content markdown-body"
            v-if="msg.sender === 'ai'"
            v-html="parseMarkdown(msg.text || (msg.reasoning && !msg.text ? '正在思考...' : ''))"
          ></div>
        </div>
      </div>

      <div class="input-area">
        <textarea
          v-model="newMessage"
          placeholder="向AI助手提问..."
          @keyup.enter.prevent="sendMessage"
          :disabled="isLoading"
        ></textarea>
        <div class="input-buttons">
          <button
            v-if="!isLoading"
            @click="sendMessage"
            :disabled="!newMessage.trim()"
            class="send-button"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 16 16"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M14.5 8L1.5 14.5V1.5L14.5 8Z"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linejoin="round"
              />
            </svg>
            <span>发送</span>
          </button>
          <button v-else @click="stopReceiving" class="stop-button">
            <svg
              width="16"
              height="16"
              viewBox="0 0 16 16"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path d="M4 4H12V12H4V4Z" fill="currentColor" />
            </svg>
            <span>停止</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import * as marked from 'marked' // 引入marked库用于解析Markdown

interface Message {
  text: string
  sender: 'user' | 'ai'
  reasoning?: string // 添加推理过程字段
  showReasoning?: boolean // 控制是否显示推理过程
}
const isCollapsed = ref(false) // 默认展开AI助手
const messages = ref<Message[]>([
  { text: '你好！我是 AI 助手，有什么可以帮助你的吗？', sender: 'ai' },
])
const newMessage = ref('')
const isLoading = ref(false)
const messagesContainer = ref<HTMLDivElement | null>(null)
let eventSource: EventSource | null = null
const selectedModel = ref('XModel') // 默认选择XModel，后续会从后端获取实际值
let messageTimeoutId: number | null = null // 添加超时处理ID
const MESSAGE_TIMEOUT = 360000 // 设置360秒超时（可根据需要调整）

// 组件挂载时获取当前模型设置
onMounted(() => {
  getCurrentModel()
})

// 解析API返回的流式JSON数据，提取content字段和reasoning内容
const parseStreamData = (data: string): { text: string; reasoning?: string } => {
  try {
    // 如果数据是[DONE]，表示流结束
    if (data === '[DONE]') return { text: '' }

    // 检查是否是纯文本数据，有些API可能直接返回文本
    if (!data.startsWith('{') && !data.startsWith('data: {')) {
      // 移除可能的"data: "前缀
      return { text: data.replace(/^data: /, '') }
    }

    // 原始数据可能包含"data: "前缀，移除它
    const cleanData = data.replace(/^data: /, '')

    // 尝试解析JSON
    const jsonData = JSON.parse(cleanData)
    console.log('解析的JSON数据:', jsonData) // 调试日志

    // 返回结构，同时处理正文和推理内容
    const result: { text: string; reasoning?: string } = { text: '' }

    // 检查是否有返回内容
    if (jsonData.choices && jsonData.choices.length > 0) {
      const choice = jsonData.choices[0]
      console.log('解析的choice:', choice) // 调试日志

      // 检查并获取常规内容（处理不同API格式）
      if (choice.delta) {
        // 处理content字段 - 存在时直接使用
        if (choice.delta.content) {
          result.text = choice.delta.content
        }

        // 处理reasoning_content字段 - 存在时作为推理内容
        if (choice.delta.reasoning_content) {
          result.reasoning = choice.delta.reasoning_content
        }

        // 处理role字段 - 不显示为文本
        if (choice.delta.role) {
          // 如果只有role没有content，保持文本为空
          if (!choice.delta.content) {
            result.text = ''
          }
        }
      } else if (choice.text) {
        // 直接使用text字段
        result.text = choice.text
      }

      return result
    }

    // 检查其他可能的数据格式 (例如单独的text或content字段)
    if (jsonData.text) {
      return { text: jsonData.text }
    }

    if (jsonData.content) {
      return { text: jsonData.content }
    }

    // 检查是否有推理内容 (例如单独的reasoning_content字段)
    if (jsonData.reasoning_content) {
      return { reasoning: jsonData.reasoning_content, text: '' }
    }

    // 如果没有匹配任何已知模式，返回空对象
    return { text: '' }
  } catch (error) {
    // 如果解析JSON失败，可能是纯文本数据
    console.error('解析数据失败:', error)
    // 尝试返回原始数据，移除可能的"data: "前缀
    return { text: data.replace(/^data: /, '') }
  }
}

// 获取当前使用的模型
const getCurrentModel = async () => {
  try {
    const response = await fetch('/llm/get')
    const data = await response.text()
    // 尝试解析为JSON，如果不是JSON则直接使用返回的文本
    try {
      const jsonData = JSON.parse(data)
      selectedModel.value = jsonData.data || 'XModel'
    } catch {
      // 如果不是JSON，直接使用返回的文本
      selectedModel.value = data || 'XModel'
    }
  } catch (error) {
    console.error('获取当前模型失败:', error)
    // 默认使用XModel
    selectedModel.value = 'XModel'
  }
}

// 切换AI模型
const changeModel = async () => {
  if (isLoading.value) return

  try {
    isLoading.value = true
    const response = await fetch(`/llm/set?llmType=${selectedModel.value}`)
    if (response.ok) {
      // 添加系统消息通知用户模型已切换
      messages.value.push({
        text: `已切换至 ${selectedModel.value} 模型`,
        sender: 'ai',
      })
    } else {
      throw new Error('设置模型失败')
    }
  } catch (error) {
    console.error('切换模型失败:', error)
    messages.value.push({
      text: `切换模型失败，请稍后重试`,
      sender: 'ai',
    })
    // 恢复原模型选择
    await getCurrentModel()
  } finally {
    isLoading.value = false
  }
}

// 监听消息变化，自动滚动到底部
watch(
  messages,
  () => {
    nextTick(() => {
      scrollToBottom()
    })
  },
  { deep: true },
)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const closeEventSource = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }

  // 清除可能存在的超时定时器
  if (messageTimeoutId !== null) {
    clearTimeout(messageTimeoutId)
    messageTimeoutId = null
  }
}

const sendMessage = async () => {
  const text = newMessage.value.trim()
  if (!text || isLoading.value) return

  // 添加用户消息
  messages.value.push({ text, sender: 'user' })
  newMessage.value = ''

  // 添加一条空的AI回复信息，后续会更新这条信息
  messages.value.push({
    text: '',
    sender: 'ai',
    reasoning: '',
    showReasoning: false,
  })

  isLoading.value = true

  // 确保关闭之前的连接
  closeEventSource()

  // 清除之前的超时
  if (messageTimeoutId !== null) {
    clearTimeout(messageTimeoutId)
    messageTimeoutId = null
  }

  try {
    // 创建SSE连接，通过API代理访问后端（添加/api前缀）
    const url = `/llm/stream-chat?param=${encodeURIComponent(text)}`
    eventSource = new EventSource(url)

    // 设置消息超时，避免永久卡在加载状态
    messageTimeoutId = window.setTimeout(() => {
      console.warn('消息响应超时，自动关闭连接')
      const aiMessage = messages.value[messages.value.length - 1]
      if (aiMessage && aiMessage.sender === 'ai') {
        // 如果已有内容，添加提示；如果没有内容，设置提示信息
        if (aiMessage.text.trim()) {
          aiMessage.text += '\n\n[响应超时，连接已自动关闭]'
        } else {
          aiMessage.text = '[响应超时，连接已自动关闭]'
        }
      }
      closeEventSource()
      isLoading.value = false
    }, MESSAGE_TIMEOUT)

    // 处理开始事件
    eventSource.addEventListener('start', (event) => {
      console.log('开始接收流式响应', event.data)
      // 确保AI消息正确初始化
      // AI消息总是最后一条消息
      const aiMessage = messages.value[messages.value.length - 1]
      if (aiMessage && aiMessage.sender === 'ai') {
        // 重置消息内容，确保没有undefined值
        aiMessage.text = ''
        aiMessage.reasoning = ''
        aiMessage.showReasoning = false
      }
    })

    // 处理数据块事件
    eventSource.addEventListener('chunk', (event) => {
      console.log('接收到数据块:', event.data) // 调试日志

      // 解析并获取数据块内容
      const parsedData = parseStreamData(event.data)
      const { text, reasoning } = parsedData
      console.log('解析后的数据:', parsedData) // 调试日志

      // 始终获取最后一条消息，这应该是AI消息
      const aiMessage = messages.value[messages.value.length - 1]
      if (aiMessage && aiMessage.sender === 'ai') {
        // 添加正文内容（确保text有定义且不是undefined）
        if (text !== undefined && text !== null && text !== '') {
          console.log('添加正文内容:', text) // 调试日志
          aiMessage.text += text
        }

        // 处理推理内容
        if (reasoning) {
          console.log('添加推理内容:', reasoning) // 调试日志
          if (!aiMessage.reasoning) {
            aiMessage.reasoning = reasoning
            aiMessage.showReasoning = false // 默认折叠推理内容
          } else {
            aiMessage.reasoning += reasoning
          }
        }
      }
    })

    // 处理结束事件
    eventSource.addEventListener('end', () => {
      console.log('收到结束事件，关闭连接')
      // 结束后清理资源
      closeEventSource()
      isLoading.value = false
    })

    // 处理错误事件
    eventSource.addEventListener('error', (event) => {
      console.error('流式聊天错误', event)
      // 显示独立的错误信息
      const aiMessage = messages.value[messages.value.length - 1]
      if (aiMessage && aiMessage.sender === 'ai') {
        if (aiMessage.text.trim()) {
          // 已经有回复内容，添加错误提示
          aiMessage.text += '\n\n[发生错误，请重试]'
        } else {
          // 还没有回复内容，设置为错误消息
          aiMessage.text = '[发生错误，请重试]'
        }
      }
      closeEventSource()
      isLoading.value = false
    })

    // 处理常规错误
    eventSource.onerror = (error) => {
      console.error('SSE连接错误', error)
      // 显示独立的错误信息
      const aiMessage = messages.value[messages.value.length - 1]
      if (aiMessage && aiMessage.sender === 'ai') {
        if (aiMessage.text.trim()) {
          // 已经有回复内容，添加错误提示
          aiMessage.text += '\n\n[连接错误，请检查后端服务是否正常运行]'
        } else {
          // 还没有回复内容，设置为错误消息
          aiMessage.text = '[连接错误，请检查后端服务是否正常运行]'
        }
      }
      closeEventSource()
      isLoading.value = false
    }

    // 设置消息超时
    messageTimeoutId = window.setTimeout(() => {
      // 超过时间限制仍在加载，认为是错误
      const aiMessage = messages.value[messages.value.length - 1]
      if (aiMessage && aiMessage.sender === 'ai') {
        if (!aiMessage.text.trim()) {
          aiMessage.text = '[消息加载超时，请重试]'
        }
      }
      closeEventSource()
      isLoading.value = false
    }, MESSAGE_TIMEOUT)
  } catch (error) {
    console.error('创建SSE连接失败', error)
    const aiMessage = messages.value[messages.value.length - 1]
    if (aiMessage && aiMessage.sender === 'ai') {
      aiMessage.text = '连接服务器失败，请检查网络设置或稍后重试。'
    }
    isLoading.value = false
  }
}

// 停止接收消息
const stopReceiving = () => {
  console.log('用户手动停止接收消息')
  const aiMessage = messages.value[messages.value.length - 1]
  if (aiMessage && aiMessage.sender === 'ai') {
    if (aiMessage.text.trim()) {
      aiMessage.text += '\n\n[已手动停止接收消息]'
    } else {
      aiMessage.text = '[已手动停止接收消息]'
    }
  }
  closeEventSource()
  isLoading.value = false
}

// 重置聊天
const resetChat = () => {
  // 清空消息记录，确保初始消息有完整结构
  messages.value = [
    {
      text: '你好！我是 AI 助手，有什么可以帮助你的吗？',
      sender: 'ai',
      reasoning: '',
      showReasoning: false,
    },
  ]
  newMessage.value = ''
  isLoading.value = false

  // 关闭当前的SSE连接
  closeEventSource()
}

// 切换推理内容的显示和隐藏
const toggleReasoning = (message: Message) => {
  if (message.reasoning) {
    message.showReasoning = !message.showReasoning
  }
}

// 组件销毁时清理资源
onUnmounted(() => {
  closeEventSource()
  // 额外确保所有计时器被清除
  if (messageTimeoutId !== null) {
    clearTimeout(messageTimeoutId)
    messageTimeoutId = null
  }
})

// 解析Markdown文本
const parseMarkdown = (text: string): string => {
  if (!text) return ''
  try {
    // 使用marked.parse同步解析markdown
    return marked.parse(text, { async: false }) as string
  } catch (error) {
    console.error('解析Markdown失败:', error)
    return text
  }
}
</script>

<style scoped>
.ai-chat-sidebar {
  position: fixed;
  top: 0;
  right: 0;
  height: 100vh;
  width: 320px;
  background-color: var(--color-background);
  border-left: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  z-index: 1000;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.2);
}

.ai-chat-sidebar.collapsed {
  width: 0; /* 折叠侧边栏不占据宽度 */
  overflow: visible; /* 允许子元素溢出显示 */
}

/* 展开按钮样式 */
.expand-button {
  position: absolute;
  top: 50%;
  right: 0; /* 将按钮定位到右侧边缘 */
  width: 28px; /* 减小宽度 */
  height: auto;
  padding: 10px 0;
  border: none;
  background-color: var(--color-background-soft);
  color: var(--color-text);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1001;
  border-left: 1px solid var(--color-border);
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
  transform: translateY(-50%);
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
}

.expand-button svg {
  margin-bottom: 8px;
}

.expand-button span {
  writing-mode: vertical-rl;
  transform: rotate(180deg);
  text-orientation: mixed;
  padding: 5px 0;
  font-size: 14px;
  letter-spacing: 1px;
  font-weight: 500;
}

.expand-button:hover {
  background-color: var(--color-background-mute);
  color: var(--color-primary);
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid var(--color-border);
  background-color: var(--color-background-soft);
  height: 48px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: var(--color-heading);
  overflow: hidden;
}

.copilot-icon {
  display: flex;
  align-items: center;
  color: var(--color-accent);
}

.header-controls {
  display: flex;
  gap: 8px;
}

.icon-button {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text);
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-button:hover {
  background-color: var(--color-background-mute);
}

.icon-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.sidebar-content {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 48px);
  overflow: hidden;
}

.model-selector {
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  border-bottom: 1px solid var(--color-border);
  background-color: var(--color-component-bg);
}

.model-selector select {
  background-color: var(--color-card-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

/* 开关样式 */
.toggle-switch {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  margin-left: auto;
}

.toggle-switch input {
  display: none;
}

.slider {
  position: relative;
  width: 32px;
  height: 16px;
  background-color: var(--color-card-bg);
  border-radius: 16px;
  transition: 0.3s;
}

.slider:before {
  position: absolute;
  content: '';
  height: 12px;
  width: 12px;
  left: 2px;
  bottom: 2px;
  background-color: var(--color-text);
  border-radius: 50%;
  transition: 0.3s;
}

.toggle-switch input:checked + .slider {
  background-color: var(--color-primary);
}

.toggle-switch input:checked + .slider:before {
  transform: translateX(16px);
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: var(--color-background);
}

.message {
  border-radius: var(--border-radius);
  padding: 12px;
  max-width: 100%;
  word-break: break-word;
  white-space: pre-wrap;
  font-size: 14px;
}

.message.user {
  background-color: var(--color-background-soft);
  align-self: flex-end;
  border-left: 3px solid var(--color-accent);
}

.message.ai {
  background-color: var(--color-card-bg);
  align-self: flex-start;
  border-left: 3px solid var(--color-primary);
}

.message-header {
  font-weight: 500;
  margin-bottom: 4px;
  color: var(--color-heading);
  font-size: 12px;
}

.message-content {
  color: var(--color-text);
  line-height: 1.5;
}

/* 推理内容样式 */
.reasoning-container {
  margin-bottom: 8px;
  border-radius: 6px;
  background-color: rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.reasoning-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: rgba(0, 0, 0, 0.2);
  cursor: pointer;
  user-select: none;
}

.reasoning-header:hover {
  background-color: rgba(0, 0, 0, 0.3);
}

.reasoning-title {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-muted);
}

.reasoning-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.reasoning-content {
  padding: 8px 12px;
  font-size: 13px;
  color: var(--color-text-muted);
  white-space: pre-wrap;
  line-height: 1.4;
  max-height: 300px;
  overflow-y: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.input-area {
  padding: 12px 16px;
  border-top: 1px solid var(--color-border);
  background-color: var(--color-component-bg);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-area textarea {
  width: 100%;
  resize: none;
  height: 80px;
  padding: 8px;
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius);
  background-color: var(--color-card-bg);
  color: var(--color-text);
  font-family: inherit;
}

.input-area textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

.input-buttons {
  display: flex;
  justify-content: flex-end;
}

.send-button,
.stop-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-size: 13px;
  transition: background-color 0.2s;
}

.send-button {
  background-color: var(--color-primary);
  color: white;
}

.send-button:hover {
  background-color: var(--color-primary-dark);
}

.send-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.stop-button {
  background-color: var(--color-error);
  color: white;
}

.stop-button:hover {
  opacity: 0.9;
}

/* Markdown 样式 */
.markdown-body {
  line-height: 1.6;
}

:deep(.markdown-body h1),
:deep(.markdown-body h2),
:deep(.markdown-body h3),
:deep(.markdown-body h4),
:deep(.markdown-body h5),
:deep(.markdown-body h6) {
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
  line-height: 1.25;
  color: var(--color-heading);
}

:deep(.markdown-body h1) {
  font-size: 1.5em;
}

:deep(.markdown-body h2) {
  font-size: 1.3em;
}

:deep(.markdown-body h3) {
  font-size: 1.15em;
}

:deep(.markdown-body h4) {
  font-size: 1em;
}

:deep(.markdown-body p),
:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  margin-top: 0.5em;
  margin-bottom: 0.5em;
}

:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  padding-left: 1.5em;
}

:deep(.markdown-body li) {
  margin: 0.25em 0;
}

:deep(.markdown-body code) {
  padding: 0.2em 0.4em;
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
  font-family: SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.9em;
}

:deep(.markdown-body pre) {
  margin: 0.5em 0;
  padding: 0.8em;
  overflow: auto;
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}

:deep(.markdown-body pre code) {
  padding: 0;
  background-color: transparent;
  white-space: pre;
}

:deep(.markdown-body blockquote) {
  margin: 0.5em 0;
  padding: 0 1em;
  color: var(--color-text-muted);
  border-left: 0.25em solid var(--color-primary);
}

:deep(.markdown-body a) {
  color: var(--color-primary);
  text-decoration: underline;
}

:deep(.markdown-body table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0.5em 0;
}

:deep(.markdown-body table th),
:deep(.markdown-body table td) {
  padding: 0.5em;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.markdown-body table th) {
  background-color: rgba(0, 0, 0, 0.2);
  text-align: left;
}

:deep(.markdown-body img) {
  max-width: 100%;
}

/* 适配小屏幕设备 */
@media (max-width: 768px) {
  .ai-chat-sidebar {
    width: 100%;
    right: 0;
  }

  .ai-chat-sidebar.collapsed {
    width: 0;
    transform: none; /* 移动端不使用transform */
  }

  .expand-button {
    width: 28px;
    height: auto;
    top: 100px; /* 在移动设备上固定在顶部附近，避免被其他元素遮挡 */
    transform: none; /* 不需要垂直居中 */
    right: 0;
  }
}
</style>

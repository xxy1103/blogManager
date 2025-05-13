<template>
  <div :class="['ai-chat-window', { collapsed: isCollapsed }]">
    <button @click="toggleCollapse" class="collapse-toggle-btn">
      <span v-if="isCollapsed">展开 AI 助手</span>
      <span v-else>折叠 AI 助手</span>
    </button>
    <div v-if="!isCollapsed" class="chat-content">
      <div class="model-selector">
        <span>选择大模型：</span>
        <select v-model="selectedModel" @change="changeModel" :disabled="isLoading">
          <option value="XModel">XModel</option>
          <option value="BigModel">BigModel</option>
        </select>
        <label class="toggle-label">
          <input type="checkbox" v-model="showReasoning" :disabled="isLoading" />
          <span>显示推理</span>
        </label>
        <button @click="resetChat" class="reset-btn" :disabled="isLoading">
          <span>重置对话</span>
        </button>
      </div>
      <div class="messages-area" ref="messagesContainer">
        <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.sender]">
          <div v-if="msg.sender === 'user'" class="message-header user-header">我:</div>
          <div v-else class="message-header ai-header">AI:</div>
          <p>{{ msg.text }}</p>
        </div>
      </div>
      <div class="input-area">
        <textarea
          v-model="newMessage"
          placeholder="输入消息..."
          @keyup.enter.prevent="sendMessage"
          :disabled="isLoading"
        ></textarea>
        <button v-if="!isLoading" @click="sendMessage" :disabled="!newMessage.trim()">发送</button>
        <button v-else @click="stopReceiving" class="stop-btn">停止接收</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'

interface Message {
  text: string
  sender: 'user' | 'ai'
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
const showReasoning = ref(false) // 默认不显示推理内容
let messageTimeoutId: number | null = null // 添加超时处理ID
const MESSAGE_TIMEOUT = 90000 // 设置90秒超时（可根据需要调整）

// 组件挂载时获取当前模型设置
onMounted(() => {
  getCurrentModel()
})

// 解析API返回的流式JSON数据，提取content字段
const parseStreamData = (data: string): string => {
  try {
    // 如果数据是[DONE]，表示流结束
    if (data === '[DONE]') return ''

    // 检查是否是纯文本数据，有些API可能直接返回文本
    if (!data.startsWith('{') && !data.startsWith('data: {')) {
      // 移除可能的"data: "前缀
      return data.replace(/^data: /, '')
    }

    // 原始数据可能包含"data: "前缀，移除它
    const cleanData = data.replace(/^data: /, '')

    // 尝试解析JSON
    const jsonData = JSON.parse(cleanData)

    // 检查是否有返回内容
    if (jsonData.choices && jsonData.choices.length > 0) {
      const choice = jsonData.choices[0]

      // 优先获取delta.content字段（正文内容）
      if (choice.delta && choice.delta.content) {
        return choice.delta.content
      }

      // 如果没有delta.content，检查delta.role，但不返回role信息
      if (choice.delta && choice.delta.role) {
        return '' // 不显示role信息
      }

      // 检查delta.reasoning_content（你的示例数据中有这个字段）
      if (choice.delta && choice.delta.reasoning_content) {
        // 根据用户设置决定是否显示推理内容
        return showReasoning.value ? choice.delta.reasoning_content : ''
      }

      // 检查其他可能的字段格式
      if (choice.text) {
        return choice.text
      }
    }

    // 检查其他可能的数据格式
    if (jsonData.text) {
      return jsonData.text
    }

    if (jsonData.content) {
      return jsonData.content
    }

    return ''
  } catch (error) {
    // 如果解析JSON失败，可能是纯文本数据
    console.error('解析数据失败:', error)
    // 尝试返回原始数据，移除可能的"data: "前缀
    return data.replace(/^data: /, '')
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
  messages.value.push({ text: '', sender: 'ai' })

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
      // 确保AI消息以空白开始，这样后续的内容会从新行开始
      // AI消息总是最后一条消息
      const aiMessage = messages.value[messages.value.length - 1]
      if (aiMessage && aiMessage.sender === 'ai' && aiMessage.text === '') {
        // 初始化AI消息内容为空字符串
        aiMessage.text = ''
      }
    })

    // 处理数据块事件
    eventSource.addEventListener('chunk', (event) => {
      // 解析并获取数据块内容
      const content = parseStreamData(event.data)
      // 只有当有新内容时才追加到最后一条AI消息
      if (content) {
        // 始终获取最后一条消息，这应该是AI消息
        const aiMessage = messages.value[messages.value.length - 1]
        if (aiMessage && aiMessage.sender === 'ai') {
          aiMessage.text += content
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
  // 清空消息记录
  messages.value = [{ text: '你好！我是 AI 助手，有什么可以帮助你的吗？', sender: 'ai' }]
  newMessage.value = ''
  isLoading.value = false

  // 关闭当前的SSE连接
  closeEventSource()
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
</script>

<style scoped>
.ai-chat-window {
  position: fixed;
  right: 0;
  top: 60px; /* Adjust based on your header or other fixed elements */
  width: 350px;
  height: calc(100vh - 70px); /* Adjust height as needed */
  background-color: #f9f9f9;
  border-left: 1px solid #ccc;
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease-in-out;
  z-index: 1001; /* Ensure it's above other content like save messages */
}

.ai-chat-window.collapsed {
  transform: translateX(calc(100% - 40px)); /* Show only the button width */
}

.ai-chat-window.collapsed .chat-content {
  display: none;
}

.collapse-toggle-btn {
  position: absolute;
  left: -30px; /* Position button outside the chat window when expanded */
  top: 10px;
  writing-mode: vertical-rl;
  transform: rotate(180deg) translateX(100%);
  transform-origin: right bottom;
  padding: 10px 5px;
  background-color: #007bff;
  color: white;
  border: none;
  cursor: pointer;
  border-top-left-radius: 5px;
  border-bottom-left-radius: 5px;
  font-size: 14px;
  white-space: nowrap;
  z-index: 1002; /* 增加z-index确保按钮始终在最上层 */
}

.ai-chat-window.collapsed .collapse-toggle-btn {
  left: -40px; /* 将按钮定位在折叠窗口的左侧而不是右侧 */
  right: auto;
  transform: none; /* 移除可能导致点击区域计算错误的变形 */
  writing-mode: vertical-rl; /* 保持垂直文本 */
  border-top-left-radius: 5px;
  border-bottom-left-radius: 5px;
  width: 40px;
  height: auto;
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ai-chat-window.collapsed .collapse-toggle-btn span {
  writing-mode: vertical-rl;
  transform: none; /* 移除可能导致点击区域计算错误的变形 */
}

.chat-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.model-selector {
  padding: 10px;
  background-color: #f1f1f1;
  border-bottom: 1px solid #ccc;
  display: flex;
  align-items: center;
}

.model-selector span {
  font-size: 0.9em;
  margin-right: 10px;
}

.model-selector select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9em;
  margin-right: 10px;
}

.toggle-label {
  display: flex;
  align-items: center;
  font-size: 0.9em;
  margin-right: 10px;
  cursor: pointer;
}

.toggle-label input {
  margin-right: 5px;
}

.reset-btn {
  padding: 8px 15px;
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9em;
  margin-left: auto;
}

.reset-btn:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.messages-area {
  flex-grow: 1;
  overflow-y: auto;
  padding: 15px;
  background-color: #fff;
  border-bottom: 1px solid #eee;
}

.message {
  margin-bottom: 12px;
  padding: 10px 15px;
  border-radius: 10px;
  max-width: 80%;
  word-wrap: break-word;
  position: relative;
}

.message-header {
  font-size: 0.8em;
  font-weight: bold;
  margin-bottom: 4px;
}

.user-header {
  text-align: right;
  color: #0056b3;
}

.ai-header {
  color: #343a40;
}

.message.user {
  background-color: #007bff;
  color: white;
  margin-left: auto;
  border-bottom-right-radius: 3px;
}

.message.ai {
  background-color: #e9ecef;
  color: #333;
  margin-right: auto;
  border-bottom-left-radius: 3px;
}

.message.user .message-header {
  color: white;
  opacity: 0.8;
}

.message.user p {
  margin-top: 2px;
  text-align: right;
}

.message.ai p {
  margin-top: 2px;
}

.message p {
  margin: 0;
  font-size: 0.9em;
  white-space: pre-wrap; /* 保留换行符和空格 */
}

.input-area {
  display: flex;
  padding: 10px;
  border-top: 1px solid #ccc;
  background-color: #f1f1f1;
}

.input-area textarea {
  flex-grow: 1;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: none;
  min-height: 40px;
  max-height: 100px;
  font-size: 0.9em;
  margin-right: 8px;
}

.input-area textarea:disabled {
  background-color: #f8f8f8;
  cursor: not-allowed;
}

.input-area button {
  padding: 8px 15px;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9em;
  min-width: 80px; /* 确保按钮在加载状态时不会改变宽度 */
}

.input-area button.stop-btn {
  background-color: #dc3545;
}

.input-area button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.stop-btn {
  padding: 8px 15px;
  background-color: #ffc107;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9em;
  margin-left: 10px;
}

.stop-btn:hover {
  background-color: #e0a800;
}
</style>

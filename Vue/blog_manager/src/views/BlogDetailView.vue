<template>
  <div class="blog-detail-view">
    <div v-if="loading" class="loading">正在加载博客内容...</div>
    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="saveMessage" :class="['save-message', saveSuccess ? 'success' : 'error']">
      {{ saveMessage }}
    </div>

    <article v-if="blog" class="blog-content">
      <div class="blog-actions">
        <button @click="toggleEditMode" class="btn">
          {{ isEditing ? '取消编辑' : '编辑博客' }}
        </button>
        <button v-if="isEditing" @click="saveBlogContent" class="btn save-btn" :disabled="isSaving">
          {{ isSaving ? '保存中...' : '保存更改' }}
        </button>
      </div>

      <h1>{{ blog.title }}</h1>
      <p class="meta">
        <span>发布于：{{ formatDate(blog.date) }}</span> |
        <span>分类：{{ blog.categories }}</span> |
        <span>标签：{{ blog.tags.join(', ') }}</span>
      </p>
      <div class="saying"><strong>摘要：</strong> {{ blog.saying }}</div>
      <hr />

      <!-- 编辑模式：显示文本编辑区 -->
      <div v-if="isEditing" class="edit-content">
        <textarea
          v-model="editContent"
          class="content-editor"
          placeholder="在此编辑博客内容..."
        ></textarea>
      </div>

      <!-- 预览模式：显示渲染后的 HTML -->
      <div v-else v-html="blog.content" class="content-html"></div>
    </article>

    <div v-if="!loading && !blog && !error" class="not-found">博客未找到。</div>
    <router-link to="/blogs" class="back-link">返回博客列表</router-link>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getBlogDetail, updateBlogContent } from '@/services/blogService'
import type { BlogDetail } from '@/types/blog'
import { marked } from 'marked' // 用于将 Markdown 转换为 HTML

const route = useRoute()
const blog = ref<BlogDetail | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const isEditing = ref(false)
const editContent = ref('')
const originalContent = ref('') // 存储原始的 Markdown 内容
const isSaving = ref(false)
const saveMessage = ref('')
const saveSuccess = ref(false)

const fetchBlog = async () => {
  loading.value = true
  error.value = null
  blog.value = null

  const { year, month, day, filename } = route.params
  if (
    Array.isArray(year) ||
    Array.isArray(month) ||
    Array.isArray(day) ||
    Array.isArray(filename)
  ) {
    error.value = '无效的博客链接参数'
    loading.value = false
    return
  }

  try {
    // 确保使用正确的文件名 (它已在 route 处理中自动解码)
    const fetchedBlog = await getBlogDetail(
      year as string,
      month as string,
      day as string,
      filename as string,
    )
    if (fetchedBlog) {
      // 保存原始 Markdown 内容用于编辑
      originalContent.value = fetchedBlog.content
      editContent.value = fetchedBlog.content

      // 渲染HTML用于显示
      blog.value = {
        ...fetchedBlog,
        content: marked(fetchedBlog.content) as string, // 转换 Markdown 为 HTML
      }
    } else {
      error.value = '博客加载失败或未找到。'
    }
  } catch (err) {
    if (err instanceof Error) {
      error.value = `加载博客详情失败: ${err.message}`
    } else {
      error.value = '加载博客详情失败，请稍后再试。'
    }
    console.error(err)
  }
  loading.value = false
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 切换编辑模式
const toggleEditMode = () => {
  if (isEditing.value) {
    // 退出编辑模式，恢复初始内容
    editContent.value = originalContent.value
  }
  isEditing.value = !isEditing.value
  saveMessage.value = ''
}

// 保存博客内容
const saveBlogContent = async () => {
  if (!blog.value) return

  const { year, month, day, filename } = route.params
  if (
    Array.isArray(year) ||
    Array.isArray(month) ||
    Array.isArray(day) ||
    Array.isArray(filename)
  ) {
    error.value = '无效的博客链接参数'
    return
  }

  isSaving.value = true
  saveMessage.value = ''

  try {
    const result = await updateBlogContent(
      year as string,
      month as string,
      day as string,
      filename as string,
      editContent.value,
    )

    saveSuccess.value = result.success
    saveMessage.value = result.message

    if (result.success) {
      // 更新成功，更新原始内容并渲染新的HTML
      originalContent.value = editContent.value
      blog.value.content = marked(editContent.value) as string

      // 3秒后自动关闭编辑模式
      setTimeout(() => {
        if (isEditing.value) {
          isEditing.value = false
          saveMessage.value = ''
        }
      }, 3000)
    }
  } catch (err) {
    saveSuccess.value = false
    saveMessage.value = err instanceof Error ? err.message : '保存失败，请重试'
    console.error('Error saving blog content:', err)
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  fetchBlog()
})

// 如果路由参数变化，重新加载博客
watch(
  () => route.params,
  (newParams, oldParams) => {
    if (newParams.filename && newParams.filename !== oldParams.filename) {
      // 退出编辑模式并重新加载博客
      isEditing.value = false
      fetchBlog()
    }
  },
)
</script>

<style scoped>
.blog-detail-view {
  max-width: 900px;
  margin: 20px auto;
  padding: 20px;
  font-family: sans-serif;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.loading,
.error,
.not-found {
  text-align: center;
  padding: 30px;
  font-size: 1.3em;
}

.error {
  color: #d9534f; /* Bootstrap danger color */
}

.blog-content h1 {
  font-size: 2.5em;
  color: #333;
  margin-bottom: 15px;
  border-bottom: 2px solid #eee;
  padding-bottom: 10px;
}

.meta {
  font-size: 0.95em;
  color: #777;
  margin-bottom: 20px;
}

.meta span {
  margin-right: 10px;
}

.saying {
  background-color: #f9f9f9;
  border-left: 4px solid #337ab7; /* Bootstrap primary color */
  padding: 15px;
  margin-bottom: 25px;
  font-style: italic;
  color: #555;
}

.content-html {
  line-height: 1.8;
  font-size: 1.1em;
  color: #444;
}

/* Add some basic styling for HTML elements that might come from Markdown */
.content-html ::v-deep(h2) {
  font-size: 1.8em;
  margin-top: 30px;
  margin-bottom: 15px;
  border-bottom: 1px solid #eee;
  padding-bottom: 5px;
}

.content-html ::v-deep(h3) {
  font-size: 1.5em;
  margin-top: 25px;
  margin-bottom: 10px;
}

.content-html ::v-deep(p) {
  margin-bottom: 15px;
}

.content-html ::v-deep(ul),
.content-html ::v-deep(ol) {
  margin-left: 20px;
  margin-bottom: 15px;
}

.content-html ::v-deep(li) {
  margin-bottom: 8px;
}

.content-html ::v-deep(blockquote) {
  border-left: 3px solid #ddd;
  padding-left: 15px;
  color: #666;
  margin-left: 0;
  margin-right: 0;
  font-style: italic;
}

.content-html ::v-deep(pre) {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  overflow-x: auto;
  font-family: 'Courier New', Courier, monospace;
}

.content-html ::v-deep(code) {
  font-family: 'Courier New', Courier, monospace;
  background-color: #f0f0f0;
  padding: 2px 4px;
  border-radius: 3px;
  font-size: 0.95em;
}

.content-html ::v-deep(pre code) {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
}

.content-html ::v-deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 10px 0;
}

.back-link {
  display: inline-block;
  margin-top: 30px;
  padding: 10px 15px;
  background-color: #5cb85c; /* Bootstrap success color */
  color: white;
  text-decoration: none;
  border-radius: 5px;
  transition: background-color 0.2s;
}

.back-link:hover {
  background-color: #4cae4c;
}

/* 编辑相关样式 */
.blog-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
  gap: 10px;
}

.btn {
  padding: 8px 15px;
  border-radius: 4px;
  border: none;
  color: white;
  font-size: 14px;
  cursor: pointer;
  background-color: #337ab7;
  transition: background-color 0.2s;
}

.btn:hover {
  background-color: #2e6da4;
}

.btn:disabled {
  background-color: #95a5a6;
  cursor: not-allowed;
}

.save-btn {
  background-color: #5cb85c;
}

.save-btn:hover {
  background-color: #4cae4c;
}

.content-editor {
  width: 100%;
  min-height: 400px;
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
}

.save-message {
  padding: 10px 15px;
  margin: 10px 0;
  border-radius: 4px;
  text-align: center;
  animation: fadeIn 0.5s ease-in-out;
}

.save-message.success {
  background-color: #dff0d8;
  color: #3c763d;
  border: 1px solid #d6e9c6;
}

.save-message.error {
  background-color: #f2dede;
  color: #a94442;
  border: 1px solid #ebccd1;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>

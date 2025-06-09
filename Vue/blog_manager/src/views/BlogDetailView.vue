<template>
  <div class="blog-detail-view">
    <div class="banner-image">
      <div class="banner-overlay"></div>
      <div class="banner-content">
        <h2>博客详情</h2>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>正在加载博客内容...</p>
    </div>
    <div v-if="error" class="error-message global-message">{{ error }}</div>

    <!-- 新增：用于显示删除操作的反馈信息 -->
    <div
      v-if="deleteMessage"
      :class="[
        'message-feedback',
        deleteMessage.type === 'success' ? 'success-message' : 'error-message',
      ]"
      role="alert"
    >
      {{ deleteMessage.text }}
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="showDeleteConfirm" class="delete-confirm-overlay">
      <div class="delete-confirm-dialog">
        <h3>确认删除</h3>
        <p>您确定要删除博客 "{{ blog?.title }}" 吗？此操作无法撤销。</p>
        <div class="dialog-actions">
          <button @click="cancelDelete" class="btn cancel-btn">取消</button>
          <button @click="executeDelete" class="btn delete-btn">确认删除</button>
        </div>
      </div>
    </div>

    <!-- 博客信息更新对话框 -->
    <div v-if="showInfoUpdateModal" class="info-update-overlay">
      <div class="info-update-dialog">
        <h3>更新博客信息</h3>
        <div class="form-group">
          <label for="title">标题:</label>
          <input type="text" id="title" v-model="editForm.title" class="form-control" />
        </div>
        <div class="form-group">
          <label for="categories">分类:</label>
          <input type="text" id="categories" v-model="editForm.categories" class="form-control" />
        </div>
        <div class="form-group">
          <label for="tags">标签 (用逗号分隔):</label>
          <input type="text" id="tags" v-model="editForm.tagsString" class="form-control" />
        </div>
        <div class="form-group">
          <label for="saying">摘要:</label>
          <textarea id="saying" v-model="editForm.saying" class="form-control" rows="3"></textarea>
        </div>

        <div v-if="updateMessage" class="update-message" :class="updateMessage.type">
          {{ updateMessage.text }}
        </div>

        <div class="dialog-actions">
          <button @click="cancelInfoUpdate" class="btn cancel-btn">取消</button>
          <button @click="executeInfoUpdate" class="btn save-btn" :disabled="infoUpdateInProgress">
            {{ infoUpdateInProgress ? '更新中...' : '保存更新' }}
          </button>
        </div>
      </div>
    </div>

    <div class="content-container">
      <article v-if="blog" class="blog-content">
        <div class="blog-actions">
          <button @click="navigateToEditPage" class="btn">编辑博客</button>
          <button @click="confirmDelete" class="btn delete-btn">删除博客</button>
        </div>

        <h1>{{ blog.title }}</h1>
        <p class="meta">
          <span>📅 发布于：{{ formatDate(blog.dateTime) }}</span> |
          <span>📁 分类：{{ blog.categories }}</span> |
          <span>🏷️ 标签：{{ blog.tags.join(', ') }}</span>
        </p>
        <div class="saying"><strong>摘要：</strong> {{ blog.saying }}</div>
        <hr />

        <!-- 非编辑模式：显示渲染后的 HTML -->
        <div v-html="renderedContent" class="content-html"></div>
      </article>
    </div>

    <div v-if="!loading && !blog && !error" class="not-found">博客未找到。</div>
    <router-link to="/blogs" class="back-link">返回博客列表</router-link>

    <!-- 添加齿轮图标 -->
    <div v-if="blog" class="settings-icon" @click="openInfoUpdateModal">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">
        <path fill="currentColor" d="M12 15a3 3 0 100-6 3 3 0 000 6z" />
        <path
          fill="currentColor"
          d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 13.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"
        />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
// 导入 watch 和 RouteParams 类型
import { ref, onMounted, computed, watch, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router' // Removed RouteParams
// 导入 processBlogContentForDisplay
import {
  getBlogDetail,
  deleteBlog,
  updateBlogMetadata, // Changed from updateBlogInfo
  processBlogContentForDisplay,
} from '../services/blogService.js'
import type { BlogDetail } from '../types/blog.js'
import { marked } from 'marked'

const route = useRoute()
const router = useRouter()

const blog = ref<BlogDetail | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const showDeleteConfirm = ref(false)
const deleteInProgress = ref(false)
const deleteMessage = ref<{ type: 'success' | 'error'; text: string } | null>(null)

// 博客信息更新相关状态
const showInfoUpdateModal = ref(false)
const infoUpdateInProgress = ref(false)
const updateMessage = ref<{ type: 'success' | 'error'; text: string } | null>(null)
const editForm = reactive({
  title: '',
  categories: '',
  tagsString: '',
  saying: '',
})

const renderedContent = computed(() => {
  if (blog.value && blog.value.content) {
    const processedContent = processBlogContentForDisplay(blog.value.content)
    return marked(processedContent)
  }
  return ''
})

const fetchBlog = async () => {
  loading.value = true
  error.value = null
  blog.value = null

  const idParam = route.params.id
  console.log('BlogDetailView received idParam:', idParam)

  if (typeof idParam !== 'string' || idParam === 'undefined' || !idParam) {
    error.value = '无效或未提供博客 ID'
    loading.value = false
    console.error('Invalid or missing blog ID from route params:', idParam)
    return
  }

  const numericId = parseInt(idParam, 10)
  console.log('Parsed numericId:', numericId)

  if (isNaN(numericId)) {
    error.value = '博客 ID 格式无效'
    loading.value = false
    console.error('Invalid blog ID format after parsing:', idParam)
    return
  }

  try {
    const fetchedBlog = await getBlogDetail(numericId) // Use numericId
    if (fetchedBlog) {
      blog.value = fetchedBlog
    } else {
      error.value = '博客加载失败或未找到。'
    }
  } catch (err) {
    if (err instanceof Error) {
      error.value = `加载博客详情失败: ${err.message}`
    } else {
      error.value = '加载博客详情失败，请稍后再试。'
    }
    console.error('Error in fetchBlog:', err)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateTimeString: string | undefined) => {
  // Changed parameter to dateTimeString
  if (!dateTimeString) {
    return '日期不可用'
  }
  const date = new Date(dateTimeString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const navigateToEditPage = () => {
  if (!blog.value) return
  router.push({
    name: 'blog-edit-standalone',
    params: { id: blog.value.id.toString() }, // Use blog.id and convert to string
  })
}

// 删除博客相关函数
const confirmDelete = () => {
  showDeleteConfirm.value = true
}

const cancelDelete = () => {
  showDeleteConfirm.value = false
}

const executeDelete = async () => {
  if (!blog.value || deleteInProgress.value) return

  deleteInProgress.value = true
  deleteMessage.value = null // 清除之前的消息

  try {
    const result = await deleteBlog(blog.value.id)

    if (result.success) {
      deleteMessage.value = { type: 'success', text: result.message }
      setTimeout(() => {
        // 确保在组件仍然挂载且路由存在时执行跳转
        if (router && router.currentRoute.value.name !== 'blogs') {
          router.push({ path: '/blogs' })
        }
      }, 1500) // 延迟1.5秒以便用户看到消息
    } else {
      deleteMessage.value = { type: 'error', text: result.message || '删除失败，请稍后再试。' }
    }
  } catch (err) {
    console.error('Error during blog deletion process:', err)
    deleteMessage.value = {
      type: 'error',
      text: err instanceof Error ? `删除出错: ${err.message}` : '删除博客时发生未知错误。',
    }
  } finally {
    deleteInProgress.value = false
    showDeleteConfirm.value = false // 无论成功或失败，都关闭确认对话框
  }
}

// 更新博客信息相关函数
const openInfoUpdateModal = () => {
  if (!blog.value) return
  // 初始化编辑表单
  editForm.title = blog.value.title
  editForm.categories = blog.value.categories
  editForm.tagsString = blog.value.tags.join(', ')
  editForm.saying = blog.value.saying
  showInfoUpdateModal.value = true
}

const cancelInfoUpdate = () => {
  showInfoUpdateModal.value = false
  updateMessage.value = null
}

const executeInfoUpdate = async () => {
  if (infoUpdateInProgress.value || !blog.value) return // Ensure blog.value exists

  infoUpdateInProgress.value = true
  updateMessage.value = null

  const blogMetadataToUpdate = {
    title: editForm.title,
    categories: editForm.categories,
    tags: editForm.tagsString
      .split(',')
      .map((tag) => tag.trim())
      .filter((tag) => tag !== ''),
    saying: editForm.saying,
  }

  try {
    // Pass blog.value.id and the metadata object
    const result = await updateBlogMetadata(blog.value.id, blogMetadataToUpdate)

    if (result.success) {
      updateMessage.value = { type: 'success', text: result.message }
      // Optionally, refresh blog data locally or re-fetch
      if (blog.value) {
        blog.value.title = blogMetadataToUpdate.title
        blog.value.categories = blogMetadataToUpdate.categories
        blog.value.tags = blogMetadataToUpdate.tags
        blog.value.saying = blogMetadataToUpdate.saying
      }
      setTimeout(() => {
        showInfoUpdateModal.value = false
        updateMessage.value = null
      }, 1500)
    } else {
      updateMessage.value = { type: 'error', text: result.message || '更新失败，请稍后再试。' }
    }
  } catch (err) {
    console.error('Error during blog info update:', err)
    updateMessage.value = {
      type: 'error',
      text: err instanceof Error ? `更新出错: ${err.message}` : '更新博客信息时发生未知错误。',
    }
  } finally {
    infoUpdateInProgress.value = false
  }
}

onMounted(() => {
  fetchBlog()
})

watch(
  () => route.params.id, // Watch the 'id' param specifically
  (newId, oldId) => {
    console.log('Route ID changed from', oldId, 'to', newId)
    if (newId && newId !== oldId && typeof newId === 'string' && newId !== 'undefined') {
      fetchBlog()
    }
  },
)
</script>

<style scoped>
.blog-detail-view {
  width: 100%;
  margin: 0 auto;
  padding: 0;
  font-family: Inter, sans-serif;
  position: relative;
}

.banner-image {
  position: relative;
  width: 100%;
  height: 300px;
  background-image: url('https://images.unsplash.com/photo-1488190211105-8b0e65b80b4e?ixlib=rb-4.0.3');
  background-size: cover;
  background-position: center;
  overflow: hidden;
  margin-bottom: 2rem;
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(rgba(30, 36, 59, 0.7), rgba(30, 36, 59, 0.9));
}

.banner-content {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.banner-content h2 {
  font-size: 2.5rem;
  margin: 0;
  color: var(--color-heading);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 1px;
}

.content-container {
  max-width: 900px;
  margin: 0 auto 2rem;
  padding: 1.5rem;
  background-color: var(--color-card-bg);
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
}

.loading,
.error,
.not-found {
  text-align: center;
  padding: 2rem;
  font-size: 1.3rem;
  max-width: 900px;
  margin: 2rem auto;
  background-color: var(--color-card-bg);
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  border-top-color: var(--color-primary);
  animation: spin 1s infinite ease-in-out;
  margin: 0 auto 20px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error {
  color: var(--color-error);
}

.blog-content {
  padding: 0;
  border-radius: var(--border-radius);
}

.blog-content h1 {
  font-size: 2.2rem;
  color: var(--color-heading);
  margin-bottom: 1rem;
  border-bottom: 2px solid var(--color-border);
  padding-bottom: 0.8rem;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.meta {
  font-size: 0.95em;
  color: var(--color-text-muted);
  margin-bottom: 1.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.meta span {
  margin-right: 0.5rem;
  transition: color 0.3s ease;
}

.meta span:hover {
  color: var(--color-primary);
}

.saying {
  background-color: var(--color-background-soft);
  border-left: 4px solid var(--color-primary);
  padding: 1rem;
  margin-bottom: 1.5rem;
  font-style: italic;
  color: var(--color-text);
  border-radius: 0 var(--border-radius) var(--border-radius) 0;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.content-html {
  line-height: 1.8;
  font-size: 1.1rem;
  color: var(--color-text);
}

.content-html ::v-deep(h2) {
  font-size: 1.8rem;
  margin-top: 2rem;
  margin-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 0.5rem;
  color: var(--color-heading);
}

.content-html ::v-deep(h3) {
  font-size: 1.5rem;
  margin-top: 1.5rem;
  margin-bottom: 1rem;
  color: var(--color-heading);
}

.content-html ::v-deep(p) {
  margin-bottom: 1rem;
  color: var(--color-text);
}

.content-html ::v-deep(ul),
.content-html ::v-deep(ol) {
  margin-left: 1.5rem;
  margin-bottom: 1.2rem;
  color: var(--color-text);
}

.content-html ::v-deep(li) {
  margin-bottom: 0.5rem;
}

.content-html ::v-deep(blockquote) {
  border-left: 3px solid var(--color-primary);
  padding: 0.8rem 1rem;
  color: var(--color-text-muted);
  margin: 1rem 0;
  background-color: var(--color-background-soft);
  border-radius: 0 var(--border-radius) var(--border-radius) 0;
  font-style: italic;
}

.content-html ::v-deep(pre) {
  background-color: var(--color-background);
  padding: 1rem;
  border-radius: var(--border-radius);
  overflow-x: auto;
  font-family: 'Courier New', Courier, monospace;
  border: 1px solid var(--color-border);
  margin: 1rem 0;
}

.content-html ::v-deep(code) {
  font-family: 'Courier New', Courier, monospace;
  background-color: var(--color-background);
  padding: 0.2rem 0.4rem;
  border-radius: 3px;
  font-size: 0.95em;
  color: var(--color-primary-light);
}

.content-html ::v-deep(pre code) {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
}

.content-html ::v-deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--border-radius);
  margin: 1rem 0;
  box-shadow: var(--box-shadow);
  transition: transform 0.3s ease;
}

.content-html ::v-deep(img:hover) {
  transform: scale(1.01);
}

.back-link {
  display: inline-block;
  margin: 2rem auto;
  padding: 0.8rem 1.5rem;
  background-color: var(--color-primary);
  color: var(--color-heading);
  text-decoration: none;
  border-radius: var(--border-radius);
  transition: var(--transition-default);
  font-weight: 500;
  box-shadow: var(--box-shadow);
}

.back-link:hover {
  background-color: var(--color-primary-dark);
  transform: translateY(-2px);
  box-shadow: var(--box-shadow-hover);
}

.blog-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1.5rem;
  gap: 0.8rem;
}

.btn {
  padding: 0.6rem 1.2rem;
  border-radius: var(--border-radius);
  border: none;
  color: var(--color-heading);
  font-size: 0.9rem;
  cursor: pointer;
  background-color: var(--color-primary);
  transition: var(--transition-default);
  font-weight: 500;
  box-shadow: var(--box-shadow);
}

.btn:hover {
  background-color: var(--color-primary-dark);
  transform: translateY(-2px);
  box-shadow: var(--box-shadow-hover);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.save-btn {
  background-color: var(--color-success);
}

.save-btn:hover {
  background-color: #3d9c40;
}

.edit-content {
  width: 100%;
  margin-bottom: 1.5rem;
}

.save-message {
  padding: 0.8rem 1rem;
  margin: 1rem 0;
  border-radius: var(--border-radius);
  text-align: center;
  animation: fadeIn 0.5s ease-in-out;
}

.save-message.success {
  background-color: rgba(76, 175, 80, 0.2);
  color: var(--color-success);
  border: 1px solid rgba(76, 175, 80, 0.3);
}

.save-message.error {
  background-color: rgba(244, 67, 54, 0.2);
  color: var(--color-error);
  border: 1px solid rgba(244, 67, 54, 0.3);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

hr {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 1.5rem 0;
}

/* 删除确认对话框样式 */
.delete-confirm-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
  animation: fadeIn 0.3s ease-out;
}

.delete-confirm-dialog {
  width: 90%;
  max-width: 400px;
  background-color: var(--color-card-bg);
  border-radius: var(--border-radius);
  padding: 1.5rem;
  box-shadow: var(--box-shadow-hover);
  border: 1px solid var(--color-border);
  animation: scaleIn 0.3s ease-out;
}

@keyframes scaleIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.delete-confirm-dialog h3 {
  margin-top: 0;
  color: var(--color-error);
  font-size: 1.5rem;
  margin-bottom: 1rem;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.delete-btn {
  background-color: var(--color-error);
}

.delete-btn:hover {
  background-color: #d32f2f;
}

.cancel-btn {
  background-color: var(--color-background-soft);
  color: var(--color-text);
}

.cancel-btn:hover {
  background-color: var(--color-background-mute);
}

/* 更新博客信息对话框样式 */
.info-update-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
  animation: fadeIn 0.3s ease-out;
}

.info-update-dialog {
  width: 90%;
  max-width: 500px;
  background-color: var(--color-card-bg);
  border-radius: var(--border-radius);
  padding: 1.5rem;
  box-shadow: var(--box-shadow-hover);
  border: 1px solid var(--color-border);
  animation: scaleIn 0.3s ease-out;
}

.info-update-dialog h3 {
  margin-top: 0;
  color: var(--color-primary);
  font-size: 1.5rem;
  margin-bottom: 1rem;
}

.form-group {
  margin-bottom: 1.2rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--color-text);
}

.form-control {
  width: 100%;
  padding: 0.8rem;
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius);
  font-size: 1rem;
  background-color: var(--color-background-soft);
  color: var(--color-text);
  transition: var(--transition-default);
}

.form-control:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(74, 136, 229, 0.2);
  outline: none;
}

.update-message {
  padding: 0.8rem;
  margin: 1rem 0;
  border-radius: var(--border-radius);
  text-align: center;
  animation: fadeIn 0.5s ease-in-out;
}

.update-message.success {
  background-color: rgba(76, 175, 80, 0.2);
  color: var(--color-success);
  border: 1px solid rgba(76, 175, 80, 0.3);
}

.update-message.error {
  background-color: rgba(244, 67, 54, 0.2);
  color: var(--color-error);
  border: 1px solid rgba(244, 67, 54, 0.3);
}

.settings-icon {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 50px;
  height: 50px;
  background-color: var(--color-primary);
  color: var(--color-heading);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: var(--transition-default);
  z-index: 100;
  box-shadow: var(--box-shadow);
}

.settings-icon:hover {
  background-color: var(--color-primary-dark);
  transform: translateY(-3px) rotate(30deg);
  box-shadow: var(--box-shadow-hover);
}

/* Global styles for Toast UI Editor if needed, e.g., z-index */
.toastui-editor-defaultUI {
  width: 100%;
  z-index: 100; /* Adjust if necessary */
}
.toastui-editor-popup {
  z-index: 101; /* Ensure popups are above the editor */
}
</style>
<style>
.message-feedback {
  padding: 10px;
  margin-bottom: 15px;
  border-radius: 4px;
  text-align: center;
}

.success-message {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.error-message {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

/* 如果 global-message 已经有类似样式，可以调整或复用 */
.global-message {
  padding: 10px;
  margin-bottom: 15px;
  border-radius: 4px;
  text-align: center;
}
/* 确保 error-message (如果用于一般错误) 和 deleteMessage 的 error 样式一致或协调 */
.error-message.global-message {
  /* 特指用于 general error 的样式 */
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}
</style>

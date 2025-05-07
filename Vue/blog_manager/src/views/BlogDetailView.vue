<template>
  <div class="blog-detail-view">
    <div v-if="loading" class="loading">正在加载博客内容...</div>
    <div v-if="error" class="error">{{ error }}</div>

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

    <article v-if="blog" class="blog-content">
      <div class="blog-actions">
        <button @click="navigateToEditPage" class="btn">编辑博客</button>
        <button @click="confirmDelete" class="btn delete-btn">删除博客</button>
      </div>

      <h1>{{ blog.title }}</h1>
      <p class="meta">
        <span>发布于：{{ formatDate(blog.date) }}</span> |
        <span>分类：{{ blog.categories }}</span> |
        <span>标签：{{ blog.tags.join(', ') }}</span>
      </p>
      <div class="saying"><strong>摘要：</strong> {{ blog.saying }}</div>
      <hr />

      <!-- 非编辑模式：显示渲染后的 HTML -->
      <div v-html="renderedContent" class="content-html"></div>
    </article>

    <div v-if="!loading && !blog && !error" class="not-found">博客未找到。</div>
    <router-link to="/blogs" class="back-link">返回博客列表</router-link>
  </div>
</template>

<script setup lang="ts">
// 导入 watch 和 RouteParams 类型
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter, type RouteParams } from 'vue-router'
import { getBlogDetail, deleteBlog } from '@/services/blogService'
import type { BlogDetail } from '@/types/blog'
import { marked } from 'marked'

const route = useRoute()
const router = useRouter()

const blog = ref<BlogDetail | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const showDeleteConfirm = ref(false)
const deleteInProgress = ref(false)
const deleteMessage = ref<{ type: 'success' | 'error'; text: string } | null>(null)

const renderedContent = computed(() => {
  return blog.value ? marked(blog.value.content) : ''
})

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
    const fetchedBlog = await getBlogDetail(
      year as string,
      month as string,
      day as string,
      filename as string,
    )
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
    console.error(err)
  } finally {
    loading.value = false
  }
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

const navigateToEditPage = () => {
  if (!blog.value) return
  const { year, month, day, filename } = route.params
  router.push({
    name: 'blog-edit-standalone',
    params: { year, month, day, filename },
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
  const { year, month, day, filename } = route.params

  try {
    if (
      Array.isArray(year) ||
      Array.isArray(month) ||
      Array.isArray(day) ||
      Array.isArray(filename)
    ) {
      throw new Error('无效的博客参数')
    }

    const result = await deleteBlog(
      year as string,
      month as string,
      day as string,
      filename as string,
    )

    if (result.success) {
      deleteMessage.value = { type: 'success', text: result.message }
      // 删除成功后导航回博客列表页面
      setTimeout(() => {
        router.push({ path: '/blogs' })
      }, 1500)
    } else {
      deleteMessage.value = { type: 'error', text: result.message }
      showDeleteConfirm.value = false
    }
  } catch (err) {
    deleteMessage.value = {
      type: 'error',
      text: err instanceof Error ? err.message : '删除博客时发生错误',
    }
    showDeleteConfirm.value = false
  } finally {
    deleteInProgress.value = false
  }
}

onMounted(() => {
  fetchBlog()
})

watch(
  () => route.params,
  (newParams: RouteParams, oldParams: RouteParams) => {
    // 确保 filename 存在且发生变化, 或者其他关键参数发生变化
    const newKey = `${newParams.year}-${newParams.month}-${newParams.day}-${newParams.filename}`
    const oldKey = `${oldParams.year}-${oldParams.month}-${oldParams.day}-${oldParams.filename}`
    if (newParams.filename && newKey !== oldKey) {
      fetchBlog()
    }
  },
  { deep: true },
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

.edit-content {
  width: 100%;
  margin-bottom: 20px;
  /* Ensure editor has enough space */
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

/* Remove editor-loading and edit-tips if not used by ToastUI or reimplement if needed */
.editor-loading,
.edit-tips {
  display: none;
}

/* 删除确认对话框样式 */
.delete-confirm-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.delete-confirm-dialog {
  width: 90%;
  max-width: 400px;
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.delete-confirm-dialog h3 {
  margin-top: 0;
  color: #d9534f;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.delete-btn {
  background-color: #d9534f; /* Bootstrap danger color */
}

.delete-btn:hover {
  background-color: #c9302c; /* Darker red */
}

.cancel-btn {
  background-color: #95a5a6; /* Gray */
}

.cancel-btn:hover {
  background-color: #7f8c8d;
}
</style>
<style>
/* Global styles for Toast UI Editor if needed, e.g., z-index */
.toastui-editor-defaultUI {
  width: 100%;
  z-index: 100; /* Adjust if necessary */
}
.toastui-editor-popup {
  z-index: 101; /* Ensure popups are above the editor */
}
</style>

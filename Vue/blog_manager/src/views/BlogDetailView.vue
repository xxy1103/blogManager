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
          {{ isEditing ? '退出编辑' : '编辑博客' }}
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

      <!-- 编辑模式：显示 Toast UI Editor -->
      <div v-if="isEditing" class="edit-content">
        <div ref="editorRefElement"></div>
      </div>

      <!-- 非编辑模式：显示渲染后的 HTML -->
      <div v-else v-html="renderedContent" class="content-html"></div>
    </article>

    <div v-if="!loading && !blog && !error" class="not-found">博客未找到。</div>
    <router-link to="/blogs" class="back-link">返回博客列表</router-link>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed, nextTick, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { getBlogDetail, updateBlogContent } from '@/services/blogService'
import type { BlogDetail } from '@/types/blog'
import { marked } from 'marked' // 用于将 Markdown 转换为 HTML
import Editor from '@toast-ui/editor'
import '@toast-ui/editor/dist/toastui-editor.css' // Editor's Style

const route = useRoute()
const blog = ref<BlogDetail | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const isEditing = ref(false)
const editContent = ref('') // This will hold the markdown content from the editor
const originalContent = ref('') // 存储原始的 Markdown 内容
const isSaving = ref(false)
const saveMessage = ref('')
const saveSuccess = ref(false)

const editorRefElement = ref<HTMLElement | null>(null)
let editorInstance: Editor | null = null

// 计算属性：渲染的HTML内容 (用于非编辑模式)
const renderedContent = computed(() => {
  // When not editing, blog.value.content is assumed to be the raw Markdown.
  return blog.value ? marked(blog.value.content) : ''
})

const initEditor = () => {
  if (editorRefElement.value && !editorInstance) {
    editorInstance = new Editor({
      el: editorRefElement.value,
      height: '500px',
      initialEditType: 'markdown', // Or 'wysiwyg'
      previewStyle: 'vertical', // Or 'tab'
      initialValue: editContent.value, // Use editContent which holds the raw markdown
      events: {
        change: () => {
          if (editorInstance) {
            editContent.value = editorInstance.getMarkdown()
          }
        },
      },
    })
  }
}

const destroyEditor = () => {
  if (editorInstance) {
    editorInstance.destroy()
    editorInstance = null
  }
}

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
      originalContent.value = fetchedBlog.content // Store raw Markdown
      editContent.value = fetchedBlog.content // Set initial edit content to raw Markdown
      blog.value = { ...fetchedBlog } // Store the fetched blog details

      // If already in editing mode when blog is fetched (e.g., on route change while editing),
      // update the editor content.
      if (isEditing.value && editorInstance) {
        editorInstance.setMarkdown(editContent.value)
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

const toggleEditMode = async () => {
  isEditing.value = !isEditing.value
  saveMessage.value = ''

  if (isEditing.value) {
    // Entering edit mode
    editContent.value = originalContent.value // Ensure editor starts with original content
    await nextTick() // Wait for DOM update (editorRefElement to be available)
    initEditor()
  } else {
    // Exiting edit mode
    if (editContent.value !== originalContent.value) {
      if (!confirm('您有未保存的更改，确定要放弃吗？')) {
        isEditing.value = true // Revert to editing mode if user cancels
        return
      }
    }
    editContent.value = originalContent.value // Reset editContent to original
    destroyEditor()
  }
}

const saveBlogContent = async () => {
  if (!blog.value || !editorInstance) return

  const currentMarkdown = editorInstance.getMarkdown() // Get content from editor

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
      currentMarkdown, // Save the markdown from the editor
    )

    saveSuccess.value = result.success
    saveMessage.value = result.message

    if (result.success) {
      originalContent.value = currentMarkdown
      if (blog.value) {
        blog.value.content = currentMarkdown // Update the blog's content with the new markdown
      }
      editContent.value = currentMarkdown // Sync editContent as well

      // Optionally, exit edit mode after saving
      // isEditing.value = false;
      // destroyEditor();

      setTimeout(() => {
        saveMessage.value = ''
      }, 3000)
    } else {
      // If save failed, keep editor open with current content
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

onBeforeUnmount(() => {
  destroyEditor()
})

watch(
  () => route.params,
  (newParams, oldParams) => {
    if (newParams.filename && newParams.filename !== oldParams.filename) {
      if (isEditing.value) {
        // If editing, ask before discarding changes or save them
        if (editContent.value !== originalContent.value) {
          if (confirm('您有未保存的更改，切换博客将丢失这些更改。确定要切换吗？')) {
            isEditing.value = false
            destroyEditor()
            fetchBlog() // Fetch new blog post
          } else {
            // User chose not to switch, potentially revert route or do nothing
            // This part might need more sophisticated route guard logic depending on UX requirements
            return
          }
        } else {
          isEditing.value = false
          destroyEditor()
          fetchBlog()
        }
      } else {
        fetchBlog() // If not editing, just fetch the new blog
      }
    }
  },
  { deep: true }, // Watch route params deeply if they are objects
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

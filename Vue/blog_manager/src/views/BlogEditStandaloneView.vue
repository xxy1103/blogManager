<template>
  <div class="blog-edit-standalone-view">
    <!-- 添加横幅 -->
    <div class="banner-image">
      <div class="banner-overlay"></div>
      <div class="banner-content">
        <h2>编辑博客</h2>
      </div>
    </div>

    <div v-if="loading" class="loading-fullscreen">
      <div class="loading-spinner"></div>
      <p>正在加载博客内容...</p>
    </div>
    <div v-if="error" class="error-fullscreen">{{ error }}</div>
    <div v-if="saveMessage" :class="['save-message-toast', saveSuccess ? 'success' : 'error']">
      {{ saveMessage }}
    </div>

    <div v-if="blog" class="editor-layout">
      <div class="editor-header">
        <h2>正在编辑: {{ blog.title }}</h2>
        <div class="edit-actions">
          <button @click="saveBlog" class="btn save-btn" :disabled="isSaving">
            {{ isSaving ? '保存中...' : '保存更改' }}
          </button>
          <button @click="goBack" class="btn cancel-btn">
            {{ hasUnsavedChanges ? '放弃更改并返回' : '返回详情页' }}
          </button>
        </div>
      </div>
      <div ref="editorRefElement" class="editor-container-fullscreen"></div>
    </div>

    <div v-if="!loading && !blog && !error" class="not-found-fullscreen">
      博客未找到，无法编辑。
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBlogDetail, updateBlogContent, uploadImage } from '../services/blogService.js'
import type { BlogDetail } from '../types/blog.js'
// 修复导入方式，确保 Editor 是一个值而不仅仅是一个类型
import Editor from '@toast-ui/editor'
import '@toast-ui/editor/dist/toastui-editor.css'
import AIChatWindow from '../components/AIChatWindow.vue' // Added import for AI Chat Window

const route = useRoute()
const router = useRouter()

const blog = ref<BlogDetail | null>(null)
const initialContent = ref('') // Store the content as it was when the editor loaded
const loading = ref(true)
const error = ref<string | null>(null)
const isSaving = ref(false)
const saveMessage = ref('')
const saveSuccess = ref(false)

const editorRefElement = ref<HTMLElement | null>(null)
let editorInstance: Editor | null = null

const hasUnsavedChanges = computed(() => {
  if (!editorInstance || !blog.value) return false
  return editorInstance.getMarkdown() !== initialContent.value
})

const fetchBlogToEdit = async () => {
  loading.value = true
  error.value = null
  // const { year, month, day, filename } = route.params; // Old way
  const idParam = route.params.id // New: Get id from route params

  if (typeof idParam !== 'string' || idParam === 'undefined' || !idParam) {
    error.value = '无效或未提供博客 ID'
    loading.value = false
    console.error('Invalid or missing blog ID from route params:', idParam)
    return
  }

  const numericId = parseInt(idParam, 10)
  console.log('Parsed numericId for editing:', numericId)

  if (isNaN(numericId)) {
    error.value = '博客 ID 格式无效'
    loading.value = false
    console.error('Invalid blog ID format after parsing for editing:', idParam)
    return
  }

  try {
    const fetchedBlog = await getBlogDetail(numericId) // Call with numericId
    if (fetchedBlog) {
      blog.value = fetchedBlog
      initialContent.value = fetchedBlog.content // Store initial content
      await nextTick()
      initEditor(fetchedBlog.content)
    } else {
      error.value = '博客加载失败或未找到。'
    }
  } catch (err) {
    error.value =
      err instanceof Error ? `加载博客失败: ${err.message}` : '加载博客失败，请稍后再试。'
  } finally {
    loading.value = false
  }
}

const initEditor = (content: string) => {
  if (editorRefElement.value && !editorInstance) {
    // 创建编辑器实例
    editorInstance = new Editor({
      el: editorRefElement.value,
      initialValue: content,
      previewStyle: 'vertical',
      height: '100%', // Ensure editor takes full height of its container
      initialEditType: 'markdown',
      usageStatistics: false,
      // 显式定义工具栏项目，确保它们按需显示
      toolbarItems: [
        ['heading', 'bold', 'italic', 'strike'],
        ['hr', 'quote'],
        ['ul', 'ol', 'task', 'indent', 'outdent'],
        ['table', 'image', 'link'],
        ['code', 'codeblock'],
      ],
      hooks: {
        addImageBlobHook: async (blob: File, callback: (url: string, altText: string) => void) => {
          if (!blog.value) {
            console.error('Blog context is not available for image upload.')
            // Optionally, inform the user via UI
            callback('error_blog_context_missing', 'Error: Blog context missing')
            return
          }

          // Use blog ID as part of the relative path for the image directory
          // This ensures images are somewhat grouped by blog post if desired,
          // or use a more general path if preferred.
          const relativePathForImage = `blog-images/${blog.value.id}`

          const timestamp = Date.now()
          const extension = blob.name.split('.').pop() || blob.type.split('/')[1] || 'png'
          const timestampedFilename = `${timestamp}.${extension}`

          // Create a new File object with the timestamped name, as backend expects this
          const imageFile = new File([blob], timestampedFilename, {
            type: blob.type,
          })

          try {
            const imageUrl = await uploadImage(imageFile, relativePathForImage)
            // imageUrl from uploadImage is "image/relativePathForImage/timestampedFilename.ext"
            // This is the format required for saving in markdown.
            // The alt text can be the filename.
            callback(imageUrl, timestampedFilename)
          } catch (uploadError) {
            console.error('Failed to upload image:', uploadError)
            // Notify the user, e.g., by inserting an error message or using a toast notification
            // For now, we'll call the callback with an error placeholder.
            // The editor might show this as a broken image link.
            callback('error_uploading_image', 'Error uploading image')
            // Optionally, display a more user-friendly error message on the UI
            saveMessage.value = `图片上传失败: ${uploadError instanceof Error ? uploadError.message : '未知错误'}`
            saveSuccess.value = false
            // Auto-hide message after a few seconds
            setTimeout(() => {
              saveMessage.value = ''
            }, 5000)
          }
        },
      },
    })
    initialContent.value = editorInstance.getMarkdown() // Store initial content after editor is fully initialized

    // 获取编辑器DOM元素
    const editorEl = editorRefElement.value.querySelector('.toastui-editor') as HTMLElement
    if (editorEl) {
      // 为编辑器添加特定的键盘事件处理，这会覆盖编辑器内部的 Ctrl+S 处理
      editorEl.addEventListener(
        'keydown',
        (e) => {
          if (e.ctrlKey && e.key === 's') {
            e.preventDefault()
            e.stopPropagation()
            saveBlog()
            return false
          }
        },
        true,
      ) // 使用捕获模式，确保我们的事件处理器先于编辑器的内部处理器执行
    }
  }
}

const saveBlog = async () => {
  if (!blog.value || !editorInstance) return

  const currentMarkdown = editorInstance.getMarkdown()

  // Prepare the data to be sent to the backend, matching the API requirements
  const blogDataToUpdate = {
    title: blog.value.title, // Assuming title is part of blog.value and doesn't change here
    categories: blog.value.categories, // Assuming categories are part of blog.value
    tags: blog.value.tags, // Assuming tags are part of blog.value
    saying: blog.value.saying, // Assuming saying is part of blog.value
    content: currentMarkdown, // The updated content from the editor
  }

  isSaving.value = true
  saveMessage.value = ''
  try {
    // Call updateBlogContent with the blog's ID and the prepared data
    const result = await updateBlogContent(blog.value.id, blogDataToUpdate)

    saveSuccess.value = result.success
    saveMessage.value = result.message
    if (result.success) {
      initialContent.value = currentMarkdown // Update initial content to current saved content
      if (blog.value) blog.value.content = currentMarkdown

      setTimeout(() => {
        saveMessage.value = ''
      }, 3000)
    }
  } catch (err) {
    saveSuccess.value = false
    saveMessage.value = err instanceof Error ? err.message : '保存失败，请重试'
  } finally {
    isSaving.value = false
  }
}

const goBack = () => {
  if (hasUnsavedChanges.value) {
    if (!confirm('您有未保存的更改，确定要放弃并返回吗？')) {
      return
    }
  }
  // Navigate back to the detail view
  const { year, month, day, filename } = route.params
  router.push({
    name: 'blog-detail',
    params: { year, month, day, filename },
  })
}

const handleKeyDown = (event: KeyboardEvent) => {
  // Ctrl+S 快捷键处理
  if (event.ctrlKey && event.key === 's') {
    event.preventDefault() // 阻止浏览器默认保存行为
    event.stopPropagation() // 阻止事件冒泡，防止编辑器接收到这个事件
    saveBlog() // 调用我们自己的保存函数
    return false // 确保不会有其他处理
  }
}

onMounted(() => {
  fetchBlogToEdit()

  // 添加全局键盘事件监听器
  window.addEventListener('keydown', handleKeyDown, true) // 使用捕获模式

  // 添加阻止默认Ctrl+S行为的全局处理器
  document.addEventListener('keydown', globalKeydownHandler, true) // 使用捕获模式
})

// 保存全局事件处理函数的引用，以便日后可以移除
const globalKeydownHandler = function (e: KeyboardEvent) {
  if (e.ctrlKey && e.key === 's') {
    e.preventDefault()
    e.stopPropagation()
    return false
  }
}

onBeforeUnmount(() => {
  if (editorInstance) {
    editorInstance.destroy()
    editorInstance = null
  }

  // 移除所有添加的事件监听器
  window.removeEventListener('keydown', handleKeyDown, true)
  document.removeEventListener('keydown', globalKeydownHandler, true)

  // 如果编辑器元素仍然存在，移除其事件监听器
  const editorEl = editorRefElement.value?.querySelector('.toastui-editor') as HTMLElement
  if (editorEl) {
    editorEl.removeEventListener('keydown', handleKeyDown, true)
  }
})
</script>

<style scoped>
.blog-edit-standalone-view {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  overflow-x: hidden;
  position: relative;
  background-color: var(--color-background);
  color: var(--color-text);
}

/* 添加横幅样式 */
.banner-image {
  position: relative;
  width: 100%;
  height: 200px;
  background-image: url('https://images.unsplash.com/photo-1510936111840-65e151ad71bb?ixlib=rb-4.0.3');
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
  font-size: 2.2rem;
  margin: 0;
  color: var(--color-heading);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 1px;
}

/* Loading spinner style */
.loading-spinner {
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top: 4px solid #007bff;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.editor-layout {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto 2rem;
  padding: 0;
  background-color: var(--color-card-bg);
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  overflow: hidden;
}

.editor-header {
  padding: 1rem 1.5rem;
  background-color: var(--color-background-soft);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.editor-header h2 {
  margin: 0;
  font-size: 1.3rem;
  color: var(--color-heading);
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.edit-actions {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: var(--border-radius);
  border: none;
  color: var(--color-heading);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition-default);
  box-shadow: var(--box-shadow);
}

.save-btn {
  background-color: var(--color-primary);
}

.save-btn:hover {
  background-color: var(--color-primary-dark);
  transform: translateY(-2px);
  box-shadow: var(--box-shadow-hover);
}

.save-btn:disabled {
  opacity: 0.6;
  background-color: var(--color-background-mute);
  cursor: not-allowed;
  transform: none;
}

.cancel-btn {
  background-color: var(--color-background-mute);
}

.cancel-btn:hover {
  background-color: var(--color-background-soft);
  transform: translateY(-2px);
  box-shadow: var(--box-shadow-hover);
}

.editor-container-fullscreen {
  flex-grow: 1;
  width: 100%;
  border-top: 1px solid var(--color-border);
  position: relative;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  min-height: 500px;
  background-color: var(--color-background-soft);
}

/* 隐藏编辑器中的垂直分隔线 */
.editor-container-fullscreen :deep(.toastui-editor-md-splitter) {
  display: none;
}

/* 修改预览区文字颜色 - 更强的选择器确保颜色生效 */
.editor-container-fullscreen :deep(.toastui-editor-contents),
.editor-container-fullscreen :deep(.toastui-editor-md-preview),
.editor-container-fullscreen :deep(.toastui-editor-md-preview p),
.editor-container-fullscreen :deep(.toastui-editor-md-preview span),
.editor-container-fullscreen :deep(.toastui-editor-contents p),
.editor-container-fullscreen :deep(.toastui-editor-contents span) {
  color: #ffffff !important;
}

/* 定制 Toast UI 编辑器样式以适应深色主题 */
:deep(.toastui-editor),
:deep(.toastui-editor-defaultUI) {
  display: flex !important;
  flex-direction: column !important;
  height: 100% !important;
  width: 100% !important;
  border: none !important;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
  background-color: var(--color-background-soft) !important;
  color: var(--color-text) !important;
}

/* 自定义工具栏样式 */
:deep(.toastui-editor-defaultUI-toolbar) {
  background-color: var(--color-background) !important;
  border-bottom: 1px solid var(--color-border) !important;
}

:deep(.toastui-editor-toolbar-icons) {
  color: var(--color-text) !important;
}

:deep(.toastui-editor-toolbar-icons:hover) {
  background-color: var(--color-background-mute) !important;
  color: var(--color-primary) !important;
}

/* 编辑器内容区域样式 */
:deep(.toastui-editor .ProseMirror) {
  padding: 12px 16px !important;
  width: 100% !important;
  box-sizing: border-box !important;
  color: var(--color-text) !important;
  line-height: 1.7 !important;
  font-size: 1.05rem !important;
}

:deep(.toastui-editor-main),
:deep(.toastui-editor-main-container) {
  margin: 0 !important;
  padding: 0 !important;
  width: 100% !important;
  box-sizing: border-box !important;
  background-color: var(--color-background-soft) !important;
}

/* 确保没有边框和额外空间占用 */
:deep(.toastui-editor) {
  border: none !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

/* 为标题添加样式 */
:deep(.toastui-editor h1),
:deep(.toastui-editor h2),
:deep(.toastui-editor h3),
:deep(.toastui-editor h4) {
  color: var(--color-heading) !important;
  font-weight: 600 !important;
}

/* 添加预览区标题高亮样式 */
:deep(.toastui-editor-md-preview h1),
:deep(.toastui-editor-md-preview h2),
:deep(.toastui-editor-md-preview h3),
:deep(.toastui-editor-md-preview h4),
:deep(.toastui-editor-md-preview h5),
:deep(.toastui-editor-md-preview h6) {
  color: #ffffff !important;
  font-weight: 700 !important;
  margin: 1.2em 0 0.6em !important;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3) !important;
}

:deep(.toastui-editor-md-preview h1) {
  font-size: 2em !important;
  border-bottom: 1px solid var(--color-border) !important;
  padding-bottom: 0.3em !important;
}

:deep(.toastui-editor-md-preview h2) {
  font-size: 1.6em !important;
  border-bottom: 1px solid var(--color-border) !important;
  padding-bottom: 0.2em !important;
}

:deep(.toastui-editor-md-preview h3) {
  font-size: 1.4em !important;
}

/* Markdown和所见即所得编辑器容器样式 */
:deep(.toastui-editor-md-container),
:deep(.toastui-editor-ww-container) {
  display: flex !important;
  height: 100% !important;
  width: 100% !important;
  min-height: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
  background-color: var(--color-background-soft) !important;
}

/* CodeMirror 编辑器样式增强 */
:deep(.toastui-editor .CodeMirror) {
  background-color: var(--color-background) !important;
  color: #ffffff !important; /* 编辑区域也使用高对比度的白色文本 */
  font-family: 'Fira Code', Consolas, Monaco, 'Andale Mono', monospace !important;
  padding: 20px 25px !important; /* 与预览区域保持一致的内边距 */
  line-height: 1.8 !important;
  font-size: 1.05rem !important;
}

:deep(.toastui-editor .CodeMirror-lines) {
  padding-top: 0 !important;
}

:deep(.toastui-editor .CodeMirror-line) {
  padding-left: 0 !important;
  padding-right: 0 !important;
}

/* CodeMirror 语法高亮增强 */
:deep(.toastui-editor .cm-header) {
  color: var(--color-accent) !important;
  font-weight: bold !important;
}

:deep(.toastui-editor .cm-header-1) {
  font-size: 1.6em !important;
}

:deep(.toastui-editor .cm-header-2) {
  font-size: 1.4em !important;
}

:deep(.toastui-editor .cm-header-3) {
  font-size: 1.2em !important;
}

:deep(.toastui-editor .cm-link) {
  color: var(--color-primary-light) !important;
  text-decoration: underline !important;
}

:deep(.toastui-editor .cm-url) {
  color: var(--color-primary) !important;
}

:deep(.toastui-editor .cm-string) {
  color: #7ec699 !important;
}

:deep(.toastui-editor .cm-strong) {
  color: var(--color-accent-light) !important;
  font-weight: bold !important;
}

:deep(.toastui-editor .cm-em) {
  color: #a2d5f2 !important;
  font-style: italic !important;
}

:deep(.toastui-editor .cm-comment) {
  color: #7c7c7c !important;
}

:deep(.toastui-editor .cm-code) {
  background-color: rgba(255, 255, 255, 0.1) !important;
  color: #ffcc66 !important; /* 与预览区域保持一致 */
  border-radius: 3px !important;
  padding: 0.2em 0.4em !important;
}

:deep(.toastui-editor .cm-quote) {
  color: var(--color-text) !important;
  font-style: italic !important;
  border-left: 3px solid var(--color-primary) !important;
  padding-left: 10px !important;
  margin-left: 0px !important;
}

/* CodeMirror 光标和选择区域 */
:deep(.toastui-editor .CodeMirror-cursor) {
  border-left: 2px solid var(--color-primary-light) !important;
  border-right: none !important;
  width: 0 !important;
}

:deep(.toastui-editor .CodeMirror-selected) {
  background-color: rgba(74, 136, 229, 0.3) !important;
}

/* 强调、粗体、斜体文本的样式 */
:deep(.toastui-editor-md-preview strong),
:deep(.toastui-editor-md-preview b) {
  color: var(--color-accent-light) !important; /* 强调色使粗体文本更明显 */
  font-weight: 700 !important;
}

:deep(.toastui-editor-md-preview em),
:deep(.toastui-editor-md-preview i) {
  color: #a2d5f2 !important; /* 浅蓝色为斜体文本 */
  font-style: italic !important;
}

/* 复选框样式增强 */
:deep(.toastui-editor-md-preview .task-list-item) {
  list-style-type: none !important;
  padding-left: 0 !important;
  margin-left: -1em !important;
}

:deep(.toastui-editor-md-preview .task-list-item input[type='checkbox']) {
  margin-right: 0.5em !important;
  vertical-align: middle !important;
}

/* 链接悬停状态 */
:deep(.toastui-editor-md-preview a:hover) {
  color: var(--color-accent) !important;
  text-decoration: underline !important;
}

/* 提高行内代码与普通文本的区分度 */
:deep(.toastui-editor-md-preview *:not(pre) > code) {
  font-family: 'Fira Code', Consolas, Monaco, 'Andale Mono', monospace !important;
  background-color: rgba(255, 255, 255, 0.1) !important;
  color: #ffcc66 !important; /* 使用黄色增强可见度 */
  border-radius: 3px !important;
  padding: 0.2em 0.4em !important;
  font-size: 0.9em !important;
  white-space: nowrap !important;
}

/* 提高预览区域的滚动条可见性 */
:deep(.toastui-editor-md-preview::-webkit-scrollbar) {
  width: 8px !important;
}

:deep(.toastui-editor-md-preview::-webkit-scrollbar-track) {
  background: var(--color-background) !important;
}

:deep(.toastui-editor-md-preview::-webkit-scrollbar-thumb) {
  background-color: var(--color-primary) !important;
  border-radius: 20px !important;
}

/* 预览区域中的脚注增强 */
:deep(.toastui-editor-md-preview .footnote-ref),
:deep(.toastui-editor-md-preview .footnote-backref) {
  color: var(--color-primary-light) !important;
  text-decoration: none !important;
}

:deep(.toastui-editor-md-preview .footnotes) {
  border-top: 1px solid var(--color-border) !important;
  padding-top: 1em !important;
  margin-top: 2em !important;
  font-size: 0.9em !important;
}

/* 强化表格样式 */
:deep(.toastui-editor-md-preview table) {
  border-collapse: collapse !important;
  width: 100% !important;
  margin: 1em 0 !important;
}

:deep(.toastui-editor-md-preview th),
:deep(.toastui-editor-md-preview td) {
  border: 1px solid var(--color-border) !important;
  padding: 8px 12px !important;
  color: #ffffff !important;
  font-weight: 500 !important;
}

:deep(.toastui-editor-md-preview th) {
  background-color: rgba(255, 255, 255, 0.1) !important;
  font-weight: 700 !important;
  color: #ffffff !important;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.2) !important;
}

/* 编辑区域和预览区域的边框和背景增强 */
:deep(.toastui-editor-md-container) {
  border: 1px solid var(--color-border) !important;
  border-radius: var(--border-radius) !important;
  overflow: hidden !important;
}

/* 增强代码高亮 */
:deep(.toastui-editor-md-preview .hljs) {
  display: block !important;
  overflow-x: auto !important;
  padding: 1em !important;
  color: #e0e0e0 !important;
  background-color: var(--color-background-soft) !important;
}

:deep(.toastui-editor-md-preview .hljs-comment),
:deep(.toastui-editor-md-preview .hljs-quote) {
  color: #7c7c7c !important;
}

:deep(.toastui-editor-md-preview .hljs-keyword),
:deep(.toastui-editor-md-preview .hljs-selector-tag),
:deep(.toastui-editor-md-preview .hljs-addition) {
  color: #cc99cd !important;
}

:deep(.toastui-editor-md-preview .hljs-number),
:deep(.toastui-editor-md-preview .hljs-string),
:deep(.toastui-editor-md-preview .hljs-meta .hljs-meta-string),
:deep(.toastui-editor-md-preview .hljs-literal),
:deep(.toastui-editor-md-preview .hljs-doctag),
:deep(.toastui-editor-md-preview .hljs-regexp) {
  color: #7ec699 !important;
}

:deep(.toastui-editor-md-preview .hljs-title),
:deep(.toastui-editor-md-preview .hljs-section),
:deep(.toastui-editor-md-preview .hljs-name),
:deep(.toastui-editor-md-preview .hljs-selector-id),
:deep(.toastui-editor-md-preview .hljs-selector-class) {
  color: #f8c555 !important;
}

:deep(.toastui-editor-md-preview .hljs-attribute),
:deep(.toastui-editor-md-preview .hljs-attr),
:deep(.toastui-editor-md-preview .hljs-variable),
:deep(.toastui-editor-md-preview .hljs-template-variable),
:deep(.toastui-editor-md-preview .hljs-class .hljs-title),
:deep(.toastui-editor-md-preview .hljs-type) {
  color: #7ca9f2 !important;
}

/* 图片显示增强 */
:deep(.toastui-editor-md-preview img) {
  max-width: 100% !important;
  border-radius: var(--border-radius) !important;
  border: 1px solid var(--color-border) !important;
  margin: 1em 0 !important;
  display: block !important;
}

/* 列表项增强 */
:deep(.toastui-editor-md-preview ul),
:deep(.toastui-editor-md-preview ol) {
  padding-left: 2em !important;
  margin: 1em 0 !important;
}

:deep(.toastui-editor-md-preview li + li) {
  margin-top: 0.5em !important;
}

/* WYSIWYG模式特定样式增强 */
:deep(.toastui-editor-ww-container .toastui-editor-ww-viewer) {
  background-color: var(--color-background) !important;
  color: #ffffff !important; /* 使用高对比度白色 */
}

:deep(.toastui-editor-ww-container .ProseMirror) {
  background-color: var(--color-background) !important;
  color: #ffffff !important;
  padding: 20px 25px !important; /* 与Markdown预览保持一致的内边距 */
  line-height: 1.8 !important;
  font-size: 1.05rem !important;
}

/* 增强WYSIWYG模式中的标题 */
:deep(.toastui-editor-ww-container .ProseMirror h1),
:deep(.toastui-editor-ww-container .ProseMirror h2),
:deep(.toastui-editor-ww-container .ProseMirror h3) {
  color: var(--color-accent) !important;
  border-bottom: 1px solid var(--color-border) !important;
  padding-bottom: 0.3em !important;
  margin-bottom: 1em !important;
  margin-top: 1.5em !important;
}

/* WYSIWYG模式中的链接样式 */
:deep(.toastui-editor-ww-container .ProseMirror a) {
  color: var(--color-primary-light) !important;
  text-decoration: underline !important;
}

:deep(.toastui-editor-ww-container .ProseMirror a:hover) {
  color: var(--color-accent) !important;
}

/* WYSIWYG模式中代码块和行内代码样式 */
:deep(.toastui-editor-ww-container .ProseMirror pre) {
  background-color: var(--color-background-soft) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--border-radius) !important;
  padding: 1em !important;
  margin: 1em 0 !important;
}

:deep(.toastui-editor-ww-container .ProseMirror code) {
  background-color: rgba(255, 255, 255, 0.1) !important;
  color: #ffcc66 !important; /* 使用黄色增强可见度 */
  border-radius: 3px !important;
  padding: 0.2em 0.4em !important;
  font-family: 'Fira Code', Consolas, Monaco, 'Andale Mono', monospace !important;
  font-size: 0.9em !important;
}

/* WYSIWYG模式中的引用样式 */
:deep(.toastui-editor-ww-container .ProseMirror blockquote) {
  border-left: 4px solid var(--color-primary) !important;
  color: var(--color-text) !important;
  background-color: rgba(74, 136, 229, 0.1) !important;
  padding: 0.8rem 1rem !important;
  margin: 1em 0 !important;
}

/* 强化WYSIWYG模式中表格的样式 */
:deep(.toastui-editor-ww-container .ProseMirror table) {
  border-collapse: collapse !important;
  width: 100% !important;
  margin: 1em 0 !important;
}

:deep(.toastui-editor-ww-container .ProseMirror th),
:deep(.toastui-editor-ww-container .ProseMirror td) {
  border: 1px solid var(--color-border) !important;
  padding: 8px 12px !important;
  color: #ffffff !important; /* 确保表格内文本清晰可见 */
  font-weight: 500 !important;
}

:deep(.toastui-editor-ww-container .ProseMirror th) {
  background-color: rgba(255, 255, 255, 0.1) !important;
  font-weight: 700 !important;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.2) !important;
}

/* 为编辑区域的表格也添加高亮文本样式 */
:deep(.toastui-editor .CodeMirror .cm-table) {
  color: #ffffff !important;
  font-weight: 500 !important;
}

:deep(.toastui-editor .CodeMirror .cm-table-header) {
  color: #ffffff !important;
  font-weight: 700 !important;
  background-color: rgba(255, 255, 255, 0.05) !important;
}

/* WYSIWYG模式中的列表项样式 */
:deep(.toastui-editor-ww-container .ProseMirror ul),
:deep(.toastui-editor-ww-container .ProseMirror ol) {
  padding-left: 2em !important;
  margin: 1em 0 !important;
  color: #ffffff !important;
}

:deep(.toastui-editor-ww-container .ProseMirror li + li) {
  margin-top: 0.5em !important;
}

/* WYSIWYG模式中的粗体和斜体文本样式 */
:deep(.toastui-editor-ww-container .ProseMirror strong),
:deep(.toastui-editor-ww-container .ProseMirror b) {
  color: var(--color-accent-light) !important;
  font-weight: 700 !important;
}

:deep(.toastui-editor-ww-container .ProseMirror em),
:deep(.toastui-editor-ww-container .ProseMirror i) {
  color: #a2d5f2 !important;
  font-style: italic !important;
}

/* 在编辑器元素上添加过渡效果 */
:deep(.toastui-editor),
:deep(.toastui-editor-defaultUI),
:deep(.toastui-editor-md-container),
:deep(.toastui-editor-ww-container) {
  transition: all 0.3s ease-in-out !important;
}

/* 添加工具栏按钮的过渡效果 */
:deep(.toastui-editor-toolbar-icons) {
  transition: all 0.2s ease !important;
}

:deep(.toastui-editor-toolbar-icons:hover) {
  transform: translateY(-2px) !important;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2) !important;
}

:deep(.toastui-editor-toolbar-icons:active) {
  transform: translateY(0) !important;
}

/* 增强选项卡切换动画 */
:deep(.toastui-editor .tab-item) {
  transition: all 0.3s ease !important;
}

:deep(.toastui-editor .tab-item:hover) {
  background-color: var(--color-background-mute) !important;
}

:deep(.toastui-editor .tab-item.active) {
  font-weight: bold !important;
  color: var(--color-primary) !important;
  border-bottom: 2px solid var(--color-primary) !important;
}

/* 提高编辑器主题一致性 */
:deep(.toastui-editor-dark) {
  --toastui-editor-bg-color: var(--color-background) !important;
  --toastui-editor-border: var(--color-border) !important;
  --toastui-editor-primary: var(--color-primary) !important;
  --toastui-editor-text-color: #ffffff !important;
}

/* 自定义占位符文本 */
:deep(.toastui-editor .placeholder) {
  color: var(--color-text-muted) !important;
  font-style: italic !important;
}

/* 编辑器焦点状态增强 */
:deep(.toastui-editor-md-container:focus-within),
:deep(.toastui-editor-ww-container:focus-within) {
  box-shadow: 0 0 0 2px var(--color-primary-light) !important;
}

/* 消除轮廓线，使用自定义焦点效果 */
:deep(.toastui-editor *:focus) {
  outline: none !important;
}

/* 确保预览区域中的内容与编辑器宽度一致 */
:deep(.toastui-editor-md-preview) {
  max-width: 100% !important;
  overflow-x: hidden !important;
}

:deep(.toastui-editor-md-preview > *) {
  max-width: 100% !important;
  overflow-wrap: break-word !important;
  word-wrap: break-word !important;
}

/* 优化移动设备上的体验 */
@media (max-width: 768px) {
  :deep(.toastui-editor-md-container .toastui-editor-md-editor),
  :deep(.toastui-editor-md-container .toastui-editor-md-preview) {
    flex: 1 1 100% !important;
    width: 100% !important;
  }

  :deep(.toastui-editor-md-container) {
    flex-direction: column !important;
  }

  :deep(.toastui-editor .CodeMirror),
  :deep(.toastui-editor-md-preview),
  :deep(.toastui-editor-ww-container .ProseMirror) {
    padding: 15px !important;
    font-size: 0.95rem !important;
  }
}
</style>

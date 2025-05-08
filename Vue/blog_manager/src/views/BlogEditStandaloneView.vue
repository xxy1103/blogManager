<template>
  <div class="blog-edit-standalone-view">
    <div v-if="loading" class="loading-fullscreen">正在加载博客内容...</div>
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
import { getBlogDetail, updateBlogContent, uploadImage } from '../services/blogService.js' // Import uploadImage and correct path with .js extension
import type { BlogDetail } from '@/types/blog'
import Editor from '@toast-ui/editor'
import '@toast-ui/editor/dist/toastui-editor.css'

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
    editorInstance = new Editor({
      el: editorRefElement.value,
      initialValue: content,
      previewStyle: 'vertical',
      height: '100%', // Ensure editor takes full height of its container
      initialEditType: 'markdown',
      usageStatistics: false,
      hooks: {
        addImageBlobHook: async (blob: File, callback: (url: string, altText: string) => void) => {
          if (!blog.value) {
            console.error('Blog context is not available for image upload.')
            // Optionally, inform the user via UI
            callback('error_blog_context_missing', 'Error: Blog context missing')
            return
          }

          // Use blog filename (without .md) as the relative path for the image directory
          let blogFilename = route.params.filename as string
          if (Array.isArray(blogFilename)) {
            // Should not happen based on route setup, but good practice
            blogFilename = blogFilename[0]
          }
          const relativePathForImage = blogFilename.replace(/\.md$/, '')

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
  }
}

const saveBlog = async () => {
  if (!blog.value || !editorInstance) return

  const currentMarkdown = editorInstance.getMarkdown()
  const { year, month, day, filename } = route.params
  if (
    Array.isArray(year) ||
    Array.isArray(month) ||
    Array.isArray(day) ||
    Array.isArray(filename)
  ) {
    saveMessage.value = '无效的博客链接参数，无法保存。'
    saveSuccess.value = false
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
      currentMarkdown,
    )
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
  if (event.ctrlKey && event.key === 's') {
    event.preventDefault() // Prevent default browser save action
    saveBlog()
  }
}

onMounted(() => {
  fetchBlogToEdit()
  window.addEventListener('keydown', handleKeyDown)
})

onBeforeUnmount(() => {
  if (editorInstance) {
    editorInstance.destroy()
    editorInstance = null
  }
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.blog-edit-standalone-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%; /* 使用100%确保适应其容器 */
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  overflow-x: hidden; /* 只隐藏水平滚动条，保留垂直滚动功能 */
  /* 移除固定定位，防止顶部被遮挡 */
  position: relative; /* 使用相对定位而非固定定位 */
}

.editor-layout {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  height: 100%;
  width: 100%; /* 确保宽度占满父容器 */
  margin: 0; /* 确保没有外边距 */
  padding: 0; /* 确保没有内边距 */
}

.editor-header {
  padding: 8px 12px; /* 减少内边距，原来是15px 20px */
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0; /* Prevent header from shrinking */
}

.editor-header h2 {
  margin: 0;
  font-size: 1.5em;
  color: #333;
}

.edit-actions {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 6px 12px; /* 减小按钮的内边距，原来是8px 15px */
  border-radius: 4px;
  border: 1px solid transparent;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition:
    background-color 0.2s,
    border-color 0.2s;
}

.save-btn {
  background-color: #28a745; /* Green */
  border-color: #28a745;
}
.save-btn:hover {
  background-color: #218838;
  border-color: #1e7e34;
}
.save-btn:disabled {
  background-color: #6c757d; /* Gray */
  border-color: #6c757d;
  cursor: not-allowed;
}

.cancel-btn {
  background-color: #6c757d; /* Secondary/Gray */
  border-color: #6c757d;
}
.cancel-btn:hover {
  background-color: #5a6268;
  border-color: #545b62;
}

.editor-container-fullscreen {
  flex-grow: 1; /* Allow editor to take available vertical space */
  width: 100%; /* 使用相对宽度代替视口宽度 */
  border-top: 1px solid #ddd; /* Separator */
  position: relative; /* Good for absolutely positioned children or 100% height children */
  margin: 0; /* 确保没有外边距 */
  padding: 0; /* 确保没有内边距 */
  box-sizing: border-box; /* 确保边框和内边距计入总宽度 */
}

/* Ensure Toast UI editor itself takes full width/height of its container */
/* The root element of the editor, which has height: 100% from options or CSS */
:deep(.toastui-editor),
:deep(.toastui-editor-defaultUI) {
  /* .toastui-editor-defaultUI is often used with the default UI theme */
  display: flex !important; /* Override if it's not already flex */
  flex-direction: column !important; /* Stack toolbar and main content vertically */
  height: 100% !important; /* Takes full height of .editor-container-fullscreen */
  width: 100% !important; /* 使用相对宽度代替视口宽度 */
  border: none !important; /* Remove default border if any, to blend with layout */
  margin: 0 !important; /* 移除可能的外边距 */
  padding: 0 !important; /* 移除可能的内边距 */
  box-sizing: border-box !important; /* 边框和内边距计入总宽度 */
}

/* 修正Toast UI编辑器内部的一些默认间距 */
:deep(.toastui-editor .ProseMirror) {
  padding: 6px !important; /* 减少所见即所得编辑器的内边距 */
  width: 100% !important; /* 确保内容区域填充可用宽度 */
  box-sizing: border-box !important;
}

:deep(.toastui-editor-main),
:deep(.toastui-editor-main-container) {
  margin: 0 !important;
  padding: 0 !important;
  width: 100% !important; /* 使用相对宽度代替视口宽度 */
  box-sizing: border-box !important;
}

/* 确保没有边框和额外空间占用 */
:deep(.toastui-editor) {
  border: none !important;
  width: 100% !important; /* 使用相对宽度代替视口宽度 */
  box-sizing: border-box !important;
}

/* Markdown and WYSIWYG editor containers, now inside a correctly sized parent (.toastui-editor-main or .toastui-editor-main-container).
   They should fill this parent. */
:deep(.toastui-editor-md-container),
:deep(.toastui-editor-ww-container) {
  display: flex !important; /* They are often flex rows (e.g., editor area | preview area) */
  height: 100% !important;
  width: 100% !important; /* 使用相对宽度代替视口宽度 */
  min-height: 0 !important; /* 添加此属性以确保内容可以正确滚动 */
  margin: 0 !important; /* 移除外边距 */
  padding: 0 !important; /* 移除内边距 */
  box-sizing: border-box !important;
}

/* Specific scrollable areas (actual editor input, preview pane) */
:deep(.toastui-editor-md-preview),
:deep(.toastui-editor-md-container .toastui-editor-md-scroll-sync), /* Markdown editor's scrollable part */
:deep(.toastui-editor-ww-container .toastui-editor-ww-scroll-sync) {
  /* WYSIWYG editor's scrollable part */
  height: 100% !important; /* Fill their respective flex items within md-container/ww-container */
  overflow-y: auto !important; /* Enable vertical scrolling for these specific areas */
  min-height: 0 !important; /* 确保可以正确滚动 */
  padding: 8px !important; /* 减少内边距，原来通常是更大的值 */
}

/* 确保编辑器占满整个页面宽度 */
:deep(.toastui-editor-md-container .toastui-editor-md-splitter) {
  display: none !important; /* 移除分隔线，提供额外空间 */
}

/* 确保编辑区域和预览区域的宽度均匀分布 */
:deep(.toastui-editor-md-container .toastui-editor-md-editor),
:deep(.toastui-editor-md-container .toastui-editor-md-preview) {
  flex: 1 1 50% !important; /* 各占50%宽度 */
  width: 50% !important;
  box-sizing: border-box !important;
}

/* 确保没有水平滚动条 */
:deep(.CodeMirror-hscrollbar),
:deep(.toastui-editor-md-preview::-webkit-scrollbar-horizontal) {
  display: none !important; /* 隐藏水平滚动条 */
}

/* Toast UI Editor 特定滚动区域样式 */
/* 确保编辑区域的代码编辑器可以滚动 */
:deep(.toastui-editor .ProseMirror),
:deep(.toastui-editor .CodeMirror),
:deep(.toastui-editor-code-block.CodeMirror),
:deep(.toastui-editor-md-container .toastui-editor-md-editor),
:deep(.toastui-editor-md-container .toastui-editor-md-preview),
:deep(.toastui-editor-md-container .toastui-editor-main) {
  overflow: auto !important;
  height: 100% !important;
}

/* 让编辑器内的文本滚动容器工作正常 */
:deep(.toastui-editor-md-container .toastui-editor-md-vertical-style),
:deep(.toastui-editor-md-container .toastui-editor-md-vertical-style > div) {
  overflow: auto !important;
  height: 100% !important;
}

/* 确保界面响应滚轮事件 */
:deep(.toastui-editor-defaultUI-toolbar),
:deep(.toastui-editor-defaultUI) {
  position: relative;
  z-index: 1; /* 确保工具栏不被隐藏 */
}

/* 确保编辑器内没有多余边距 */
:deep(.toastui-editor *) {
  box-sizing: border-box;
}

/* 修正可能的水平滚动问题 */
:deep(.toastui-editor-main),
:deep(.toastui-editor-main-container),
:deep(.toastui-editor-md-container),
:deep(.toastui-editor-ww-container),
:deep(.toastui-editor-md-preview),
:deep(.toastui-editor-md-editor) {
  max-width: 100% !important;
  overflow-x: hidden !important; /* 防止水平滚动 */
}

/* 添加样式确保 loading 和 error 消息不会被遮挡 */
.loading-fullscreen,
.error-fullscreen,
.not-found-fullscreen {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  font-size: 1.5em;
  color: #555;
  position: static; /* 确保不会因为其他元素定位问题而被遮挡 */
  z-index: 2; /* 确保在其他元素之上 */
}
.error-fullscreen {
  color: #d9534f;
}

.save-message-toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 20px;
  border-radius: 5px;
  color: white;
  z-index: 1000;
  font-size: 1em;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.save-message-toast.success {
  background-color: #28a745;
}
.save-message-toast.error {
  background-color: #dc3545;
}

/* 确保在所见即所得模式下，编辑区域占满宽度 */
:deep(.toastui-editor-ww-container) {
  width: 100% !important;
  display: flex !important;
  flex-direction: column !important;
}

/* WYSIWYG模式特定样式 */
:deep(.toastui-editor.ww-mode) {
  width: 100% !important;
  height: 100% !important;
  display: flex !important;
  flex-direction: column !important;
}

:deep(.toastui-editor.ww-mode .toastui-editor-main) {
  width: 100% !important;
  height: 100% !important;
  flex: 1 1 auto !important;
  display: flex !important;
  flex-direction: column !important;
}

:deep(.toastui-editor.ww-mode .ProseMirror) {
  width: 100% !important;
  max-width: 100% !important;
  height: 100% !important;
  padding: 8px 16px !important;
  overflow-y: auto !important;
  box-sizing: border-box !important;
}

/* 确保不会出现重复的编辑器实例 */
:deep(.toastui-editor-ww-container:not(:first-child)) {
  display: none !important;
}

/* 确保模式切换时预览区域消失 */
:deep(.toastui-editor.ww-mode .toastui-editor-md-preview) {
  display: none !important;
}
</style>

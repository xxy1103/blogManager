<template>
  <div class="blog-edit-view">
    <div v-if="loading" class="loading">正在加载博客内容...</div>
    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="blog" class="editor-container">
      <input
        type="text"
        v-model="editForm.title"
        placeholder="博客标题"
        class="form-control title-input"
      />
      <div ref="editorRef" class="toast-editor"></div>
      <button @click="saveContent" class="btn save-btn" :disabled="saveInProgress">
        {{ saveInProgress ? '保存中...' : '保存内容' }}
      </button>
      <div v-if="saveMessage" :class="['save-message', saveMessage.type]">
        {{ saveMessage.text }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import {
  getBlogDetail,
  updateBlogContent,
  processBlogContentForDisplay,
  revertBlogContentForSave,
} from '../services/blogService.js'
import type { BlogDetail } from '../types/blog.js'
// @ts-ignore
import Editor from '@toast-ui/editor'
import '@toast-ui/editor/dist/toastui-editor.css'

const route = useRoute()

const blog = ref<BlogDetail | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const editorRef = ref<HTMLElement | null>(null)
let editorInstance: Editor | null = null

const editForm = reactive({
  title: '',
  content: '',
})

const saveInProgress = ref(false)
const saveMessage = ref<{ type: 'success' | 'error'; text: string } | null>(null)

const fetchBlogToEdit = async () => {
  loading.value = true
  error.value = null
  blog.value = null
  const { year, month, day, filename } = route.params
  if (
    typeof year !== 'string' ||
    typeof month !== 'string' ||
    typeof day !== 'string' ||
    typeof filename !== 'string'
  ) {
    error.value = '无效的博客路径参数'
    loading.value = false
    return
  }

  try {
    const data = await getBlogDetail(year, month, day, filename)
    if (data) {
      blog.value = data
      editForm.title = data.title
      // 使用 processBlogContentForDisplay 处理内容以在编辑器中正确显示图片
      const contentForEditor = processBlogContentForDisplay(data.content)
      editForm.content = contentForEditor
      await nextTick()
      if (editorRef.value) {
        initializeEditor(contentForEditor)
      }
    } else {
      error.value = '未找到博客或加载失败'
    }
  } catch (err) {
    console.error('获取博客内容失败:', err)
    error.value = '获取博客内容失败'
  } finally {
    loading.value = false
  }
}

const initializeEditor = (initialContent: string) => {
  if (editorRef.value && !editorInstance) {
    editorInstance = new Editor({
      el: editorRef.value,
      height: '500px',
      initialEditType: 'markdown',
      previewStyle: 'vertical',
      initialValue: initialContent,
    })
  } else if (editorInstance) {
    editorInstance.setMarkdown(initialContent)
  }
}

const saveContent = async () => {
  if (!blog.value || !editorInstance) return

  saveInProgress.value = true
  saveMessage.value = null
  const currentContentFromEditor = editorInstance.getMarkdown()
  // 使用 revertBlogContentForSave 转换图片路径以便保存
  const contentToSave = revertBlogContentForSave(currentContentFromEditor)

  const { year, month, day, filename } = route.params as {
    year: string
    month: string
    day: string
    filename: string
  }

  try {
    const result = await updateBlogContent(year, month, day, filename, contentToSave)
    if (result.success) {
      saveMessage.value = { type: 'success', text: '博客内容更新成功！' }
      if (blog.value) {
        // 更新本地的 blog 内容，确保图片路径是原始的相对路径
        blog.value.content = contentToSave
        // 如果希望编辑器也更新为保存后的原始路径内容（通常不这么做，因为编辑器需要显示 /api 前缀的图片）
        // editorInstance.setMarkdown(processBlogContentForDisplay(contentToSave));
      }
    } else {
      saveMessage.value = { type: 'error', text: result.message || '更新博客内容失败' }
    }
  } catch (err) {
    console.error('保存博客内容失败:', err)
    saveMessage.value = { type: 'error', text: '保存博客内容时发生错误' }
  } finally {
    saveInProgress.value = false
  }
}

onMounted(() => {
  fetchBlogToEdit()
})
</script>

<style scoped>
.blog-edit-view {
  max-width: 900px;
  margin: 20px auto;
  padding: 20px;
  font-family: sans-serif;
}

.loading,
.error {
  text-align: center;
  padding: 30px;
  font-size: 1.3em;
}

.error {
  color: #d9534f;
}

.editor-container {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.title-input {
  width: 100%;
  padding: 10px;
  margin-bottom: 15px;
  font-size: 1.5em;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box;
}

.toast-editor {
  margin-bottom: 20px;
}

.save-btn {
  padding: 10px 20px;
  background-color: #5cb85c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1em;
  transition: background-color 0.2s;
}

.save-btn:hover {
  background-color: #4cae4c;
}

.save-btn:disabled {
  background-color: #95a5a6;
  cursor: not-allowed;
}

.save-message {
  padding: 10px 15px;
  margin-top: 15px;
  border-radius: 4px;
  text-align: center;
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
</style>

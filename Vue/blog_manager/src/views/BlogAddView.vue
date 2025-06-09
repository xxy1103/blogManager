<template>
  <div class="blog-add-view">
    <div class="banner-image">
      <div class="banner-content">
        <h1 class="page-title">创建新博客</h1>
      </div>
      <div class="banner-overlay"></div>
    </div>

    <div class="form-container">
      <form @submit.prevent="submitForm" class="blog-form">
        <div class="form-group">
          <label for="title">标题</label>
          <input
            type="text"
            id="title"
            v-model="formData.title"
            required
            placeholder="请输入博客标题"
          />
        </div>

        <div class="form-group">
          <label for="categories">分类</label>
          <input
            type="text"
            id="categories"
            v-model="formData.categories"
            required
            placeholder="请输入博客分类"
          />
        </div>

        <div class="form-group">
          <label for="tags">标签</label>
          <div class="tags-input">
            <input
              type="text"
              id="tag-input"
              v-model="newTag"
              @keydown.enter.prevent="addTag"
              placeholder="输入标签后按回车添加"
            />
            <button type="button" @click="addTag" class="tag-button">添加</button>
          </div>
          <div class="tags-list" v-if="formData.tags.length > 0">
            <span v-for="(tag, index) in formData.tags" :key="index" class="tag">
              {{ tag }}
              <button type="button" @click="removeTag(index)" class="remove-tag">&times;</button>
            </span>
          </div>
        </div>

        <div class="form-group">
          <label for="saying">摘要</label>
          <textarea
            id="saying"
            v-model="formData.saying"
            rows="3"
            required
            placeholder="请输入博客摘要"
          ></textarea>
        </div>

        <div class="form-group">
          <label for="content">内容</label>
          <textarea
            id="content"
            v-model="formData.content"
            rows="10"
            required
            placeholder="请输入博客内容"
          ></textarea>
        </div>

        <div class="form-actions">
          <button type="submit" class="submit-button" :disabled="submitting">
            <span v-if="submitting" class="spinning-icon">⟳</span>
            {{ submitting ? '提交中...' : '创建博客' }}
          </button>
          <router-link to="/" class="cancel-button">取消</router-link>
        </div>
      </form>

      <div v-if="formMessage.show" :class="['message', formMessage.type]">
        <span v-if="formMessage.type === 'success'" class="message-icon">✓</span>
        <span v-else class="message-icon">✗</span>
        {{ formMessage.text }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { addBlog } from '../services/blogService.js'

const router = useRouter()
const newTag = ref('')
const submitting = ref(false)

const formData = reactive({
  title: '',
  categories: '',
  tags: [] as string[],
  saying: '',
  content: '', // 新增 content 字段
})

const formMessage = reactive({
  show: false,
  text: '',
  type: 'success',
})

const addTag = () => {
  if (newTag.value.trim() && !formData.tags.includes(newTag.value.trim())) {
    formData.tags.push(newTag.value.trim())
    newTag.value = ''
  }
}

const removeTag = (index: number) => {
  formData.tags.splice(index, 1)
}

const submitForm = async () => {
  if (submitting.value) return

  // 验证是否添加了标签
  if (formData.tags.length === 0) {
    formMessage.text = '请至少添加一个标签'
    formMessage.type = 'error'
    formMessage.show = true
    return
  }

  // 验证内容是否为空
  if (!formData.content.trim()) {
    formMessage.text = '博客内容不能为空'
    formMessage.type = 'error'
    formMessage.show = true
    return
  }

  submitting.value = true
  formMessage.show = false

  try {
    const result = await addBlog(
      formData.title,
      formData.categories,
      formData.tags,
      formData.saying,
      formData.content, // 使用 formData 中的 content
    )

    if (result.success) {
      formMessage.text = '博客创建成功！'
      formMessage.type = 'success'
      formMessage.show = true

      // 重置表单
      formData.title = ''
      formData.categories = ''
      formData.tags = []
      formData.saying = ''
      formData.content = '' // 重置 content 字段

      // 延迟后重定向到博客列表页
      setTimeout(() => {
        router.push('/')
      }, 2000)
    } else {
      formMessage.text = result.message || '创建博客失败'
      formMessage.type = 'error'
      formMessage.show = true
    }
  } catch (error) {
    formMessage.text = error instanceof Error ? error.message : '创建博客失败'
    formMessage.type = 'error'
    formMessage.show = true
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.blog-add-view {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.banner-image {
  width: 100%;
  height: 250px;
  background-image: url('https://images.unsplash.com/photo-1455390582262-044cdead277a');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 2rem;
  position: relative;
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(rgba(30, 36, 59, 0.7), rgba(30, 36, 59, 0.9));
  z-index: 1;
}

.banner-content {
  text-align: center;
  padding: 2rem;
  position: relative;
  z-index: 2;
}

.page-title {
  font-size: 2.2rem;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  margin: 0;
}

.form-container {
  width: 100%;
  max-width: 800px;
  margin: 0 auto 2rem;
  padding: 0 1rem;
}

.blog-form {
  background-color: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius);
  padding: 2rem;
  box-shadow: var(--box-shadow);
  margin-bottom: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--color-heading);
  font-size: 1rem;
}

input[type='text'],
textarea {
  width: 100%;
  padding: 0.8rem;
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius);
  font-size: 1rem;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

input[type='text']:focus,
textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(74, 136, 229, 0.2);
}

input[type='text']::placeholder,
textarea::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.tags-input {
  display: flex;
}

.tags-input input {
  flex-grow: 1;
  margin-right: 10px;
}

.tag-button {
  background-color: var(--color-primary);
  color: white;
  border: none;
  border-radius: 4px;
  padding: 0 1rem;
  cursor: pointer;
  transition: background-color 0.3s;
  font-weight: 500;
}

.tag-button:hover {
  background-color: var(--color-primary-dark);
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 1rem;
}

.tag {
  background-color: rgba(74, 136, 229, 0.1);
  border: 1px solid rgba(74, 136, 229, 0.3);
  border-radius: 16px;
  padding: 0.3rem 0.8rem;
  display: inline-flex;
  align-items: center;
  color: var(--color-primary-light);
}

.remove-tag {
  background: none;
  border: none;
  color: var(--color-text-muted);
  margin-left: 5px;
  cursor: pointer;
  font-size: 1.2em;
  line-height: 1;
  padding: 0 3px;
  transition: color 0.2s;
}

.remove-tag:hover {
  color: var(--color-primary-light);
}

.form-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 2rem;
}

.submit-button,
.cancel-button {
  padding: 0.8rem 1.5rem;
  border-radius: var(--border-radius);
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s ease;
  text-decoration: none;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-button {
  background-color: var(--color-accent);
  color: #262c45;
  border: none;
  flex-grow: 1;
  margin-right: 1rem;
}

.submit-button:hover:not(:disabled) {
  background-color: var(--color-accent-light);
  transform: translateY(-2px);
}

.submit-button:disabled {
  background-color: var(--color-border);
  cursor: not-allowed;
  opacity: 0.7;
}

.cancel-button {
  background-color: rgba(255, 255, 255, 0.1);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  flex-grow: 0;
  min-width: 100px;
}

.cancel-button:hover {
  background-color: rgba(255, 255, 255, 0.15);
}

.message {
  padding: 1rem 1.5rem;
  border-radius: var(--border-radius);
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  animation: fadeIn 0.5s ease-in-out;
}

.success {
  background-color: rgba(76, 175, 80, 0.1);
  color: #4caf50;
  border-left: 3px solid #4caf50;
}

.error {
  background-color: rgba(244, 67, 54, 0.1);
  color: #f44336;
  border-left: 3px solid #f44336;
}

.message-icon {
  margin-right: 0.5rem;
  font-size: 1.2rem;
}

.spinning-icon {
  display: inline-block;
  margin-right: 0.5rem;
  animation: spin 1s linear infinite;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .banner-image {
    height: 180px;
  }

  .page-title {
    font-size: 1.8rem;
  }

  .blog-form {
    padding: 1.5rem;
  }

  .form-actions {
    flex-direction: column;
    gap: 1rem;
  }

  .submit-button {
    margin-right: 0;
  }
}
</style>

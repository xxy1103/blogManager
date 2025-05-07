<template>
  <div class="blog-add-view">
    <h1>创建新博客</h1>
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

      <div class="form-actions">
        <button type="submit" class="submit-button" :disabled="submitting">
          {{ submitting ? '提交中...' : '创建博客' }}
        </button>
        <router-link to="/" class="cancel-button">取消</router-link>
      </div>
    </form>

    <div v-if="formMessage.show" :class="['message', formMessage.type]">
      {{ formMessage.text }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { addBlog } from '@/services/blogService'

const router = useRouter()
const newTag = ref('')
const submitting = ref(false)

const formData = reactive({
  title: '',
  categories: '',
  tags: [] as string[],
  saying: '',
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

  submitting.value = true
  formMessage.show = false

  try {
    const result = await addBlog(
      formData.title,
      formData.categories,
      formData.tags,
      formData.saying,
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
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
  font-family: sans-serif;
}

h1 {
  margin-bottom: 30px;
  text-align: center;
  color: #333;
}

.blog-form {
  background-color: #f9f9f9;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 25px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #555;
}

input[type='text'],
textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1em;
  transition: border-color 0.3s;
}

input[type='text']:focus,
textarea:focus {
  border-color: #4a90e2;
  outline: none;
}

.tags-input {
  display: flex;
}

.tags-input input {
  flex-grow: 1;
  margin-right: 10px;
}

.tag-button {
  background-color: #4a90e2;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 0 15px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.tag-button:hover {
  background-color: #3a7bc8;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.tag {
  background-color: #e7f3ff;
  border: 1px solid #d0e6fb;
  border-radius: 16px;
  padding: 4px 10px;
  display: inline-flex;
  align-items: center;
}

.remove-tag {
  background: none;
  border: none;
  color: #999;
  margin-left: 5px;
  cursor: pointer;
  font-size: 1.2em;
  line-height: 1;
  padding: 0 3px;
}

.remove-tag:hover {
  color: #666;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 30px;
}

.submit-button,
.cancel-button {
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.3s ease;
  text-decoration: none;
  text-align: center;
}

.submit-button {
  background-color: #4caf50;
  color: white;
  border: none;
  flex-grow: 1;
  margin-right: 10px;
}

.submit-button:hover:not(:disabled) {
  background-color: #3d9c40;
}

.submit-button:disabled {
  background-color: #a5d6a7;
  cursor: not-allowed;
}

.cancel-button {
  background-color: #f2f2f2;
  color: #666;
  border: 1px solid #ddd;
  flex-grow: 0;
  min-width: 80px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.cancel-button:hover {
  background-color: #e6e6e6;
}

.message {
  margin-top: 20px;
  padding: 15px;
  border-radius: 4px;
  text-align: center;
}

.success {
  background-color: #dff0d8;
  color: #3c763d;
  border: 1px solid #d6e9c6;
}

.error {
  background-color: #f2dede;
  color: #a94442;
  border: 1px solid #ebccd1;
}
</style>

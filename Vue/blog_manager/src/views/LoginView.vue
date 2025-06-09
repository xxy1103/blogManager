<template>
  <div class="login-container main-content">
    <div class="login-form card">
      <h2 class="form-title">登录博客管理系统</h2>

      <form @submit.prevent="handleLogin" class="space-y-6">
        <div class="form-group">
          <label for="username" class="form-label">用户名</label>
          <input
            id="username"
            v-model="formData.username"
            type="text"
            required
            :disabled="loading"
            placeholder="请输入用户名"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label for="password" class="form-label">密码</label>
          <input
            id="password"
            v-model="formData.password"
            type="password"
            required
            :disabled="loading"
            placeholder="请输入密码"
            class="form-input"
          />
        </div>

        <button type="submit" :disabled="loading" class="btn primary-btn w-full">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="form-footer">
        <p>还没有账号？ <router-link to="/register" class="link">立即注册</router-link></p>
      </div>

      <!-- 错误消息 -->
      <div v-if="errorMessage" class="error-message alert alert-danger">
        {{ errorMessage }}
      </div>

      <!-- 成功消息 -->
      <div v-if="successMessage" class="success-message alert alert-success">
        {{ successMessage }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import type { LoginRequest } from '../types/auth.js'

const router = useRouter()
const authStore = useAuthStore()

// 表单数据
const formData = reactive<LoginRequest>({
  username: '',
  password: '',
})

// 状态
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// 检查是否已登录
onMounted(() => {
  if (authStore.isAuthenticated) {
    router.push('/')
  }
})

// 处理登录
async function handleLogin() {
  if (!formData.username || !formData.password) {
    errorMessage.value = '请填写用户名和密码'
    return
  }

  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await authStore.login(formData)
    successMessage.value = '登录成功！'

    // 跳转到首页
    setTimeout(() => {
      router.push('/')
    }, 1000)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(
    100vh - var(--header-height, 0px) - var(--footer-height, 0px)
  ); /* 减去页眉页脚高度 */
  padding: 2rem;
  background-color: var(--color-background-soft); /* 与 App.vue main-content 一致 */
}

.login-form {
  background-color: var(--color-background-mute); /* 卡片背景色 */
  padding: 2.5rem;
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-medium);
  width: 100%;
  max-width: 420px; /* 稍微增大宽度 */
  border: 1px solid var(--color-border);
}

.form-title {
  text-align: center;
  margin-bottom: 2rem; /* 增大间距 */
  color: var(--color-heading);
  font-weight: 600;
  font-size: 1.75rem; /* 增大标题字号 */
}

.form-group {
  margin-bottom: 1.5rem; /* 增大间距 */
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--color-text-soft); /* 标签颜色调整 */
  font-weight: 500;
  font-size: 0.9rem;
}

.form-input {
  width: 100%;
  padding: 0.8rem 1rem; /* 调整内边距 */
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius-medium);
  font-size: 1rem;
  background-color: var(--color-background); /* 输入框背景 */
  color: var(--color-text);
  transition:
    border-color 0.3s,
    box-shadow 0.3s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-rgb), 0.2); /* 聚焦效果 */
}

.form-input:disabled {
  background-color: var(--color-background-soft);
  cursor: not-allowed;
  opacity: 0.7;
}

.btn {
  padding: 0.8rem 1.5rem;
  border-radius: var(--border-radius-medium);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition:
    background-color 0.3s,
    opacity 0.3s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  border: none;
}

.primary-btn {
  background-color: var(--color-primary);
  color: white;
}

.primary-btn:hover:not(:disabled) {
  background-color: var(--color-primary-dark); /* 鼠标悬停颜色 */
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.w-full {
  width: 100%;
}

.form-footer {
  text-align: center;
  margin-top: 1.5rem; /* 调整间距 */
}

.form-footer p {
  color: var(--color-text-soft);
  margin: 0;
  font-size: 0.9rem;
}

.form-footer .link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
}

.form-footer .link:hover {
  text-decoration: underline;
}

.alert {
  padding: 0.8rem 1rem;
  margin-top: 1.5rem;
  border-radius: var(--border-radius-medium);
  font-size: 0.9rem;
  text-align: center;
}

.alert-danger {
  background-color: rgba(var(--color-danger-rgb), 0.1);
  color: var(--color-danger);
  border: 1px solid var(--color-danger);
}

.alert-success {
  background-color: rgba(var(--color-success-rgb), 0.1);
  color: var(--color-success);
  border: 1px solid var(--color-success);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .login-form {
    padding: 2rem;
  }
  .form-title {
    font-size: 1.5rem;
  }
}
</style>

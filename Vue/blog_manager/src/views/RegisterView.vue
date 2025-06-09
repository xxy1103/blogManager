<template>
  <div class="register-container main-content">
    <div class="register-form card">
      <h2 class="form-title">注册博客管理系统</h2>

      <form @submit.prevent="handleRegister" class="space-y-6">
        <div class="form-group">
          <label for="username" class="form-label">用户名</label>
          <input
            id="username"
            v-model="formData.username"
            type="text"
            required
            :disabled="loading"
            placeholder="请输入用户名（3-50字符）"
            minlength="3"
            maxlength="50"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label for="email" class="form-label">邮箱地址</label>
          <input
            id="email"
            v-model="formData.email"
            type="email"
            required
            :disabled="loading"
            placeholder="请输入邮箱地址"
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
            placeholder="请输入密码（至少6位）"
            minlength="6"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label for="confirmPassword" class="form-label">确认密码</label>
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            type="password"
            required
            :disabled="loading"
            placeholder="请再次输入密码"
            class="form-input"
          />
        </div>

        <button type="submit" :disabled="loading" class="btn primary-btn w-full">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <div class="form-footer">
        <p>已有账号？ <router-link to="/login" class="link">立即登录</router-link></p>
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
import type { RegisterRequest } from '../types/auth.js'

const router = useRouter()
const authStore = useAuthStore()

// 表单数据
const formData = reactive<RegisterRequest>({
  username: '',
  email: '',
  password: '',
})

// 确认密码
const confirmPassword = ref('')

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

// 表单验证
function validateForm(): boolean {
  if (!formData.username || !formData.email || !formData.password) {
    errorMessage.value = '请填写所有必填字段'
    return false
  }

  if (formData.username.length < 3 || formData.username.length > 50) {
    errorMessage.value = '用户名长度必须在3-50字符之间'
    return false
  }

  if (formData.password.length < 6) {
    errorMessage.value = '密码至少需要6位字符'
    return false
  }

  if (formData.password !== confirmPassword.value) {
    errorMessage.value = '两次输入的密码不一致'
    return false
  }

  // 简单的邮箱格式验证
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(formData.email)) {
    errorMessage.value = '请输入有效的邮箱地址'
    return false
  }

  return true
}

// 处理注册
async function handleRegister() {
  errorMessage.value = ''
  successMessage.value = ''

  if (!validateForm()) {
    return
  }

  loading.value = true

  try {
    await authStore.register(formData)
    successMessage.value = '注册成功！请登录'

    // 清空表单
    formData.username = ''
    formData.email = ''
    formData.password = ''
    confirmPassword.value = ''

    // 跳转到登录页
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(
    100vh - var(--header-height, 0px) - var(--footer-height, 0px)
  ); /* 减去页眉页脚高度 */
  padding: 2rem;
  background-color: var(--color-background-soft); /* 与 App.vue main-content 一致 */
}

.register-form {
  background-color: var(--color-background-mute); /* 卡片背景色 */
  padding: 2.5rem;
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-medium);
  width: 100%;
  max-width: 450px; /* 保持原有宽度或微调 */
  border: 1px solid var(--color-border);
}

.form-title {
  text-align: center;
  margin-bottom: 2rem;
  color: var(--color-heading);
  font-weight: 600;
  font-size: 1.75rem;
}

.form-group {
  margin-bottom: 1.25rem; /* 略微调整间距 */
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--color-text-soft);
  font-weight: 500;
  font-size: 0.9rem;
}

.form-input {
  width: 100%;
  padding: 0.8rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius-medium);
  font-size: 1rem;
  background-color: var(--color-background);
  color: var(--color-text);
  transition:
    border-color 0.3s,
    box-shadow 0.3s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-rgb), 0.2);
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
  background-color: var(--color-primary-dark);
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
  margin-top: 1.5rem;
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
  .register-form {
    padding: 2rem;
  }
  .form-title {
    font-size: 1.5rem;
  }
}
</style>

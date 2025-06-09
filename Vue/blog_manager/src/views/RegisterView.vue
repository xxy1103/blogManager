<template>
  <div class="register-container">
    <div class="register-form">
      <h2>注册博客管理系统</h2>

      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="formData.username"
            type="text"
            required
            :disabled="loading"
            placeholder="请输入用户名（3-50字符）"
            minlength="3"
            maxlength="50"
          />
        </div>

        <div class="form-group">
          <label for="email">邮箱地址</label>
          <input
            id="email"
            v-model="formData.email"
            type="email"
            required
            :disabled="loading"
            placeholder="请输入邮箱地址"
          />
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="formData.password"
            type="password"
            required
            :disabled="loading"
            placeholder="请输入密码（至少6位）"
            minlength="6"
          />
        </div>

        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            type="password"
            required
            :disabled="loading"
            placeholder="请再次输入密码"
          />
        </div>

        <button type="submit" :disabled="loading" class="register-btn">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <div class="form-footer">
        <p>已有账号？ <router-link to="/login">立即登录</router-link></p>
      </div>

      <!-- 错误消息 -->
      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>

      <!-- 成功消息 -->
      <div v-if="successMessage" class="success-message">
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
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-form {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 450px;
}

h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-weight: 600;
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  margin-bottom: 5px;
  color: #555;
  font-weight: 500;
}

input[type='text'],
input[type='email'],
input[type='password'] {
  width: 100%;
  padding: 12px;
  border: 2px solid #ddd;
  border-radius: 5px;
  font-size: 16px;
  transition: border-color 0.3s;
  box-sizing: border-box;
}

input[type='text']:focus,
input[type='email']:focus,
input[type='password']:focus {
  outline: none;
  border-color: #667eea;
}

input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.register-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.3s;
}

.register-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.register-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-footer {
  text-align: center;
  margin-top: 20px;
}

.form-footer p {
  color: #666;
  margin: 0;
}

.form-footer a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.form-footer a:hover {
  text-decoration: underline;
}

.error-message {
  background-color: #fee;
  color: #c33;
  padding: 10px;
  border-radius: 5px;
  margin-top: 15px;
  text-align: center;
  border: 1px solid #fcc;
}

.success-message {
  background-color: #efe;
  color: #363;
  padding: 10px;
  border-radius: 5px;
  margin-top: 15px;
  text-align: center;
  border: 1px solid #cfc;
}
</style>

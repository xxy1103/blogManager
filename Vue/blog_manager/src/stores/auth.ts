import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { AuthService } from '../services/authService.js'
import type { User, LoginRequest, RegisterRequest } from '../types/auth.js'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)

  // 计算属性
  const isAuthenticated = computed(() => !!token.value)

  // 初始化 - 从本地存储加载认证信息
  function initAuth() {
    const savedToken = AuthService.getToken()
    const savedUser = AuthService.getUser()

    if (savedToken && savedUser) {
      token.value = savedToken
      user.value = savedUser
    }
  }

  // 登录
  async function login(credentials: LoginRequest) {
    try {
      const response = await AuthService.login(credentials)

      // 保存认证信息
      AuthService.saveAuthData(response)

      // 更新状态
      token.value = response.accessToken
      user.value = {
        id: response.id,
        username: response.username,
        email: response.email,
      }

      return response
    } catch (error) {
      throw error
    }
  }

  // 注册
  async function register(userData: RegisterRequest) {
    try {
      const response = await AuthService.register(userData)
      return response
    } catch (error) {
      throw error
    }
  }

  // 登出
  function logout() {
    // 清除本地存储
    AuthService.clearAuthData()

    // 清除状态
    token.value = null
    user.value = null
  }

  return {
    // 状态
    user,
    token,

    // 计算属性
    isAuthenticated,

    // 方法
    initAuth,
    login,
    register,
    logout,
  }
})

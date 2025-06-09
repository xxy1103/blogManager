import axios from 'axios'
import type {
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  RegisterResponse,
  User,
} from '../types/auth.js'

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:5200',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器 - 添加认证token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 响应拦截器 - 处理token过期
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token过期，清除本地存储并重定向到登录页
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export class AuthService {
  /**
   * 用户登录
   */
  static async login(credentials: LoginRequest): Promise<LoginResponse> {
    try {
      const response = await api.post<LoginResponse>('/api/auth/login', credentials)
      return response.data
    } catch (error: unknown) {
      const errorMessage =
        error instanceof Error
          ? error.message
          : (error as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            '登录失败'
      throw new Error(errorMessage)
    }
  }

  /**
   * 用户注册
   */
  static async register(userData: RegisterRequest): Promise<RegisterResponse> {
    try {
      const response = await api.post<RegisterResponse>('/api/auth/register', userData)
      return response.data
    } catch (error: unknown) {
      const errorMessage =
        error instanceof Error
          ? error.message
          : (error as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            '注册失败'
      throw new Error(errorMessage)
    }
  }

  /**
   * 保存认证信息到本地存储
   */
  static saveAuthData(loginResponse: LoginResponse): void {
    localStorage.setItem('token', loginResponse.accessToken)
    localStorage.setItem(
      'user',
      JSON.stringify({
        id: loginResponse.id,
        username: loginResponse.username,
        email: loginResponse.email,
      }),
    )
  }

  /**
   * 清除认证信息
   */
  static clearAuthData(): void {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  /**
   * 获取本地存储的token
   */
  static getToken(): string | null {
    return localStorage.getItem('token')
  }
  /**
   * 获取本地存储的用户信息
   */
  static getUser(): User | null {
    const userStr = localStorage.getItem('user')
    return userStr ? JSON.parse(userStr) : null
  }

  /**
   * 检查是否已认证
   */
  static isAuthenticated(): boolean {
    return !!this.getToken()
  }
}

export default api

// 认证相关的类型定义

export interface User {
  id: number
  username: string
  email: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  id: number
  username: string
  email: string
}

export interface RegisterResponse {
  message: string
}

export interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
}

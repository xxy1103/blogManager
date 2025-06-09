import { AuthService } from './authService.js'

export interface SystemConfig {
  blogStoragePath: string
  imageStoragePath: string
  xmodelAPIKey: string
  bigmodelAPIKey: string
}

export class ConfigService {
  /**
   * 获取系统配置信息
   */
  static async getConfig(): Promise<SystemConfig | null> {
    try {
      const token = AuthService.getToken()
      const headers: HeadersInit = {}
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      const response = await fetch('/config/get', {
        headers: headers,
      })

      if (!response.ok) {
        throw new Error(`Network response was not ok (${response.status})`)
      }

      const config: SystemConfig = await response.json()
      return config
    } catch (error) {
      console.error('Failed to fetch config:', error)
      return null
    }
  }

  /**
   * 更新系统配置
   */
  static async updateConfig(config: SystemConfig): Promise<boolean> {
    try {
      const token = AuthService.getToken()
      if (!token) {
        throw new Error('用户未认证')
      }

      const response = await fetch('/config/set', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(config),
      })

      if (!response.ok) {
        throw new Error(`Network response was not ok (${response.status})`)
      }

      const result: boolean = await response.json()
      return result
    } catch (error) {
      console.error('Failed to update config:', error)
      return false
    }
  }
}

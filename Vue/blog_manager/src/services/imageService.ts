import { AuthService } from './authService.js'

interface ImageUploadResponse {
  success: boolean
  message: string
  path?: string
}

export class ImageService {
  /**
   * 上传图片到指定路径
   */
  static async uploadImage(file: File, relativePath: string): Promise<ImageUploadResponse> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('relativePath', relativePath)

    try {
      const token = AuthService.getToken()
      const headers: HeadersInit = {}
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      const response = await fetch('/image/upload', {
        method: 'POST',
        headers: headers,
        body: formData,
      })

      if (!response.ok) {
        const errorData = await response
          .json()
          .catch(() => ({ message: `Image upload failed with status: ${response.status}` }))
        throw new Error(errorData.message || `Image upload failed: ${response.status}`)
      }

      const result: ImageUploadResponse = await response.json()
      return result
    } catch (error) {
      console.error('Failed to upload image:', error)
      const errorMessage =
        error instanceof Error ? error.message : 'An unknown error occurred during image upload'
      return { success: false, message: errorMessage }
    }
  }

  /**
   * 根据相对路径获取图片
   */
  static getImageUrl(relativePath: string): string {
    return `/image/${relativePath.replace(/^\//, '')}`
  }

  /**
   * 获取图片文件
   */
  static async getImage(relativePath: string): Promise<Blob | null> {
    try {
      const token = AuthService.getToken()
      const headers: HeadersInit = {}
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      const response = await fetch(`/image/${encodeURIComponent(relativePath)}`, {
        method: 'GET',
        headers: headers,
      })

      if (!response.ok) {
        if (response.status === 404) {
          console.warn(`Image not found: ${relativePath}`)
          return null
        }
        const errorText = await response
          .text()
          .catch(() => `Failed to fetch image ${relativePath} with status: ${response.status}`)
        throw new Error(errorText)
      }

      return await response.blob()
    } catch (error) {
      console.error(`Failed to fetch image ${relativePath}:`, error)
      return null
    }
  }
}

import type { BlogListItem, BlogDetail } from '../types/blog'
import { AuthService } from './authService.js' // 导入 AuthService

const API_BASE_URL = '/api' // 使用相对路径，将通过 Vite 代理转发

interface ApiResponse<T> {
  status: number
  data: T | null
  error: string | null
}

// Helper function to process markdown image paths for display
export function processBlogContentForDisplay(content: string): string {
  if (!content) return ''
  // Regex to find markdown images like ![alt text](image/path/to/image.png)
  // Captures: 1=altText, 2=imagePath
  const markdownImageRegex = new RegExp('!\\[([^\\]]*)\\]\\((image/[^)]+)\\)', 'g')
  return content.replace(markdownImageRegex, (match, altText, imagePath) => {
    return `![${altText}](/${imagePath})` // Changed API_BASE_URL to /image
  })
}

export async function getAllBlogs(): Promise<BlogListItem[]> {
  try {
    const token = AuthService.getToken() // 获取认证令牌
    const headers: HeadersInit = {}
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch(`${API_BASE_URL}/blogs/lists`, {
      headers: headers, // 添加请求头
    })
    if (!response.ok) {
      throw new Error('Network response was not ok')
    }
    const result: ApiResponse<BlogListItem[]> = await response.json()
    if (result.status === 0 && result.data) {
      return result.data
    } else {
      console.error('Error fetching blogs:', result.error)
      return []
    }
  } catch (error) {
    console.error('Failed to fetch blogs:', error)
    return []
  }
}

export async function getBlogDetail(id: number): Promise<BlogDetail | null> {
  try {
    const token = AuthService.getToken()
    const headers: HeadersInit = {}
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch(`${API_BASE_URL}/blogs/${id}`, {
      headers: headers,
    })

    if (!response.ok) {
      let errorMsg = `Network response was not ok (${response.status})`
      try {
        const errorData = await response.json()
        if (errorData && (errorData.error || errorData.message)) {
          errorMsg = errorData.error || errorData.message
        }
      } catch {
        /* ignore parsing error if response is not json */
      }
      console.error('Error fetching blog detail:', errorMsg)
      return null
    }

    const result: ApiResponse<BlogDetail> = await response.json()

    if (result.status === 0 && result.data) {
      if (result.data.content) {
        result.data.content = processBlogContentForDisplay(result.data.content)
      }
      return result.data
    } else {
      console.error('API error fetching blog detail:', result.error || 'Unknown API error')
      return null
    }
  } catch (error) {
    console.error('Exception in getBlogDetail:', (error as Error).message)
    return null
  }
}

export async function updateBlogContent(
  year: string,
  month: string,
  day: string,
  filename: string,
  content: string,
): Promise<{ success: boolean; message: string }> {
  try {
    const contentToSave = content
    // URL 编码文件名以处理特殊字符
    const encodedFilename = encodeURIComponent(filename)
    const response = await fetch(
      `${API_BASE_URL}/blogs/${year}/${month}/${day}/${encodedFilename}/updatecontent`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain',
        },
        body: contentToSave, // Send reverted content
      },
    )
    if (!response.ok) {
      throw new Error('Network response was not ok')
    }

    const result: ApiResponse<null> = await response.json()
    if (result.status === 0) {
      return { success: true, message: '更新博客内容成功' }
    } else {
      console.error('Error updating blog content:', result.error)
      return { success: false, message: result.error || '更新博客内容失败' }
    }
  } catch (error) {
    console.error('Failed to update blog content:', error)
    return {
      success: false,
      message: error instanceof Error ? error.message : '更新博客内容失败',
    }
  }
}

// 根据API文档 2.3 创建博客
export async function addBlog(
  title: string,
  categories: string,
  tags: string[],
  saying: string,
  content: string, // 确保 content 参数已定义
): Promise<{ success: boolean; message: string; data?: null }> {
  // API响应 data 为 null
  try {
    const token = AuthService.getToken()
    if (!token) {
      return { success: false, message: '用户未认证' }
    }

    const blogData = {
      title,
      categories,
      tags,
      saying,
      content, // 使用传入的 content 参数
    }

    const response = await fetch(`${API_BASE_URL}/blogs`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(blogData),
    })

    if (!response.ok) {
      let errorMsg = `Network response was not ok (${response.status})`
      try {
        const errorData = await response.json()
        if (errorData && (errorData.error || errorData.message)) {
          errorMsg = errorData.error || errorData.message
        }
      } catch {
        // ignore parsing error if response is not json
      }
      console.error('Error adding blog:', errorMsg)
      return { success: false, message: errorMsg }
    }

    const result: ApiResponse<null> = await response.json() // API响应 data 为 null

    if (result.status === 0) {
      return { success: true, message: '博客创建成功' }
    } else {
      console.error('Error adding blog:', result.error)
      return { success: false, message: result.error || '创建博客失败' }
    }
  } catch (error) {
    console.error('Failed to add blog:', error)
    return {
      success: false,
      message: error instanceof Error ? error.message : '创建博客失败',
    }
  }
}

export async function deleteBlog(
  year: string,
  month: string,
  day: string,
  filename: string,
): Promise<{ success: boolean; message: string }> {
  try {
    // URL 编码文件名以处理特殊字符
    const encodedFilename = encodeURIComponent(filename)
    const response = await fetch(
      `${API_BASE_URL}/blogs/${year}/${month}/${day}/${encodedFilename}/delete/`,
    )
    if (!response.ok) {
      throw new Error('Network response was not ok')
    }

    const result: ApiResponse<null> = await response.json()
    if (result.status === 0) {
      return { success: true, message: '删除博客成功' }
    } else {
      console.error('Error deleting blog:', result.error)
      return { success: false, message: result.error || '删除博客失败' }
    }
  } catch (error) {
    console.error('Failed to delete blog:', error)
    return {
      success: false,
      message: error instanceof Error ? error.message : '删除博客失败',
    }
  }
}

export async function updateBlogInfo(
  year: string,
  month: string,
  day: string,
  filename: string,
  title: string,
  categories: string,
  tags: string[],
  saying: string,
): Promise<{ success: boolean; message: string }> {
  try {
    // URL 编码文件名以处理特殊字符
    const encodedFilename = encodeURIComponent(filename)
    const tagsParams = tags.map((tag) => `tags=${encodeURIComponent(tag)}`).join('&')

    const url = `${API_BASE_URL}/blogs/${year}/${month}/${day}/${encodedFilename}/updateinfo?title=${encodeURIComponent(title)}&categories=${encodeURIComponent(categories)}&${tagsParams}&saying=${encodeURIComponent(saying)}`

    const response = await fetch(url)
    if (!response.ok) {
      throw new Error('Network response was not ok')
    }

    const result: ApiResponse<null> = await response.json()
    if (result.status === 0) {
      return { success: true, message: '更新博客信息成功' }
    } else {
      console.error('Error updating blog info:', result.error)
      return { success: false, message: result.error || '更新博客信息失败' }
    }
  } catch (error) {
    console.error('Failed to update blog info:', error)
    return {
      success: false,
      message: error instanceof Error ? error.message : '更新博客信息失败',
    }
  }
}

export async function uploadImage(file: File, relativePath: string): Promise<string> {
  const formData = new FormData()
  // The backend API /image/upload expects 'file' and 'relativePath'
  // The filename of the 'file' part in FormData should be the desired timestamped filename
  formData.append('file', file)
  formData.append('relativePath', relativePath)

  try {
    // As per API documentation, /image/upload is not prefixed with /api
    const response = await fetch('/image/upload', {
      method: 'POST',
      body: formData,
      // Headers like 'Content-Type': 'multipart/form-data' are typically set automatically by the browser for FormData
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`Image upload failed: ${response.status} ${errorText}`)
    }

    const result = await response.json()

    if (result.success && result.path) {
      // result.path is expected to be "relativePathOnServer/actualFilename.ext"
      // The markdown requires a root-relative path like "/image/relativePathOnServer/actualFilename.ext"
      return `/image/${result.path}` // Ensure the path is root-relative
    } else {
      throw new Error(result.message || 'Image upload failed due to server error')
    }
  } catch (error) {
    console.error('Failed to upload image:', error)
    // Re-throw the error so it can be caught by the calling function in the Vue component
    if (error instanceof Error) {
      throw error
    }
    throw new Error('An unknown error occurred during image upload')
  }
}

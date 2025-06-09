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

// Updated to align with API PUT /api/blogs/{id}
export async function updateBlogContent(
  id: number,
  blogData: {
    title: string
    categories: string
    tags: string[]
    saying: string
    content: string
  },
): Promise<{ success: boolean; message: string }> {
  try {
    const token = AuthService.getToken() // Get authentication token
    if (!token) {
      return { success: false, message: '用户未认证' }
    }

    const response = await fetch(`${API_BASE_URL}/blogs/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`, // Add Authorization header
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
        /* ignore parsing error if response is not json */
      }
      console.error('Error updating blog content:', errorMsg)
      return { success: false, message: errorMsg }
    }

    const result: ApiResponse<null> = await response.json() // API response data is null for successful update
    if (result.status === 0) {
      return { success: true, message: '博客更新成功' } // Adjusted success message
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

// Renamed from updateBlogInfo to reflect its actual operation of updating blog metadata
// Aligns with API PUT /api/blogs/{id} but only for metadata fields (excluding content)
export async function updateBlogMetadata(
  id: number,
  blogMetadata: {
    title: string
    categories: string
    tags: string[]
    saying: string
    // content is handled by updateBlogContent
  },
): Promise<{ success: boolean; message: string }> {
  try {
    const token = AuthService.getToken()
    if (!token) {
      return { success: false, message: '用户未认证' }
    }

    // Fetch the current blog content first, as the API expects the full blog object for an update.
    // Alternatively, the backend API could be modified to accept partial updates (PATCH request).
    // For now, we assume PUT requires all fields. If content is not meant to be changed here,
    // we need to send the existing content.
    const currentBlog = await getBlogDetail(id) // This service function already handles token
    if (!currentBlog) {
      return { success: false, message: '无法获取当前博客内容以进行更新' }
    }

    const blogDataForUpdate = {
      ...blogMetadata, // title, categories, tags, saying from input
      content: currentBlog.content, // Preserve existing content
    }

    const response = await fetch(`${API_BASE_URL}/blogs/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(blogDataForUpdate),
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
      console.error('Error updating blog metadata:', errorMsg)
      return { success: false, message: errorMsg }
    }

    const result: ApiResponse<null> = await response.json()
    if (result.status === 0) {
      return { success: true, message: '博客信息更新成功' }
    } else {
      console.error('Error updating blog metadata:', result.error)
      return { success: false, message: result.error || '更新博客信息失败' }
    }
  } catch (error) {
    console.error('Failed to update blog metadata:', error)
    return {
      success: false,
      message: error instanceof Error ? error.message : '更新博客信息失败',
    }
  }
}

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
  id: number, // Updated to use blog ID as per API docs
): Promise<{ success: boolean; message: string }> {
  try {
    const token = AuthService.getToken() // 获取认证令牌
    if (!token) {
      return { success: false, message: '用户未认证' }
    }

    const response = await fetch(`${API_BASE_URL}/blogs/${id}`, {
      // Updated endpoint
      method: 'DELETE', // Added DELETE method
      headers: {
        Authorization: `Bearer ${token}`, // Added Authorization header
      },
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
      console.error('Error deleting blog:', errorMsg)
      return { success: false, message: errorMsg }
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

// Updated uploadImage function based on the new API documentation
export async function uploadImage(
  file: File,
): Promise<{ success: boolean; message: string; imageId?: number; imageUrl?: string }> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const token = AuthService.getToken()
    const headers: HeadersInit = {}
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    // The API endpoint is /image/upload and is not prefixed with /api
    const response = await fetch('/image/upload', {
      // Ensure this matches your server configuration (e.g., http://localhost:8080/image/upload if not using a proxy)
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

    const result = await response.json()

    if (result.success && result.imageId && result.fileName) {
      // Construct the URL to access the image, based on the GET /image/{filename} endpoint
      // This assumes the image will be accessible via this path structure immediately after upload.
      const imageUrl = `/image/${result.fileName}` // Root-relative path for use in markdown or <img> src
      return {
        success: true,
        message: result.message,
        imageId: result.imageId,
        imageUrl: imageUrl, // Return the directly usable URL
      }
    } else {
      throw new Error(result.message || 'Image upload failed: Invalid server response')
    }
  } catch (error) {
    console.error('Failed to upload image:', error)
    const errorMessage =
      error instanceof Error ? error.message : 'An unknown error occurred during image upload'
    return { success: false, message: errorMessage } // Return a structured error
  }
}

// Function to get an image by its filename
// Based on API: GET /image/{filename}
export async function getImageByFilename(filename: string): Promise<Blob | null> {
  try {
    const token = AuthService.getToken()
    const headers: HeadersInit = {}
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
      // Add any other headers your backend might require for authenticated image access
    }

    // The API endpoint is /image/{filename}
    const response = await fetch(`/image/${encodeURIComponent(filename)}`, {
      method: 'GET',
      headers: headers,
    })

    if (!response.ok) {
      if (response.status === 404) {
        console.warn(`Image not found: ${filename}`)
        return null
      }
      const errorText = await response
        .text()
        .catch(() => `Failed to fetch image ${filename} with status: ${response.status}`)
      throw new Error(errorText)
    }

    // The response body is the raw image data
    return await response.blob()
  } catch (error) {
    console.error(`Failed to fetch image ${filename}:`, error)
    return null // Or re-throw if the caller should handle it more specifically
  }
}

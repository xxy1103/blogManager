import type { BlogListItem, BlogDetail } from '../types/blog'

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
    return `![${altText}](${API_BASE_URL}/${imagePath})`
  })
}

// Helper function to revert image paths for saving
export function revertBlogContentForSave(content: string): string {
  if (!content) return ''
  // Regex to find markdown images with the API base URL like ![alt text](/api/image/path/to/image.png)
  // Captures: 1=altText, 2=fullApiPath starting with /api/
  const apiImageRegex = new RegExp('!\\[([^\\]]*)\\]\\(((\\/api)?\\/image\\/[^)]+)\\)', 'g')
  return content.replace(apiImageRegex, (match, altText, fullApiPath) => {
    // Remove the API_BASE_URL part (e.g., '/api') to get the original relative path
    const pathWithoutApiBase = fullApiPath.substring(API_BASE_URL.length)
    // If pathWithoutApiBase is '/image/path.png', remove leading '/' to make it 'image/path.png'
    if (pathWithoutApiBase.startsWith('/')) {
      return `![${altText}](${pathWithoutApiBase.substring(1)})`
    }
    return `![${altText}](${pathWithoutApiBase})` // Fallback, though should usually have leading slash
  })
}

export async function getAllBlogs(): Promise<BlogListItem[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/blogs/lists`)
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

export async function getBlogDetail(
  year: string,
  month: string,
  day: string,
  filename: string,
): Promise<BlogDetail | null> {
  try {
    // URL 编码文件名以处理特殊字符
    const encodedFilename = encodeURIComponent(filename)
    const response = await fetch(
      `${API_BASE_URL}/blogs/${year}/${month}/${day}/${encodedFilename}/`,
    )
    if (!response.ok) {
      throw new Error('Network response was not ok')
    }
    const result: ApiResponse<BlogDetail> = await response.json()
    if (result.status === 0 && result.data) {
      if (result.data.content) {
        result.data.content = processBlogContentForDisplay(result.data.content)
      }
      return result.data
    } else {
      console.error('Error fetching blog detail:', result.error)
      return null
    }
  } catch (error) {
    console.error('Failed to fetch blog detail:', error)
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
    const contentToSave = revertBlogContentForSave(content)
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

export async function addBlog(
  title: string,
  categories: string,
  tags: string[],
  saying: string,
): Promise<{ success: boolean; message: string; filename?: string }> {
  try {
    const queryParams = new URLSearchParams({
      title,
      categories,
      saying,
    })
    tags.forEach((tag) => queryParams.append('tags', tag))

    const response = await fetch(`${API_BASE_URL}/blogs/add?${queryParams.toString()}`)
    if (!response.ok) {
      throw new Error('Network response was not ok')
    }
    const result: ApiResponse<{ filename: string }> = await response.json() // Assuming API returns filename
    if (result.status === 0) {
      return { success: true, message: '添加博客成功', filename: result.data?.filename }
    } else {
      console.error('Error adding blog:', result.error)
      return { success: false, message: result.error || '添加博客失败' }
    }
  } catch (error) {
    console.error('Failed to add blog:', error)
    return {
      success: false,
      message: error instanceof Error ? error.message : '添加博客失败',
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

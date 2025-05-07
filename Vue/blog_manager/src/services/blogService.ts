import type { BlogListItem, BlogDetail } from '../types/blog'

const API_BASE_URL = '/api' // 使用相对路径，将通过 Vite 代理转发

interface ApiResponse<T> {
  status: number
  data: T | null
  error: string | null
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
    // URL 编码文件名以处理特殊字符
    const encodedFilename = encodeURIComponent(filename)
    const response = await fetch(
      `${API_BASE_URL}/blogs/${year}/${month}/${day}/${encodedFilename}/updatecontent`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain',
        },
        body: content,
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
): Promise<{ success: boolean; message: string }> {
  try {
    const tagsParams = tags.map((tag) => `tags=${encodeURIComponent(tag)}`).join('&')
    const url = `${API_BASE_URL}/blogs/add?title=${encodeURIComponent(title)}&categories=${encodeURIComponent(categories)}&${tagsParams}&saying=${encodeURIComponent(saying)}`

    const response = await fetch(url)
    if (!response.ok) {
      throw new Error('Network response was not ok')
    }

    const result: ApiResponse<null> = await response.json()
    if (result.status === 0) {
      return { success: true, message: '添加博客成功' }
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

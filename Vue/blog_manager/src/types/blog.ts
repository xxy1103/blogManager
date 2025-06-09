// src/types/blog.ts
export interface BlogListItem {
  id: number // 添加 id 字段
  title: string
  filename: string
  categories: string
  tags: string[]
  saying: string
  dateTime: string // 将 date 修改为 dateTime
}

export interface BlogDetail extends BlogListItem {
  filepath: string
  content: string
}

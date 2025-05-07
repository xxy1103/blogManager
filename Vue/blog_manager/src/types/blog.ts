// src/types/blog.ts
export interface BlogListItem {
  title: string
  filename: string
  categories: string
  tags: string[]
  saying: string
  date: string
}

export interface BlogDetail extends BlogListItem {
  filepath: string
  content: string
}

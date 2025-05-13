<template>
  <div class="blog-list-view">
    <div class="header-container">
      <h1>我的博客</h1>
      <router-link to="/blog/add" class="add-blog-btn"> <span>+</span> 创建新博客 </router-link>
    </div>
    <div v-if="loading" class="loading">正在加载...</div>
    <div v-if="error" class="error">{{ error }}</div>
    <ul v-if="!loading && !error && blogs.length > 0" class="blog-list">
      <li v-for="blog in blogs" :key="blog.filename" class="blog-item">
        <router-link :to="getBlogDetailLink(blog)">
          <h2>{{ blog.title }}</h2>
        </router-link>
        <p class="date">{{ formatDate(blog.date) }}</p>
        <p class="categories">分类：{{ blog.categories }}</p>
        <p class="tags">标签：{{ blog.tags.join(', ') }}</p>
        <p class="saying">{{ blog.saying }}</p>
      </li>
    </ul>
    <div v-if="!loading && !error && blogs.length === 0" class="no-blogs">暂无博客</div>

    <!-- 设置图标 -->
    <router-link to="/settings" class="settings-icon"> ⚙️ </router-link>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAllBlogs } from '@/services/blogService'
import type { BlogListItem } from '@/types/blog'

const blogs = ref<BlogListItem[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const fetchBlogs = async () => {
  loading.value = true
  error.value = null
  try {
    blogs.value = await getAllBlogs()
  } catch (err) {
    error.value = '加载博客列表失败，请稍后再试。'
    console.error(err)
  }
  loading.value = false
}

const getBlogDetailLink = (blog: BlogListItem) => {
  // 解析 ISO 格式的日期字符串 (如 "2025-04-25T23:10:05")
  const date = new Date(blog.date)
  const year = date.getFullYear()
  // getMonth() 返回 0-11，需要 +1
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  // URL 编码文件名以处理特殊字符
  const encodedFilename = encodeURIComponent(blog.filename)
  return `/blog/${year}/${month}/${day}/${encodedFilename}`
}

const formatDate = (dateString: string) => {
  // ISO 格式的日期字符串可以直接传递给 Date 构造函数
  const date = new Date(dateString)
  // 格式化日期和时间
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  fetchBlogs()
})
</script>

<style scoped>
.blog-list-view {
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
  font-family: sans-serif;
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.add-blog-btn {
  display: inline-block;
  background-color: #4caf50;
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  text-decoration: none;
  font-weight: bold;
  transition: background-color 0.3s;
  display: flex;
  align-items: center;
}

.add-blog-btn span {
  font-size: 18px;
  margin-right: 5px;
}

.add-blog-btn:hover {
  background-color: #3d9c40;
}

.loading,
.error,
.no-blogs {
  text-align: center;
  padding: 20px;
  font-size: 1.2em;
}

.error {
  color: red;
}

.blog-list {
  list-style: none;
  padding: 0;
}

.blog-item {
  background-color: #f9f9f9;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease-in-out;
}

.blog-item:hover {
  transform: translateY(-5px);
}

.blog-item h2 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 1.8em;
}

.blog-item a {
  text-decoration: none;
  color: #337ab7;
}

.blog-item a:hover {
  text-decoration: underline;
}

.date {
  font-size: 0.9em;
  color: #777;
  margin-bottom: 5px;
}

.categories,
.tags {
  font-size: 0.9em;
  color: #555;
  margin-bottom: 5px;
}

.saying {
  font-size: 1em;
  color: #333;
  line-height: 1.6;
}

.settings-icon {
  position: fixed;
  bottom: 20px;
  right: 20px;
  font-size: 24px; /* 调整图标大小 */
  padding: 10px;
  background-color: #f0f0f0;
  border-radius: 50%;
  cursor: pointer;
  text-decoration: none;
  color: #333;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  transition: background-color 0.3s;
}

.settings-icon:hover {
  background-color: #e0e0e0;
}
</style>

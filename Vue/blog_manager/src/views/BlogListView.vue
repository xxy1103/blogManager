<template>
  <div class="blog-list-view">
    <div class="header-container">
      <h1 class="page-title">我的博客</h1>
      <div class="header-actions">
        <router-link to="/blog/search" class="search-blog-btn">
          <span>🔍</span> 搜索博客
        </router-link>
        <router-link to="/blog/add" class="add-blog-btn"> <span>+</span> 创建新博客 </router-link>
      </div>
    </div>

    <div class="banner-image">
      <div class="banner-content">
        <div class="daily-task">今日任务：努力 (1/1) _</div>
      </div>
      <div class="banner-overlay"></div>
    </div>

    <div class="page-scroll-indicator">
      <span class="scroll-arrow">▼</span>
    </div>

    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>正在加载文章...</p>
    </div>

    <div v-if="error" class="error">{{ error }}</div>

    <div class="blog-list-container">
      <ul v-if="!loading && !error && blogs.length > 0" class="blog-list">
        <li v-for="blog in blogs" :key="blog.id" class="blog-item">
          <div class="blog-item-content">
            <div class="blog-thumbnail-container">
              <img src="../assets/logo.svg" class="blog-thumbnail" alt="Blog thumbnail" />
            </div>

            <div class="blog-info">
              <router-link :to="getBlogDetailLink(blog)" class="blog-title-link">
                <h2 class="blog-title">{{ blog.title }}</h2>
              </router-link>

              <div class="blog-meta">
                <span class="blog-date">📅 {{ formatDate(blog.date) }}</span>
                <span class="blog-categories">📁 {{ blog.categories }}</span>
              </div>

              <div class="blog-tags-container">
                <span v-for="(tag, index) in blog.tags" :key="index" class="blog-tag">
                  #{{ tag }}
                </span>
              </div>

              <p class="blog-excerpt" v-if="blog.saying">{{ blog.saying }}</p>
            </div>
          </div>
        </li>
      </ul>
    </div>

    <div v-if="!loading && !error && blogs.length === 0" class="no-blogs">
      <p>暂无博客</p>
      <p>点击右上角的"创建新博客"按钮开始写作吧！</p>
    </div>


  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAllBlogs } from '../services/blogService.js'
import type { BlogListItem } from '../types/blog.js'

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
  return `/blog/${blog.id}` // Use blog.id for the link
}

const formatDate = (dateTimeString: string | undefined) => {
  if (!dateTimeString) {
    return '日期不可用'
  }
  const date = new Date(dateTimeString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

onMounted(() => {
  fetchBlogs()
})
</script>

<style scoped>
.blog-list-view {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  max-width: 900px;
  margin-bottom: 20px;
  padding: 0 1rem;
  flex-direction: column;
  align-items: flex-start;
  gap: 15px;
}

.page-title {
  font-size: 1.8em;
  color: var(--color-heading);
  margin: 0;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
  justify-content: space-between;
}

.search-blog-btn {
  display: inline-flex;
  align-items: center;
  background-color: var(--color-accent);
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  font-weight: 500;
  transition: all 0.3s ease;
  text-decoration: none;
  flex: 1;
  justify-content: center;
  min-width: 0;
}

.search-blog-btn span {
  margin-right: 5px;
  font-size: 16px;
}

.search-blog-btn:hover {
  background-color: var(--color-accent-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
}

.add-blog-btn {
  display: inline-flex;
  align-items: center;
  background-color: var(--color-primary);
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  font-weight: 500;
  transition: all 0.3s ease;
  flex: 1;
  justify-content: center;
  min-width: 0;
}

.add-blog-btn span {
  margin-right: 5px;
  font-size: 16px;
  font-weight: bold;
}

.add-blog-btn:hover {
  background-color: var(--color-primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
}

/* 添加图片中看到的横幅背景 */
.banner-image {
  width: 100%;
  height: 400px;
  background-image: url('https://images.unsplash.com/photo-1434394354979-a235cd36269d');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 2rem;
  position: relative;
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(rgba(30, 36, 59, 0.7), rgba(30, 36, 59, 0.9));
  z-index: 1;
}

.banner-content {
  text-align: center;
  padding: 2rem;
  position: relative;
  z-index: 2;
}

.daily-task {
  font-size: 1.8rem;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 1px;
  font-weight: 500;
}

.page-scroll-indicator {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  animation: bounce 2s infinite;
  z-index: 2;
}

@keyframes bounce {
  0%,
  20%,
  50%,
  80%,
  100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-20px);
  }
  60% {
    transform: translateY(-10px);
  }
}

.scroll-arrow {
  color: var(--color-accent);
  font-size: 24px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: var(--color-text);
}

.loading-spinner {
  border: 4px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  border-top: 4px solid var(--color-primary);
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.error {
  color: var(--color-error);
  text-align: center;
  padding: 1.5rem;
  background-color: rgba(244, 67, 54, 0.1);
  border-radius: var(--border-radius);
  border-left: 3px solid var(--color-error);
  margin: 2rem auto;
  max-width: 800px;
  width: 100%;
}

.blog-list-container {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  padding: 0 1rem;
}

.blog-list {
  list-style: none;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.blog-item {
  background-color: var(--color-card-bg);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: var(--box-shadow);
  transition: all 0.3s ease;
  border: 1px solid var(--color-border);
}

.blog-item:hover {
  transform: translateY(-5px);
  box-shadow: var(--box-shadow-hover);
  border-color: var(--color-primary-light);
}

.blog-item-content {
  padding: 0;
  display: flex;
  flex-direction: column;
}

.blog-thumbnail-container {
  width: 100%;
  height: 150px;
  overflow: hidden;
  position: relative;
  background-color: var(--color-background-mute);
  display: flex;
  align-items: center;
  justify-content: center;
}

.blog-thumbnail {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-bottom: 1px solid var(--color-border);
  transition: transform 0.3s ease;
}

.blog-item:hover .blog-thumbnail {
  transform: scale(1.05);
}

.blog-info {
  padding: 15px;
}

.blog-title {
  margin: 0 0 10px 0;
  font-size: 1.3rem;
  font-weight: 600;
  line-height: 1.4;
}

.blog-title-link {
  color: var(--color-heading);
  text-decoration: none;
}

.blog-title-link:hover {
  color: var(--color-primary);
}

.blog-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 10px;
}

.blog-date,
.blog-categories {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.blog-tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.blog-tag {
  background-color: rgba(74, 136, 229, 0.1);
  color: var(--color-primary-light);
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.blog-tag:hover {
  background-color: rgba(74, 136, 229, 0.2);
  transform: translateY(-1px);
}

.blog-excerpt {
  font-size: 0.9rem;
  color: var(--color-text);
  line-height: 1.6;
  margin: 0;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.no-blogs {
  text-align: center;
  padding: 3rem;
  background-color: var(--color-card-bg);
  border-radius: var(--border-radius);
  margin: 2rem 0;
  border: 1px dashed var(--color-border);
  color: var(--color-text-muted);
}

.no-blogs p:first-child {
  font-size: 1.2rem;
  margin-bottom: 1rem;
  font-weight: 500;
}

.settings-icon {
  position: fixed;
  bottom: 20px;
  right: 20px;
  font-size: 24px;
  padding: 15px;
  background-color: var(--color-primary);
  color: white;
  border-radius: 50%;
  cursor: pointer;
  text-decoration: none;
  box-shadow: var(--box-shadow);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  z-index: 100;
}

.settings-icon:hover {
  background-color: var(--color-primary-dark);
  transform: rotate(45deg);
}

@media (max-width: 768px) {
  .blog-list {
    grid-template-columns: 1fr;
  }

  .header-container {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .search-blog-btn,
  .add-blog-btn {
    flex: 1;
    justify-content: center;
    min-width: 0;
  }

  .banner-image {
    height: 300px;
  }

  .daily-task {
    font-size: 1.4rem;
  }
}
</style>

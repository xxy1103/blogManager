<template>
  <div class="blog-search-container">
    <div class="search-header">
      <h1>搜索博客</h1>
      <p class="search-description">根据标题、分类或标签搜索您的博客文章</p>
    </div>

    <!-- 搜索表单 -->
    <div class="search-form-container">
      <form @submit.prevent="performSearch" class="search-form">
        <div class="search-fields">
          <div class="field-group">
            <label for="title">标题关键字</label>
            <input
              id="title"
              v-model="searchParams.title"
              type="text"
              placeholder="输入标题关键字..."
              class="search-input"
            />
          </div>

          <div class="field-group">
            <label for="categories">分类</label>
            <input
              id="categories"
              v-model="searchParams.categories"
              type="text"
              placeholder="输入分类..."
              class="search-input"
            />
          </div>

          <div class="field-group">
            <label for="tags">标签</label>
            <input
              id="tags"
              v-model="searchParams.tags"
              type="text"
              placeholder="输入标签..."
              class="search-input"
            />
          </div>
        </div>

        <div class="search-actions">
          <button type="submit" class="search-btn" :disabled="isSearching">
            <span v-if="!isSearching">搜索</span>
            <span v-else>搜索中...</span>
          </button>
          <button type="button" @click="clearSearch" class="clear-btn">清空</button>
        </div>
      </form>
    </div>

    <!-- 搜索结果 -->
    <div class="search-results" v-if="hasSearched">
      <div class="results-header">
        <h2>搜索结果</h2>
        <span class="results-count">找到 {{ searchResults.length }} 条结果</span>
      </div>

      <!-- 无结果提示 -->
      <div v-if="searchResults.length === 0" class="no-results">
        <div class="no-results-icon">🔍</div>
        <h3>未找到相关博客</h3>
        <p>尝试调整搜索条件或使用不同的关键字</p>
      </div>

      <!-- 结果列表 -->
      <div v-else class="results-list">
        <div
          v-for="blog in searchResults"
          :key="blog.id"
          class="blog-card"
          @click="goToBlogDetail(blog.id)"
        >
          <div class="blog-header">
            <h3 class="blog-title">{{ blog.title }}</h3>
            <span class="blog-date">{{ formatDate(blog.date) }}</span>
          </div>

          <div class="blog-meta">
            <div class="blog-category" v-if="blog.categories">
              <span class="meta-label">分类：</span>
              <span class="category-tag">{{ blog.categories }}</span>
            </div>

            <div class="blog-tags" v-if="blog.tags && blog.tags.length > 0">
              <span class="meta-label">标签：</span>
              <span v-for="tag in blog.tags" :key="tag" class="tag">
                {{ tag }}
              </span>
            </div>
          </div>

          <div class="blog-actions">
            <button @click.stop="goToBlogDetail(blog.id)" class="view-btn">查看详情</button>
            <button @click.stop="goToBlogEdit(blog.id)" class="edit-btn">编辑</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="isSearching" class="loading-overlay">
      <div class="loading-spinner"></div>
      <p>正在搜索...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { searchBlogs } from '../services/blogService.js'
import type { BlogListItem } from '../types/blog.js'

const router = useRouter()

// 搜索参数
const searchParams = reactive({
  title: '',
  categories: '',
  tags: '',
})

// 搜索结果
const searchResults = ref<BlogListItem[]>([])
const isSearching = ref(false)
const hasSearched = ref(false)

// 执行搜索
async function performSearch() {
  // 检查是否至少有一个搜索条件
  const hasSearchTerms =
    searchParams.title.trim() || searchParams.categories.trim() || searchParams.tags.trim()

  if (!hasSearchTerms) {
    alert('请至少输入一个搜索条件')
    return
  }

  isSearching.value = true
  hasSearched.value = true

  try {
    // 构建搜索参数，只传递非空值
    const params: any = {}
    if (searchParams.title.trim()) {
      params.title = searchParams.title.trim()
    }
    if (searchParams.categories.trim()) {
      params.categories = searchParams.categories.trim()
    }
    if (searchParams.tags.trim()) {
      params.tags = searchParams.tags.trim()
    }

    const results = await searchBlogs(params)
    searchResults.value = results
  } catch (error) {
    console.error('搜索失败:', error)
    alert('搜索失败，请稍后重试')
  } finally {
    isSearching.value = false
  }
}

// 清空搜索
function clearSearch() {
  searchParams.title = ''
  searchParams.categories = ''
  searchParams.tags = ''
  searchResults.value = []
  hasSearched.value = false
}

// 导航到博客详情
function goToBlogDetail(id: number) {
  router.push(`/blog/${id}`)
}

// 导航到博客编辑
function goToBlogEdit(id: number) {
  router.push(`/blog/${id}/edit`)
}

// 格式化日期
function formatDate(dateTime: string): string {
  const date = new Date(dateTime)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style scoped>
.blog-search-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background-color: var(--color-background);
  min-height: 100vh;
}

.search-header {
  text-align: center;
  margin-bottom: 40px;
}

.search-header h1 {
  color: var(--color-heading);
  font-size: 2.5rem;
  margin-bottom: 10px;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.search-description {
  color: var(--color-text-muted);
  font-size: 1.1rem;
  margin: 0;
}

.search-form-container {
  background: var(--color-card-bg);
  border-radius: 12px;
  padding: 30px;
  box-shadow: var(--box-shadow);
  margin-bottom: 30px;
  border: 1px solid var(--color-border);
}

.search-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-fields {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-group label {
  font-weight: 600;
  color: var(--color-text);
  font-size: 0.95rem;
}

.search-input {
  padding: 12px 16px;
  border: 2px solid var(--color-border);
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.3s ease;
  background-color: var(--color-background-soft);
  color: var(--color-text);
}

.search-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(74, 136, 229, 0.2);
}

.search-input::placeholder {
  color: var(--color-text-muted);
}

.search-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 10px;
}

.search-btn,
.clear-btn {
  padding: 12px 30px;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.search-btn {
  background: var(--color-primary);
  color: white;
}

.search-btn:hover:not(:disabled) {
  background: var(--color-primary-dark);
  transform: translateY(-2px);
  box-shadow: var(--box-shadow-hover);
}

.search-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.clear-btn {
  background: var(--color-text-muted);
  color: white;
}

.clear-btn:hover {
  background: var(--color-background-mute);
  transform: translateY(-2px);
}

.search-results {
  background: var(--color-card-bg);
  border-radius: 12px;
  padding: 30px;
  box-shadow: var(--box-shadow);
  border: 1px solid var(--color-border);
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 2px solid var(--color-border);
}

.results-header h2 {
  color: var(--color-heading);
  margin: 0;
  font-size: 1.8rem;
}

.results-count {
  color: var(--color-text-muted);
  font-size: 1rem;
  background: var(--color-background-mute);
  padding: 6px 12px;
  border-radius: 20px;
}

.no-results {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-muted);
}

.no-results-icon {
  font-size: 4rem;
  margin-bottom: 20px;
}

.no-results h3 {
  color: var(--color-heading);
  margin-bottom: 10px;
}

.results-list {
  display: grid;
  gap: 20px;
}

.blog-card {
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--color-background-soft);
}

.blog-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--box-shadow-hover);
  transform: translateY(-3px);
}

.blog-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.blog-title {
  color: var(--color-heading);
  margin: 0;
  font-size: 1.4rem;
  font-weight: 700;
  flex: 1;
  margin-right: 15px;
}

.blog-date {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  white-space: nowrap;
  background: var(--color-background-mute);
  padding: 4px 10px;
  border-radius: 15px;
}

.blog-meta {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.blog-category,
.blog-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meta-label {
  font-weight: 600;
  color: var(--color-text);
  font-size: 0.9rem;
}

.category-tag {
  background: var(--color-error);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 500;
}

.tag {
  background: rgba(74, 136, 229, 0.1);
  color: var(--color-primary-light);
  padding: 4px 10px;
  border-radius: 15px;
  font-size: 0.8rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.tag:hover {
  background: rgba(74, 136, 229, 0.2);
  transform: translateY(-1px);
}

.blog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.view-btn,
.edit-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.view-btn {
  background: var(--color-success);
  color: white;
}

.view-btn:hover {
  background: var(--color-primary-dark);
  transform: translateY(-1px);
}

.edit-btn {
  background: var(--color-warning);
  color: white;
}

.edit-btn:hover {
  background: var(--color-accent-dark);
  transform: translateY(-1px);
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(30, 36, 59, 0.9);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid var(--color-border);
  border-top: 4px solid var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-overlay p {
  color: var(--color-text);
  font-size: 1.1rem;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .blog-search-container {
    padding: 15px;
  }

  .search-header h1 {
    font-size: 2rem;
  }

  .search-form-container,
  .search-results {
    padding: 20px;
  }

  .search-fields {
    grid-template-columns: 1fr;
  }

  .search-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .blog-header {
    flex-direction: column;
    gap: 10px;
  }

  .blog-date {
    align-self: flex-start;
  }

  .blog-actions {
    justify-content: stretch;
  }

  .view-btn,
  .edit-btn {
    flex: 1;
  }
}
</style>

<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h1>仪表盘</h1>
      <p class="welcome-text">欢迎回来，{{ authStore.user?.username }}！</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon">📝</div>
        <div class="stat-info">
          <h3>{{ stats.totalBlogs }}</h3>
          <p>总博客数</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">📅</div>
        <div class="stat-info">
          <h3>{{ stats.thisMonthBlogs }}</h3>
          <p>本月发布</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">🏷️</div>
        <div class="stat-info">
          <h3>{{ stats.totalCategories }}</h3>
          <p>分类数量</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">🔖</div>
        <div class="stat-info">
          <h3>{{ stats.totalTags }}</h3>
          <p>标签数量</p>
        </div>
      </div>
    </div>

    <!-- 快速操作 -->
    <div class="quick-actions">
      <h2>快速操作</h2>
      <div class="action-buttons">
        <button @click="$router.push('/blog/add')" class="action-btn primary">
          <span class="btn-icon">✏️</span>
          创建新博客
        </button>
        <button @click="$router.push('/blogs')" class="action-btn">
          <span class="btn-icon">📚</span>
          查看所有博客
        </button>
        <button @click="$router.push('/settings')" class="action-btn">
          <span class="btn-icon">⚙️</span>
          系统设置
        </button>
        <button @click="showAIChat = true" class="action-btn">
          <span class="btn-icon">🤖</span>
          AI助手
        </button>
      </div>
    </div>

    <!-- 最近博客 -->
    <div class="recent-blogs">
      <h2>最近博客</h2>
      <div v-if="recentBlogs.length === 0" class="empty-state">
        <p>暂无博客，<router-link to="/blog/add">创建第一篇博客</router-link></p>
      </div>
      <div v-else class="blog-list">
        <div
          v-for="blog in recentBlogs"
          :key="blog.id"
          class="blog-item"
          @click="$router.push(`/blog/${blog.id}`)"
        >
          <div class="blog-content">
            <h3>{{ blog.title }}</h3>
            <p class="blog-meta">
              <span class="category">{{ blog.categories }}</span>
              <span class="date">{{ formatDate(blog.dateTime) }}</span>
            </p>
            <div class="blog-tags">
              <span v-for="tag in blog.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
          <div class="blog-actions">
            <button @click.stop="$router.push(`/blog/${blog.id}/edit`)" class="edit-btn">
              编辑
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 系统状态 -->
    <div class="system-status">
      <h2>系统状态</h2>
      <div class="status-grid">
        <div class="status-item">
          <span class="status-label">LLM服务状态:</span>
          <span :class="['status-value', llmStatus.isActive ? 'active' : 'inactive']">
            {{ llmStatus.isActive ? '活跃' : '未激活' }}
          </span>
        </div>
        <div class="status-item">
          <span class="status-label">当前LLM:</span>
          <span class="status-value">{{ llmStatus.currentLLM || '未设置' }}</span>
        </div>
      </div>
    </div>

    <!-- AI聊天窗口 -->
    <AIChatWindow v-if="showAIChat" @close="showAIChat = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { getAllBlogs } from '../services/blogService'
import { LLMService } from '../services/llmService'
import { ConfigService, type SystemConfig } from '../services/configService'
import type { BlogListItem } from '../types/blog'
import AIChatWindow from '../components/AIChatWindow.vue'

const authStore = useAuthStore()
const showAIChat = ref(false)

// 统计数据
const stats = ref({
  totalBlogs: 0,
  thisMonthBlogs: 0,
  totalCategories: 0,
  totalTags: 0,
})

// 最近博客
const recentBlogs = ref<BlogListItem[]>([])

// LLM状态
const llmStatus = ref({
  isActive: false,
  currentLLM: '',
})

// 系统配置
const systemConfig = ref<SystemConfig | null>(null)

// 加载数据
onMounted(async () => {
  await loadDashboardData()
})

async function loadDashboardData() {
  try {
    // 加载博客数据
    const blogs = await getAllBlogs()

    // 计算统计数据
    stats.value.totalBlogs = blogs.length

    // 计算本月博客数
    const currentMonth = new Date().getMonth()
    const currentYear = new Date().getFullYear()
    stats.value.thisMonthBlogs = blogs.filter((blog) => {
      const blogDate = new Date(blog.dateTime)
      return blogDate.getMonth() === currentMonth && blogDate.getFullYear() === currentYear
    }).length

    // 计算分类数量
    const categories = new Set(blogs.map((blog) => blog.categories).filter(Boolean))
    stats.value.totalCategories = categories.size

    // 计算标签数量
    const allTags = blogs.flatMap((blog) => blog.tags || [])
    const uniqueTags = new Set(allTags)
    stats.value.totalTags = uniqueTags.size

    // 获取最近5篇博客
    recentBlogs.value = blogs
      .sort((a, b) => new Date(b.dateTime).getTime() - new Date(a.dateTime).getTime())
      .slice(0, 5)

    // 加载LLM状态
    await loadLLMStatus()

    // 加载系统配置
    await loadSystemConfig()
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
  }
}

async function loadLLMStatus() {
  try {
    const currentLLM = await LLMService.getCurrentLLM()
    llmStatus.value.currentLLM = currentLLM
    llmStatus.value.isActive = currentLLM !== 'null' && currentLLM !== ''
  } catch (error) {
    console.error('获取LLM状态失败:', error)
  }
}

async function loadSystemConfig() {
  try {
    systemConfig.value = await ConfigService.getConfig()
  } catch (error) {
    console.error('获取系统配置失败:', error)
  }
}

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
</script>

<style scoped>
/* Base styles */
:root {
  --bg-primary: #f8f9fa; /* Light gray for main background */
  --bg-secondary: #ffffff; /* White for cards/sections */
  --text-primary: #212529; /* Very dark gray, near black */
  --text-secondary: #6c757d; /* Medium gray */
  --text-tertiary: #adb5bd; /* Light gray for less important meta text */
  --accent-primary: #007bff; /* A standard, clean blue */
  --accent-primary-darker: #0056b3; /* For hover states */
  --border-color: #dee2e6; /* Light gray for borders */
  --shadow-color: rgba(0, 0, 0, 0.075); /* Softer shadow */
  --status-active: #28a745; /* Green */
  --status-inactive: #dc3545; /* Red */
  --tag-bg: #e9ecef; /* Light gray for tags/categories */
  --tag-text: #495057; /* Darker gray for tag text */
}

.dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px; /* Increased padding slightly */
  background-color: var(--bg-primary);
  color: var(--text-primary);
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; /* Modern system font stack */
}

.dashboard-header {
  margin-bottom: 32px; /* Increased margin */
}

.dashboard-header h1 {
  color: var(--text-primary);
  margin-bottom: 8px;
  font-size: 2.2rem; /* Slightly reduced for a cleaner look */
  font-weight: 600; /* Slightly less bold */
}

.welcome-text {
  color: var(--text-secondary);
  font-size: 1.1rem;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); /* Adjusted minmax */
  gap: 24px; /* Increased gap */
  margin-bottom: 40px;
}

.stat-card {
  background: var(--bg-secondary);
  border-radius: 10px; /* Slightly softer radius */
  padding: 24px;
  box-shadow: 0 3px 5px var(--shadow-color); /* Softer shadow */
  display: flex;
  align-items: center;
  gap: 20px; /* Increased gap */
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-3px); /* Slightly more pronounced hover */
  box-shadow: 0 6px 10px var(--shadow-color); /* Softer, larger hover shadow */
}

.stat-icon {
  font-size: 2.2rem; /* Adjusted size */
  width: 56px; /* Adjusted size */
  height: 56px; /* Adjusted size */
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-primary); /* Use accent for icon background */
  color: var(--bg-secondary); /* White icon color */
  border-radius: 10px;
}

.stat-info h3 {
  font-size: 1.8rem; /* Adjusted size */
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.stat-info p {
  color: var(--text-secondary);
  margin: 0;
  font-size: 0.9rem;
}

/* Quick Actions */
.quick-actions {
  margin-bottom: 40px;
}

.quick-actions h2 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 1.6rem; /* Adjusted size */
  font-weight: 600;
}

.action-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); /* Adjusted minmax */
  gap: 16px;
}

.action-btn {
  padding: 14px 20px; /* Adjusted padding */
  border: 1px solid var(--border-color); /* Subtle border */
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 0.95rem; /* Adjusted size */
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center; /* Center content */
  gap: 8px;
  box-shadow: 0 2px 3px var(--shadow-color);
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px var(--shadow-color);
  border-color: var(--accent-primary); /* Highlight border on hover */
}

.action-btn.primary {
  background: var(--accent-primary);
  color: var(--bg-secondary);
  border-color: var(--accent-primary);
}

.action-btn.primary:hover {
  background: var(--accent-primary-darker);
  border-color: var(--accent-primary-darker);
}

.btn-icon {
  font-size: 1.1rem; /* Adjusted size */
}

/* Recent Blogs */
.recent-blogs {
  margin-bottom: 40px;
}

.recent-blogs h2 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 1.6rem; /* Adjusted size */
  font-weight: 600;
}

.empty-state {
  text-align: center;
  padding: 40px;
  background-color: var(--bg-secondary); /* Add background */
  border-radius: 8px;
  box-shadow: 0 2px 3px var(--shadow-color);
  color: var(--text-secondary);
}

.empty-state a {
  color: var(--accent-primary);
  text-decoration: none;
  font-weight: 500;
}

.empty-state a:hover {
  text-decoration: underline;
  color: var(--accent-primary-darker);
}

.blog-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.blog-item {
  background: var(--bg-secondary);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px var(--shadow-color);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid transparent; /* For hover effect */
}

.blog-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px var(--shadow-color);
  border-color: var(--accent-primary);
}

.blog-content {
  flex: 1;
}

.blog-content h3 {
  color: var(--text-primary);
  margin: 0 0 8px 0;
  font-size: 1.15rem; /* Adjusted size */
  font-weight: 600;
}

.blog-meta {
  color: var(--text-secondary);
  font-size: 0.85rem; /* Adjusted size */
  margin: 0 0 12px 0;
  display: flex;
  gap: 12px; /* Adjusted gap */
}

.category,
.tag {
  /* Combined for similar styling */
  background: var(--tag-bg);
  color: var(--tag-text);
  padding: 3px 8px; /* Adjusted padding */
  border-radius: 6px; /* Softer radius */
  font-size: 0.75rem; /* Adjusted size */
  font-weight: 500;
}

.blog-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.blog-actions {
  display: flex;
  gap: 8px;
}

.edit-btn {
  padding: 8px 14px; /* Adjusted padding */
  border: 1px solid var(--accent-primary);
  border-radius: 6px;
  background: transparent;
  color: var(--accent-primary);
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.85rem; /* Adjusted size */
  font-weight: 500;
}

.edit-btn:hover {
  background: var(--accent-primary);
  color: var(--bg-secondary);
}

/* System Status */
.system-status {
  margin-bottom: 20px; /* Adjusted margin */
}

.system-status h2 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 1.6rem; /* Adjusted size */
  font-weight: 600;
}

.status-grid {
  background: var(--bg-secondary);
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 4px var(--shadow-color);
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.status-item:last-child {
  border-bottom: none;
}

.status-label {
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 0.9rem; /* Adjusted size */
}

.status-value {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 0.9rem; /* Adjusted size */
}

.status-value.active {
  color: var(--status-active);
}

.status-value.inactive {
  color: var(--status-inactive);
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .dashboard {
    padding: 16px;
  }

  .stats-grid,
  .action-buttons {
    grid-template-columns: 1fr; /* Full width on smaller screens */
  }

  .stat-card {
    flex-direction: column; /* Stack icon and info on small screens */
    align-items: flex-start;
    gap: 16px;
  }

  .stat-icon {
    margin-bottom: 8px; /* Add some space below icon when stacked */
  }

  .blog-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .blog-actions {
    align-self: flex-end;
    margin-top: 12px; /* Add space when actions are at bottom */
  }

  .dashboard-header h1 {
    font-size: 1.8rem;
  }
  .quick-actions h2,
  .recent-blogs h2,
  .system-status h2 {
    font-size: 1.4rem;
  }
}
</style>

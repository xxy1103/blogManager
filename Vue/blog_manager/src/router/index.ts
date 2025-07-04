import { createRouter, createWebHistory } from 'vue-router'
// 移除了 HomeView 的导入
import BlogListView from '../views/BlogListView.vue'
import BlogDetailView from '../views/BlogDetailView.vue'
import BlogEditStandaloneView from '../views/BlogEditStandaloneView.vue'
import BlogAddView from '../views/BlogAddView.vue' // 新增创建博客页面导入
import BlogSearchView from '../views/BlogSearchView.vue' // 新增搜索博客页面导入
// import SettingsView from '../views/SettingsView.vue' // 导入设置页面组件 - 已移除
import DashboardView from '../views/DashboardView.vue' // 导入仪表盘页面
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import { useAuthStore } from '../stores/auth.js'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: DashboardView, // 修改为使用仪表盘作为首页
      meta: { requiresAuth: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: { requiresAuth: true },
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
    // 注意：博客列表路由已经移至首页，但保留此路由作为别名
    {
      path: '/blogs',
      name: 'blog-list',
      component: BlogListView,
    },
    {
      path: '/blog/:id', // 修改路径以匹配 ID
      name: 'blog-detail',
      component: BlogDetailView,
      props: true, // 将路由参数作为 props 传递给组件
    },
    {
      // 新增的编辑路由
      path: '/blog/:id/edit', // 修改路径以匹配 ID
      name: 'blog-edit-standalone',
      component: BlogEditStandaloneView,
      props: true,
      meta: { requiresAuth: true },
    },
    {
      // 新增博客创建路由
      path: '/blog/add',
      name: 'blog-add',
      component: BlogAddView,
      meta: { requiresAuth: true },
    },
    {
      // 新增博客搜索路由
      path: '/blog/search',
      name: 'blog-search',
      component: BlogSearchView,
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { hideForAuth: true },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { hideForAuth: true },
    },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 初始化认证状态
  authStore.initAuth()

  // 需要认证的路由
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }

  // 已登录用户访问登录/注册页面时重定向到首页
  if (to.meta.hideForAuth && authStore.isAuthenticated) {
    next('/')
    return
  }

  next()
})

export default router

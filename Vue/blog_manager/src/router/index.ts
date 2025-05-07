import { createRouter, createWebHistory } from 'vue-router'
// 移除了 HomeView 的导入
import BlogListView from '../views/BlogListView.vue'
import BlogDetailView from '../views/BlogDetailView.vue'
import BlogEditStandaloneView from '../views/BlogEditStandaloneView.vue' // 新增导入

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: BlogListView, // 修改为直接使用博客列表页面作为首页
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
      path: '/blog/:year/:month/:day/:filename',
      name: 'blog-detail',
      component: BlogDetailView,
      props: true, // 将路由参数作为 props 传递给组件
    },
    {
      // 新增的编辑路由
      path: '/blog/:year/:month/:day/:filename/edit',
      name: 'blog-edit-standalone',
      component: BlogEditStandaloneView,
      props: true,
    },
  ],
})

export default router

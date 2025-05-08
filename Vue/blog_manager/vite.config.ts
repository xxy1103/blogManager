import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:5200', // 您的后端 API 地址
        changeOrigin: true,
        // rewrite: (path) => path.replace(/^\/api/, '') // 如果后端不需要 /api 前缀
      },
      '/image': {
        // <--- 确保有这个或类似的配置
        target: 'http://localhost:5200', // 您的后端图片服务地址
        changeOrigin: true,
        // rewrite: (path) => path.replace(/^\/image/, '/actual/backend/image/prefix') // 如果后端图片路径需要重写
      },
    },
  },
})

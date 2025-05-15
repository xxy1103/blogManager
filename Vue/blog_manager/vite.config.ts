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
    host: '0.0.0.0', // 允许其他设备访问
    port: 5173, // 可选，指定端口号
    proxy: {
      '/api': {
        target: 'http://localhost:5200',
        changeOrigin: true,
      },
      '/image': {
        target: 'http://localhost:5200',
        changeOrigin: true,
      },
      '/config': {
        target: 'http://localhost:5200',
        changeOrigin: true,
      },
      '/llm': {
        target: 'http://localhost:5200',
        changeOrigin: true,
      },
    },
  },
})

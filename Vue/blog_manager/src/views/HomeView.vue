<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 当组件挂载完成后，重定向到博客列表页面
onMounted(() => {
  router.push('/')
})
</script>

<template>
  <div class="redirect-loading">
    <p>正在加载博客列表...</p>
  </div>
</template>

<style scoped>
.redirect-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: var(--color-text);
}
</style>

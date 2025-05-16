<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import AIChatWindow from './components/AIChatWindow.vue'
</script>

<template>
  <header class="site-header">
    <div class="wrapper">
      <RouterLink to="/" class="header-title-link">
        <h1>Ulna's Blog</h1>
      </RouterLink>
      <nav>
        <RouterLink to="/" class="nav-link">首页</RouterLink>
        <RouterLink to="/blog/add" class="nav-link">写作</RouterLink>
        <RouterLink to="/about" class="nav-link">关于</RouterLink>
        <span class="nav-link search-icon">🔍</span>
      </nav>
    </div>
  </header>

  <main class="main-content">
    <RouterView />
  </main>

  <!-- AI聊天助手侧边栏 -->
  <AIChatWindow />

  <footer class="site-footer">
    <div class="footer-wrapper">
      <div class="footer-content">
        <p class="footer-branding">Hexo ♡ Fluid</p>
        <p class="footer-copyright">&copy; 2025 Rookie's Blog. All Rights Reserved.</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.site-header {
  line-height: 1.5;
  background-color: var(--color-background);
  padding: 0.8rem 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  width: 100%;
  z-index: 1000;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: sticky;
  top: 0;
  left: 0;
  right: 0;
}

.site-header .wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 0 2rem;
  box-sizing: border-box;
}

.site-header h1 {
  font-size: 1.3rem;
  color: white;
  margin: 0;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.header-title-link {
  text-decoration: none;
  color: white;
  transition: all 0.3s ease;
}

.header-title-link:hover {
  transform: scale(1.05);
}

nav {
  font-size: 1rem;
  text-align: center;
  display: flex;
  align-items: center;
}

.nav-link {
  display: inline-block;
  padding: 0.5rem 1rem;
  color: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
  margin-left: 0.5rem;
  border-radius: 4px;
  font-weight: 500;
}

.nav-link:hover,
nav a.router-link-exact-active {
  color: white;
  background-color: rgba(255, 255, 255, 0.1);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.search-icon {
  cursor: pointer;
}

.search-icon:hover {
  color: white;
  transform: scale(1.1);
}

.main-content {
  flex: 1;
  width: 100%;
  box-sizing: border-box;
  padding: 1rem 0 3rem;
  background-color: var(--color-background);
  min-height: calc(100vh - 150px);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.main-content > * {
  width: 100%;
  max-width: 1280px;
  padding-left: 1rem;
  padding-right: 1rem;
  box-sizing: border-box;
}

.site-footer {
  width: 100%;
  background-color: var(--color-background);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1.5rem 0;
  text-align: center;
  left: 0;
  right: 0;
}

.footer-wrapper {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 2rem;
  box-sizing: border-box;
}

.footer-content {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  line-height: 1.5;
}

.footer-branding {
  margin-bottom: 0.5rem;
  color: rgba(255, 255, 255, 0.7);
}

.footer-copyright {
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.8rem;
}

@media (min-width: 1024px) {
  nav {
    text-align: right;
  }

  .nav-link {
    margin-left: 1rem;
  }
}

@media (max-width: 768px) {
  .site-header .wrapper {
    padding: 0 1rem;
  }

  .site-header h1 {
    font-size: 1.1rem;
  }

  nav {
    font-size: 0.9rem;
  }

  .nav-link {
    padding: 0.4rem 0.6rem;
    margin-left: 0.2rem;
  }

  .footer-wrapper {
    padding: 0 1rem;
  }
}
</style>

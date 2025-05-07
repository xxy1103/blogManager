<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header>
    <div class="wrapper">
      <RouterLink to="/" class="header-title-link">
        <h1>博客管理系统</h1>
      </RouterLink>
      <nav>
        <RouterLink to="/">博客列表</RouterLink>
        <RouterLink to="/about">关于</RouterLink>
      </nav>
    </div>
  </header>

  <main class="main-content">
    <RouterView />
  </main>
</template>

<style scoped>
header {
  line-height: 1.5;
  background-color: var(--color-background-soft);
  padding: 1rem 2rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  z-index: 1000;
}

header .wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1280px;
  margin: 0 auto;
  width: 100%;
}

header h1 {
  font-size: 1.5rem;
  color: var(--color-heading);
  margin: 0;
}

.header-title-link {
  text-decoration: none; /* Remove underline from link */
  color: inherit; /* Inherit color from parent, which is h1's color */
}

nav {
  font-size: 1rem;
  text-align: center;
}

nav a {
  display: inline-block;
  padding: 0.5rem 1rem;
  border-left: 1px solid var(--color-border);
  color: var(--color-text);
  transition:
    color 0.3s ease,
    background-color 0.3s ease;
}

nav a:first-of-type {
  border-left: 0;
}

nav a.router-link-exact-active {
  color: var(--color-primary);
  font-weight: bold;
}

nav a:hover {
  background-color: var(--color-background-mute);
  color: var(--color-primary-dark);
}

.main-content {
  padding: 2rem; /* Add padding around the main content area */
  width: 100%;
  max-width: 1280px; /* Optional: constrain max-width of content area */
  margin: 0 auto; /* Center content area if max-width is used */
}

/* Removed .logo styles as it's not in the template */

@media (min-width: 1024px) {
  nav {
    text-align: right; /* Align nav to the right on larger screens */
  }
}
</style>

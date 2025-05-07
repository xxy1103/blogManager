<template>
  <div class="about-view">
    <div class="content-wrapper">
      <h1>关于博客管理系统</h1>
      <p>欢迎来到博客管理系统！这是一个使用 Vue 3 和 Vite 构建的现代化博客平台。</p>
      <section>
        <h2>主要特性</h2>
        <ul>
          <li>基于 Markdown 的博客撰写和展示</li>
          <li>响应式设计，适配各种屏幕尺寸</li>
          <li>清晰的博客列表和详情页面</li>
          <li>便捷的博客内容在线编辑功能</li>
          <li>通过 RESTful API 与后端服务交互</li>
        </ul>
      </section>
      <section>
        <h2>技术栈</h2>
        <p>本系统前端主要采用以下技术构建：</p>
        <ul>
          <li><strong>Vue 3:</strong> 用于构建用户界面的渐进式 JavaScript 框架。</li>
          <li><strong>Vue Router:</strong> 为单页应用提供路由功能。</li>
          <li><strong>Pinia:</strong> Vue 的状态管理库。</li>
          <li><strong>Vite:</strong> 下一代前端构建工具，提供极速的冷启动和热模块替换 (HMR)。</li>
          <li><strong>TypeScript:</strong> 为 JavaScript 添加了静态类型检查。</li>
          <li><strong>Marked.js:</strong> 用于将 Markdown 解析为 HTML。</li>
        </ul>
      </section>
      <section>
        <h2>未来展望</h2>
        <p>我们致力于不断改进和扩展此博客系统，未来可能包括：</p>
        <ul>
          <li>用户认证和权限管理</li>
          <li>更丰富的博客编辑和预览体验</li>
          <li>评论系统集成</li>
          <li>主题定制功能</li>
        </ul>
      </section>
      <p>感谢您的使用！如果您有任何建议或反馈，请随时与我们联系。</p>
    </div>
  </div>
</template>

<style scoped>
.about-view {
  padding: 2rem;
  font-family: var(--font-family-base, sans-serif); /* 使用全局字体变量 */
  color: var(--color-text);
  line-height: 1.7;
}

.content-wrapper {
  max-width: 900px; /* 与 BlogDetailView 类似的最大宽度 */
  margin: 0 auto;
  background-color: var(--color-background-soft); /* 使用全局背景色变量 */
  padding: 2rem;
  border-radius: var(--border-radius, 8px); /* 使用全局圆角变量 */
  box-shadow: var(--box-shadow, 0 2px 10px rgba(0, 0, 0, 0.05)); /* 使用全局阴影变量 */
}

h1 {
  color: var(--color-heading);
  border-bottom: 2px solid var(--color-border);
  padding-bottom: 0.5rem;
  margin-bottom: 1.5rem;
  font-size: 2em;
}

h2 {
  color: var(--color-primary); /* 使用主题主色 */
  margin-top: 2rem;
  margin-bottom: 1rem;
  font-size: 1.5em;
  border-bottom: 1px solid var(--color-border-hover);
  padding-bottom: 0.3rem;
}

p {
  margin-bottom: 1rem;
}

ul {
  list-style-type: disc;
  margin-left: 2rem;
  margin-bottom: 1.5rem;
}

li {
  margin-bottom: 0.5rem;
}

section {
  margin-bottom: 2rem;
}

/* 移除或调整原有的特定于大屏幕的样式，因为全局 App.vue 和 main.css 已处理布局 */
/* @media (min-width: 1024px) {
  .about-view {
    min-height: 100vh;
    display: flex;
    align-items: center;
  }
} */
</style>

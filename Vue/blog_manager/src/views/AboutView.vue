<template>
  <div class="about-view">
    <div class="banner-image">
      <div class="banner-content">
        <h1 class="banner-title">关于我的博客</h1>
      </div>
    </div>

    <div class="content-wrapper">
      <section>
        <h2>博客简介</h2>
        <p>
          欢迎来到我的博客！这是一个使用 Vue 3 和 Vite
          构建的现代化博客平台，专注于技术分享和个人心得。
        </p>
      </section>

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
        <p>本站点主要采用以下技术构建：</p>
        <ul>
          <li><strong>Vue 3:</strong> 用于构建用户界面的渐进式 JavaScript 框架</li>
          <li><strong>Vue Router:</strong> 为单页应用提供路由功能</li>
          <li><strong>Pinia:</strong> Vue 的状态管理库</li>
          <li><strong>Vite:</strong> 下一代前端构建工具，提供极速的开发体验</li>
          <li><strong>TypeScript:</strong> 为 JavaScript 添加了静态类型检查</li>
          <li><strong>GitHub Actions:</strong> 用于自动构建和部署网站</li>
        </ul>
      </section>

      <section>
        <h2>关于我</h2>
        <p>
          我是一名热爱技术的开发者，致力于分享有关前端开发、后端技术和云服务的知识和经验。通过这个博客，我希望能够记录自己的学习历程，同时也能够帮助到其他开发者。
        </p>
      </section>

      <section>
        <h2>联系方式</h2>
        <p>如果您有任何问题或建议，欢迎通过以下方式与我联系：</p>
        <ul class="contact-list">
          <li>
            <span class="contact-icon">📧</span>
            <span>Email: example@example.com</span>
          </li>
          <li>
            <span class="contact-icon">🔗</span>
            <span
              >GitHub:
              <a href="https://github.com" target="_blank">github.com/yourusername</a></span
            >
          </li>
        </ul>
      </section>

      <footer class="about-footer">
        <p>© 2025 Rookie's Blog. 保留所有权利。</p>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.about-view {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.banner-image {
  width: 100%;
  height: 300px;
  background-image:
    linear-gradient(rgba(30, 36, 59, 0.8), rgba(30, 36, 59, 0.9)),
    url('https://images.unsplash.com/photo-1471107340929-a87cd0f5b5f3');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 2rem;
}

.banner-content {
  text-align: center;
  padding: 2rem;
}

.banner-title {
  font-size: 2.5rem;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  margin: 0;
  border-bottom: none;
}

.content-wrapper {
  max-width: 800px;
  width: 100%;
  background-color: var(--color-card-bg);
  padding: 2rem;
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  margin-bottom: 2rem;
}

h2 {
  color: var(--color-primary);
  margin-top: 2rem;
  margin-bottom: 1rem;
  font-size: 1.5em;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 0.5rem;
}

p {
  color: var(--color-text);
  line-height: 1.7;
  margin-bottom: 1rem;
}

ul {
  list-style-type: disc;
  margin-left: 2rem;
  margin-bottom: 1.5rem;
  color: var(--color-text);
}

li {
  margin-bottom: 0.8rem;
  line-height: 1.5;
}

section {
  margin-bottom: 2rem;
}

strong {
  color: var(--color-primary-light);
  font-weight: 600;
}

a {
  color: var(--color-primary);
  text-decoration: none;
  transition: color 0.3s;
}

a:hover {
  color: var(--color-primary-light);
  text-decoration: underline;
}

.contact-list {
  list-style-type: none;
  margin-left: 0;
}

.contact-list li {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}

.contact-icon {
  font-size: 1.5em;
  margin-right: 1rem;
  display: inline-block;
}

.about-footer {
  margin-top: 3rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--color-border);
  text-align: center;
  font-size: 0.9rem;
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .banner-image {
    height: 200px;
  }

  .banner-title {
    font-size: 2rem;
  }

  .content-wrapper {
    padding: 1.5rem;
  }
}
</style>

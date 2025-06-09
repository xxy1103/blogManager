-- 博客管理系统数据库创建脚本
-- 使用UTF8字符集以支持中文内容

-- 创建数据库
CREATE DATABASE IF NOT EXISTS blog_manager 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE blog_manager;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建博客表
CREATE TABLE IF NOT EXISTS blogs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    categories VARCHAR(255),
    tags VARCHAR(500),
    saying VARCHAR(500),
    filename VARCHAR(255),
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_title (title),
    INDEX idx_created_at (created_at),
    FULLTEXT idx_content_search (title, content)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 插入默认管理员用户（密码是加密的"admin123"）
-- 注意：实际使用时密码会通过Spring Security的BCrypt加密
INSERT INTO users (username, email, password, role) VALUES 
('admin', 'admin@blogmanager.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM0jmD0e4WGqaLEOkb8i', 'ADMIN')
ON DUPLICATE KEY UPDATE username=username;

-- 创建示例博客数据
INSERT INTO blogs (title, content, categories, tags, saying, user_id) VALUES 
(
    '欢迎使用博客管理系统',
    '# 欢迎使用博客管理系统\n\n这是一个基于Spring Boot开发的现代化博客管理系统。\n\n## 主要功能\n\n- 用户注册和登录\n- 博客文章的创建、编辑和删除\n- 分类和标签管理\n- 响应式用户界面\n\n开始你的博客之旅吧！',
    '系统公告',
    '欢迎,教程,开始',
    '博客让思想飞翔，代码让梦想成真。',
    1
),
(
    'Spring Boot 开发指南',
    '# Spring Boot 开发指南\n\n## 简介\n\nSpring Boot 是一个基于 Spring 框架的开源 Java 应用程序框架...\n\n## 核心特性\n\n1. 自动配置\n2. 嵌入式服务器\n3. 生产就绪的功能\n\n## 快速开始\n\n```java\n@SpringBootApplication\npublic class Application {\n    public static void main(String[] args) {\n        SpringApplication.run(Application.class, args);\n    }\n}\n```',
    '技术教程',
    'Spring Boot,Java,教程',
    '学而时习之，不亦说乎。',
    1
)
ON DUPLICATE KEY UPDATE title=title;

-- 显示创建结果
SHOW TABLES;
SELECT COUNT(*) as user_count FROM users;
SELECT COUNT(*) as blog_count FROM blogs;

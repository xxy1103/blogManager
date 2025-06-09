# 博客管理系统 - 快速启动指南

## 🚀 数据库设置

### 快速设置（推荐）
运行自动化脚本：
```cmd
setup_database.bat
```

### 手动设置
1. 确保MySQL服务正在运行
2. 执行SQL脚本：
```cmd
mysql -u root -p < database_setup.sql
```

3. 修改 `src/main/resources/application.properties` 中的数据库密码：
```properties
spring.datasource.password=你的MySQL密码
```

## 🔧 启动应用

### 使用Maven
```cmd
mvn spring-boot:run
```

### 使用IDE
运行 `src/main/java/com/ulna/blog_manager/BlogManagerApplication.java`

## 🌐 访问应用

- 应用地址: http://localhost:5200
- 默认管理员:
  - 用户名: `admin`
  - 密码: `admin123`

## 📋 数据库表结构

- **users**: 用户信息表
- **blogs**: 博客文章表

详细信息请查看: `DATABASE_SETUP.md`

---
**首次运行请确保数据库配置正确！**

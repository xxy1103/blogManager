# 博客管理系统数据库配置指南

## 1. 数据库环境要求

- **MySQL 版本**: 5.7+ 或 8.0+
- **字符集**: UTF8MB4 (支持emoji和特殊字符)
- **存储引擎**: InnoDB

## 2. 数据库创建步骤

### 方法一：使用SQL脚本创建（推荐）

1. 启动MySQL服务
2. 使用MySQL客户端连接到数据库服务器：
   ```bash
   mysql -u root -p
   ```

3. 执行数据库创建脚本：
   ```sql
   source d:/windows/desktop/blogManager/blog_manager/database_setup.sql
   ```

### 方法二：手动创建

1. 创建数据库：
   ```sql
   CREATE DATABASE blog_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. 使用Spring Boot自动建表功能：
   - 项目配置了 `spring.jpa.hibernate.ddl-auto=update`
   - 启动应用时会自动创建表结构

## 3. 数据库配置

### application.properties 配置说明

```properties
# 数据库连接配置
spring.datasource.url=jdbc:mysql://localhost:3306/blog_manager?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate 配置
spring.jpa.hibernate.ddl-auto=update    # 自动更新数据库表结构
spring.jpa.show-sql=true               # 显示SQL语句
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true  # 格式化SQL输出
```

### 配置参数说明

- **数据库名**: `blog_manager`
- **默认端口**: `3306`
- **用户名**: `root` (可根据实际情况修改)
- **密码**: `password` (⚠️ 请修改为你的MySQL密码)

## 4. 数据库表结构

### users 表 (用户信息)
| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 用户ID | 主键，自增 |
| username | VARCHAR(50) | 用户名 | 唯一，非空 |
| email | VARCHAR(255) | 邮箱 | 唯一，非空 |
| password | VARCHAR(255) | 密码(加密) | 非空 |
| role | VARCHAR(20) | 角色 | 默认USER |
| created_at | TIMESTAMP | 创建时间 | 自动设置 |
| updated_at | TIMESTAMP | 更新时间 | 自动更新 |

### blogs 表 (博客文章)
| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 博客ID | 主键，自增 |
| title | VARCHAR(200) | 标题 | 非空 |
| content | TEXT | 内容 | 非空 |
| categories | VARCHAR(255) | 分类 | 可空 |
| tags | VARCHAR(500) | 标签 | 可空 |
| saying | VARCHAR(500) | 感悟/摘要 | 可空 |
| filename | VARCHAR(255) | 文件名 | 可空 |
| user_id | BIGINT | 作者ID | 外键 |
| created_at | TIMESTAMP | 创建时间 | 自动设置 |
| updated_at | TIMESTAMP | 更新时间 | 自动更新 |

## 5. 默认数据

系统会自动创建：
- 管理员账户：
  - 用户名: `admin`
  - 邮箱: `admin@blogmanager.com`
  - 密码: `admin123`
  - 角色: `ADMIN`

- 示例博客文章2篇

## 6. 数据库连接测试

启动应用后，检查控制台输出：
- 如果看到SQL建表语句，说明数据库连接成功
- 访问 `http://localhost:5200` 验证应用运行正常

## 7. 常见问题解决

### 连接失败
1. 检查MySQL服务是否启动
2. 确认数据库名称、用户名、密码是否正确
3. 检查防火墙设置

### 字符编码问题
确保MySQL配置文件(my.cnf/my.ini)中设置：
```ini
[mysql]
default-character-set=utf8mb4

[mysqld]
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
```

### 权限问题
确保MySQL用户有足够权限：
```sql
GRANT ALL PRIVILEGES ON blog_manager.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

## 8. 生产环境建议

1. **修改默认密码**: 更改application.properties中的数据库密码
2. **使用环境变量**: 敏感信息通过环境变量配置
3. **备份策略**: 定期备份数据库
4. **性能优化**: 根据数据量调整MySQL配置参数

---

**注意**: 首次运行项目前，请确保已正确配置数据库连接信息！

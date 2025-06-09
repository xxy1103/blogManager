package com.ulna.blog_manager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "blogs")
public class BlogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "博客标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200字符")
    @Column(nullable = false)
    private String title;
    
    @NotBlank(message = "博客内容不能为空")
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "categories")
    private String categories;
    
    @Column(name = "tags")
    private String tags; // 存储为逗号分隔的字符串
    
    @Column(name = "saying")
    private String saying;
    
    @Column(name = "filename")
    private String filename;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // 构造函数
    public BlogEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public BlogEntity(String title, String content, String categories, String tags, String saying, User user) {
        this();
        this.title = title;
        this.content = content;
        this.categories = categories;
        this.tags = tags;
        this.saying = saying;
        this.user = user;
    }
    
    // 从原Blog对象转换的构造函数
    public BlogEntity(Blog blog, User user) {
        this();
        this.title = blog.getTitle();
        this.content = blog.getContent() != null ? blog.getContent() : blog.loadContent();
        this.categories = blog.getCategories();
        this.tags = blog.getTags() != null ? String.join(",", blog.getTags()) : null;
        this.saying = blog.getSaying();
        this.filename = blog.getFilename();
        this.user = user;
        if (blog.getDate() != null) {
            this.createdAt = blog.getDate();
        }    }
    
    // 转换为Blog对象
    public Blog toBlog() {
        Blog blog = new Blog();
        blog.setId(this.id);
        blog.setTitle(this.title);
        blog.setContent(this.content);
        blog.setCategories(this.categories);
        if (this.tags != null && !this.tags.isEmpty()) {
            blog.setTags(this.tags.split(","));
        }
        blog.setSaying(this.saying);
        blog.setFilename(this.filename);
        blog.setDate(this.createdAt);
        return blog;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getCategories() {
        return categories;
    }
    
    public void setCategories(String categories) {
        this.categories = categories;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public String getSaying() {
        return saying;
    }
    
    public void setSaying(String saying) {
        this.saying = saying;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

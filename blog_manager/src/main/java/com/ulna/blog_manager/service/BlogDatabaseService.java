package com.ulna.blog_manager.service;

import com.ulna.blog_manager.model.Blog;
import com.ulna.blog_manager.model.BlogEntity;
import com.ulna.blog_manager.model.User;
import com.ulna.blog_manager.repository.BlogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BlogDatabaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(BlogDatabaseService.class);
    
    @Autowired
    private BlogRepository blogRepository;
    
    // 创建自定义格式的 DateTimeFormatter
    private static final DateTimeFormatter formatterPrint = DateTimeFormatter.ofPattern("'_'yyyyMMdd'_'HHmmss");
    
    /**
     * 获取用户的所有博客
     */
    public List<Blog> listUserBlogs(User user) {
        logger.debug("获取用户 {} 的所有博客", user.getUsername());
        List<BlogEntity> blogEntities = blogRepository.findByUserOrderByCreatedAtDesc(user);
        return blogEntities.stream()
                .map(BlogEntity::toBlog)
                .collect(Collectors.toList());
    }
    
    /**
     * 保存博客
     */
    public boolean saveBlog(Blog blog, User user) {
        try {
            BlogEntity blogEntity;
            if (blog.getFilename() == null || blog.getFilename().isEmpty()) {
                // 新博客
                String filename = blog.getTitle() + LocalDateTime.now().format(formatterPrint) + ".md";
                blog.setFilename(filename);
                blogEntity = new BlogEntity(blog, user);
            } else {
                // 更新现有博客
                Optional<BlogEntity> existingBlog = blogRepository.findByUser(user)
                        .stream()
                        .filter(b -> b.getFilename().equals(blog.getFilename()))
                        .findFirst();
                
                if (existingBlog.isPresent()) {
                    blogEntity = existingBlog.get();
                    blogEntity.setTitle(blog.getTitle());
                    blogEntity.setContent(blog.getContent() != null ? blog.getContent() : blog.loadContent());
                    blogEntity.setCategories(blog.getCategories());
                    blogEntity.setTags(blog.getTags() != null ? String.join(",", blog.getTags()) : null);
                    blogEntity.setSaying(blog.getSaying());
                } else {
                    blogEntity = new BlogEntity(blog, user);
                }
            }
            
            blogRepository.save(blogEntity);
            logger.info("成功保存博客: {}", blog.getTitle());
            return true;
        } catch (Exception e) {
            logger.error("保存博客失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 删除博客
     */
    public boolean deleteBlog(Blog blog, User user) {
        try {
            Optional<BlogEntity> blogEntity = blogRepository.findByUser(user)
                    .stream()
                    .filter(b -> b.getFilename().equals(blog.getFilename()))
                    .findFirst();
            
            if (blogEntity.isPresent()) {
                blogRepository.delete(blogEntity.get());
                logger.info("成功删除博客: {}", blog.getTitle());
                return true;
            } else {
                logger.warn("要删除的博客不存在: {}", blog.getFilename());
                return false;
            }
        } catch (Exception e) {
            logger.error("删除博客失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 添加新博客
     */
    public boolean addBlog(Blog blog, User user) {
        if (blog.getFilename() == null || blog.getFilename().isEmpty()) {
            String filename = blog.getTitle() + LocalDateTime.now().format(formatterPrint) + ".md";
            blog.setFilename(filename);
        }
        return saveBlog(blog, user);
    }
    
    /**
     * 更新博客信息
     */
    public boolean updateBlogInfo(Blog oldBlog, Blog newBlog, User user) {
        try {
            Optional<BlogEntity> blogEntity = blogRepository.findByUser(user)
                    .stream()
                    .filter(b -> b.getFilename().equals(oldBlog.getFilename()))
                    .findFirst();
            
            if (blogEntity.isPresent()) {
                BlogEntity entity = blogEntity.get();
                entity.setTitle(newBlog.getTitle());
                entity.setCategories(newBlog.getCategories());
                entity.setTags(newBlog.getTags() != null ? String.join(",", newBlog.getTags()) : null);
                entity.setSaying(newBlog.getSaying());
                // 保留原始内容和文件名
                
                blogRepository.save(entity);
                logger.info("成功更新博客信息: {}", newBlog.getTitle());
                return true;
            } else {
                logger.warn("要更新的博客不存在: {}", oldBlog.getFilename());
                return false;
            }
        } catch (Exception e) {
            logger.error("更新博客信息失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 更新博客内容
     */
    public boolean updateBlogContent(Blog blog, String content, User user) {
        try {
            Optional<BlogEntity> blogEntity = blogRepository.findByUser(user)
                    .stream()
                    .filter(b -> b.getFilename().equals(blog.getFilename()))
                    .findFirst();
            
            if (blogEntity.isPresent()) {
                BlogEntity entity = blogEntity.get();
                entity.setContent(content);
                
                blogRepository.save(entity);
                logger.info("成功更新博客内容: {}", blog.getTitle());
                return true;
            } else {
                logger.warn("要更新的博客不存在: {}", blog.getFilename());
                return false;
            }
        } catch (Exception e) {
            logger.error("更新博客内容失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 根据标题搜索博客
     */
    public List<Blog> searchBlogsByTitle(String title, User user) {
        logger.debug("搜索用户 {} 的博客，标题包含: {}", user.getUsername(), title);
        List<BlogEntity> blogEntities = blogRepository.findByUserAndTitleContaining(user, title);
        return blogEntities.stream()
                .map(BlogEntity::toBlog)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据分类搜索博客
     */
    public List<Blog> searchBlogsByCategory(String category, User user) {
        logger.debug("搜索用户 {} 的博客，分类包含: {}", user.getUsername(), category);
        List<BlogEntity> blogEntities = blogRepository.findByUserAndCategoriesContaining(user, category);
        return blogEntities.stream()
                .map(BlogEntity::toBlog)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据ID查找用户的博客
     */
    public Blog findUserBlogById(Long id, User user) {
        logger.debug("查找用户 {} 的博客，ID: {}", user.getUsername(), id);
        Optional<BlogEntity> blogEntity = blogRepository.findByIdAndUser(id, user);
        return blogEntity.map(BlogEntity::toBlog).orElse(null);
    }
    
    /**
     * 更新博客
     */
    public boolean updateBlog(Blog blog, User user) {
        try {
            Optional<BlogEntity> existingEntity = blogRepository.findByIdAndUser(blog.getId(), user);
            if (existingEntity.isPresent()) {
                BlogEntity entity = existingEntity.get();
                entity.setTitle(blog.getTitle());
                entity.setContent(blog.getContent());
                entity.setCategories(blog.getCategories());
                entity.setTags(blog.getTags() != null ? String.join(",", blog.getTags()) : null);
                entity.setSaying(blog.getSaying());
                entity.setUpdatedAt(LocalDateTime.now());
                
                blogRepository.save(entity);
                logger.info("成功更新博客: {}", blog.getTitle());
                return true;
            } else {
                logger.warn("要更新的博客不存在或无权限，ID: {}", blog.getId());
                return false;
            }
        } catch (Exception e) {
            logger.error("更新博客失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 根据ID删除用户的博客
     */
    public boolean deleteUserBlog(Long id, User user) {
        try {
            Optional<BlogEntity> blogEntity = blogRepository.findByIdAndUser(id, user);
            if (blogEntity.isPresent()) {
                blogRepository.delete(blogEntity.get());
                logger.info("成功删除博客，ID: {}", id);
                return true;
            } else {
                logger.warn("要删除的博客不存在或无权限，ID: {}", id);
                return false;
            }
        } catch (Exception e) {
            logger.error("删除博客失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 搜索用户的博客
     */
    public List<Blog> searchUserBlogs(User user, String title, String categories, String tags) {
        logger.debug("搜索用户 {} 的博客", user.getUsername());
        try {
            List<BlogEntity> blogEntities;
            
            if (title != null && !title.trim().isEmpty()) {
                blogEntities = blogRepository.findByUserAndTitleContaining(user, title);
            } else if (categories != null && !categories.trim().isEmpty()) {
                blogEntities = blogRepository.findByUserAndCategoriesContaining(user, categories);
            } else if (tags != null && !tags.trim().isEmpty()) {
                blogEntities = blogRepository.findByUserAndTagsContaining(user, tags);
            } else {
                blogEntities = blogRepository.findByUserOrderByCreatedAtDesc(user);
            }
            
            return blogEntities.stream()
                    .map(BlogEntity::toBlog)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("搜索博客失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}

package com.ulna.blog_manager.controller;

import com.ulna.blog_manager.model.Message;
import com.ulna.blog_manager.model.Blog;
import com.ulna.blog_manager.model.User;
import com.ulna.blog_manager.service.BlogDatabaseService;
import com.ulna.blog_manager.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;



@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    private static final Logger logger = LoggerFactory.getLogger(BlogController.class);
    
    @Autowired
    private BlogDatabaseService blogDatabaseService;
    
    @Autowired 
    private UserService userService;

    /**
     * 获取当前登录用户 - 临时实现，返回第一个用户
     */
    private User getCurrentUser() {
        // 临时实现：获取第一个用户用于测试
        Optional<User> user = userService.findByUsername("admin");
        if (user.isPresent()) {
            return user.get();
        } else {
            // 如果没有用户，创建一个默认用户
            User defaultUser = new User();
            defaultUser.setUsername("admin");
            defaultUser.setEmail("admin@test.com");
            defaultUser.setPassword("password");
            return userService.saveUser(defaultUser);
        }
    }

    /**
     * 获取当前用户的所有博客列表
     */
    @GetMapping("/lists")
    public Message listUserBlogs() {
        System.out.println("获取用户博客列表");
        try {
            User currentUser = getCurrentUser();
            logger.info("获取用户 {} 的博客列表", currentUser.getUsername());
            
            List<Blog> blogs = blogDatabaseService.listUserBlogs(currentUser);
            logger.debug("找到 {} 个博客", blogs.size());
            
            return new Message(0, blogs, null);
        } catch (Exception e) {
            logger.error("获取博客列表失败：{}", e.getMessage());
            return new Message(1, null, "获取博客列表失败");
        }
    }

    /**
     * 根据ID获取博客详情
     */
    @GetMapping("/{id}")
    public Message getBlogById(@PathVariable Long id) {
        System.out.println("获取博客详情，ID: " + id);
        try {
            User currentUser = getCurrentUser();
            logger.info("用户 {} 获取博客 {}", currentUser.getUsername(), id);
            
            Blog blog = blogDatabaseService.findUserBlogById(id, currentUser);
            if (blog == null) {
                return new Message(1, null, "博客不存在或无权限访问");
            }
            
            return new Message(0, blog, null);
        } catch (Exception e) {
            logger.error("获取博客失败：{}", e.getMessage());
            return new Message(1, null, "获取博客失败");
        }
    }

    /**
     * 创建新博客
     */
    @PostMapping
    public Message createBlog(@RequestBody Blog blog) {
        try {
            User currentUser = getCurrentUser();
            logger.info("用户 {} 创建博客：{}", currentUser.getUsername(), blog.getTitle());
            
            boolean success = blogDatabaseService.saveBlog(blog, currentUser);
            if (success) {
                return new Message(0, null, "博客创建成功");
            } else {
                return new Message(1, null, "博客创建失败");
            }
        } catch (Exception e) {
            logger.error("创建博客失败：{}", e.getMessage());
            return new Message(1, null, "创建博客失败");
        }
    }

    /**
     * 更新博客
     */
    @PutMapping("/{id}")
    public Message updateBlog(@PathVariable Long id, @RequestBody Blog blog) {
        try {
            User currentUser = getCurrentUser();
            logger.info("用户 {} 更新博客 {}", currentUser.getUsername(), id);
            
            blog.setId(id);
            boolean success = blogDatabaseService.updateBlog(blog, currentUser);
            if (success) {
                return new Message(0, null, "博客更新成功");
            } else {
                return new Message(1, null, "博客更新失败或无权限");
            }
        } catch (Exception e) {
            logger.error("更新博客失败：{}", e.getMessage());
            return new Message(1, null, "更新博客失败");
        }
    }

    /**
     * 删除博客
     */
    @DeleteMapping("/{id}")
    public Message deleteBlog(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            logger.info("用户 {} 删除博客 {}", currentUser.getUsername(), id);
            
            boolean success = blogDatabaseService.deleteUserBlog(id, currentUser);
            if (success) {
                return new Message(0, null, "博客删除成功");
            } else {
                return new Message(1, null, "博客删除失败或无权限");
            }
        } catch (Exception e) {
            logger.error("删除博客失败：{}", e.getMessage());
            return new Message(1, null, "删除博客失败");
        }
    }

    /**
     * 搜索博客
     */
    @GetMapping("/search")
    public Message searchBlogs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String categories,
            @RequestParam(required = false) String tags) {
        try {
            User currentUser = getCurrentUser();
            logger.info("用户 {} 搜索博客", currentUser.getUsername());
            
            List<Blog> blogs = blogDatabaseService.searchUserBlogs(currentUser, title, categories, tags);
            return new Message(0, blogs, null);
        } catch (Exception e) {
            logger.error("搜索博客失败：{}", e.getMessage());
            return new Message(1, null, "搜索博客失败");
        }
    }
}

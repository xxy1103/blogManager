package com.ulna.blog_manager.controller;


import com.ulna.blog_manager.model.Blog;
import com.ulna.blog_manager.repository.BlogFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable; // 用于处理路径变量
import org.springframework.web.bind.annotation.RequestParam; // 用于处理请求参数


import com.ulna.blog_manager.repository.BlogFileService;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;


@RestController // 标记为 REST 控制器，方法返回值直接作为响应体 (通常是 JSON)
@RequestMapping("/api/blogs") // 为该控制器下的所有请求路径添加 /api/blogs 前缀 (可选，但推荐)
public class BlogController {

    private static final Logger logger = LoggerFactory.getLogger(BlogController.class);
    private final BlogFileService blogFileService; // 声明对 Service 的依赖
    private List<Blog> blogList; // 声明博客列表

    @Autowired // 自动注入 BlogFileService 实例
    public BlogController(BlogFileService blogFileService) {
        this.blogFileService = blogFileService; // 初始化成员变量
    }

    @GetMapping("/lists") // 处理 GET 请求，路径为 /api/blogs/lists
    public List<Blog> listBlogFilenames() {
        logger.info("接收到请求：获取所有博客文件名");
        this.blogList = blogFileService.listPostFilenames(); // 调用 Service 层方法
        logger.debug("找到 {} 个文件名", blogList.size());
        return blogList; // 返回列表，Spring Boot 会自动转为 JSON
    }

    @GetMapping("/{year}/{month}/{day}/{filename}/")
    public Blog returnBlogByPath(
        @PathVariable int year,
        @PathVariable int month,
        @PathVariable int day,
        @PathVariable String filename) {
        logger.info("接收到请求：获取博客内容，文件名为 {}", filename);
        if(this.blogList == null) {
            this.blogList = blogFileService.listPostFilenames(); // 调用 Service 层方法
        }
        LocalDateTime dateTime = LocalDateTime.of(year, month, day, 0, 0, 0);
        Blog tmpblog =  new Blog();
        tmpblog.setDate(dateTime);
        tmpblog.setFilename(filename); // 设置临时博客对象的日期和标题
        int index = Collections.binarySearch(this.blogList, tmpblog);
        if (index >= 0) {
            Blog blog = this.blogList.get(index);
            logger.debug("找到博客：{}", blog.getFilename());
            try {
                tmpblog = blog.clone(); // 克隆博客对象
            } catch (Exception e) {
                logger.error("克隆博客失败：{}", e.getMessage());
            }
            tmpblog.setContent(blog.loadContent()); // 加载博客内容
            return tmpblog; // 返回找到的博客对象
        } 
        int insertionPoint = -(index + 1);
        Blog nextBlog = null; // Declare nextBlog outside the loop and initialize
        do {
            insertionPoint--;
            // Boundary check for insertionPoint to prevent IndexOutOfBoundsException
            if (insertionPoint < 0 || insertionPoint >= this.blogList.size()) {
                break; // Exit loop if insertionPoint is out of bounds
            }
            nextBlog = this.blogList.get(insertionPoint); // Assign to the outer-scoped variable
            if (nextBlog.equals(tmpblog)) {
                logger.debug("找到博客：{}", nextBlog.getFilename());
                
                try {
                    tmpblog = nextBlog.clone(); // Clone the nextBlog object
                    tmpblog.loadContent(); // Load the content of the next blog    
                } catch (Exception e) {
                    logger.error("加载博客内容失败：{}", e.getMessage());
                }
                tmpblog.setContent(nextBlog.loadContent());
                System.out.println(tmpblog.getContent());
                return tmpblog; // 返回找到的博客对象
            }
        } while (dateTime.toLocalDate().equals(nextBlog.getDate().toLocalDate()));
        logger.warn("未找到博客：文件名为 {},日期为 {}-{}-{}", filename, year, month, day);
        return null; // 返回 null 或者可以抛出异常
            
    }
}

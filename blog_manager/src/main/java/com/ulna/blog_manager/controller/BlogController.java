package com.ulna.blog_manager.controller;


import com.ulna.blog_manager.model.Blog;
import com.ulna.blog_manager.repository.BlogFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ulna.blog_manager.repository.BlogFileService;

import java.util.List;


@RestController // 标记为 REST 控制器，方法返回值直接作为响应体 (通常是 JSON)
@RequestMapping("/api/blogs") // 为该控制器下的所有请求路径添加 /api/blogs 前缀 (可选，但推荐)
public class BlogController {

    private static final Logger logger = LoggerFactory.getLogger(BlogController.class);
    private final BlogFileService blogFileService; // 声明对 Service 的依赖

    @Autowired // 自动注入 BlogFileService 实例
    public BlogController(BlogFileService blogFileService) {
        this.blogFileService = blogFileService; // 初始化成员变量
    }

    @GetMapping("/lists") // 处理 GET 请求，路径为 /api/blogs/lists
    public List<Blog> listBlogFilenames() {
        logger.info("接收到请求：获取所有博客文件名");
        List<Blog> blogList = blogFileService.listPostFilenames(); // 调用 Service 层方法
        logger.debug("找到 {} 个文件名", blogList.size());
        return blogList; // 返回列表，Spring Boot 会自动转为 JSON
    }
}

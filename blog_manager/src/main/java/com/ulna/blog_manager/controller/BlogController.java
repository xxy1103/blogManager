package com.ulna.blog_manager.controller;

import com.ulna.blog_manager.model.Message;
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
import org.springframework.web.bind.annotation.RequestBody; // 用于处理请求体
import org.springframework.web.bind.annotation.RequestMethod; // 用于处理请求方法


import com.ulna.blog_manager.repository.BlogFileService;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import org.springframework.web.bind.annotation.PostMapping;



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

    /*
     * 搜索博客
     * @param tmpblog 临时博客对象，包含搜索条件
     * @return 返回搜索结果的博客对象
     */
    public Blog searchBydata(Blog tmpblog) {
        int index = Collections.binarySearch(this.blogList, tmpblog);
        Blog blog = null; // 声明博客对象
        if (index >= 0) {
            blog = this.blogList.get(index);
            logger.debug("找到博客：{}", blog.getFilename());
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
                blog = nextBlog; // 如果找到匹配的博客，赋值给 blog
                break; // 找到匹配的博客，退出循环
            }
        } while (tmpblog.getDate().toLocalDate().equals(nextBlog.getDate().toLocalDate()));

        if(blog == null) {
            logger.warn("未找到博客：文件名为 {},日期为 {}-{}-{}", tmpblog.getFilename(), tmpblog.getDate().getYear(), tmpblog.getDate().getMonthValue(), tmpblog.getDate().getDayOfMonth());
            return null; // 返回 null 或者可以抛出异常
        }
        
        return blog; // 返回博客对象
        
    }

    @GetMapping("/lists") // 处理 GET 请求，路径为 /api/blogs/lists
    public Message listBlogFilenames() {
        try{
            logger.info("接收到请求：获取所有博客文件名");
            this.blogList = blogFileService.listPostFilenames(); // 调用 Service 层方法
            logger.debug("找到 {} 个文件名", blogList.size());
            return  new Message(0,blogList,null); // 返回列表，Spring Boot 会自动转为 JSON
        }
        catch (Exception e){
            logger.error("获取博客文件名失败：{}", e.getMessage());
            return new Message(1, null, "获取博客文件名失败"); // 返回错误信息
        }
    }

    @GetMapping("/{year}/{month}/{day}/{filename}/")
    public Message returnBlogByPath(
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
        Blog blog = searchBydata(tmpblog); // 调用搜索方法
        if(blog == null) {
            logger.warn("未找到博客：文件名为 {},日期为 {}-{}-{}", filename, year, month, day);
            return new Message(1, null, "未找到博客"); // 返回错误信息
        }
        Blog blogClone = null; // 声明博客克隆对象
        try{
            blogClone =  blog.clone(); // 返回博客对象的克隆，避免直接修改原对象
            blogClone.setContent(blog.loadContent());
        } catch (CloneNotSupportedException e) {
            //fallback to return the original object
            logger.error("克隆博客失败：{}", e.getMessage());
            blogClone = new Blog();
            blogClone.setTitle(blog.getTitle());
            blogClone.setCategories(blog.getCategories());
            blogClone.setTags(blog.getTags());
            blogClone.setSaying(blog.getSaying());
            blogClone.setDate(blog.getDate());
            blogClone.setFilename(blog.getFilename());
            blogClone.setFilepath(blog.getFilepath());
            blogClone.setContent(blog.loadContent()); // 加载博客内容
        }
        logger.debug("找到博客：{}", blogClone.getFilename());
        return new Message(0, blogClone, null); // 返回博客对象的克隆，避免直接修改原对象
    }

    @GetMapping("/{year}/{month}/{day}/{filename}/updateinfo") // 处理 GET 请求，路径为 /api/blogs/update
    public Message updateBlogInfo(
        @PathVariable int year,
        @PathVariable int month,
        @PathVariable int day,
        @PathVariable String filename,
        @RequestParam String title,
        @RequestParam String categories,
        @RequestParam String[] tags,
        @RequestParam String saying
    ){
        logger.info("接收到请求：更新博客，文件名为 {}", filename);
        if(this.blogList == null) {
            this.blogList = blogFileService.listPostFilenames(); // 调用 Service 层方法
        }
        LocalDateTime dateTime = LocalDateTime.of(year, month, day, 0, 0, 0);
        Blog tmpblog =  new Blog(title, categories, tags, saying,dateTime);
        Blog blog = searchBydata(tmpblog); // 调用搜索方法
        if(blog == null) {
            logger.warn("未找到博客：文件名为 {},日期为 {}-{}-{}", filename, year, month, day);
            return new Message(1, null, "未找到博客"); // 返回错误信息
        }
        logger.debug("找到博客：{}", blog.getFilename());
        try {
            blogFileService.updateBlogInfo(blog, tmpblog); // 调用 Service 层方法
            return new Message(0, null, "更新博客成功"); // 返回成功信息
        } catch (Exception e) {
            logger.error("更新博客失败：{}", e.getMessage());
            return new Message(1, null, "更新博客失败"); // 返回错误信息
        }
    }

    @PostMapping("/{year}/{month}/{day}/{filename}/updatecontent") // 处理 POST 请求，路径为 /api/blogs/updatecontent
    public Message updateBlogContent(
        @PathVariable int year,
        @PathVariable int month,
        @PathVariable int day,
        @PathVariable String filename,
        @RequestBody String content // 获取请求体中的内容
    ){
        logger.info("接收到请求：更新博客内容，文件名为 {}", filename);
        if(this.blogList == null) {
            this.blogList = blogFileService.listPostFilenames(); // 调用 Service 层方法
        }
        LocalDateTime dateTime = LocalDateTime.of(year, month, day, 0, 0, 0);
        Blog tmpblog =  new Blog();
        tmpblog.setDate(dateTime);
        tmpblog.setFilename(filename); // 设置临时博客对象的日期和标题
        Blog blog = searchBydata(tmpblog); // 调用搜索方法
        if(blog == null) {
            logger.warn("未找到博客：文件名为 {},日期为 {}-{}-{}", filename, year, month, day);
            return new Message(1, null, "未找到博客"); // 返回错误信息
        }
        logger.debug("找到博客：{}", blog.getFilename());
        try {
            blogFileService.updateBlogContent(blog, content); // 调用 Service 层方法
            return new Message(0, null, "更新博客内容成功"); // 返回成功信息
        } catch (Exception e) {
            logger.error("更新博客内容失败：{}", e.getMessage());
            return new Message(1, null, "更新博客内容失败"); // 返回错误信息
        }
    }
    public String postMethodName(@RequestBody String entity) {
        //TODO: process POST request
        
        return entity;
    }
    

    

    @GetMapping("/add") // 处理 GET 请求，路径为 /api/blogs/add
    public Message addBlog(
        @RequestParam String title,
        @RequestParam String categories,
        @RequestParam String[] tags,
        @RequestParam String saying
    ){
        logger.info("接收到请求：添加博客，标题为 {}", title);
        Blog blog = new Blog(title, categories, tags, saying);
        
        if(blogFileService.addBlog(blog)){ // 调用 Service 层方法
            logger.debug("成功添加博客：{}", title);
            return new Message(0, null, "添加博客成功"); // 返回成功信息
        } else {
            logger.error("添加博客失败：{}", title);
            return new Message(1, null, "添加博客失败"); // 返回错误信息
        }
    }
    @GetMapping("/{year}/{month}/{day}/{filename}/delete/") // 处理 GET 请求，路径为 /api/blogs/delete
    public Message deleteBlog(
        @PathVariable int year,
        @PathVariable int month,
        @PathVariable int day,
        @PathVariable String filename
    ){
        logger.info("接收到请求：删除博客，文件名为 {}", filename);
        if(this.blogList == null) {
            this.blogList = blogFileService.listPostFilenames(); // 调用 Service 层方法
        }
        LocalDateTime dateTime = LocalDateTime.of(year, month, day, 0, 0, 0);
        Blog tmpblog =  new Blog();
        tmpblog.setDate(dateTime);
        tmpblog.setFilename(filename); // 设置临时博客对象的日期和标题
        Blog blog = searchBydata(tmpblog); // 调用搜索方法
        if(blog == null) {
            logger.warn("未找到博客：文件名为 {},日期为 {}-{}-{}", filename, year, month, day);
            return new Message(1, null, "未找到博客"); // 返回错误信息
        }
        logger.debug("找到博客：{}", blog.getFilename());
        try {
            blogFileService.deleteBlog(blog); // 调用 Service 层方法
            return new Message(0, null, "删除博客成功"); // 返回成功信息
        } catch (Exception e) {
            logger.error("删除博客失败：{}", e.getMessage());
            return new Message(1, null, "删除博客失败"); // 返回错误信息
        }
    }
}

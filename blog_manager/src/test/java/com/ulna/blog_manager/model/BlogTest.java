package com.ulna.blog_manager.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BlogTest {

    @Test
    void testBlogGettersAndSetters() {
        // 创建测试博客对象
        Blog blog = new Blog();
        
        // 测试标题
        String title = "测试博客标题";
        blog.setTitle(title);
        assertEquals(title, blog.getTitle());

        // 测试日期
        String date = "2025-05-05";
        blog.setDate(date);
        assertEquals(date, blog.getDate());
        
        // 测试分类
        String categories = "技术笔记";
        blog.setCategories(categories);
        assertEquals(categories, blog.getCategories());
        
        // 测试标签
        String[] tags = {"Java", "Spring Boot", "单元测试"};
        blog.setTags(tags);
        assertArrayEquals(tags, blog.getTags());
        
        // 测试谚语
        String saying = "好的测试是成功的基石";
        blog.setSaying(saying);
        assertEquals(saying, blog.getSaying());
        
        // 测试内容
        String content = "这是博客的内容，描述了如何进行单元测试...";
        blog.setContent(content);
        assertEquals(content, blog.getContent());
    }
    
    @Test
    void testToString() {
        // 创建测试博客对象并设置属性
        Blog blog = new Blog();
        blog.setTitle("测试博客");
        blog.setDate("2025-05-05");
        blog.setCategories("测试");
        blog.setTags(new String[]{"测试", "Java"});
        blog.setSaying("测试谚语");
        blog.setContent("这是一个很长的内容，需aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa要确保toString方法能正确截断");
        
        // 测试toString方法
        String result = blog.toString();
        
        
        System.out.println(result); // 打印结果以便调试
    }
}

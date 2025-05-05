package com.ulna.blog_manager.model;

import com.ulna.blog_manager.repository.BlogFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
public class BlogServiceParserTest {

    @Autowired
    private BlogFileService blogFileService;

    @Test
    public void testReadAndParseBlog() throws IOException {
        // 1. 获取博客文件列表
        List<String> blogFiles = blogFileService.listPostFilenames();
        System.out.println("找到的博客文件列表:");
        for (String filename : blogFiles) {
            System.out.println(" - " + filename);
        }

        // 确保列表不为空
        if (!blogFiles.isEmpty()) {
            // 2. 选择第一个文件进行读取
            String firstBlogFile = blogFiles.get(0);
            System.out.println("\n读取文件: " + firstBlogFile);
            
            // 3. 读取文件内容
            String fileContent = blogFileService.readPostContent(firstBlogFile);
            System.out.println("\n文件内容预览 (前200个字符):");
            System.out.println(fileContent.substring(0, Math.min(fileContent.length(), 200)) + "...");
            
            // 4. 使用BlogParser解析内容
            BlogParser parser = new BlogParser();
            Blog blog = parser.parseBlogContent(fileContent);
            
            // 5. 输出解析结果
            System.out.println("\n解析后的Blog对象:");
            System.out.println(blog.toString());
            
            // 6. 输出简洁信息
            System.out.println("\nBlog简洁信息:");
            System.out.println(blog.Info());
        } else {
            System.out.println("没有找到博客文件，请确保配置路径下有.md文件。");
        }
    }
}

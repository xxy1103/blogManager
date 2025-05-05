package com.ulna.blog_manager.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "blog.storage.path=${java.io.tmpdir}/blog-test-folder"
})

public class BlogFileServiceTest {

    // 使用JUnit 5的@TempDir注解创建临时目录
    @TempDir
    static Path tempDir;
    
    private BlogFileService blogFileService;
    
    @TestConfiguration
    static class BlogFileServiceTestConfig {
        // 提供一个测试专用的 BlogFileService 实例，使用临时目录
        @Bean
        @Primary
        public BlogFileService testBlogFileService() {
            return new BlogFileService(tempDir.toString());
        }
    }
    
    @BeforeEach
    void setUp() {
        // 创建 BlogFileService 实例，使用临时目录
        blogFileService = new BlogFileService(tempDir.toString());
        // 手动调用初始化方法，确保目录存在
        blogFileService.init();
    }
    
    @Test
    void testResolve() {
        // 测试正常解析
        String filename = "test.md";
        Path resolved = blogFileService.resolve(filename);
        assertEquals(tempDir.resolve(filename).normalize(), resolved);
        
        // 测试无效文件名
        assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.resolve(null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.resolve("");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.resolve("../attempt-traverse.md");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.resolve("/absolute/path.md");
        });
    }
    
    @Test
    void testListPostFilenames() throws IOException {
        // 准备一些测试文件
        Files.writeString(tempDir.resolve("test1.md"), "test content 1");
        Files.writeString(tempDir.resolve("test2.md"), "test content 2");
        Files.writeString(tempDir.resolve("not-md.txt"), "not a markdown file");
        
        // 获取文件列表
        List<String> filenames = blogFileService.listPostFilenames();
        
        // 验证结果
        assertEquals(2, filenames.size());
        assertTrue(filenames.contains("test1.md"));
        assertTrue(filenames.contains("test2.md"));
        assertFalse(filenames.contains("not-md.txt"));
    }
    
    @Test
    void testReadPostContent() throws IOException {
        // 准备测试文件
        String content = "# Test Markdown\nThis is test content.";
        Path testFile = tempDir.resolve("read-test.md");
        Files.writeString(testFile, content);
        
        // 读取文件内容
        String readContent = blogFileService.readPostContent("read-test.md");
        
        // 验证内容
        assertEquals(content, readContent);
        
        // 测试读取不存在的文件
        assertThrows(NoSuchFileException.class, () -> {
            blogFileService.readPostContent("non-existent.md");
        });
    }
    
    @Test
    void testSavePost() throws IOException {
        // 测试保存新文件
        String content = "# New Post\nThis is a new post content.";
        blogFileService.savePost("new-post.md", content);
        
        // 验证文件是否正确保存
        Path newFile = tempDir.resolve("new-post.md");
        assertTrue(Files.exists(newFile));
        String readContent = Files.readString(newFile, StandardCharsets.UTF_8);
        assertEquals(content, readContent);
        
        // 测试更新现有文件
        String updatedContent = "# Updated Post\nThis content is updated.";
        blogFileService.savePost("new-post.md", updatedContent);
        
        // 验证文件是否正确更新
        String readUpdatedContent = Files.readString(newFile, StandardCharsets.UTF_8);
        assertEquals(updatedContent, readUpdatedContent);
    }
    
    @Test
    void testDeletePost() throws IOException {
        // 准备测试文件
        Path testFile = tempDir.resolve("delete-test.md");
        Files.writeString(testFile, "This file will be deleted");
        
        // 确认文件存在
        assertTrue(Files.exists(testFile));
        
        // 删除文件
        blogFileService.deletePost("delete-test.md");
        
        // 验证文件已被删除
        assertFalse(Files.exists(testFile));
        
        // 测试删除不存在的文件
        assertThrows(NoSuchFileException.class, () -> {
            blogFileService.deletePost("non-existent.md");
        });
    }


}

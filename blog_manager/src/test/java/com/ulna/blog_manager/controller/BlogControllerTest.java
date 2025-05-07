package com.ulna.blog_manager.controller;

import com.ulna.blog_manager.model.Blog;
import com.ulna.blog_manager.model.Message;
import com.ulna.blog_manager.repository.BlogFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException; // Added for throws IOException
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays; // Added for Arrays.equals
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString; // Added for anyString
import static org.mockito.ArgumentMatchers.argThat; // Added for argThat
import static org.mockito.ArgumentMatchers.eq; // Added for eq
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class BlogControllerTest {

    @Mock
    private BlogFileService blogFileService;

    @InjectMocks
    private BlogController blogController;

    private List<Blog> mockBlogListInController; // Used to simulate the controller's internal blogList

    @Mock 
    private Blog testBlog;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        mockBlogListInController = new ArrayList<>();
        
        when(testBlog.getTitle()).thenReturn("测试博客标题");
        when(testBlog.getCategories()).thenReturn("测试分类");
        when(testBlog.getTags()).thenReturn(new String[]{"测试标签1", "测试标签2"});
        when(testBlog.getSaying()).thenReturn("测试谚语");
        when(testBlog.getDate()).thenReturn(LocalDateTime.of(2025, 5, 1, 10, 0, 0));
        when(testBlog.getFilename()).thenReturn("测试博客_20250501_100000.md");
        try {
            when(testBlog.clone()).thenReturn(testBlog); 
        } catch (CloneNotSupportedException e) {
            fail("Clone not supported in mock setup");
        }

        when(blogFileService.listPostFilenames()).thenAnswer(invocation -> {
            mockBlogListInController.clear(); 
            mockBlogListInController.add(testBlog);
            return new ArrayList<>(mockBlogListInController);
        });
    }

    private void initializeControllerBlogList() {
        blogController.listBlogFilenames(); 
    }

    @Test
    public void testListBlogFilenames() {
        Message result = blogController.listBlogFilenames();
        
        assertEquals(0, result.getStatus());
        assertNotNull(result.getData());
        assertTrue(result.getData() instanceof List);
        List<?> resultList = (List<?>) result.getData();
        assertEquals(1, resultList.size(), "列表大小应该为1");
        assertSame(testBlog, resultList.get(0), "列表中的博客对象不匹配");
        assertNull(result.getError());
        
        verify(blogFileService, times(1)).listPostFilenames();
    }

    @Test
    public void testReturnBlogByPath() throws Exception {
        String testContent = "测试博客内容";
        when(testBlog.loadContent()).thenReturn(testContent); 
        
        initializeControllerBlogList(); 

        Message result = blogController.returnBlogByPath(
                2025, 5, 1, "测试博客_20250501_100000.md");
        
        assertEquals(0, result.getStatus(), "状态码应为0 (成功)");
        assertNotNull(result.getData(), "返回数据不应为null");
        assertTrue(result.getData() instanceof Blog, "返回数据应为Blog类型");
        Blog resultBlog = (Blog) result.getData();
        assertSame(testBlog, resultBlog, "返回的博客对象应为mock的testBlog"); 
        assertEquals(testContent, resultBlog.getContent(), "博客内容不匹配");
        assertEquals("测试博客标题", resultBlog.getTitle(), "博客标题不匹配"); 
        assertNull(result.getError(), "错误信息应为null");
    }

    @Test
    public void testReturnBlogByPath_NotFound() {
        initializeControllerBlogList(); 

        Message result = blogController.returnBlogByPath(
                2025, 5, 2, "不存在的博客.md");
        
        assertEquals(1, result.getStatus());
        assertNull(result.getData());
        assertEquals("未找到博客", result.getError());
    }

    @Test
    public void testUpdateBlogInfo() throws IOException { 
        initializeControllerBlogList(); 
        doNothing().when(blogFileService).updateBlogInfo(any(Blog.class), any(Blog.class));

        Message result = blogController.updateBlogInfo(
                2025, 5, 1, "测试博客_20250501_100000.md",
                "更新后的标题", "更新后的分类", new String[]{"更新标签1", "更新标签2"}, "更新后的谚语");
        
        assertEquals(0, result.getStatus(), "更新成功时状态码应为0");
        assertNull(result.getData());
        assertEquals("更新博客成功", result.getError(), "成功信息不匹配");

        verify(blogFileService, times(1)).updateBlogInfo(eq(testBlog), argThat(updatedBlog ->
            updatedBlog.getTitle().equals("更新后的标题") &&
            updatedBlog.getCategories().equals("更新后的分类") &&
            Arrays.equals(updatedBlog.getTags(), new String[]{"更新标签1", "更新标签2"}) &&
            updatedBlog.getSaying().equals("更新后的谚语")
        ));
    }

    @Test
    public void testUpdateBlogInfo_NotFound() throws IOException { 
        initializeControllerBlogList();

        Message result = blogController.updateBlogInfo(
                2025, 5, 2, "不存在的博客.md",
                "更新后的标题", "更新后的分类", new String[]{"更新标签1", "更新标签2"}, "更新后的谚语");
        
        assertEquals(1, result.getStatus());
        assertNull(result.getData());
        assertEquals("未找到博客", result.getError());
        verify(blogFileService, never()).updateBlogInfo(any(), any());
    }

    @Test
    public void testUpdateBlogContent() throws IOException { 
        String newContent = "更新后的博客内容";
        initializeControllerBlogList(); 
        doNothing().when(blogFileService).updateBlogContent(any(Blog.class), anyString());
        
        Message result = blogController.updateBlogContent(
                2025, 5, 1, "测试博客_20250501_100000.md", newContent);
        
        assertEquals(0, result.getStatus(), "更新内容成功时状态码应为0");
        assertNull(result.getData());
        assertEquals("更新博客内容成功", result.getError(), "成功信息不匹配");
        
        verify(blogFileService, times(1)).updateBlogContent(eq(testBlog), eq(newContent));
    }

    @Test
    public void testUpdateBlogContent_NotFound() throws IOException { 
        String newContent = "更新后的博客内容";
        initializeControllerBlogList(); 
        
        Message result = blogController.updateBlogContent(
                2025, 5, 2, "不存在的博客.md", newContent);
        
        assertEquals(1, result.getStatus());
        assertNull(result.getData());
        assertEquals("未找到博客", result.getError());
        verify(blogFileService, never()).updateBlogContent(any(), anyString());
    }

    @Test
    public void testAddBlog() {
        when(blogFileService.addBlog(any(Blog.class))).thenReturn(true);

        Message result = blogController.addBlog(
                "新博客标题", "新博客分类", new String[]{"新标签1", "新标签2"}, "新谚语");
        
        assertEquals(0, result.getStatus());
        assertNull(result.getData());
        assertEquals("添加博客成功", result.getError());
        
        verify(blogFileService, times(1)).addBlog(argThat(addedBlog ->
            addedBlog.getTitle().equals("新博客标题") &&
            addedBlog.getCategories().equals("新博客分类")
        ));
    }

    @Test
    public void testAddBlog_Failure() {
        when(blogFileService.addBlog(any(Blog.class))).thenReturn(false);
        
        Message result = blogController.addBlog(
                "新博客标题", "新博客分类", new String[]{"新标签1", "新标签2"}, "新谚语");
        
        assertEquals(1, result.getStatus());
        assertNull(result.getData());
        assertEquals("添加博客失败", result.getError());
        
        verify(blogFileService, times(1)).addBlog(any(Blog.class)); 
    }

    @Test
    public void testDeleteBlog() throws IOException { 
        initializeControllerBlogList(); 
        doNothing().when(blogFileService).deleteBlog(any(Blog.class));

        Message result = blogController.deleteBlog(
                2025, 5, 1, "测试博客_20250501_100000.md");
        
        assertEquals(0, result.getStatus(), "删除成功时状态码应为0");
        assertNull(result.getData());
        assertEquals("删除博客成功", result.getError(), "成功信息不匹配");
        
        verify(blogFileService, times(1)).deleteBlog(testBlog); 
    }

    @Test
    public void testDeleteBlog_NotFound() throws IOException { 
        initializeControllerBlogList(); 

        Message result = blogController.deleteBlog(
                2025, 5, 2, "不存在的博客.md");
        
        assertEquals(1, result.getStatus());
        assertNull(result.getData());
        assertEquals("未找到博客", result.getError());
        verify(blogFileService, never()).deleteBlog(any());
    }

    @Test
    public void testSearchBydata() throws Exception { 
        initializeControllerBlogList(); 
        when(testBlog.loadContent()).thenReturn("一些内容"); 

        Message result = blogController.returnBlogByPath( 
                testBlog.getDate().getYear(), 
                testBlog.getDate().getMonthValue(), 
                testBlog.getDate().getDayOfMonth(), 
                testBlog.getFilename());
        
        assertEquals(0, result.getStatus(), "通过searchByData查找应成功");
        assertNotNull(result.getData());
        assertTrue(result.getData() instanceof Blog);
        assertSame(testBlog, result.getData(), "searchByData应返回mock的testBlog");
    }
}

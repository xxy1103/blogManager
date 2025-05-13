package com.ulna.blog_manager.repository; 

import com.ulna.blog_manager.Config.Config;
import com.ulna.blog_manager.model.Blog; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import jakarta.annotation.PostConstruct; // 如果使用较新的 Spring Boot/Jakarta EE
// import javax.annotation.PostConstruct; // 如果使用较旧的 Spring Boot/Java EE
import java.io.IOException;
import java.nio.charset.StandardCharsets; // 明确指定字符集是个好习惯
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlogFileService {

    private static final Logger logger = LoggerFactory.getLogger(BlogFileService.class);

    private final Path storageLocation; // 博客文件存储的根目录路径对象


    private Config config;

    @Autowired
    public BlogFileService(Config config) {
        this.config = config;
        // 从配置中获取存储路径，并确保是绝对路径
        this.storageLocation = Paths.get(config.getBlogStoragePath()).toAbsolutePath().normalize();
        logger.info("博客存储目录: {}", storageLocation);
    }

    /**
     * 初始化方法，在服务bean创建完成后执行。
     * 检查配置的存储目录是否存在，如果不存在则尝试创建。
     * 如果路径存在但不是一个目录，则抛出异常。
     */
    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(storageLocation)) {
                // 如果目录不存在，递归创建所有父目录及该目录
                Files.createDirectories(storageLocation);
                logger.info("成功创建博客存储目录: {}", storageLocation);
            } else if (!Files.isDirectory(storageLocation)) {
                // 如果路径存在但不是一个目录，这是个配置错误
                 logger.error("配置的存储路径存在但不是一个目录: {}", storageLocation);
                 throw new RuntimeException("配置的存储路径存在但不是一个目录: " + storageLocation);
            }
        } catch (IOException e) {
            // 处理创建目录时可能发生的 IO 异常
            logger.error("无法初始化存储位置: {}", storageLocation, e);
            throw new RuntimeException("无法初始化存储位置: " + storageLocation, e);
        }
    }

    /**
     * 解析文件名，将其与存储根目录结合，并进行安全检查以防止路径遍历攻击。
     *
     * @param filename 要解析的文件名 (例如 "my-post.md")
     * @return 指向目标文件的、经过安全检查的 Path 对象
     * @throws IllegalArgumentException 如果文件名无效 (null, 空白, 或包含 "..")
     * @throws RuntimeException 如果解析后的路径试图访问存储目录之外的位置 (路径遍历攻击)
     */
    public Path resolve(String filename) {
        // 检查文件名是否为空或仅包含空白字符
        if (filename == null || filename.isBlank()) {
             throw new IllegalArgumentException("文件名不能为空");
        }
        // 基础清理：移除首尾空白，并拒绝包含 ".." 的路径以防止简单遍历
        String sanitizedFilename = filename.trim();
        if (sanitizedFilename.contains("..")) {
             logger.warn("检测到文件名中包含 '..' 序列，已拒绝: {}", filename);
             throw new IllegalArgumentException("文件名不能包含 '..' 序列: " + filename);
        }
        if (sanitizedFilename.startsWith("/") || sanitizedFilename.startsWith("\\")) {
             logger.warn("检测到文件名以路径分隔符开头，已拒绝: {}", filename);
             throw new IllegalArgumentException("文件名不能以路径分隔符开头: " + filename);
        }

        // 将文件名附加到存储根目录，并再次规范化
        Path filePath = this.storageLocation.resolve(sanitizedFilename).normalize();

        // 最重要的安全检查：确保最终解析的路径仍然在允许的存储目录下
        if (!filePath.startsWith(this.storageLocation)) {
            logger.warn("路径遍历攻击尝试被阻止，目标文件名: {}", filename);
            throw new RuntimeException("不允许访问指定存储目录之外的文件: " + filename);
        }
        return filePath;
    }

    /**
     * 递归列出存储目录及其所有子目录下的 Markdown (.md) 文件的文件名。
     *
     * @return 包含所有 .md 文件的 Blog 对象列表，按字母顺序排序。
     *         如果目录为空或发生错误，可能返回空列表。
     * @throws RuntimeException 如果访问目录时发生 I/O 错误。
     */
    public List<Blog> listPostFilenames() {
        
        List<Blog> blogList = new ArrayList<>();
        logger.debug("正在递归列出目录 '{}' 中的 .md 文件", storageLocation);

        try {
            // 使用 Files.walk 递归遍历目录树
            Files.walk(storageLocation)
                .filter(Files::isRegularFile)  // 只处理常规文件，不处理目录
                .filter(path -> path.toString().endsWith(".md"))  // 只处理 .md 文件
                .forEach(path -> {
                    try {
                        // 读取文件内容并创建 Blog 对象
                        String content = Files.readString(path, StandardCharsets.UTF_8);
                        blogList.add(new Blog(content, path)); // 直接使用 Path 对象
                    } catch (IOException e) {
                        // 记录单个文件读取错误，但继续处理其他文件
                        logger.error("读取文件 '{}' 时发生错误", path, e);
                    }
                });
        } catch (IOException e) {
            // 记录错误并抛出运行时异常
            logger.error("无法递归列出目录 '{}' 中的文件", this.storageLocation, e);
            throw new RuntimeException("无法列出博客文章文件于 " + this.storageLocation, e);
        }

        logger.debug("找到 {} 个 .md 文件", blogList.size());
        // 对文件名进行排序
        Collections.sort(blogList);
        return blogList;
    }

    /**
     * 保存（创建或覆盖）指定文件名的 Markdown 文章内容。
     *
     * @param filename 要保存的文件名 (例如 "new-post.md" 或 "existing-post.md")
     * @param content 要写入文件的 Markdown 内容字符串。
     * @throws IOException 如果在写入文件时发生 I/O 错误。
     * @throws RuntimeException 如果发生路径解析安全问题。
     */    
    public void savePost(Blog blog) throws IOException {
        // 首先获取安全的文件路径
        Path filePath = blog.getFilepath(); // 直接使用 Blog 对象的文件路径

        logger.debug("正在保存文件: {}", filePath);
        try {
            // 确保父目录存在，如果不存在则递归创建
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                logger.debug("创建父目录: {}", parentDir);
                Files.createDirectories(parentDir);
            }
            
            // 将字符串内容写入文件，使用 UTF-8 编码。
            // Files.writeString 默认选项是 CREATE, TRUNCATE_EXISTING, WRITE
            // 即：如果文件不存在则创建，如果存在则清空内容再写入。
            //System.out.println(blog.toString());
            Files.writeString(filePath, blog.toString(), StandardCharsets.UTF_8);
            logger.info("成功保存文件: {}", filePath);
        } catch (IOException e) {
            logger.error("保存文件 '{}' 时发生错误", filePath, e);
            // 重新抛出异常
            throw e;
        }
    }

    /**
     * 删除指定文件名的 Markdown 文章。
     *
     * @param filename 要删除的文件名 (例如 "old-post.md")
     * @throws IOException 如果文件不存在或在删除时发生 I/O 错误。
     * @throws RuntimeException 如果发生路径解析安全问题。
     */
    public void deleteBlog(Blog blog) throws IOException {
        // 首先获取安全的文件路径
        Path filePath = blog.getFilepath();

        logger.debug("正在尝试删除文件: {}", filePath);
        try {
            // 尝试删除文件。如果文件不存在，Files.delete() 会抛出 NoSuchFileException
            Files.delete(filePath);
            logger.info("成功删除文件: {}", filePath);
        } catch (NoSuchFileException e) {
            // 文件本身就不存在，可能不需要作为错误处理，或者根据业务逻辑决定
            logger.warn("尝试删除不存在的文件: {}", filePath);
            // 可以选择重新抛出，或者静默处理/返回 false 等
            throw e;
        } catch (IOException e) {
            // 处理其他可能的 IO 错误，例如权限问题
            logger.error("删除文件 '{}' 时发生错误", filePath, e);
            // 重新抛出异常
            throw e;
        }
    }

    public boolean addBlog(Blog blog) {
        if(blog.getFilepath() == null) {
            // Construct the path from categories and filename
            String relativePath = blog.getCategories() + "/" + blog.getFilename(); // Ensure no leading slash for correct resolution
            Path newBlogPath = this.storageLocation.resolve(relativePath).normalize();

            // Security check: Ensure the resolved path is still within the main storage directory
            if (!newBlogPath.startsWith(this.storageLocation)) {
                logger.warn("Attempt to create blog outside designated storage area. Base: {}, Target: {}", this.storageLocation, newBlogPath);
                throw new IllegalArgumentException("Generated blog path is outside the designated storage area: " + newBlogPath.toString());
            }
            blog.setFilepath(newBlogPath); // 设置文件路径
        }
        try {
            savePost(blog); // 调用保存方法
            return true; // 返回成功状态
        } catch (IOException e) {
            logger.error("添加博客失败: {}", e.getMessage());
            return false; // 返回失败状态
        }
    }
    
    public boolean updateBlogInfo(Blog oldBlog, Blog newBlog) {
        newBlog.setFilename(oldBlog.getFilename()); // 保留原文件名
        newBlog.setDate(oldBlog.getDate()); // 保留原日期
        String relativePath = newBlog.getCategories() + "/" + newBlog.getFilename(); // Ensure no leading slash for correct resolution
        Path newBlogPath = this.storageLocation.resolve(relativePath).normalize();
        // Security check: Ensure the resolved path is still within the main storage directory
        if (!newBlogPath.startsWith(this.storageLocation)) {
            logger.warn("Attempt to create blog outside designated storage area. Base: {}, Target: {}", this.storageLocation, newBlogPath);
            throw new IllegalArgumentException("Generated blog path is outside the designated storage area: " + newBlogPath.toString());
        }
        newBlog.setFilepath(newBlogPath); // 设置文件路径
        Blog tmpBlog;
        try {
            tmpBlog = newBlog.clone(); // 克隆一个新的对象
        } catch (CloneNotSupportedException e) {
            logger.error("克隆博客对象失败: {}", e.getMessage());
            //fallback
            tmpBlog = new Blog();
            // Manually copy properties from newBlog to tmpBlog
            tmpBlog.setFilepath(newBlog.getFilepath());
            tmpBlog.setFilename(newBlog.getFilename());
            tmpBlog.setTitle(newBlog.getTitle());
            tmpBlog.setDate(newBlog.getDate());
            tmpBlog.setCategories(newBlog.getCategories());
            tmpBlog.setTags(newBlog.getTags() != null ? newBlog.getTags().clone() : null); // Deep copy for array
            tmpBlog.setSaying(newBlog.getSaying());
            // Content will be set from oldBlog.loadContent() later
            logger.warn("克隆博客对象失败，使用手动复制作为后备。");
            
        }
        tmpBlog.setContent(oldBlog.loadContent()); // 设置内容为旧博客的内容
        try{
            deleteBlog(oldBlog); // 删除旧博客
            savePost(tmpBlog); // 保存新博客
            return true; // 返回成功状态    
        }
        catch (IOException e) {
            logger.error("更新博客失败: {}", e.getMessage());
            try{
                Blog tmpOBlog = oldBlog.clone(); 
                tmpOBlog.setContent(tmpBlog.getContent());
                savePost(oldBlog);
            }
            catch(Exception ee){
                return false;
            }
            return false; // 返回失败状态
        }
    }

    public boolean updateBlogContent(Blog blog,String content) {
        Blog blogClone = null;
        try {
            blogClone = blog.clone(); // 克隆一个新的对象
            blogClone.setContent(content); // 设置新的内容
        } catch (Exception e) {
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
        try {
            deleteBlog(blog); // 删除旧博客
            savePost(blogClone); // 保存新博客
            return true; // 返回成功状态    
        } catch (IOException e) {
            logger.error("更新博客内容失败: {}", e.getMessage());
            try{
                Blog tmpOBlog = blog.clone(); 
                tmpOBlog.setContent(blogClone.getContent());
                savePost(tmpOBlog);
            }
            catch(Exception ee){
                return false;
            }
            return false; // 返回失败状态
        }
    }
}

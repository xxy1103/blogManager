package com.ulna.blog_manager.repository; // 替换为你的实际包名

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /**
     * 服务构造函数。
     * 使用 @Value 注解从配置文件 (application.properties/yml) 注入博客存储路径。
     * 初始化 storageLocation 字段，并确保路径是绝对且规范化的。
     *
     * @param storagePath 从配置中注入的存储路径字符串
     */
    public BlogFileService(@Value("${blog.storage.path}") String storagePath) {
        // 将字符串路径转换为 Path 对象，获取绝对路径并规范化 (移除冗余的 . 和 ..)
        this.storageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        logger.info("博客存储位置初始化为: {}", this.storageLocation);
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
     * 列出存储目录下所有 Markdown (.md) 文件的文件名。
     *
     * @return 包含所有 .md 文件名的字符串列表，按字母顺序排序。如果目录为空或发生错误，可能返回空列表。
     * @throws RuntimeException 如果访问目录时发生 I/O 错误。
     */
    public List<String> listPostFilenames() {
        List<String> filenames = new ArrayList<>();
        // 定义 Glob 模式以匹配所有以 .md 结尾的文件
        String glob = "*.md";

        logger.debug("正在列出目录 '{}' 中匹配 '{}' 的文件", storageLocation, glob);

        // 使用 try-with-resources 确保 DirectoryStream 被正确关闭
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageLocation, glob)) {
            for (Path entry : stream) {
                // 将找到的文件名添加到列表中
                filenames.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            // 记录错误并抛出运行时异常
            logger.error("无法列出目录 '{}' 中的文件 (Glob: '{}')", this.storageLocation, glob, e);
            // 在实际应用中，可以考虑定义更具体的自定义异常，如 StorageException
            throw new RuntimeException("无法列出博客文章文件于 " + this.storageLocation, e);
        }

        logger.debug("找到 {} 个匹配 '{}' 的文件", filenames.size(), glob);
        // 对文件名进行排序
        Collections.sort(filenames);
        return filenames;
    }

    /**
     * 读取指定文件名的 Markdown 文章内容。
     *
     * @param filename 要读取的文件名 (例如 "my-post.md")
     * @return 文件内容的字符串形式。
     * @throws IOException 如果文件不存在、不是一个常规文件、或在读取时发生 I/O 错误。
     * @throws RuntimeException 如果发生路径解析安全问题。
     */
    public String readPostContent(String filename) throws IOException {
        // 首先获取安全的文件路径
        Path filePath = resolve(filename);

        // 检查文件是否存在并且是一个普通文件 (不是目录)
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            logger.warn("尝试读取不存在或不是常规文件的文件: {}", filePath);
            // 抛出 FileNotFoundException 或 NoSuchFileException 更符合语义
            throw new NoSuchFileException("文件未找到或不是一个常规文件: " + filename);
        }

        logger.debug("正在读取文件: {}", filePath);
        try {
            // 读取文件所有内容为字符串，使用 UTF-8 编码 (推荐)
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("读取文件 '{}' 时发生错误", filePath, e);
            // 重新抛出异常，让上层调用者处理
            throw e;
        }
    }

    /**
     * 保存（创建或覆盖）指定文件名的 Markdown 文章内容。
     *
     * @param filename 要保存的文件名 (例如 "new-post.md" 或 "existing-post.md")
     * @param content 要写入文件的 Markdown 内容字符串。
     * @throws IOException 如果在写入文件时发生 I/O 错误。
     * @throws RuntimeException 如果发生路径解析安全问题。
     */
    public void savePost(String filename, String content) throws IOException {
        // 首先获取安全的文件路径
        Path filePath = resolve(filename);

        // 检查传入的内容是否为 null，如果是，可以视情况处理（抛异常或写入空字符串）
        if (content == null) {
            content = ""; // 或者抛出 IllegalArgumentException("Content cannot be null");
        }

        logger.debug("正在保存文件: {}", filePath);
        try {
            // 将字符串内容写入文件，使用 UTF-8 编码。
            // Files.writeString 默认选项是 CREATE, TRUNCATE_EXISTING, WRITE
            // 即：如果文件不存在则创建，如果存在则清空内容再写入。
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
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
    public void deletePost(String filename) throws IOException {
        // 首先获取安全的文件路径
        Path filePath = resolve(filename);

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
}
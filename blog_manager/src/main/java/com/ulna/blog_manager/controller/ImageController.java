package com.ulna.blog_manager.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController // 标记为 REST 控制器，方法返回值直接作为响应体 (通常是 JSON)
@RequestMapping("/api") // 为该控制器下的所有请求路径添加 /api 前缀 (可选，但推荐)
public class ImageController {

    private final String imageStoragePath;
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    public ImageController(@Value("${image.storage.path}") String imageStoragePath) {
        this.imageStoragePath = imageStoragePath;
    }

    /**
     * 根据图片相对路径返回图片资源
     * @param relativePath 图片的相对路径，如 "计算机网络第五章--网络层_20250425_151005/1745565448467.png"
     * @return 图片资源的响应实体
     */
    @GetMapping("/{*relativePath}")
    public ResponseEntity<Resource> getImage(@PathVariable("relativePath") String relativePath) {
        try {

            // 构建完整的文件路径
            Path imagePath = Paths.get(imageStoragePath, relativePath);
            File imageFile = imagePath.toFile();
            
            // 检查文件是否存在
            if (!imageFile.exists() || !imageFile.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            // 创建文件资源
            FileSystemResource resource = new FileSystemResource(imageFile);
            
            // 设置响应头部
            HttpHeaders headers = new HttpHeaders();
            
            // 根据文件扩展名确定MIME类型
            String filename = imageFile.getName();
            MediaType mediaType = getMediaTypeForFilename(filename);
            headers.setContentType(mediaType);

            logger.info("Returning image: {}", filename);

            // 返回图片资源
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据文件名确定MIME类型
     * @param filename 文件名
     * @return 对应的MediaType
     */
    private MediaType getMediaTypeForFilename(String filename) {
        String extension = "";
        int i = filename.lastIndexOf('.');
        if (i > 0) {
            extension = filename.substring(i + 1).toLowerCase();
        }
        
        switch (extension) {
            case "png":
                return MediaType.IMAGE_PNG;
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "gif":
                return MediaType.IMAGE_GIF;
            case "svg":
                return new MediaType("image", "svg+xml");
            case "webp":
                return new MediaType("image", "webp");
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 上传图片到指定的相对路径
     * @param file 要上传的图片文件
     * @param relativePath 保存图片的相对路径，如 "image/计算机网络第五章--网络层_20250425_151005"
     * @return 上传结果信息
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("relativePath") String relativePath) {
        
        //打印日志
        logger.info("Uploading image to path: {}", relativePath);

        Map<String, Object> response = new HashMap<>();
        
        try {
            // 确保相对路径不包含特殊字符
            if (relativePath.contains("..")) {
                response.put("success", false);
                response.put("message", "不允许使用相对路径符号");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 构建保存文件的完整路径
            Path directoryPath = Paths.get(imageStoragePath, relativePath);
            File directory = directoryPath.toFile();
            
            // 如果目录不存在，创建目录
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            String filename = originalFilename != null ? originalFilename : System.currentTimeMillis() + ".png";
            
            // 构建完整的文件保存路径
            Path filePath = Paths.get(directoryPath.toString(), filename);
            
            // 保存文件
            Files.copy(file.getInputStream(), filePath);
            
            logger.info("Image uploaded successfully: {}", filePath);
            
            // 构建访问路径
            String accessPath = relativePath + "/" + filename;
            
            // 返回成功响应
            response.put("success", true);
            response.put("message", "图片上传成功");
            response.put("path", accessPath);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            logger.error("Failed to upload image", e);
            response.put("success", false);
            response.put("message", "上传图片失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

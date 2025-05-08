package com.ulna.blog_manager.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

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
}

package com.ulna.blog_manager.controller;

import com.ulna.blog_manager.model.Image;
import com.ulna.blog_manager.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController // 标记为 REST 控制器，方法返回值直接作为响应体 (通常是 JSON)
@RequestMapping("/image") // 设置请求路径前缀为 /image
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageService imageService;

    /**
     * 根据图片名称返回图片资源
     * @param filename 图片的名称
     * @return 图片资源的响应实体
     */
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable("filename") String filename) {
        Optional<Image> imageOptional = imageService.getImageByFileName(filename);
        System.out.println("Fetching image with filename: " + filename);
        if (imageOptional.isPresent()) {
            Image image = imageOptional.get();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getFileName() + "\"")
                    .body(image.getData());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 上传图片到数据库
     * @param file 要上传的图片文件
     * @return 上传结果信息
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();
        System.out.println("Received file: " + file.getOriginalFilename());
        try {
            Image savedImage = imageService.saveImage(file);
            response.put("success", true);
            response.put("message", "图片上传成功");
            response.put("imageId", savedImage.getId());
            response.put("fileName", savedImage.getFileName());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("Failed to upload image", e);
            response.put("success", false);
            response.put("message", "上传图片失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

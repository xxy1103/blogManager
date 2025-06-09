package com.ulna.blog_manager.service;

import com.ulna.blog_manager.model.Image;
import com.ulna.blog_manager.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
public class ImageService {

    @Autowired
    private ImageRepository imageRepository;

    public Image saveImage(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        byte[] data = file.getBytes();
        Image image = new Image(fileName, contentType, data);
        return imageRepository.save(image);
    }

    public Optional<Image> getImageByFileName(String fileName) {
        return imageRepository.findByFileName(fileName);
    }

    public Optional<Image> getImageById(Long id) {
        return imageRepository.findById(id);
    }
}

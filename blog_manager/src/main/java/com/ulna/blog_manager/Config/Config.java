package com.ulna.blog_manager.Config;

import java.nio.file.Path;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
    private Path blogStoragePath;
    private Path imageStoragePath;
    
    @JsonProperty("XModelAPIKey")
    private String XModelAPIKey;

    public String getXModelAPIKey() {
        return XModelAPIKey;
    }

    public void setXModelAPIKey(String XModelAPIKey) {
        this.XModelAPIKey = XModelAPIKey;
    }
    public Path getBlogStoragePath() {
        return blogStoragePath;
    }
    public void setBlogStoragePath(Path blogStoragePath) {
        this.blogStoragePath = blogStoragePath;
    }
    public Path getImageStoragePath() {
        return imageStoragePath;
    }
    public void setImageStoragePath(Path imageStoragePath) {
        this.imageStoragePath = imageStoragePath;
    }

}

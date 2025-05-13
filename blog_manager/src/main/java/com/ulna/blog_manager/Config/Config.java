package com.ulna.blog_manager.Config;

import java.nio.file.Path;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
    private String blogStoragePath;
    private String imageStoragePath;
    private String xmodelAPIKey;

    public String getXmodelAPIKey() {
        return xmodelAPIKey;
    }

    public void setXmodelAPIKey(String xmodelAPIKey) {
        this.xmodelAPIKey = xmodelAPIKey;
    }
    public String getBlogStoragePath() {
        return blogStoragePath;
    }
    public void setBlogStoragePath(String blogStoragePath) {
        this.blogStoragePath = blogStoragePath;
    }
    public String getImageStoragePath() {
        return imageStoragePath;
    }
    public void setImageStoragePath(String imageStoragePath) {
        this.imageStoragePath = imageStoragePath;
    }

}

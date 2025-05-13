package com.ulna.blog_manager.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.io.IOException;
import jakarta.annotation.PostConstruct; // 如果使用较新的 Spring Boot/Jakarta EE

@Configuration
public class ConfigService {
    private Path configPath;
    private Config config;

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    /**
     * 服务构造函数。
     * 使用 @Value 注解从配置文件 (application.properties/yml) 注入博客配置文件路径
     * 初始化 configPath 字段，并确保路径是绝对且规范化的。
     *
     * @param configPath 从配置中注入的配置文件路径字符串
     */
    public ConfigService(@Value("${config.storage.path}") String configPath) {
        this.configPath = Paths.get(configPath).toAbsolutePath().normalize();
    }
    
    @PostConstruct
    public void loadConfig() {
        // 创建用于序列化和反序列化的ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置ObjectMapper以方便调试
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 忽略未知属性
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // 美化输出JSON
        
        // 确保配置路径的父目录存在
        try {
            if (configPath.getParent() != null && !Files.exists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
                logger.info("配置目录创建成功: " + configPath.getParent());
            }
            
            // 检查配置文件是否存在
            if (!Files.exists(configPath)) {
                logger.info("配置文件不存在，将创建默认配置: " + configPath);
                createDefaultConfig(objectMapper);
            } else {
                logger.info("配置文件已存在: " + configPath);
                // 读取配置文件
                try {
                    byte[] jsonData = Files.readAllBytes(configPath);
                    config = objectMapper.readValue(jsonData, Config.class);
                    logger.info("成功导入配置文件: " + config);
                    
                    // 检查配置是否有必要的字段，如果没有则设置默认值
                    validateAndUpdateConfig(objectMapper);
                    
                } catch (IOException e) {
                    logger.error("读取配置文件失败: " + e.getMessage());
                    // 创建默认配置
                    createDefaultConfig(objectMapper);
                }
            }
        } catch (Exception e) {
            logger.error("配置加载失败: " + e.getMessage());
            // 创建内存中的默认配置
            config = createDefaultConfigInMemory();
        }
    }
    
    /**
     * 验证配置并更新缺失的值
     */
    private void validateAndUpdateConfig(ObjectMapper objectMapper) throws IOException {
        boolean needsUpdate = false;
        
        if (config == null) {
            config = createDefaultConfigInMemory();
            needsUpdate = true;
        } else {
            // 检查并设置默认路径
            if (config.getBlogStoragePath() == null) {
                config.setBlogStoragePath("");
                needsUpdate = true;
            }
            if (config.getImageStoragePath() == null) {
                config.setImageStoragePath("");
                needsUpdate = true;
            }
            if (config.getXmodelAPIKey() == null) {
                config.setXmodelAPIKey("");
                needsUpdate = true;
            }
            if (config.getBigmodelAPIKey() == null) {
                config.setBigmodelAPIKey("");
                needsUpdate = true;
            }
        }
        
        // 如果有更新，保存配置
        if (needsUpdate) {
            saveConfig(objectMapper);
        }
    }
    
    /**
     * 创建默认配置并保存到文件
     */
    private void createDefaultConfig(ObjectMapper objectMapper) {
        config = createDefaultConfigInMemory();
        saveConfig(objectMapper);
    }
    
    /**
     * 创建内存中的默认配置对象
     */
    private Config createDefaultConfigInMemory() {
        Config defaultConfig = new Config();
        defaultConfig.setBlogStoragePath("");
        defaultConfig.setImageStoragePath("");
        defaultConfig.setXmodelAPIKey("");
        defaultConfig.setBigmodelAPIKey("");
        return defaultConfig;
    }
    
    /**
     * 保存配置到文件
     */
    private void saveConfig(ObjectMapper objectMapper) {
        try {
            objectMapper.writeValue(configPath.toFile(), config);
            logger.info("配置已保存到: " + configPath);
        } catch (IOException e) {
            logger.error("保存配置文件失败: " + e.getMessage());
        }
    }
    
    
    /**
     * 获取已加载的配置对象
     * 
     * @return 配置对象
     */
    @Bean
    public Config getConfig() {
        return config;
    }

    /**
     * 更新配置并写入文件
     * 
     * @param newConfig 新的配置对象
     * @return 更新是否成功
     */
    public boolean updateConfig(Config newConfig) {
        if (newConfig == null) {
            logger.error("更新配置失败：提供的配置对象为空");
            return false;
        }
        
        // 保存旧配置，以便更新失败时可以回滚
        Config oldConfig = this.config;
        
        try {
            // 更新当前内存中的配置
            this.config.setBlogStoragePath(newConfig.getBlogStoragePath());
            this.config.setImageStoragePath(newConfig.getImageStoragePath());
            this.config.setXmodelAPIKey(newConfig.getXmodelAPIKey());
            this.config.setBigmodelAPIKey(newConfig.getBigmodelAPIKey());
            // 记录更新的配置
            logger.info("更新配置: " + this.config);
            
            // 创建用于序列化的ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // 美化输出JSON
            
            // 保存到文件
            saveConfig(objectMapper);
            
            logger.info("配置更新成功");
            return true;
        } catch (Exception e) {
            // 发生异常，回滚到旧配置
            this.config = oldConfig;
            logger.error("更新配置失败：" + e.getMessage());
            return false;
        }
    }
}

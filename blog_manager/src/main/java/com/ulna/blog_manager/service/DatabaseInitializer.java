package com.ulna.blog_manager.service;

import com.ulna.blog_manager.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    
    @Autowired
    private UserService userService;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("开始初始化数据库...");
        
        // 创建默认管理员用户
        if (!userService.existsByUsername("admin")) {
            try {
                User admin = userService.createUser("admin", "admin@test.com", "admin123");
                logger.info("创建默认管理员用户成功: {}", admin.getUsername());
            } catch (Exception e) {
                logger.error("创建默认管理员用户失败: {}", e.getMessage());
            }
        } else {
            logger.info("管理员用户已存在，跳过创建");
        }
        
        logger.info("数据库初始化完成");
    }
}

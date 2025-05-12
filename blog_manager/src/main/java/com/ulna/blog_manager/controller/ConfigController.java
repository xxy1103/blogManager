package com.ulna.blog_manager.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ulna.blog_manager.Config.Config;
import com.ulna.blog_manager.Config.ConfigService;


@RestController
@RequestMapping("/config")
public class ConfigController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);
    private final ConfigService config;
    
    @Autowired
    public ConfigController(ConfigService config) {
        this.config = config;
    }

    @RequestMapping("/get")
    public Config getConfig() {
        logger.info("Fetching configuration");
        return config.getConfig();
    }
    @RequestMapping("/set")
    public boolean setConfig(@RequestBody Config newConfig) {
        logger.info("Setting configuration");
        return config.updateConfig(newConfig);
    }

}

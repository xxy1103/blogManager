package com.ulna.blog_manager.Config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigLoaderTest {

    @TempDir
    Path tempDir;
    
    private Path configPath;
    private ObjectMapper objectMapper = new ObjectMapper();
    
    private ConfigLoader configLoader;
    
    @BeforeEach
    void setUp() {
        configPath = tempDir.resolve("config.json");
    }
    
    /**
     * 测试正常情况下加载配置文件
     */
    @Test
    void loadConfig_whenConfigFileExists_shouldLoadConfig() throws IOException {        // 准备测试数据
        Config testConfig = new Config();
        testConfig.setBlogStoragePath(Paths.get("test/blog/path"));
        testConfig.setImageStoragePath(Paths.get("test/image/path"));
        testConfig.setXModelAPIKey("test-api-key");
        
        // 将测试配置写入临时文件
        objectMapper.writeValue(configPath.toFile(), testConfig);
        
        // 创建ConfigLoader实例，注入临时配置文件路径
        configLoader = new ConfigLoader(configPath.toString());
        
        // 调用被测试方法
        configLoader.loadConfig();
        
        // 获取私有字段值进行断言
        Config loadedConfig = (Config) ReflectionTestUtils.getField(configLoader, "config");        // 验证配置是否正确加载
        assertNotNull(loadedConfig);
        // 比较路径时验证末尾部分而不是整个路径，避免平台差异
        assertTrue(loadedConfig.getBlogStoragePath().toString().endsWith("test\\blog\\path") 
                || loadedConfig.getBlogStoragePath().toString().endsWith("test/blog/path"));
        assertTrue(loadedConfig.getImageStoragePath().toString().endsWith("test\\image\\path")
                || loadedConfig.getImageStoragePath().toString().endsWith("test/image/path"));
        assertEquals("test-api-key", loadedConfig.getXModelAPIKey());
    }
    
    /**
     * 测试配置目录不存在时的情况
     */
    @Test
    void loadConfig_whenConfigDirectoryDoesNotExist_shouldCreateDirectory() {
        // 准备一个不存在的路径
        Path nonExistentPath = tempDir.resolve("non-existent-dir").resolve("config.json");
        
        // 创建ConfigLoader实例，注入不存在的路径
        configLoader = new ConfigLoader(nonExistentPath.toString());
        
        // 调用被测试方法
        configLoader.loadConfig();
        
        // 验证目录是否被创建
        assertTrue(Files.exists(nonExistentPath.getParent()));
    }
    
    /**
     * 测试配置文件读取失败的情况
     */
    @Test
    void loadConfig_whenConfigFileReadFails_shouldHandleException() throws IOException {
        // 创建一个不可读的配置文件（创建目录而不是文件）
        Files.createDirectory(configPath);
        
        // 创建ConfigLoader实例
        configLoader = new ConfigLoader(configPath.toString());
        
        // 调用被测试方法 - 不应抛出异常，而是记录错误
        assertDoesNotThrow(() -> configLoader.loadConfig());
        
        // 获取私有字段值进行断言
        Config loadedConfig = (Config) ReflectionTestUtils.getField(configLoader, "config");
        
        // 验证配置为空
        assertNull(loadedConfig);
    }
    
    /**
     * 测试获取已加载的配置
     */
    @Test
    void getConfig_shouldReturnLoadedConfig() throws IOException, NoSuchFieldException {
        // 准备测试数据
        Config testConfig = new Config();
        testConfig.setXModelAPIKey("test-key");
        
        // 创建ConfigLoader实例
        configLoader = new ConfigLoader(configPath.toString());
        
        // 手动设置配置对象
        ReflectionTestUtils.setField(configLoader, "config", testConfig);
        
        // 获取配置
        Config returnedConfig = configLoader.getConfig();
        
        // 验证返回的配置是否正确
        assertNotNull(returnedConfig);
        assertEquals("test-key", returnedConfig.getXModelAPIKey());
    }
}

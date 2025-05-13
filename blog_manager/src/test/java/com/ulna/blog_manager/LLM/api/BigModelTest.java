package com.ulna.blog_manager.LLM.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ulna.blog_manager.service.LLM.api.BigModel;
import com.ulna.blog_manager.service.LLM.callback.StreamCallback;
import com.ulna.blog_manager.service.LLM.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BigModel的单元测试类，使用Spring的测试框架
 */
@SpringBootTest
@ActiveProfiles("test")
public class BigModelTest {

    // 测试用的常量值
    private static final String TEST_API_KEY = "6218f29ff45a4671b5130d678a71eff2.XdxZh1NJljuZbux1"; 
    private static final String TEST_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"; 
    private static final String TEST_MODEL = "glm-4-flash-250414"; 
    private static final String TEST_PROMPT = 
    """
    # 测试提示词
    
    这是一个用于测试BigModel类的提示词。
    该提示词用于验证BigModel的API调用功能是否正常工作。
    """;
    
    private static final String TEST_CONTENT = "这是测试内容。";


    @Test
    void testStreamOutput() {
        // 创建BigModel实例
        BigModel bigModel = new BigModel(TEST_API_KEY, TEST_API_URL, TEST_MODEL);
        
        bigModel.setIsStream(true); // 设置为流式输出
        
        // 创建一个模拟的回调
        StreamCallback mockCallback = mock(StreamCallback.class);
        
    // 调用API
        System.out.println("开始流式调用...");
        bigModel.callLLM(TEST_PROMPT, TEST_CONTENT, mockCallback);
        
        // 验证回调方法被调用（注意：这里只能验证调用发生，不能验证实际结果，因为结果依赖于实际API响应）
        // 对于流式输出，我们期望多次回调
        verify(mockCallback, timeout(10000).atLeast(1)).onResponse(anyString(), anyBoolean());
        
        // 等待最终的isDone回调
        verify(mockCallback, timeout(10000)).onResponse(anyString(), eq(true));
    }

    
}
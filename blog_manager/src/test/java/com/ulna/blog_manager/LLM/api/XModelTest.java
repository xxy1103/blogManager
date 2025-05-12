package com.ulna.blog_manager.LLM.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.ulna.blog_manager.service.LLM.LLMinterface.LLM;
import com.ulna.blog_manager.service.LLM.api.XModel;
import com.ulna.blog_manager.service.LLM.callback.StreamCallback;
import com.ulna.blog_manager.service.LLM.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * XModel的单元测试类，使用Spring的测试框架
 */
@SpringBootTest
@ActiveProfiles("test")
public class XModelTest {

    private static final String TEST_API_KEY = "gisUjZJyVpMBPUsPRzuN:uvNKueGSPAnDuhDrgwZI";
    private static final String TEST_API_URL = "https://spark-api-open.xf-yun.com/v2/chat/completions";
    private static final String TEST_MODEL = "x1";
    private static final String TEST_PROMPT = 
    """
    # 下载a1111原版web ui

通过git将github库克隆到本地：

```
git clone git@github.com:AUTOMATIC1111/stable-diffusion-webui.git
```

注意：连接上github可能需要魔法。

这里是作者的github库，喜欢的朋友可以去点一个star。[网页链接](https://github.com/AUTOMATIC1111/stable-diffusion-webui)

完成这一步，我们就已经将web ui装在了我们的电脑上，但是想要正常运行它，我们可能还缺少关键一步。
    """;


    /**
     * 测试XModel的构造函数是否正确初始化
     */
    @Test
    void testXModelConstructor() {
        // 创建XModel实例
        XModel xModel = new XModel(TEST_API_KEY, TEST_API_URL, TEST_MODEL);
        
        // 验证构造函数设置了正确的值
        assertEquals(TEST_API_KEY, xModel.getAPIKey());
        assertEquals(TEST_API_URL, xModel.getAPIUrl());
        assertEquals(TEST_MODEL, xModel.getModel());
    }

    /**
     * 测试XModel的setter和getter方法
     */
    @Test
    void testXModelSettersAndGetters() {
        // 创建XModel实例
        XModel xModel = new XModel("default-key", "default-url", "default-model");
        
        // 设置新的值
        xModel.setAPIKey(TEST_API_KEY);
        xModel.setAPIUrl(TEST_API_URL);
        xModel.setModel(TEST_MODEL);
        xModel.setIsStream(false);
        xModel.setTemperature("0.7");
        // 验证setter方法设置了正确的值
        assertEquals(TEST_API_KEY, xModel.getAPIKey());
        assertEquals(TEST_API_URL, xModel.getAPIUrl());
        assertEquals(TEST_MODEL, xModel.getModel());
        assertFalse(xModel.getIsStream());
        assertEquals("0.7", xModel.getTemperature());
    }
    @Test
    void testOutput() {
        // 创建XModel实例
        XModel xModel = new XModel(TEST_API_KEY, TEST_API_URL, TEST_MODEL);

        xModel.setIsStream(false); // 设置为非流式输出

        // 创建回调处理流式输出
        StreamCallback callback = new StreamCallback() {
            @Override
            public void onResponse(String chunk, boolean isDone) {
                if (!isDone) {
                    System.out.println("收到数据块: " + chunk);
                } else {
                    System.out.println("数据流结束");
                }
            }

            @Override
            public void onError(String error) {
                System.err.println("发生错误: " + error);
            }
        };
        
        // 调用流式API
        System.out.println("开始流式调用...");
        xModel.callLLM(Prompt.SuggestPrompt, TEST_PROMPT, callback);

        // 因为是异步调用，需要等待一段时间让流式输出完成
        try {
            Thread.sleep(10000); // 等待10秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
package com.ulna.blog_manager.controller;


import com.ulna.blog_manager.Config.Config;
import com.ulna.blog_manager.service.LLM.LLMinterface.LLM;
import com.ulna.blog_manager.service.LLM.factory.XModelFactory;
import com.ulna.blog_manager.service.LLM.prompt.Prompt;
import com.ulna.blog_manager.service.LLM.callback.StreamCallback;
import com.ulna.blog_manager.service.LLM.factory.BigModelFactory;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



@RestController
@RequestMapping("/llm")
public class LLMController {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMController.class);
    private LLM llm;
    private final Config config;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public LLMController(Config config) {
        this.config = config;
        // 初始化时默认选择 XModel
        logger.info("LLMController 初始化，默认设置 LLM 类型为 XModel");
        XModelFactory factory = new XModelFactory(config);
        this.llm = factory.createLLM();
    }

    /**
     * 根据 LLM 类型获取相应的 LLM 实例
     * @param llmType LLM 类型，如 "XModel"
     */
    @RequestMapping("/set")
    public void getLlm(@RequestParam String llmType) {

        logger.info("获取 LLM 实例，类型: " + llmType);
        switch (llmType) {
            case "XModel":
                // 创建XModelFactory实例并调用createLLM方法
                XModelFactory factory = new XModelFactory(config);
                llm = factory.createLLM();
                break;

            case "BigModel":
                // 创建BigModelFactory实例并调用createLLM方法
                BigModelFactory bigModelFactory = new BigModelFactory(config);
                llm = bigModelFactory.createLLM();
                break;
            default:{
                logger.error("不支持的 LLM 类型: " + llmType);
                llm = null;
            }
        }
    }
    /**
     * 获取当前的 LLM 实例
     * @return 当前的 LLM 实例
     */
    @RequestMapping("/get")
    public String getLlmName(){
        if (llm != null) {
            return llm.getClass().getSimpleName();
        } else {
            logger.error("LLM 实例未设置或不支持的 LLM 类型");
            return "null";
        }
    }

    @RequestMapping("/getsuggestion")
    public String getSuggestion(@RequestParam String param) {
        if (llm == null) {
            logger.error("LLM 实例未设置");
            return "LLM 实例未设置";
        }
        
        // 创建回调处理输出
        final StringBuilder responseText = new StringBuilder();
        StreamCallback callback = new StreamCallback() {
            @Override
            public void onResponse(String chunk, boolean isDone) {
                responseText.append(chunk);
            }

            @Override
            public void onError(String error) {
                logger.error("获取建议时发生错误: " + error);
                responseText.append("处理请求时发生错误: " + error);
            }
        };
        
        // 调用LLM API
        llm.setIsStream(false); // 非流式调用
        llm.callLLM(Prompt.SuggestPrompt, param, callback);
        
        // 等待响应完成
        try {
            // 等待一段时间让LLM处理完成
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            logger.error("等待LLM响应时被中断", e);
            Thread.currentThread().interrupt();
        }
        
        return responseText.toString();
    }
    
    @RequestMapping("/chat")
    public String chat(@RequestParam String param) {
        if (llm == null) {
            logger.error("LLM 实例未设置");
            return "LLM 实例未设置";
        }
        
        // 创建回调处理输出
        final StringBuilder responseText = new StringBuilder();
        StreamCallback callback = new StreamCallback() {
            @Override
            public void onResponse(String chunk, boolean isDone) {
                responseText.append(chunk);
            }

            @Override
            public void onError(String error) {
                logger.error("聊天时发生错误: " + error);
                responseText.append("处理请求时发生错误: " + error);
            }
        };
        
        // 调用LLM API
        llm.setIsStream(false); // 非流式调用
        llm.callLLM(Prompt.ChatPrompt, param, callback);
        
        // 等待响应完成
        try {
            // 等待一段时间让LLM处理完成
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            logger.error("等待LLM响应时被中断", e);
            Thread.currentThread().interrupt();
        }
        
        return responseText.toString();
    }
    
    /**
     * 流式聊天接口，使用 SSE 将 LLM 的响应流式传输到前端
     * @param param 用户输入的聊天内容
     * @return SseEmitter 用于流式传输数据
     */
    @RequestMapping(value = "/stream-chat", method = RequestMethod.GET)
    public SseEmitter streamChat(@RequestParam String param) {
        if (llm == null) {
            logger.error("LLM 实例未设置");
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("LLM 实例未设置"));
            return emitter;
        }
        
        // 创建 SseEmitter 实例，超时时间设为 1 小时
        SseEmitter emitter = new SseEmitter(3600000L);
        
        // 设置 SSE 完成、超时和错误的回调处理
        emitter.onCompletion(() -> logger.info("SSE 完成"));
        emitter.onTimeout(() -> logger.info("SSE 超时"));
        emitter.onError(ex -> logger.error("SSE 错误: {}", ex.getMessage()));
        
        // 使用 CompletableFuture 异步处理
        CompletableFuture.runAsync(() -> {
            try {
                // 发送一个初始事件
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("开始处理请求"));
                
                // 创建回调处理流式输出
                StreamCallback callback = new StreamCallback() {
                    @Override
                    public void onResponse(String chunk, boolean isDone) {
                        try {
                            // 发送数据块
                            emitter.send(SseEmitter.event()
                                .name(isDone ? "end" : "chunk")
                                .data(chunk));
                            
                            // 如果是最后一块数据，完成 SSE
                            if (isDone) {
                                emitter.complete();
                            }
                        } catch (IOException e) {
                            logger.error("发送 SSE 数据时出错", e);
                            emitter.completeWithError(e);
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        try {
                            // 发送错误信息
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data(error));
                            emitter.completeWithError(new RuntimeException(error));
                        } catch (IOException e) {
                            logger.error("发送 SSE 错误信息时出错", e);
                            emitter.completeWithError(e);
                        }
                    }
                };
                
                // 调用 LLM API 并设置为流式输出
                llm.setIsStream(true);
                llm.callLLM(Prompt.ChatPrompt, param, callback);
                
            } catch (Exception e) {
                logger.error("处理流式聊天请求时出错", e);
                emitter.completeWithError(e);
            }
        }, executor);
        
        return emitter;
    }
    
    /**
     * 流式生成建议接口
     * @param param 用户输入的内容
     * @return SseEmitter 用于流式传输数据
     */
    @RequestMapping(value = "/stream-suggestion", method = RequestMethod.GET)
    public SseEmitter streamSuggestion(@RequestParam String param) {
        if (llm == null) {
            logger.error("LLM 实例未设置");
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("LLM 实例未设置"));
            return emitter;
        }
        
        // 创建 SseEmitter 实例，超时时间设为 1 小时
        SseEmitter emitter = new SseEmitter(3600000L);
        
        // 设置 SSE 完成、超时和错误的回调处理
        emitter.onCompletion(() -> logger.info("SSE 完成"));
        emitter.onTimeout(() -> logger.info("SSE 超时"));
        emitter.onError(ex -> logger.error("SSE 错误: {}", ex.getMessage()));
        
        // 使用 CompletableFuture 异步处理
        CompletableFuture.runAsync(() -> {
            try {
                // 发送一个初始事件
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("开始处理请求"));
                
                // 创建回调处理流式输出
                StreamCallback callback = new StreamCallback() {
                    @Override
                    public void onResponse(String chunk, boolean isDone) {
                        try {
                            // 发送数据块
                            emitter.send(SseEmitter.event()
                                .name(isDone ? "end" : "chunk")
                                .data(chunk));
                            
                            // 如果是最后一块数据，完成 SSE
                            if (isDone) {
                                emitter.complete();
                            }
                        } catch (IOException e) {
                            logger.error("发送 SSE 数据时出错", e);
                            emitter.completeWithError(e);
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        try {
                            // 发送错误信息
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data(error));
                            emitter.completeWithError(new RuntimeException(error));
                        } catch (IOException e) {
                            logger.error("发送 SSE 错误信息时出错", e);
                            emitter.completeWithError(e);
                        }
                    }
                };
                
                // 调用 LLM API 并设置为流式输出
                llm.setIsStream(true);
                llm.callLLM(Prompt.SuggestPrompt, param, callback);
                
            } catch (Exception e) {
                logger.error("处理流式建议请求时出错", e);
                emitter.completeWithError(e);
            }
        }, executor);
        
        return emitter;
    }
}
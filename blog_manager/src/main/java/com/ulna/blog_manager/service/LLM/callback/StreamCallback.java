package com.ulna.blog_manager.service.LLM.callback;

/**
 * 流式输出回调接口
 * 用于处理LLM API的流式响应
 */
public interface StreamCallback {
    /**
     * 处理流式输出的回调方法
     * 
     * @param chunk 当前接收到的数据块
     * @param isDone 是否是最后一个数据块
     */
    void onResponse(String chunk, boolean isDone);
    
    /**
     * 错误处理回调方法
     * 
     * @param error 错误信息
     */
    void onError(String error);
}

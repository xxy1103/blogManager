package com.ulna.blog_manager.service.LLM.LLMinterface;

import com.ulna.blog_manager.service.LLM.callback.StreamCallback;
import com.ulna.blog_manager.service.LLM.POST.POSTMessage;

import java.util.ArrayList;



public abstract class LLM {
    private String APIKey;
    private String APIUrl;
    private String model;
    protected ArrayList<POSTMessage> messagesArray;


    private Boolean IsStream = true;
    private String temperature = "0.5";


    public LLM(String APIKey, String APIUrl, String model) {
        this.APIKey = APIKey;
        this.APIUrl = APIUrl;
        this.model = model;
        this.messagesArray = new ArrayList<>();
    }

    public String getAPIKey() { return APIKey; }
    public String getAPIUrl() { return APIUrl; }
    public String getModel() { return model; }
    public Boolean getIsStream() { return IsStream; }
    public String getTemperature() { return temperature; }

    public void setAPIKey(String APIKey) { this.APIKey = APIKey; }
    public void setAPIUrl(String APIUrl) { this.APIUrl = APIUrl; }
    public void setModel(String model) { this.model = model; }
    public void setIsStream(Boolean isStream) { IsStream = isStream; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    /**
     * 调用 LLM 的 API 接口
     * @param prompt 提示词
     * @param callback 流式输出回调接口
     * @return LLM 的响应
     */
    public abstract void callLLM(String prompt, String content, StreamCallback callback);
    
}
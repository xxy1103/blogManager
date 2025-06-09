package com.ulna.blog_manager.service.LLM.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ulna.blog_manager.service.LLM.LLMinterface.LLM;
import com.ulna.blog_manager.service.LLM.POST.POSTMessage;
import com.ulna.blog_manager.service.LLM.POST.POST;
import com.ulna.blog_manager.service.LLM.callback.StreamCallback;


class XModelPOST extends POST {
    private String user;
    private Double temperature;
    private Integer top_k;
    private Integer max_tokens;
    private Double presence_penalty;
    private Double frequency_penalty;

    public XModelPOST(String model, Boolean stream, POSTMessage[] messages, String user) {
        super(model, stream, messages);
        this.user = user;
        // 设置讯飞星火的默认参数
        this.temperature = 0.5;
        this.top_k = 4;
        this.max_tokens = 1024;
        this.presence_penalty = 1.0;
        this.frequency_penalty = 1.0;
    }

    // 重写getters以确保正确的JSON序列化
    @Override
    public String getModel() {
        return super.getModel();
    }

    @Override
    public Boolean getStream() {
        return super.getStream();
    }

    @Override
    public POSTMessage[] getMessages() {
        return super.getMessages();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getTop_k() {
        return top_k;
    }

    public void setTop_k(Integer top_k) {
        this.top_k = top_k;
    }

    public Integer getMax_tokens() {
        return max_tokens;
    }

    public void setMax_tokens(Integer max_tokens) {
        this.max_tokens = max_tokens;
    }

    public Double getPresence_penalty() {
        return presence_penalty;
    }

    public void setPresence_penalty(Double presence_penalty) {
        this.presence_penalty = presence_penalty;
    }

    public Double getFrequency_penalty() {
        return frequency_penalty;
    }

    public void setFrequency_penalty(Double frequency_penalty) {
        this.frequency_penalty = frequency_penalty;
    }
}



public class XModel extends LLM {
    private static final Logger logger = LoggerFactory.getLogger(XModel.class);

    public XModel(String APIKey, String APIUrl, String model) {
        super(APIKey, APIUrl, model);
    }
    @Override
    public void callLLM(String prompt,String content, StreamCallback callback) {
        // 生成用户唯一ID（可以根据实际需求修改）
        String userId = "user_" + System.currentTimeMillis();
        try {
            // 初始化消息数组
            if (this.messagesArray == null) {
                this.messagesArray = new ArrayList<>();
                // 添加系统消息（讯飞星火建议的格式）
                POSTMessage systemMessage = new POSTMessage("system", "你是知识渊博的助理");
                this.messagesArray.add(systemMessage);
            }

            POSTMessage message = new POSTMessage("user", prompt + content);
            this.messagesArray.add(message);
            POSTMessage[] messages = messagesArray.toArray(new POSTMessage[0]);
            
            // 使用XModelPOST来支持讯飞星火的参数格式
            XModelPOST post = new XModelPOST(this.getModel(), this.getIsStream(), messages, userId);
            // 设置温度参数
            if (this.getTemperature() != null) {
                try {
                    post.setTemperature(Double.parseDouble(this.getTemperature()));
                } catch (NumberFormatException e) {
                    logger.warn("温度参数格式错误，使用默认值: " + e.getMessage());
                }
            }

            Gson gson = new Gson();

            String json = gson.toJson(post);
            logger.info("发送到讯飞星火的请求数据: " + json);

            String header = "Bearer " + this.getAPIKey();
            URL obj = new URL(this.getAPIUrl());

            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", header);
            con.setDoOutput(true);

            OutputStream os = con.getOutputStream();
            os.write(json.getBytes());
            os.flush();
            os.close();

            int responseCode = con.getResponseCode();
            logger.debug("讯飞星火响应码: " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer LLMtext = new StringBuffer();
            if (this.getIsStream()) {
                Boolean isDone = false;
                // 流式传输模式：每读取一行就回调一次
                while ((inputLine = in.readLine()) != null) {
                    // 判断是否是最后的数据 (可根据实际API响应格式调整判断条件)
                    if(inputLine.startsWith("data: ")) {
                        String data = inputLine.substring(6); // 去掉前缀 "data: "
                        if (data.equals("[DONE]")) {
                            isDone = true;
                            callback.onResponse(inputLine, isDone);
                            System.out.println("流式传输结束");
                            break;
                        }
                        String LLMcontent = "";
                        try{
                            JsonObject jsonResponse = gson.fromJson(data, JsonObject.class);
                            // 适配讯飞星火的响应格式
                            if(jsonResponse.has("choices") &&
                               jsonResponse.getAsJsonArray("choices").size() > 0 &&
                               jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject().has("delta")) {
                                JsonObject delta = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("delta");
                                if (delta.has("content")) {
                                    LLMcontent = delta.get("content").getAsString();
                                }
                            }
                            
                        } catch (Exception e) {
                            logger.error("解析数据块时发生错误: " + e.getMessage());
                            logger.error("原始数据: " + data);
                        }
                        callback.onResponse(inputLine, isDone);             

                        System.out.println("流式传输数据: " + inputLine);
                        LLMtext.append(LLMcontent);
                        
                        // 添加调试日志
                        if (!LLMcontent.isEmpty()) {
                            logger.debug("提取的内容: " + LLMcontent);
                        }
                    }
                }
            } else {
                // 非流式传输模式：收集所有数据，一次性回调
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                    logger.debug("收到数据块: " + inputLine);
                }
                callback.onResponse(response.toString(), true);

                JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
                String LLMcontent = "";
                if (jsonResponse.has("choices") &&
                        jsonResponse.getAsJsonArray("choices").size() > 0 &&
                        jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject().has("message") &&
                        jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").has("content")) {
                    LLMcontent = jsonResponse.getAsJsonArray("choices").get(0)
                            .getAsJsonObject().getAsJsonObject("message")
                            .get("content").getAsString();
                    LLMtext.append(LLMcontent);
                }
                // 完整响应一次性回调
            }
            in.close();

            POSTMessage message1 = new POSTMessage("assistant", LLMtext.toString());
            this.messagesArray.add(message1);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("调用LLM时发生错误: " + e.getMessage());
        }
    }
}

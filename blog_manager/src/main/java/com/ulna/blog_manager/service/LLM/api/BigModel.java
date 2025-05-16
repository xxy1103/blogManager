package com.ulna.blog_manager.service.LLM.api;



import com.ulna.blog_manager.service.LLM.POST.POST;
import com.ulna.blog_manager.service.LLM.POST.POSTMessage;
import com.ulna.blog_manager.controller.BlogController;
import com.ulna.blog_manager.service.LLM.LLMinterface.LLM;
import com.ulna.blog_manager.service.LLM.callback.StreamCallback;


import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



class BigModelPOST extends POST {
    private String temperature;

    public BigModelPOST(String model, Boolean stream, String temperature, POSTMessage[] messages) {
        super(model, stream, messages);
        this.temperature = temperature;
    }

    public String getTemperature() {
        return temperature;
    }
    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }
}




public class BigModel extends LLM {
    private static final Logger logger = LoggerFactory.getLogger(BigModel.class);


    public BigModel(String APIKey, String APIUrl, String model) {
        super(APIKey, APIUrl, model);
    }

    @Override
    public void callLLM(String prompt,String content, StreamCallback callback) {
        try{
            POSTMessage message = new POSTMessage("user", prompt + content);
            if (this.messagesArray == null) {
                this.messagesArray = new ArrayList<>();
            }
            this.messagesArray.add(message);
            POSTMessage[] messages = messagesArray.toArray(new POSTMessage[0]);
            POST post = new BigModelPOST(this.getModel(), this.getIsStream(), this.getTemperature(), messages);

            Gson gson = new Gson();

            String json = gson.toJson(post);
            logger.debug("请求数据: " + json);

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
            logger.debug("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer LLMtext = new StringBuffer();
            if (this.getIsStream()) {
                Boolean isDone = false;
                // 流式传输模式：每读取一行就回调一次
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("data: ")) {
                        String data = inputLine.substring(6);
                        if (data.equals("[DONE]")) {
                            isDone = true;
                            callback.onResponse(inputLine, isDone);
                            break;
                        }
                        String LLMcontent = "";
                        try {
                            JsonObject jsonResponse = gson.fromJson(data, JsonObject.class);
                            
                            if (jsonResponse.has("choices") && 
                                jsonResponse.getAsJsonArray("choices").size() > 0 &&
                                jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject().has("delta") &&
                                jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("delta").has("content")) {
                                LLMcontent = jsonResponse.getAsJsonArray("choices").get(0)
                                        .getAsJsonObject().getAsJsonObject("delta")
                                        .get("content").getAsString();
                            }
                            
                        } catch (Exception e) {
                            logger.error("解析数据块时发生错误: " + e.getMessage());
                        }
                        callback.onResponse(inputLine, isDone);
                        LLMtext.append(LLMcontent);
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
                // 解析完整响应
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

package com.ulna.blog_manager.service.LLM.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.google.gson.Gson;
import com.ulna.blog_manager.service.LLM.LLMinterface.LLM;
import com.ulna.blog_manager.service.LLM.POST.POSTMessage;
import com.ulna.blog_manager.service.LLM.POST.POST;
import com.ulna.blog_manager.service.LLM.callback.StreamCallback;


class XModelPOSTMessage extends POSTMessage {
    private String temperature;

    public XModelPOSTMessage(String role, String content, String temperature) {
        super(role, content);
        this.temperature = temperature;
    }
}



public class XModel extends LLM {
    private static final Logger logger = LoggerFactory.getLogger(XModel.class);

    public XModel(String APIKey, String APIUrl, String model) {
        super(APIKey, APIUrl, model);
    }
    // 还没有实现将机器返回消息加入到消息数组中
    @Override
    public void callLLM(String prompt,String content, StreamCallback callback) {
        // 这里调用 LLM 的 API 接口
        String userId = "用户ID";
        try {

            POSTMessage message = new XModelPOSTMessage("user", prompt + content, this.getTemperature());
            if (this.messagesArray == null) {
                this.messagesArray = new ArrayList<>();
            }
            this.messagesArray.add(message);
            POSTMessage[] messages = messagesArray.toArray(new POSTMessage[0]);
            // POST 请求体
            POST post = new POST(this.getModel(), this.getIsStream(), messages);

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

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            
            if (this.getIsStream()) {
                // 流式传输模式：每读取一行就回调一次
                while ((inputLine = in.readLine()) != null) {
                    boolean isDone = false;
                    // 判断是否是最后的数据 (可根据实际API响应格式调整判断条件)
                    if (inputLine.contains("data: [DONE]")) {
                        isDone = true;
                    }
                    logger.debug("收到数据块: " + inputLine);
                    callback.onResponse(inputLine, isDone);

                    if (isDone) {
                        break;
                    }
                }
            } else {
                // 非流式传输模式：收集所有数据，一次性回调
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                    
                }
                // 完整响应一次性回调
                callback.onResponse(response.toString(), true);
            }
            
            in.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("调用LLM时发生错误: " + e.getMessage());
        }
    }
}

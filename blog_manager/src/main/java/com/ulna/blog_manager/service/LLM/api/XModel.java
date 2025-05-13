package com.ulna.blog_manager.service.LLM.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.ulna.blog_manager.service.LLM.LLMinterface.LLM;
import com.ulna.blog_manager.service.LLM.callback.StreamCallback;

public class XModel extends LLM {
    
    public XModel(String APIKey, String APIUrl, String model) {
        super(APIKey, APIUrl, model);
    }
    // 还没有实现将机器返回消息加入到消息数组中
    @Override
    public void callLLM(String prompt,String content, StreamCallback callback) {
        // 这里调用 LLM 的 API 接口
        String userId = "用户ID";
        try {
            // 创建最外层的JSON对象
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user", userId);
            jsonObject.put("model", this.getModel());


            // 创建单个消息的JSON对象
            JSONObject messageObject = new JSONObject();
            String temp = prompt + content;
            messageObject.put("role", "user");
            messageObject.put("content", temp);
            messageObject.put("temperature", this.getTemperature());
            // 将单个消息对象添加到messages数组中
            this.messagesArray.put(messageObject);
            // 将messages数组添加到最外层的JSON对象中
            jsonObject.put("messages", messagesArray);
            // 设置stream属性
            jsonObject.put("stream", this.getIsStream());
            jsonObject.put("max_tokens", 4096);
            // System.err.println(jsonObject);
            String header = "Bearer " + this.getAPIKey(); 

            URL obj = new URL(this.getAPIUrl());

            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", header);
            con.setDoOutput(true);

            OutputStream os = con.getOutputStream();
            os.write(jsonObject.toString().getBytes());
            os.flush();
            os.close();

            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            
            if (this.getIsStream()) {
                // 流式传输模式：每读取一行就回调一次
                while ((inputLine = in.readLine()) != null) {
                    boolean isDone = false;
                    // 判断是否是最后的数据 (可根据实际API响应格式调整判断条件)
                    if (inputLine.contains("\"finish_reason\":") && inputLine.contains("\"stop\"")) {
                        isDone = true;
                    }
                    callback.onResponse(inputLine, isDone);
                    //System.out.println("收到数据块: " + inputLine);
                    if (isDone) {
                        break;
                    }
                }
            } else {
                // 非流式传输模式：收集所有数据，一次性回调
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                    //System.out.println("收到数据块: " + inputLine);
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

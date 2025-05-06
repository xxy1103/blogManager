package com.ulna.blog_manager.model;

public class Message {
    private int status; // 状态码0表示成功，1表示失败
    private Object data; // 数据内容，可以是任意类型
    private String error; // 错误信息，默认为空字符串

    public Message(int status, Object data, String error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }
}

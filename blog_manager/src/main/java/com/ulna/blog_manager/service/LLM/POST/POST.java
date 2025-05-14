package com.ulna.blog_manager.service.LLM.POST;

import com.ulna.blog_manager.service.LLM.POST.POSTMessage;



public class POST {
    private String model;
    private Boolean stream;
    private POSTMessage[] messages;

    public POST(String model, Boolean stream, POSTMessage[] messages) {
        this.model = model;
        this.stream = stream;

        this.messages = messages;
    }
    public String getModel() {
        return model;
    }
    public void setModel(String model) {
        this.model = model;
    }
    public Boolean getStream() {
        return stream;
    }
    public void setStream(Boolean stream) {
        this.stream = stream;
    }
    public POSTMessage[] getMessages() {
        return messages;
    }
    public void setMessages(POSTMessage[] messages) {
        this.messages = messages;
    }

}

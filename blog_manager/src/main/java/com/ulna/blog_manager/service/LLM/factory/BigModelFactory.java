package com.ulna.blog_manager.service.LLM.factory;

import com.ulna.blog_manager.service.LLM.LLMinterface.LLM;
import com.ulna.blog_manager.service.LLM.api.BigModel;

import org.springframework.beans.factory.annotation.Autowired;

import com.ulna.blog_manager.Config.Config;


public class BigModelFactory implements FactoryInterface {
    private String APIKey;
    private final String APIUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private final String model = "glm-4-flash-250414";

    /**
     * 构造函数
     * @param config 配置类
     */
    public BigModelFactory(Config config) {
        this.APIKey = config.getBigmodelAPIKey();
   }

    @Override
    public LLM createLLM() {
        return new BigModel(APIKey, APIUrl, model);
    }

}

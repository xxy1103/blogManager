package com.ulna.blog_manager.service.LLM.factory;

import com.ulna.blog_manager.service.LLM.LLMinterface.LLM;
import com.ulna.blog_manager.service.LLM.api.XModel;

import org.springframework.beans.factory.annotation.Autowired;

import com.ulna.blog_manager.Config.Config;

public class XModelFactory implements FactoryInterface {

    private String APIKey;
    private final String APIUrl = "https://spark-api-open.xf-yun.com/v2/chat/completions";
    private final String model = "x1";

    /**
     * 构造函数
     * @param config 配置类
     */
    public XModelFactory(Config config) {
        this.APIKey = config.getXmodelAPIKey();
    }


    @Override
    public LLM createLLM() {
        return new XModel(APIKey, APIUrl, model);
    }

}

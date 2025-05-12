package com.ulna.blog_manager.service.LLM.factory;

import com.ulna.blog_manager.service.LLM.LLMinterface.LLM;
import com.ulna.blog_manager.service.LLM.api.XModel;

public class XModelFactory implements FactoryInterface {
    @Override
    public LLM createLLM(String APIKey, String APIUrl, String model) {
        return new XModel(APIKey, APIUrl, model);
    }

}

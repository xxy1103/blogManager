package com.ulna.blog_manager.service.LLM.factory;

import com.ulna.blog_manager.service.LLM.LLMinterface.LLM;

public interface FactoryInterface {
    LLM createLLM(String APIKey, String APIUrl, String model);
}

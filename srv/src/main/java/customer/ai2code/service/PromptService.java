package customer.ai2code.service;

import java.util.List;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.ContextNodes;

public interface PromptService {
    public String parse(PromptTexts prompt,ContextNodes contextNode,String contextPath);
    
    public List<PromptTexts> getPrompts(String botTypeId);
}

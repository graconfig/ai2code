package customer.ai2code.service;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.ContextNodes;

public interface PromptService {
    public String parse(PromptTexts prompt,ContextNodes contextNode,String contextPath);
}

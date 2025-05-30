package customer.ai2code.service;

import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.configservice.FunctionCalls;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
// import customer.ai2code.model.Bot;
import customer.ai2code.model.config.AIServiceConfig;

public interface AIService {
    public String chatWithAI(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            String content,
            AIServiceConfig serviceConfig,
            String modelName);

    public SseEmitter chatWithAIStreaming(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            String content,
            AIServiceConfig serviceConfig,
            String modelName);

    public String functionCalling(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            FunctionCalls functionCall,
            AIServiceConfig serviceConfig,
            String modelName);

}

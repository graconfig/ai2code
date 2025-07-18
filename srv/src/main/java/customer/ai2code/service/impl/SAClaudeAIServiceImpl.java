package customer.ai2code.service.impl;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import customer.ai2code.model.ai.config.model.AIModel;
import customer.ai2code.service.AIService;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.processor.StreamingCompletedProcessor;

@Service
public class SAClaudeAIServiceImpl implements AIService{

    @Override
    public String chatWithAI(List<BotMessages> messages, List<PromptTexts> prompts, String content, AIModel model) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'chatWithAI'");
    }

    @Override
    public SseEmitter chatWithAIStreaming(List<BotMessages> messages, List<PromptTexts> prompts, String content,
            AIModel model, ExecutorService executor, StreamingCompletedProcessor streamingCompletionProcessor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'chatWithAIStreaming'");
    }

    @Override
    public <T extends BotExecution> Object functionCalling(List<BotMessages> messages, List<PromptTexts> prompts,
            T botExecutionInstance, AIModel model) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'functionCalling'");
    }
    
}

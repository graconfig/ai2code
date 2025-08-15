package customer.ai2code.service.handler.factory;

import customer.ai2code.service.constant.AIConstants;
import customer.ai2code.service.handler.AIResponseHandler;
import customer.ai2code.service.handler.SAPClaudeAIResponseHandler;
import customer.ai2code.service.handler.SAPGeminiAIResponseHandler;
import customer.ai2code.service.handler.SAPOpenAIResponseHandler;
import org.springframework.stereotype.Component;

@Component
public class AIResponseHandlerFactory {
    private final SAPOpenAIResponseHandler sapOpenAIHandler;
    private final SAPClaudeAIResponseHandler sapClaudeHandler;
    private final SAPGeminiAIResponseHandler sapGeminiHandler;

    public AIResponseHandlerFactory(
            SAPOpenAIResponseHandler sapOpenAIHandler,
            SAPClaudeAIResponseHandler sapClaudeHandler,
            SAPGeminiAIResponseHandler sapGeminiHandler) {
        this.sapOpenAIHandler = sapOpenAIHandler;
        this.sapGeminiHandler = sapGeminiHandler;
        this.sapClaudeHandler = sapClaudeHandler;
    }

    public AIResponseHandler getHandler(AIConstants.AIServiceType type) {
        switch (type) {
            case SAPOPENAI:
                return sapOpenAIHandler;
            case SAPCLAUDE:
                return sapClaudeHandler;
            case SAPGEMINI:
                return sapGeminiHandler;
            default:
                throw new UnsupportedOperationException("Unsupported AI service type: " + type);
        }
    }
}
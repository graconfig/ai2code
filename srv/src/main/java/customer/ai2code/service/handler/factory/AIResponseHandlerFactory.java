package customer.ai2code.service.handler.factory;

import customer.ai2code.service.constant.AIConstants;
import customer.ai2code.service.handler.AIResponseHandler;
import customer.ai2code.service.handler.SAPClaudeAIResponseHandler;
import customer.ai2code.service.handler.SAPOpenAIResponseHandler;
import org.springframework.stereotype.Component;

@Component
public class AIResponseHandlerFactory {
    private final SAPOpenAIResponseHandler sapOpenAIHandler;
    private final SAPClaudeAIResponseHandler sapClaudeHandler;

    public AIResponseHandlerFactory(
            SAPOpenAIResponseHandler sapOpenAIHandler,
            SAPClaudeAIResponseHandler sapClaudeHandler) {
        this.sapOpenAIHandler = sapOpenAIHandler;
        this.sapClaudeHandler = sapClaudeHandler;
    }

    public AIResponseHandler getHandler(AIConstants.AIServiceType type) {
        switch (type) {
            case SAPOPENAI:
                return sapOpenAIHandler;
            case SAPCLAUDE:
                return sapClaudeHandler;
            default:
                throw new UnsupportedOperationException("Unsupported AI service type: " + type);
        }
    }
}
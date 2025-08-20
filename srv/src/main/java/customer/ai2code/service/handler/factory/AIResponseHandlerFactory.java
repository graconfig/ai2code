package customer.ai2code.service.handler.factory;

import customer.ai2code.service.constant.AIConstants;
import customer.ai2code.service.handler.AIResponseHandler;
import customer.ai2code.service.handler.SAPClaudeAIResponseHandler;
import customer.ai2code.service.handler.SAPDeepseekAIResponseHandler;
import customer.ai2code.service.handler.SAPOpenAIResponseHandler;
import org.springframework.stereotype.Component;

@Component
public class AIResponseHandlerFactory {
    private final SAPOpenAIResponseHandler sapOpenAIHandler;
    private final SAPClaudeAIResponseHandler sapClaudeHandler;
    private final SAPDeepseekAIResponseHandler sapDeepseekHandler;

    public AIResponseHandlerFactory(
            SAPOpenAIResponseHandler sapOpenAIHandler,
            SAPClaudeAIResponseHandler sapClaudeHandler,
            SAPDeepseekAIResponseHandler sapDeepseekHandler) {
        this.sapOpenAIHandler = sapOpenAIHandler;
        this.sapClaudeHandler = sapClaudeHandler;
        this.sapDeepseekHandler = sapDeepseekHandler;
    }

    public AIResponseHandler getHandler(AIConstants.AIServiceType type) {
        switch (type) {
            case SAPOPENAI:
                return sapOpenAIHandler;
            case SAPCLAUDE:
                return sapClaudeHandler;
            case DEEPSEEK:
                return sapDeepseekHandler;
            default:
                throw new UnsupportedOperationException("Unsupported AI service type: " + type);
        }
    }
}
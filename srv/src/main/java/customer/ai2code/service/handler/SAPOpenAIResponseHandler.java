package customer.ai2code.service.handler;

import org.springframework.stereotype.Component;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionOutput;
import customer.ai2code.service.model.adapter.SAPOpenAIResponseAdapter;
import customer.ai2code.service.model.AIResponse;

@Component
public class SAPOpenAIResponseHandler implements AIResponseHandler {
    
    @Override
    public AIResponse processResponse(Object rawResponse) {
        if (!(rawResponse instanceof OpenAiChatCompletionOutput)) {
            throw new IllegalArgumentException("Expected OpenAiChatCompletionOutput but got: " 
                + rawResponse.getClass().getName());
        }
        
        return new SAPOpenAIResponseAdapter((OpenAiChatCompletionOutput) rawResponse);
    }
}
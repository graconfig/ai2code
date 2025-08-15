package customer.ai2code.service.handler;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionOutput;

import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.response.AIResponse;
import customer.ai2code.model.ai.response.adapter.SAPClaudeAIResponseAdapter;
import customer.ai2code.model.ai.response.adapter.SAPGeminiAIResponseAdapter;
import customer.ai2code.model.ai.response.adapter.SAPOpenAIResponseAdapter;
import customer.ai2code.model.aicore.claude.ConverseResponse;
import customer.ai2code.model.aicore.claude.InvokeResponse;

@Component
public class SAPGeminiAIResponseHandler implements AIResponseHandler {
    
    private final ObjectMapper objectMapper;

    public SAPGeminiAIResponseHandler() {
        this.objectMapper = new ObjectMapper();
    }
    /**
     * Processes the raw response from the AI service and converts it into a standard AIResponse object.
     *
     * @param rawResponse The raw response object from the AI service, which can be either an InvokeResponse or ConverseResponse.
     * @return An AIResponse object containing the processed response data.
     * @throws IllegalArgumentException if the raw response is neither an InvokeResponse nor a ConverseResponse.
     */
    @Override
    public AIResponse processResponse(Object rawResponse) {
        if (rawResponse instanceof InvokeResponse) {
            return new SAPGeminiAIResponseAdapter((InvokeResponse) rawResponse);
        } else if (rawResponse instanceof ConverseResponse) {
            try {
                return new SAPGeminiAIResponseAdapter((ConverseResponse) rawResponse, objectMapper);
            } catch (JsonProcessingException e) {
                // throw e;
                throw new BusinessException("null",e);
            }
        }

        throw new IllegalArgumentException("Expected InvokeResponse or ConverseResponse but got: " 
            + rawResponse.getClass().getName());
    }
}
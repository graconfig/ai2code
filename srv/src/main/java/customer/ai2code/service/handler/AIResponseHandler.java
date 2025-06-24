package customer.ai2code.service.handler;

import customer.ai2code.model.ai.response.AIResponse;

public interface AIResponseHandler {
    AIResponse processResponse(Object rawResponse);
}
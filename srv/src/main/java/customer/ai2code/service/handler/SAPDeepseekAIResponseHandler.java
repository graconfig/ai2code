package customer.ai2code.service.handler;

import org.springframework.stereotype.Component;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionOutput;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.response.AIResponse;
import customer.ai2code.model.ai.response.adapter.SAPDeepseekAIResponseAdapter;

/**
 * DeepSeek响应处理器
 */
@Component
public class SAPDeepseekAIResponseHandler implements AIResponseHandler {

    @Override
    public AIResponse processResponse(Object rawResponse) {
        // 严格校验响应类型（与SAPOpenAIResponseHandler逻辑一致）
        if (!(rawResponse instanceof OpenAiChatCompletionOutput)) {
            throw new BusinessException("Expected OpenAiChatCompletionOutput for DeepSeek, but got: "
                    + rawResponse.getClass().getName());
        }
        // 适配响应格式（与OpenAI的响应适配逻辑一致）
        return new SAPDeepseekAIResponseAdapter((OpenAiChatCompletionOutput) rawResponse);
    }
}
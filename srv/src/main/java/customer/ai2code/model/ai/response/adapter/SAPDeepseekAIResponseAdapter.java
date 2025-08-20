package customer.ai2code.model.ai.response.adapter;

import customer.ai2code.model.ai.response.AIResponse;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionOutput;

/**
 * DeepSeek非流式响应适配器（仅处理完整响应）
 */
public class SAPDeepseekAIResponseAdapter implements AIResponse {
    private final String content;

    // 仅处理非流式完整响应
    public SAPDeepseekAIResponseAdapter(OpenAiChatCompletionOutput output) {
        this.content = output.getContent(); // 直接获取完整内容
    }

    @Override
    public String getContent() {
        return content;
    }
}
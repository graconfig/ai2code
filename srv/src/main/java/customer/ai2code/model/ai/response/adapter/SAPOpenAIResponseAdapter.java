package customer.ai2code.model.ai.response.adapter;


import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionOutput;

import customer.ai2code.model.ai.response.AIResponse;

public class SAPOpenAIResponseAdapter implements AIResponse {
    private final OpenAiChatCompletionOutput output;

    public SAPOpenAIResponseAdapter(OpenAiChatCompletionOutput output) {
        this.output = output;
    }

    public String getFinishReason() {
        return output.getChoices().get(0).getFinishReason();
    }

    @Override
    public String getContent() {
        // return output.getContent();
        if ("tool_calls".equals(this.getFinishReason())){
             return this.output.getChoices().get(0).getMessage().getToolCalls().get(0).getFunction().getArguments();
        } else {
            return output.getContent();
        }
    }
}
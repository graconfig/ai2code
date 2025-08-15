package customer.ai2code.model.factory;

import org.springframework.stereotype.Component;

import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatMessage;

import customer.ai2code.model.aicore.claude.ContentBlock;
import customer.ai2code.model.aicore.claude.ConverseRequestAssistantMessage;
import customer.ai2code.model.aicore.claude.ConverseRequestUserMessage;
import customer.ai2code.model.aicore.claude.SystemContentBlock;
import customer.ai2code.model.aicore.openai.OpenAiChatAssistantMessage2;

@Component
public class SAPGeminiAIChatMessageFactory {
    public SystemContentBlock createSystemBlock(String content) {
        return new SystemContentBlock().text(content);
    }

    public ConverseRequestUserMessage createUserMessage(String content) {
        return new ConverseRequestUserMessage()
            .role(ConverseRequestUserMessage.RoleEnum.USER)
            .addContentItem(new ContentBlock().text(content));
    }

    public ConverseRequestAssistantMessage createAssistantMessage(String content) {
        return new ConverseRequestAssistantMessage()
            .role(ConverseRequestAssistantMessage.RoleEnum.ASSISTANT)
            .addContentItem(new ContentBlock().text(content));
    }
}

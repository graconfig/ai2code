package customer.ai2code.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collections;
import java.util.HashMap;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonBoolean;
import com.openai.core.JsonField;
import com.openai.core.http.StreamResponse;
import com.openai.models.FunctionDefinition;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionFunction;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionTool;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionTool.ToolType;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.core.JsonValue;
import com.openai.models.FunctionParameters;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.config.model.AIModel;
import customer.ai2code.model.ai.config.service.SAPAIDeepSeekConfig;
import customer.ai2code.model.execution.functioncall.FunctionInfo;
import customer.ai2code.model.factory.SAPOpenAIChatMessageFactory;
import customer.ai2code.service.AIService;
import customer.ai2code.service.constant.AIConstants;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.execution.functioncall.FunctionCallProcessor;
import customer.ai2code.service.execution.functioncall.adapter.OpenAIFunctionCallAdapter;
import customer.ai2code.service.handler.SAPDeepseekAIResponseHandler;

@Service
public class SAPDeepseekAIServiceImpl implements AIService {

    private final SAPOpenAIChatMessageFactory messageFactory;
    private final SAPDeepseekAIResponseHandler responseHandler;
    private final FunctionCallProcessor functionCallProcessor;
    private final OpenAIFunctionCallAdapter openAIFunctionCallAdapter;

    public SAPDeepseekAIServiceImpl(
            SAPOpenAIChatMessageFactory messageFactory,
            SAPDeepseekAIResponseHandler responseHandler,
            FunctionCallProcessor functionCallProcessor,
            OpenAIFunctionCallAdapter openAIFunctionCallAdapter) {
        this.messageFactory = messageFactory;
        this.responseHandler = responseHandler;
        this.functionCallProcessor = functionCallProcessor;
        this.openAIFunctionCallAdapter = openAIFunctionCallAdapter;
    }

    private OpenAIClient getAiClientForDeepSeek(@Nonnull SAPAIDeepSeekConfig config) {
        return OpenAIOkHttpClient.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getApiUrl())
                .build();
    }

    @Override
    public String chatWithAI(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            String content,
            AIModel model) {
        SAPAIDeepSeekConfig config = (SAPAIDeepSeekConfig) model.parseModelConfigs();
        OpenAIClient aiClient = getAiClientForDeepSeek(config);
        ChatCompletionCreateParams params = buildChatParams(messages, prompts, content);

        var response = aiClient.chat().completions().create(params);
        return response.choices().get(0).message().content().orElse("");
    }

    @Override
    public <T extends BotExecution> Object functionCalling(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            T botExecutionInstance,
            AIModel model) {
        SAPAIDeepSeekConfig config = (SAPAIDeepSeekConfig) model.parseModelConfigs();
        OpenAIClient aiClient = getAiClientForDeepSeek(config);
        ChatCompletionCreateParams params = buildChatParams(messages, prompts, "");

        List<FunctionInfo> functionInfos = functionCallProcessor
                .extractFunctionInfosFromInstance(botExecutionInstance);

        if (functionInfos.isEmpty()) {
            throw new BusinessException("No executable methods found");
        }

        List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
                .convertToOpenAIFormat(functionInfos);

        List<ChatCompletionTool> tools = openAIFunctions.stream()
                .map(functionDef -> {
                    // 创建函数定义
                    FunctionDefinition function = FunctionDefinition.builder()
                            .name((String) functionDef.get("name"))
                            .description((String) functionDef.get("description"))
                            .parameters((FunctionParameters) functionDef.get("parameters"))
                            .build();

                    // 创建工具定义
                    return ChatCompletionTool.Companion.builder()
                            .function(JsonField.of(function))
                            .type(JsonValue.from("function"))
                            .build();
                })
                .collect(Collectors.toList());

        // 将工具添加到参数中
        params = params.toBuilder()
                .tools(tools)
                .build();

        var response = aiClient.chat().completions().create(params);

        // 检查是否包含函数调用
        if (containsFunctionCall(response)) {
            return executeFunctionCall(response, botExecutionInstance);
        } else {
            return response.choices().get(0).message().content().orElse("");
        }
    }

    private ChatCompletionCreateParams buildChatParams(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            String content) {
        // 构建系统提示
        StringBuilder systemPrompt = new StringBuilder();
        prompts.stream()
                .map(PromptTexts::getContent)
                .filter(prompt -> prompt != null && !prompt.isBlank())
                .forEach(systemPrompt::append);

        // 构建消息列表
        List<ChatCompletionMessageParam> messageParams = messages.stream()
                .map(this::convertToMessageParam)
                .collect(Collectors.toList());

        // 添加系统消息（如果有）
        if (systemPrompt.length() > 0) {
            messageParams.add(0, ChatCompletionMessageParam.ofSystem(
                    ChatCompletionSystemMessageParam.builder()
                            .content(systemPrompt.toString())
                            .build()));
        }

        // 添加用户消息（如果有）
        if (content != null && !content.isBlank()) {
            messageParams.add(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder()
                            .content(content)
                            .build()));
        }

        return ChatCompletionCreateParams.builder()
                .model("deepseek-chat") // 这里需要从配置获取
                .messages(messageParams)
                .putAdditionalBodyProperty("enable_thinking", JsonBoolean.of(false))
                .build();
    }

    private ChatCompletionMessageParam convertToMessageParam(BotMessages msg) {
        String role = msg.getRole();
        String message = msg.getMessage();

        if (role == null || message == null) {
            throw new BusinessException("Invalid message: role or content is null");
        }

        switch (role) {
            case AIConstants.Roles.SYSTEM:
                return ChatCompletionMessageParam.ofSystem(
                        ChatCompletionSystemMessageParam.builder()
                                .content(message)
                                .build());
            case AIConstants.Roles.USER:
                return ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder()
                                .content(message)
                                .build());
            case AIConstants.Roles.ASSISTANT:
                return ChatCompletionMessageParam.ofAssistant(
                        ChatCompletionAssistantMessageParam.builder()
                                .content(message)
                                .build());
            default:
                throw new BusinessException(AIConstants.Messages.UNEXPECTED_ROLE + role);
        }
    }

    private boolean containsFunctionCall(Object response) {
        // 实现函数调用检查逻辑
        // 需要根据openai-java库的响应格式调整
        return false;
    }

    private <T extends BotExecution> Object executeFunctionCall(
            Object response,
            T botExecutionInstance) {
        // 实现函数调用执行逻辑
        // 需要根据openai-java库的响应格式调整
        return null;
    }

    @Override
    public Stream<String> chatWithAIStreaming(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            String content,
            AIModel model) {
        SAPAIDeepSeekConfig config = (SAPAIDeepSeekConfig) model.parseModelConfigs();
        OpenAIClient aiClient = getAiClientForDeepSeek(config);
        ChatCompletionCreateParams params = buildChatParams(messages, prompts, content);

        StreamResponse<ChatCompletionChunk> streamResponse = aiClient.chat().completions().createStreaming(params);

        return streamResponse.stream()
                .flatMap(completionChunk -> completionChunk.choices().stream())
                .filter(choice -> choice.delta().content().isPresent())
                .map(choice -> choice.delta().content().get());
    }

    public static void send(@Nonnull final SseEmitter emitter, @Nonnull final String chunk) {
        try {
            emitter.send(chunk);
        } catch (final Exception e) {
            emitter.completeWithError(e);
        }
    }
}
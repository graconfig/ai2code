package customer.ai2code.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sap.ai.sdk.core.AiCoreService;
import com.sap.ai.sdk.foundationmodels.openai.OpenAiClient;
import com.sap.ai.sdk.foundationmodels.openai.OpenAiModel;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionOutput;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionParameters;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionTool;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatMessage;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionTool.ToolType;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionFunction;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination; // 添加此行解决Destination未识别问题
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;

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
import customer.ai2code.service.processor.StreamingCompletedProcessor;

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

    public OpenAiClient getAiClientForDeepSeek(@Nonnull SAPAIDeepSeekConfig config) {
        // 创建HTTP目标对象
        HttpDestination destination = DefaultHttpDestination
                .builder(config.getApiUrl())
                .header("Authorization", "Bearer " + config.getApiKey())
                .build();

        // 创建AI核心服务
        AiCoreService aiCoreService = new AiCoreService();
        aiCoreService.withBaseDestination(destination);

        // 获取带有模型部署的目标（现在Destination已识别）
        Destination destinationWithDeployment = aiCoreService.getInferenceDestination().forModel(
                resolveDeepSeekModel(config.getModel()));

        // 创建OpenAI客户端
        return OpenAiClient.withCustomDestination(destinationWithDeployment);
    }

    /**
     * 解析DeepSeek模型名称
     */
    private OpenAiModel resolveDeepSeekModel(String modelName) {
        return OpenAiModel.GPT_4O; // 兼容处理
    }

    @Override
    public String chatWithAI(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            String content,
            AIModel model) {
        SAPAIDeepSeekConfig config = (SAPAIDeepSeekConfig) model.parseModelConfigs();
        OpenAiClient aiClient = getAiClientForDeepSeek(config);
        OpenAiChatCompletionParameters params = buildChatParams(messages, prompts, content);
        OpenAiChatCompletionOutput rawResult = aiClient.chatCompletion(params);
        return responseHandler.processResponse(rawResult).getContent();
    }

    @Override
    public <T extends BotExecution> Object functionCalling(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            T botExecutionInstance,
            AIModel model) {
        SAPAIDeepSeekConfig config = (SAPAIDeepSeekConfig) model.parseModelConfigs();
        OpenAiClient aiClient = getAiClientForDeepSeek(config);
        OpenAiChatCompletionParameters params = buildChatParams(messages, prompts, "");

        List<FunctionInfo> functionInfos = functionCallProcessor
                .extractFunctionInfosFromInstance(botExecutionInstance);

        if (functionInfos.isEmpty()) {
            throw new BusinessException("No executable methods found");
        }

        List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
                .convertToOpenAIFormat(functionInfos);

        List<OpenAiChatCompletionTool> tools = openAIFunctions.stream()
                .map(functionDef -> {
                    OpenAiChatCompletionFunction function = new OpenAiChatCompletionFunction();
                    function.setName((String) functionDef.get("name"));
                    function.setDescription((String) functionDef.get("description"));

                    if (functionDef.get("parameters") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parameters = (Map<String, Object>) functionDef.get("parameters");
                        function.setParameters(parameters);
                    }

                    if (!functionInfos.isEmpty()) {
                        params.setToolChoiceFunction(functionInfos.get(0).getName());
                    }

                    return new OpenAiChatCompletionTool()
                            .setType(ToolType.FUNCTION)
                            .setFunction(function);
                })
                .collect(Collectors.toList());

        params.setTools(tools);
        OpenAiChatCompletionOutput rawResult = aiClient.chatCompletion(params);

        return containsFunctionCall(rawResult)
                ? executeFunctionCall(rawResult, botExecutionInstance)
                : responseHandler.processResponse(rawResult).getContent();
    }

    // 辅助方法 ====================

    private OpenAiChatCompletionParameters buildChatParams(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            String content) {
        OpenAiChatCompletionParameters params = new OpenAiChatCompletionParameters();

        StringBuilder promptContent = new StringBuilder();
        prompts.stream()
                .map(PromptTexts::getContent)
                .filter(prompt -> prompt != null && !prompt.isBlank())
                .forEach(promptContent::append);

        if (promptContent.length() > 0) {
            params.addMessages(
                    new OpenAiChatMessage.OpenAiChatSystemMessage().setContent(promptContent.toString()));
        }

        messages.stream()
                .map(this::convertToOpenAiMessage)
                .forEach(params::addMessages);

        if (content != null && !content.isBlank()) {
            params.addMessages(
                    new OpenAiChatMessage.OpenAiChatUserMessage().addText(content));
        }

        return params;
    }

    private OpenAiChatMessage convertToOpenAiMessage(BotMessages msg) {
        String role = msg.getRole();
        String message = msg.getMessage();

        if (role == null || message == null) {
            throw new BusinessException("Invalid message: role or content is null");
        }

        return switch (role) {
            case AIConstants.Roles.SYSTEM ->
                messageFactory.createSystemMessage(message)[0];
            case AIConstants.Roles.USER ->
                messageFactory.createUserMessage(message)[0];
            case AIConstants.Roles.ASSISTANT ->
                messageFactory.createAssistantMessage(message)[0];
            default -> throw new BusinessException(AIConstants.Messages.UNEXPECTED_ROLE + role);
        };
    }

    private boolean containsFunctionCall(OpenAiChatCompletionOutput result) {
        return result.getChoices() != null &&
                !result.getChoices().isEmpty() &&
                result.getChoices().get(0).getMessage() != null &&
                result.getChoices().get(0).getMessage().getToolCalls() != null &&
                !result.getChoices().get(0).getMessage().getToolCalls().isEmpty();
    }

    private <T extends BotExecution> Object executeFunctionCall(
            OpenAiChatCompletionOutput result,
            T botExecutionInstance) {
        try {
            var toolCall = result.getChoices().get(0).getMessage().getToolCalls().get(0);
            return functionCallProcessor.executeFunctionCallOnInstance(
                    toolCall.getFunction().getName(),
                    toolCall.getFunction().getArguments(),
                    botExecutionInstance);
        } catch (Exception e) {
            throw new BusinessException("Function call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Stream<String> chatWithAIStreaming(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            String content,
            AIModel model) {
        throw new UnsupportedOperationException("Streaming not implemented for DeepSeek");
    }

    public static void send(@Nonnull final SseEmitter emitter, @Nonnull final String chunk) {
        try {
            emitter.send(chunk);
        } catch (final Exception e) {
            emitter.completeWithError(e);
        }
    }
}

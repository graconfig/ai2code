package customer.ai2code.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.ai.sdk.core.AiCoreService;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatMessage;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.OAuth2DestinationBuilder;
import com.sap.cloud.sdk.cloudplatform.connectivity.OnBehalfOf;
import com.sap.cloud.security.config.ClientCredentials;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.config.model.AIModel;
import customer.ai2code.model.ai.config.service.SAPAICoreClaudeConfig;
import customer.ai2code.model.ai.response.AIResponse;
import customer.ai2code.model.aicore.claude.ContentBlock;
import customer.ai2code.model.aicore.claude.ConverseRequest;
import customer.ai2code.model.aicore.claude.ConverseRequestAssistantMessage;
import customer.ai2code.model.aicore.claude.ConverseRequestInferenceConfig;
import customer.ai2code.model.aicore.claude.ConverseRequestToolConfig;
import customer.ai2code.model.aicore.claude.ConverseResponse;
import customer.ai2code.model.aicore.claude.ConverseResponseAssistantMessage;
import customer.ai2code.model.aicore.claude.ConverseTool;
import customer.ai2code.model.aicore.claude.ConverseToolChoice;
import customer.ai2code.model.aicore.claude.SpecificToolChoice;
import customer.ai2code.model.aicore.claude.ToolInputSchema;
import customer.ai2code.model.aicore.claude.ToolSpecification;
import customer.ai2code.model.aicore.claude.client.ClaudeAiClient;
import customer.ai2code.model.aicore.claude.client.ClaudeAiModel;
import customer.ai2code.model.execution.functioncall.FunctionInfo;
import customer.ai2code.model.factory.SAPClaudeAIMessageFactory;
import customer.ai2code.service.AIService;
import customer.ai2code.service.constant.AIConstants;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.execution.functioncall.FunctionCallProcessor;
import customer.ai2code.service.execution.functioncall.adapter.ClaudeFunctionCallAdapter;
import customer.ai2code.service.handler.factory.AIResponseHandlerFactory;
import customer.ai2code.service.processor.StreamingCompletedProcessor;

@Service
public class SAClaudeAIServiceImpl implements AIService {

        private final SAPClaudeAIMessageFactory messageFactory;
        private final AIResponseHandlerFactory responseHandlerFactory;
        private final FunctionCallProcessor functionCallProcessor;
        private final ClaudeFunctionCallAdapter functionCallAdapter;
        private final ObjectMapper objectMapper;

        public SAClaudeAIServiceImpl(SAPClaudeAIMessageFactory messageFactory,
                        AIResponseHandlerFactory responseHandlerFactory,
                        FunctionCallProcessor functionCallProcessor,
                        ClaudeFunctionCallAdapter functionCallAdapter,
                        ObjectMapper objectMapper) {
                this.messageFactory = messageFactory;
                this.responseHandlerFactory = responseHandlerFactory;
                this.functionCallProcessor = functionCallProcessor;
                this.functionCallAdapter = functionCallAdapter;
                this.objectMapper = objectMapper;
        }

        @Override
        public String chatWithAI(List<BotMessages> messages, List<PromptTexts> prompts, String content, AIModel model) {
                // TODO Auto-generated method stub
                // throw new UnsupportedOperationException("Unimplemented method 'chatWithAI'");

                SAPAICoreClaudeConfig aiCoreServiceKeyConfig = (SAPAICoreClaudeConfig) model.parseModelConfigs();

                ConverseRequest request = new ConverseRequest();
                ConverseRequestInferenceConfig inferenceConfig = new ConverseRequestInferenceConfig()
                                .maxTokens(aiCoreServiceKeyConfig.getMaxTokens())
                                .temperature(aiCoreServiceKeyConfig.getTemperature());
                request.setInferenceConfig(inferenceConfig);


                String systemMessage = prompts.stream()
                                .filter(prompt -> prompt.getContent() != null && !prompt.getContent().isBlank()
                                                && prompt.getRoleCode() != null
                                                && AIConstants.Roles.SYSTEM.equals(prompt.getRoleCode()))
                                .map(PromptTexts::getContent)

                                .findFirst()
                                .orElse("");

                if (systemMessage != null) {
                        request.addSystemItem(messageFactory.createSystemBlock(systemMessage));
                }
                // history messages
                messages.stream()
                                .filter(msg -> !AIConstants.Roles.SYSTEM.equals(msg.getRole()))
                                .forEach(msg -> {
                                        if (AIConstants.Roles.USER.equals(msg.getRole())) {
                                                request.addMessagesItem(
                                                                messageFactory.createUserMessage(msg.getMessage()));
                                        } else if (AIConstants.Roles.ASSISTANT.equals(msg.getRole())) {
                                                request.addMessagesItem(
                                                                messageFactory.createAssistantMessage(
                                                                                msg.getMessage()));
                                        } else {
                                                throw new BusinessException(AIConstants.Messages.UNEXPECTED_ROLE +
                                                                msg.getRole());
                                        }
                                });
                // add user prompt
                prompts.stream().filter(prompt -> prompt.getContent() != null && !prompt.getContent().isBlank()
                                && prompt.getRoleCode() != null && AIConstants.Roles.USER.equals(prompt.getRoleCode()))
                                .forEach(prompt -> request.addMessagesItem(
                                                messageFactory.createUserMessage(prompt.getContent())));

                // add user message
                if (content != null && !content.isBlank()) {
                        request.addMessagesItem(messageFactory.createUserMessage(content));
                }

                ClaudeAiClient aiClient = getAiClientbyModelUsingBTPDestination(
                                (SAPAICoreClaudeConfig) model.parseModelConfigs(),
                                resolveClaudeAiModel(model.getModelName()));
                ConverseResponse rawResult = aiClient.chatCompletion(request);

                AIResponse aiResponse = responseHandlerFactory.getHandler(AIConstants.AIServiceType.SAPCLAUDE)
                                .processResponse(rawResult);

                return aiResponse.getContent();

        }

        @Override
        public Stream<String> chatWithAIStreaming(List<BotMessages> messages, List<PromptTexts> prompts, String content,
                        AIModel model
                        // ExecutorService executor,
                        // StreamingCompletedProcessor streamingCompletionProcessor
                        ) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'chatWithAIStreaming'");
        }

        @Override
        public <T extends BotExecution> Object functionCalling(List<BotMessages> messages, List<PromptTexts> prompts,
                        T botExecutionInstance, AIModel model) {
                // TODO Auto-generated method stub
                // throw new UnsupportedOperationException("Unimplemented method
                // 'functionCalling'");

                SAPAICoreClaudeConfig aiCoreServiceKeyConfig = (SAPAICoreClaudeConfig) model.parseModelConfigs();

                // 1. 从 botExecutionInstance 中提取函数信息
                List<FunctionInfo> functionInfos = functionCallProcessor
                                .extractFunctionInfosFromInstance(botExecutionInstance);

                if (functionInfos.isEmpty()) {
                        throw new BusinessException("No executable methods found in bot instance: "
                                        + botExecutionInstance.getClass().getSimpleName());
                }

                ConverseRequest request = new ConverseRequest();
                // Set inference config
                ConverseRequestInferenceConfig inferenceConfig = new ConverseRequestInferenceConfig()
                                .maxTokens(aiCoreServiceKeyConfig.getMaxTokens())
                                .temperature(aiCoreServiceKeyConfig.getTemperature());
                request.setInferenceConfig(inferenceConfig);

                // 将system prompt合并成一个system消息
                StringBuilder promptContent = new StringBuilder();
                prompts.stream()
                                .filter(prompt -> prompt.getContent() != null && !prompt.getContent().isBlank()
                                                && prompt.getRoleCode() != null
                                                && AIConstants.Roles.SYSTEM.equals(prompt.getRoleCode()))
                                .map(PromptTexts::getContent)

                                .forEach(promptContent::append);

                request.addSystemItem(messageFactory.createSystemBlock(promptContent.toString()));

                // history messages
                messages.stream()
                                .filter(msg -> !AIConstants.Roles.SYSTEM.equals(msg.getRole()))
                                .forEach(msg -> {
                                        if (AIConstants.Roles.USER.equals(msg.getRole())) {
                                                request.addMessagesItem(
                                                                messageFactory.createUserMessage(msg.getMessage()));
                                        } else if (AIConstants.Roles.ASSISTANT.equals(msg.getRole())) {
                                                request.addMessagesItem(
                                                                messageFactory.createAssistantMessage(
                                                                                msg.getMessage()));
                                        } else {
                                                throw new BusinessException(AIConstants.Messages.UNEXPECTED_ROLE +
                                                                msg.getRole());
                                        }
                                });
                // add user prompt
                prompts.stream().filter(prompt -> prompt.getContent() != null && !prompt.getContent().isBlank()
                                && prompt.getRoleCode() != null && AIConstants.Roles.USER.equals(prompt.getRoleCode()))
                                .forEach(prompt -> request.addMessagesItem(
                                                messageFactory.createUserMessage(prompt.getContent())));

                // 3. 转换为 OpenAI Function Calling 格式并添加到参数中
                List<Map<String, Object>> function = functionCallAdapter
                                .convertToClaudeFormat(functionInfos);

                // 添加 functions 到 OpenAI 请求参数
                List<ConverseTool> tools = function.stream()
                                .map(functionDef -> {
                                        // OpenAiChatCompletionFunction function = new OpenAiChatCompletionFunction();
                                        ConverseTool tool = new ConverseTool();
                                        ToolSpecification specification = new ToolSpecification();
                                        specification.setName(functionDef.get("name").toString());
                                        specification.setDescription(functionDef.get("description").toString());

                                        ToolInputSchema inputSchema = new ToolInputSchema();
                                        inputSchema.setJson(functionDef.get("inputSchema"));
                                        specification.setInputSchema(inputSchema);

                                        // tool.setSpecification(specification);
                                        tool.setToolSpec(specification);
                                        return tool;
                                }).toList();

                ConverseRequestToolConfig toolConfig = new ConverseRequestToolConfig();
                toolConfig.setTools(tools);

                tools.stream()
                                .findFirst()
                                .ifPresent(tool -> toolConfig.setToolChoice(new ConverseToolChoice().tool(
                                                new SpecificToolChoice().name(tool.getToolSpec().getName()))));

                request.setToolConfig(toolConfig);

                // 4. 调用 Claude AI 客户端
                ClaudeAiClient aiClient = getAiClientbyModelUsingBTPDestination(
                                (SAPAICoreClaudeConfig) model.parseModelConfigs(),
                                resolveClaudeAiModel(model.getModelName()));

                ConverseResponse rawResult = aiClient.chatCompletion(request);

                // 5. 处理 AI 响应
                AIResponse aiResponse = responseHandlerFactory.getHandler(AIConstants.AIServiceType.SAPCLAUDE)
                                .processResponse(rawResult);

                // 6. 检查是否有函数调用需要执行
                String responseContent = aiResponse.getContent();
                // 如果 AI 返回了函数调用，则执行函数调用
                if (containsFunctionCall(rawResult)) {
                        try {
                                return executeFunctionCall(rawResult, botExecutionInstance);
                                // 可以选择返回函数执行结果，或者将结果再次发送给 AI 进行后续处理
                                // return functionCallResult;
                        } catch (Exception e) {
                                throw new BusinessException("Function call execution failed: " + e.getMessage(), e);
                        }
                }

                return responseContent;

        }

        public ClaudeAiClient getAiClientbyModelUsingBTPDestination(
                        @Nonnull SAPAICoreClaudeConfig aiCoreServiceKeyConfig,
                        @Nonnull ClaudeAiModel foundationModel) {
                // build api destination
                Destination destination = OAuth2DestinationBuilder
                                .forTargetUrl(aiCoreServiceKeyConfig.getServiceUrls().getAiApiUrl())
                                .withTokenEndpoint(aiCoreServiceKeyConfig.getTokenUrl())
                                .withClient(
                                                new ClientCredentials(aiCoreServiceKeyConfig.getClientId(),
                                                                aiCoreServiceKeyConfig.getClientSecret()),
                                                OnBehalfOf.TECHNICAL_USER_PROVIDER)
                                .build();

                AiCoreService aiCoreService = new AiCoreService().withBaseDestination(destination.asHttp());
                Destination destinationWithDeployment = aiCoreService.getInferenceDestination()
                                .forModel(foundationModel);
                return ClaudeAiClient.withCustomDestination(destinationWithDeployment);
        }

        // 将模型解析逻辑内联到这里
        private ClaudeAiModel resolveClaudeAiModel(String modelName) {
                if (modelName == null || modelName.isEmpty()) {
                        return ClaudeAiModel.CLAUDE_3_7_SONNET;
                }

                return switch (modelName.toLowerCase()) {
                        case "claude-3.5-sonnet" -> ClaudeAiModel.CLAUDE_3_5_SONNET;
                        case "claude-3.7-sonnet" -> ClaudeAiModel.CLAUDE_3_7_SONNET;
                        case "claude-4-sonnet" -> ClaudeAiModel.CLAUDE_4_SONNET;

                        default -> {
                                System.out.println(
                                                "Unknown model name: " + modelName
                                                                + ", using default CLAUDE-3-7-SONNET");
                                yield ClaudeAiModel.CLAUDE_3_7_SONNET;
                        }
                };
        }

        private boolean containsFunctionCall(ConverseResponse response) {
                // 检查响应中是否包含函数调用
                // return response.getToolCalls() != null && !response.getToolCalls().isEmpty();
                return ConverseResponse.StopReasonEnum.TOOL_USE.equals(response.getStopReason());

        }

        /**
         * 执行函数调用
         */
        @SuppressWarnings("null")
        private <T extends BotExecution> Object executeFunctionCall(ConverseResponse response,
                        T botExecutionInstance) throws Exception {

                // 从 Claude 响应中提取函数调用信息
                if (response.getOutput() == null) {
                        throw new BusinessException("Response output is null");
                }
                ConverseResponseAssistantMessage assistantMessage = (ConverseResponseAssistantMessage) response
                                .getOutput()
                                .getMessage();
                ContentBlock toolUseContent = assistantMessage.getContent().stream()
                                .filter(content -> content.getToolUse() != null)
                                .findFirst().orElse(null);
                if (toolUseContent != null && toolUseContent.getToolUse().getInput() != null) {
                        String functionName = toolUseContent.getToolUse().getName();
                        // 修复：获取 JSON 字符串
                        String argumentsJson = objectMapper.writeValueAsString(toolUseContent.getToolUse().getInput());

                        // 使用 FunctionCallProcessor 执行函数调用
                        return functionCallProcessor.executeFunctionCallOnInstance(
                                        functionName, argumentsJson, botExecutionInstance);
                }

                throw new BusinessException("No valid function call found in the response.");

        }

}

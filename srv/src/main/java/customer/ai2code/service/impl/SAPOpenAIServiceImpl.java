package customer.ai2code.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.ai.sdk.core.AiCoreService;
import com.sap.ai.sdk.foundationmodels.openai.OpenAiClient;
import com.sap.ai.sdk.foundationmodels.openai.OpenAiModel;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionOutput;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionParameters;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionTool;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatMessage;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionTool.ToolType;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionFunction;
import com.sap.cloud.sdk.cloudplatform.connectivity.OAuth2DestinationBuilder;
import com.sap.cloud.sdk.cloudplatform.connectivity.OnBehalfOf;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.security.config.ClientCredentials;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.config.model.AIModel;
import customer.ai2code.model.ai.config.service.SAPAICoreConfig;
import customer.ai2code.model.ai.response.AIResponse;
import customer.ai2code.model.execution.functioncall.FunctionInfo;
import customer.ai2code.model.factory.SAPOpenAIChatMessageFactory;
import customer.ai2code.service.AIService;
import customer.ai2code.service.constant.AIConstants;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.execution.functioncall.FunctionCallProcessor;
import customer.ai2code.service.execution.functioncall.adapter.OpenAIFunctionCallAdapter;
import customer.ai2code.service.handler.factory.AIResponseHandlerFactory;
import customer.ai2code.service.processor.StreamingCompletedProcessor;

@Service
public class SAPOpenAIServiceImpl implements AIService {

        private final SAPOpenAIChatMessageFactory messageFactory;
        private final AIResponseHandlerFactory responseHandlerFactory;
        private final FunctionCallProcessor functionCallProcessor;
        private final OpenAIFunctionCallAdapter openAIFunctionCallAdapter;
        // private final ObjectMapper objectMapper; // 添加JSON处理

        public SAPOpenAIServiceImpl(SAPOpenAIChatMessageFactory messageFactory,
                        AIResponseHandlerFactory responseHandlerFactory,
                        FunctionCallProcessor functionCallProcessor,
                        OpenAIFunctionCallAdapter openAIFunctionCallAdapter
                        // ObjectMapper objectMapper
                        ) {
                this.messageFactory = messageFactory;
                this.responseHandlerFactory = responseHandlerFactory;
                this.functionCallProcessor = functionCallProcessor;
                this.openAIFunctionCallAdapter = openAIFunctionCallAdapter;
                // this.objectMapper = objectMapper;
        }

        public OpenAiClient getAiClientbyModelUsingBTPDestination(@Nonnull SAPAICoreConfig aiCoreServiceKeyConfig,
                        @Nonnull OpenAiModel foundationModel) {
                // build api destination
                // Destination destination =
                // DestinationAccessor.getDestination(aiServiceKeys.getAiCoreDestination());
                // AiCoreService aiCoreService = new AiCoreService();
                // aiCoreService.getBaseDestination()
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
                return OpenAiClient.withCustomDestination(destinationWithDeployment);
        }

        @Override
        public String chatWithAI(List<BotMessages> messages, List<PromptTexts> prompts, String content, AIModel model) {
                OpenAiChatCompletionParameters params = new OpenAiChatCompletionParameters();

                // history messages
                messages.stream()
                                .map(msg -> switch (msg.getRole()) {
                                        case AIConstants.Roles.SYSTEM ->
                                                messageFactory.createSystemMessage(msg.getMessage());
                                        case AIConstants.Roles.USER ->
                                                messageFactory.createUserMessage(msg.getMessage());
                                        case AIConstants.Roles.ASSISTANT ->
                                                messageFactory.createAssistantMessage(msg.getMessage());
                                        default -> throw new BusinessException(AIConstants.Messages.UNEXPECTED_ROLE +
                                                        msg.getRole());
                                }).forEach(params::addMessages);
                // add prompts
                prompts.stream()
                                .map(PromptTexts::getContent)
                                .filter(prompt -> prompt != null && !prompt.isBlank())
                                .forEach(prompt -> params
                                                .addMessages(new OpenAiChatMessage.OpenAiChatSystemMessage()
                                                                .setContent(prompt)));

                // add user message
                if (content != null && !content.isBlank()) {
                        params.addMessages(new OpenAiChatMessage.OpenAiChatUserMessage().addText(content));
                }

                OpenAiClient aiClient = getAiClientbyModelUsingBTPDestination(
                                (SAPAICoreConfig) model.parseModelConfigs(),
                                resolveOpenAiModel(model.getModelName()));
                OpenAiChatCompletionOutput rawResult = aiClient.chatCompletion(params);
                // return
                AIResponse aiResponse = responseHandlerFactory.getHandler(AIConstants.AIServiceType.SAPOPENAI)
                                .processResponse(rawResult);

                return aiResponse.getContent();
        }

        @Override
        public SseEmitter chatWithAIStreaming(List<BotMessages> messages, List<PromptTexts> prompts, String content,
                        AIModel model,
                        ExecutorService executor,
                        StreamingCompletedProcessor processor) {

                // Create streaming chat completion request
                OpenAiChatCompletionParameters params = new OpenAiChatCompletionParameters();
                // 1. add prompts
                // prompts.stream()
                //                 .map(PromptTexts::getContent)
                //                 .filter(prompt -> prompt != null && !prompt.isBlank())
                //                 .forEach(prompt -> params
                //                                 .addMessages(new OpenAiChatMessage.OpenAiChatSystemMessage()
                //                                                 .setContent(prompt)));
                //将prompt合并成一个system消息
                StringBuilder promptContent = new StringBuilder();
                prompts.stream()
                                .map(PromptTexts::getContent)
                                .filter(prompt -> prompt != null && !prompt.isBlank())
                                .forEach(promptContent::append);
                params.addMessages(new OpenAiChatMessage.OpenAiChatSystemMessage().setContent(promptContent.toString()));

                // 2. history messages
                messages.stream()
                                .map(msg -> switch (msg.getRole()) {
                                        case AIConstants.Roles.SYSTEM ->
                                                messageFactory.createSystemMessage(msg.getMessage());
                                        case AIConstants.Roles.USER ->
                                                messageFactory.createUserMessage(msg.getMessage());
                                        case AIConstants.Roles.ASSISTANT ->
                                                messageFactory.createAssistantMessage(msg.getMessage());
                                        default -> throw new BusinessException(AIConstants.Messages.UNEXPECTED_ROLE +
                                                        msg.getRole());
                                }).forEach(params::addMessages);


                // add user message
                if (content != null && !content.isBlank()) {
                        params.addMessages(new OpenAiChatMessage.OpenAiChatUserMessage().addText(content));
                }
                OpenAiClient aiClient = getAiClientbyModelUsingBTPDestination(
                                (SAPAICoreConfig) model.parseModelConfigs(),
                                resolveOpenAiModel(model.getModelName()));

                SseEmitter emitter = new SseEmitter(20 * 60 * 1000L); // 3 minutes timeout
                final StringBuilder responseBuilder = new StringBuilder();

                executor.execute(() -> {
                        try {
                                aiClient.streamChatCompletionDeltas(params).forEach(delta -> {
                                        // Process each delta and send it to the client
                                        AIService.send(emitter, delta.getDeltaContent());
                                        responseBuilder.append(delta);
                                });
                                // emitter.complete();
                        } catch (Exception e) {
                                emitter.completeWithError(e);
                        } finally {
                                // process other logic after streaming is complete
                                if (processor != null) {
                                        processor.process(responseBuilder.toString());
                                }
                                emitter.complete();
                        }
                });
                return emitter;

        }

        @Override
        public <T extends BotExecution> Object functionCalling(List<BotMessages> messages, List<PromptTexts> prompts,
                        T botExecutionInstance,
                        AIModel model) {

                // 1. 从 botExecutionInstance 中提取函数信息
                List<FunctionInfo> functionInfos = functionCallProcessor
                                .extractFunctionInfosFromInstance(botExecutionInstance);

                if (functionInfos.isEmpty()) {
                        throw new BusinessException("No executable methods found in bot instance: "
                                        + botExecutionInstance.getClass().getSimpleName());
                }

                // 2. 构建 OpenAI Chat Completion 参数
                OpenAiChatCompletionParameters params = new OpenAiChatCompletionParameters();

                // add prompts
                // prompts.stream()
                //                 .map(PromptTexts::getContent)
                //                 .filter(prompt -> prompt != null && !prompt.isBlank())
                //                 .forEach(prompt -> params
                //                                 .addMessages(new OpenAiChatMessage.OpenAiChatSystemMessage()
                //                                                 .setContent(prompt)));
                //将prompt合并成一个system消息
                StringBuilder promptContent = new StringBuilder();
                prompts.stream()
                                .map(PromptTexts::getContent)
                                .filter(prompt -> prompt != null && !prompt.isBlank())
                                .forEach(promptContent::append);
                params.addMessages(new OpenAiChatMessage.OpenAiChatSystemMessage().setContent(promptContent.toString()));

                // history messages
                messages.stream()
                                .map(msg -> switch (msg.getRole()) {
                                        case AIConstants.Roles.SYSTEM ->
                                                messageFactory.createSystemMessage(msg.getMessage());
                                        case AIConstants.Roles.USER ->
                                                messageFactory.createUserMessage(msg.getMessage());
                                        case AIConstants.Roles.ASSISTANT ->
                                                messageFactory.createAssistantMessage(msg.getMessage());
                                        default -> throw new BusinessException(AIConstants.Messages.UNEXPECTED_ROLE +
                                                        msg.getRole());
                                }).forEach(params::addMessages);



                // 3. 转换为 OpenAI Function Calling 格式并添加到参数中
                List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
                                .convertToOpenAIFormat(functionInfos);

                // 添加 functions 到 OpenAI 请求参数
                List<OpenAiChatCompletionTool> tools = openAIFunctions.stream()
                                .map(functionDef -> {
                                        OpenAiChatCompletionFunction function = new OpenAiChatCompletionFunction();
                                        function.setName((String) functionDef.get("name"));
                                        function.setDescription((String) functionDef.get("description"));

                                        Object parametersObj = functionDef.get("parameters");
                                        if (parametersObj instanceof Map) {
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> parameters = (Map<String, Object>) parametersObj;
                                                function.setParameters(parameters);
                                        }
                                        params.setToolChoiceFunction((String)functionDef.get("name"));
                                        return new OpenAiChatCompletionTool().setType(ToolType.FUNCTION)
                                                        .setFunction(function);
                                }).toList();

                params.setTools(tools);
                // params.setToolChoiceFunction(func)

                // 4. 调用 OpenAI API
                OpenAiClient aiClient = getAiClientbyModelUsingBTPDestination(
                                (SAPAICoreConfig) model.parseModelConfigs(),
                                resolveOpenAiModel(model.getModelName()));

                OpenAiChatCompletionOutput rawResult = aiClient.chatCompletion(params);

                // 5. 处理 AI 响应
                AIResponse aiResponse = responseHandlerFactory.getHandler(AIConstants.AIServiceType.SAPOPENAI)
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

        /**
         * 检查 OpenAI 响应中是否包含函数调用
         */
        private boolean containsFunctionCall(OpenAiChatCompletionOutput result) {
                return result.getChoices() != null &&
                                result.getChoices().stream()
                                                .anyMatch(choice -> choice.getMessage() != null &&
                                                                choice.getMessage().getToolCalls() != null &&
                                                                !choice.getMessage().getToolCalls().isEmpty());
        }

        /**
         * 执行函数调用
         */
        private <T extends BotExecution> Object executeFunctionCall(OpenAiChatCompletionOutput result,
                        T botExecutionInstance) throws Exception {
                // 从 OpenAI 响应中提取函数调用信息
                var toolCall = result.getChoices().get(0).getMessage().getToolCalls().get(0);
                String functionName = toolCall.getFunction().getName();

                // 修复：获取 JSON 字符串
                String argumentsJson = toolCall.getFunction().getArguments();


                // 使用 FunctionCallProcessor 执行函数调用
                return functionCallProcessor.executeFunctionCallOnInstance(
                                functionName, argumentsJson, botExecutionInstance);

                
                // return executionResult;
        }

        // 将模型解析逻辑内联到这里
        private OpenAiModel resolveOpenAiModel(String modelName) {
                if (modelName == null || modelName.isEmpty()) {
                        return OpenAiModel.GPT_4O;
                }

                return switch (modelName.toLowerCase()) {
                        case "gpt-3.5-turbo", "gpt35turbo" -> OpenAiModel.GPT_35_TURBO;
                        case "gpt-4", "gpt4" -> OpenAiModel.GPT_4;
                        case "gpt-4o", "gpt4o" -> OpenAiModel.GPT_4O;
                        case "gpt-4o-mini", "gpt4omini" -> OpenAiModel.GPT_4O_MINI;
                        case "text-embedding-ada-002" -> OpenAiModel.TEXT_EMBEDDING_ADA_002;
                        case "text-embedding-3-small" -> OpenAiModel.TEXT_EMBEDDING_3_SMALL;
                        case "text-embedding-3-large" -> OpenAiModel.TEXT_EMBEDDING_3_LARGE;
                        default -> {
                                System.out.println(
                                                "Unknown model name: " + modelName + ", using default GPT-3.5-turbo");
                                yield OpenAiModel.GPT_4O;
                        }
                };
        }

}

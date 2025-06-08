package customer.ai2code.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sap.ai.sdk.core.AiCoreService;
import com.sap.ai.sdk.foundationmodels.openai.OpenAiClient;
import com.sap.ai.sdk.foundationmodels.openai.OpenAiModel;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionOutput;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionParameters;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatMessage;
import com.sap.cloud.sdk.cloudplatform.connectivity.OAuth2DestinationBuilder;
import com.sap.cloud.sdk.cloudplatform.connectivity.OnBehalfOf;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.security.config.ClientCredentials;
// import cds.gen.configservice.FunctionCalls;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.AIModel;
// import customer.ai2code.model.config.AIModelResolver;
// import customer.ai2code.model.config.AIServiceConfig;
import customer.ai2code.model.config.SAPAICoreConfig;
import customer.ai2code.service.AIService;
import customer.ai2code.service.constant.AIConstants;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.handler.factory.AIResponseHandlerFactory;
import customer.ai2code.service.model.AIResponse;
import customer.ai2code.service.model.factory.SAPOpenAIChatMessageFactory;
import customer.ai2code.service.processor.StreamingCompletedProcessor;

@Service
public class SAPOpenAIServiceImpl implements AIService {

        private final SAPOpenAIChatMessageFactory messageFactory;
        private final AIResponseHandlerFactory responseHandlerFactory;
        // private final AIModelResolver aiModelResolver;

        public SAPOpenAIServiceImpl(SAPOpenAIChatMessageFactory messageFactory,
                        AIResponseHandlerFactory responseHandlerFactory
                        // AIModelResolver aiModelResolver
                        ) {
                this.messageFactory = messageFactory;
                this.responseHandlerFactory = responseHandlerFactory;
                // this.aiModelResolver = aiModelResolver;
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
        public <T extends BotExecution> String functionCalling(List<BotMessages> messages, List<PromptTexts> prompts,
                        Class<T> botExecutionClazz,
                        AIModel model) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException(
                                "Unimplemented method 'functionCalling' for SAPOpenAIServiceImpl");
                // parse functionCall parameers by clazz and parameters;
                // OpenAiChatCompletionParameters params = new OpenAiChatCompletionParameters();

                // return "";
        }

        // 将模型解析逻辑内联到这里
        private OpenAiModel resolveOpenAiModel(String modelName) {
                if (modelName == null || modelName.isEmpty()) {
                        return OpenAiModel.GPT_35_TURBO;
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
                                yield OpenAiModel.GPT_35_TURBO;
                        }
                };
        }

}

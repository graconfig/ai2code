package customer.ai2code.service.SAPAICore;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

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
import cds.gen.configservice.FunctionCalls;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.config.AIModelResolver;
import customer.ai2code.model.config.AIServiceConfig;
import customer.ai2code.model.config.SAPAICoreConfig;
import customer.ai2code.service.AIService;
import customer.ai2code.service.constant.AIConstants;
import customer.ai2code.service.handler.factory.AIResponseHandlerFactory;
import customer.ai2code.service.model.AIResponse;
import customer.ai2code.service.model.factory.SAPOpenAIChatMessageFactory;

public class SAPOpenAIServiceImpl implements AIService {

    private final SAPOpenAIChatMessageFactory messageFactory;
    private final AIResponseHandlerFactory responseHandlerFactory;

    public SAPOpenAIServiceImpl(SAPOpenAIChatMessageFactory messageFactory,
            AIResponseHandlerFactory responseHandlerFactory) {
        this.messageFactory = messageFactory;
        this.responseHandlerFactory = responseHandlerFactory;
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
    public String chatWithAI(List<BotMessages> messages, List<PromptTexts> prompts, String content,
            AIServiceConfig serviceConfig,
            String modelName) {
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
                        .addMessages(new OpenAiChatMessage.OpenAiChatSystemMessage().setContent(prompt)));

        // add user message
        if (content != null && !content.isBlank()) {
            params.addMessages(new OpenAiChatMessage.OpenAiChatUserMessage().addText(content));
        }

        OpenAiClient aiClient = getAiClientbyModelUsingBTPDestination((SAPAICoreConfig)serviceConfig,
                AIModelResolver.resolveOpenAiModel(modelName));
        OpenAiChatCompletionOutput rawResult = aiClient.chatCompletion(params);
        // return
        AIResponse aiResponse = responseHandlerFactory.getHandler(AIConstants.AIServiceType.SAPOPENAI)
                .processResponse(rawResult);

        return aiResponse.getContent();
    }

    @Override
    public SseEmitter chatWithAIStreaming(List<BotMessages> messages, List<PromptTexts> prompts, String content,
            AIServiceConfig serviceConfig,
            String modelName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'chatWithAIStreaming'");
    }

    @Override
    public String functionCalling(List<BotMessages> messages, List<PromptTexts> prompts, FunctionCalls functionCall,
            AIServiceConfig serviceConfig,
            String modelName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'functionCalling'");
    }

}

package customer.ai2code.model.aicore.claude.client;



import com.fasterxml.jackson.core.JsonParser;

// import static customer.aireport.service.SAPAICore.claude.ClaudeAiUtils.getClaudeAiObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.Beta;
import com.sap.ai.sdk.core.AiCoreService;
import com.sap.ai.sdk.core.DeploymentResolutionException;
import com.sap.ai.sdk.core.common.ClientResponseHandler;
import com.sap.ai.sdk.core.common.ClientStreamingHandler;
import com.sap.ai.sdk.core.common.StreamedDelta;


import com.sap.cloud.sdk.cloudplatform.connectivity.ApacheHttpClient5Accessor;
// import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;

import customer.ai2code.model.aicore.claude.ContentBlock;
import customer.ai2code.model.aicore.claude.ConverseRequest;
import customer.ai2code.model.aicore.claude.ConverseRequestUserMessage;
import customer.ai2code.model.aicore.claude.ConverseResponse;
import customer.ai2code.model.aicore.claude.InvokeRequest;
import customer.ai2code.model.aicore.claude.InvokeResponse;
import customer.ai2code.model.aicore.claude.RequestUserMessage;
import customer.ai2code.model.aicore.claude.RequestUserMessageContent;
import customer.ai2code.model.aicore.claude.SystemContentBlock;
import customer.ai2code.model.aicore.claude.delta.ClaudeAIChatCompletionDelta;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import static com.sap.ai.sdk.core.JacksonConfiguration.getDefaultObjectMapper;

/** Client for interacting with OpenAI models. */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClaudeAiClient {
  // private static final String DEFAULT_API_VERSION = "2024-02-01";
  static final ObjectMapper JACKSON;

  static {
    ObjectMapper mapper = getDefaultObjectMapper();
    mapper.configOverride(Map.class)
        .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
    JACKSON = mapper;
  }

  @Nullable
  private String systemPrompt = null;

  @Nonnull
  private final Destination destination;

  /**
   * Create a new OpenAI client for the given foundation model, using the default
   * resource group.
   *
   * @param foundationModel the OpenAI model which is deployed.
   * @return a new OpenAI client.
   * @throws DeploymentResolutionException if no deployment for the given model
   *                                       was found in the
   *                                       default resource group.
   */
  @Nonnull
  public static ClaudeAiClient forModel(@Nonnull final ClaudeAiModel foundationModel)
      throws DeploymentResolutionException {
    final var destination = new AiCoreService().getInferenceDestination().forModel(foundationModel);

    final var client = new ClaudeAiClient(destination);

    return client;
    // return client.withApiVersion(DEFAULT_API_VERSION);
  }

  // /**
  // * Create a new OpenAI client targeting the specified API version.
  // *
  // * @param apiVersion the API version to target.
  // * @return a new client.
  // */
  // @Beta
  // @Nonnull
  // public ClaudeAiClient withApiVersion(@Nonnull final String apiVersion) {
  // final var newDestination =
  // DefaultHttpDestination.fromDestination(this.destination)
  // // set the API version as URL query parameter
  // .property("URL.queries.api-version", apiVersion)
  // .build();
  // return new ClaudeAiClient(newDestination);
  // }

  /**
   * Create a new OpenAI client with a custom destination, allowing for a custom
   * resource group or
   * otherwise custom destination. The destination needs to be configured with a
   * URL pointing to an
   * OpenAI model deployment. Typically, such a destination should be obtained
   * using {@link
   * AiCoreService#getInferenceDestination(String)}.
   *
   * <p>
   * Example:
   *
   * <pre>{@code
   * var destination = new AiCoreService().getInferenceDestination("custom-rg").forModel(GPT_4O);
   * ClaudeAiClient.withCustomDestination(destination);
   * }</pre>
   *
   * @param destination The specific {@link HttpDestination} to use.
   * @see AiCoreService#getInferenceDestination(String)
   * @return a new OpenAI client.
   */
  @Beta
  @Nonnull
  public static ClaudeAiClient withCustomDestination(@Nonnull final Destination destination) {
    final ClaudeAiClient client = new ClaudeAiClient(destination);

    // if (destination.get("URL.queries.api-version").isDefined()) {
    return client;
    // }

    // return client.withApiVersion(DEFAULT_API_VERSION);
  }

  /**
   * Use this method to set a system prompt that should be used across multiple
   * chat completions
   * with basic string prompts
   * {@link #streamChatCompletionDeltas(ClaudeAiChatCompletionParameters)}.
   *
   * <p>
   * Note: The system prompt is ignored on chat completions invoked with
   * ClaudeAiChatCompletionPrompt.
   *
   * @param systemPrompt the system prompt
   * @return the client
   */
  @Nonnull
  public ClaudeAiClient withSystemPrompt(@Nonnull final String systemPrompt) {
    this.systemPrompt = systemPrompt;
    return this;
  }

  /**
   * Generate a completion for the given string prompt as user.
   *
   * @param prompt a text message.
   * @return the completion output
   * @throws ClaudeAiClientException if the request fails
   */
  @Nonnull
  public InvokeResponse chatCompletionWithPresetPromptInvoke(@Nonnull final String prompt)
      throws ClaudeAiClientException {

    final InvokeRequest request = new InvokeRequest();
    if (systemPrompt != null) {
      request.setSystem(systemPrompt);
    }
    // parameters.addMessages(new ClaudeAiChatUserMessage().addText(prompt));
    RequestUserMessageContent userMessageContent = new RequestUserMessageContent.InnerString(prompt);
    RequestUserMessage userMessage = new RequestUserMessage();
    userMessage.setContent(userMessageContent);
    // Set necessary fields on userMessageContent here if required
    request.addMessagesItem(userMessage);
    return chatCompletion(request);
  }

  @Nonnull
  public ConverseResponse chatCompletionWithPresetPromptConverse(@Nonnull final String prompt)
      throws ClaudeAiClientException {

    final ConverseRequest request = new ConverseRequest();
    if (systemPrompt != null) {
      request.addSystemItem(new SystemContentBlock().text(systemPrompt));
    }

    request.addMessagesItem(new ConverseRequestUserMessage().role(ConverseRequestUserMessage.RoleEnum.USER)
        .content(List.of(new ContentBlock().text(prompt))));
    return chatCompletion(request);
  }

  // /**
  // * Generate a completion for the given conversation and request parameters.
  // *
  // * @param request the completion request.
  // * @return the completion output
  // * @throws ClaudeAiClientException if the request fails
  // * @since 1.4.0
  // */
  // @Beta
  // @Nonnull
  // public ClaudeAiChatCompletionResponse chatCompletion(
  // @Nonnull final ClaudeAiChatCompletionRequest request) throws
  // ClaudeAiClientException {
  // // warnIfUnsupportedUsage();
  // return new ClaudeAiChatCompletionResponse(
  // chatCompletion(request.createCreateChatCompletionRequest()));
  // }

  /**
   * Generate a completion for the given low-level request object.
   *
   * @param request the completion request.
   * @return the completion output
   * @throws ClaudeAiClientException if the request fails
   * @since 1.4.0
   */
  @Beta
  @Nonnull
  public InvokeResponse chatCompletion(
      @Nonnull final InvokeRequest request) throws ClaudeAiClientException {
    // return execute("/chat/completions", request,
    // CreateChatCompletionResponse.class);
    return execute("/invoke", request, InvokeResponse.class);
  }

  @Beta
  @Nonnull
  public ConverseResponse chatCompletion(
      @Nonnull final ConverseRequest request) throws ClaudeAiClientException {
    // return execute("/chat/completions", request,
    // CreateChatCompletionResponse.class);
    return execute("/converse", request, ConverseResponse.class);
  }

  // /**
  // * Generate a completion for the given conversation and request parameters.
  // *
  // * @param parameters the completion request.
  // * @return the completion output
  // * @throws ClaudeAiClientException if the request fails
  // */
  // @Nonnull
  // public ClaudeAiChatCompletionOutput chatCompletion(
  // @Nonnull final ClaudeAiChatCompletionParameters parameters) throws
  // ClaudeAiClientException {
  // // warnIfUnsupportedUsage();
  // // return execute("/chat/completions", parameters,
  // ClaudeAiChatCompletionOutput.class);
  // return execute("/invoke", parameters, ClaudeAiChatCompletionOutput.class);
  // }

  // /**
  // * Stream a completion for the given string prompt as user.
  // *
  // * <p>Returns a <b>lazily</b> populated stream of text chunks. To access more
  // details about the
  // * individual chunks, use {@link
  // #streamChatCompletionDeltas(ClaudeAiChatCompletionRequest)}.
  // *
  // * <p>The stream should be consumed using a try-with-resources block to ensure
  // that the underlying
  // * HTTP connection is closed.
  // *
  // * <p>Example:
  // *
  // * <pre>{@code
  // * try (var stream = client.streamChatCompletion("...")) {
  // * stream.forEach(System.out::println);
  // * }
  // * }</pre>
  // *
  // * <p>Please keep in mind that using a terminal stream operation like {@link
  // Stream#forEach} will
  // * block until all chunks are consumed. Also, for obvious reasons, invoking
  // {@link
  // * Stream#parallel()} on this stream is not supported.
  // *
  // * @param prompt a text message.
  // * @return A stream of text chunks
  // * @throws ClaudeAiClientException if the request fails or if the finish
  // reason is content_filter
  // * @see #streamChatCompletionDeltas(ClaudeAiChatCompletionRequest)
  // */
  // @Nonnull
  // public Stream<String> streamChatCompletion(@Nonnull final String prompt)
  // throws ClaudeAiClientException {
  // final var userPrompt = ClaudeAiMessage.user(prompt);

  // final var request =
  // systemPrompt != null
  // ? new ClaudeAiChatCompletionRequest(ClaudeAiMessage.system(systemPrompt),
  // userPrompt)
  // : new ClaudeAiChatCompletionRequest(userPrompt);

  // return
  // streamChatCompletionDeltas(request.createCreateChatCompletionRequest())
  // .peek(ClaudeAiClient::throwOnContentFilter)
  // .map(ClaudeAiChatCompletionDelta::getDeltaContent);
  // }

  // private static void throwOnContentFilter(@Nonnull final
  // ClaudeAiChatCompletionDelta delta) {
  // final String finishReason = delta.getFinishReason();
  // if (finishReason != null && finishReason.equals("content_filter")) {
  // throw new ClaudeAiClientException("Content filter filtered the output.");
  // }
  // }

  // /**
  // * Stream a completion for the given conversation and request parameters.
  // *
  // * <p>Returns a <b>lazily</b> populated stream of delta objects. To simply
  // stream the text chunks
  // * use {@link #streamChatCompletion(String)}
  // *
  // * <p>The stream should be consumed using a try-with-resources block to ensure
  // that the underlying
  // * HTTP connection is closed.
  // *
  // * <p>Example:
  // *
  // * <pre>{@code
  // * try (var stream = client.streamChatCompletionDeltas(prompt)) {
  // * stream
  // * .peek(delta -> System.out.println(delta.getUsage()))
  // * .map(ClaudeAiChatCompletionDelta::getDeltaContent)
  // * .forEach(System.out::println);
  // * }
  // * }</pre>
  // *
  // * <p>Please keep in mind that using a terminal stream operation like {@link
  // Stream#forEach} will
  // * block until all chunks are consumed. Also, for obvious reasons, invoking
  // {@link
  // * Stream#parallel()} on this stream is not supported.
  // *
  // * @param request The prompt, including a list of messages.
  // * @return A stream of message deltas
  // * @throws ClaudeAiClientException if the request fails or if the finish
  // reason is content_filter
  // * @see #streamChatCompletion(String)
  // * @since 1.4.0
  // */
  // @Beta
  // @Nonnull
  // public Stream<ClaudeAiChatCompletionDelta> streamChatCompletionDeltas(
  // @Nonnull final ClaudeAiChatCompletionRequest request) throws
  // ClaudeAiClientException {
  // return
  // streamChatCompletionDeltas(request.createCreateChatCompletionRequest());
  // }

  // /**
  // * Stream a completion for the given low-level request object. Returns a
  // <b>lazily</b> populated
  // * stream of delta objects.
  // *
  // * @param request The completion request.
  // * @return A stream of message deltas
  // * @throws ClaudeAiClientException if the request fails or if the finish
  // reason is content_filter
  // * @see #streamChatCompletionDeltas(ClaudeAiChatCompletionRequest) for a
  // higher-level API
  // * @since 1.4.0
  // */
  // @Beta
  // @Nonnull
  // public Stream<ClaudeAiChatCompletionDelta> streamChatCompletionDeltas(
  // @Nonnull final CreateChatCompletionRequest request) throws
  // ClaudeAiClientException {
  // request.stream(true).streamOptions(new
  // ChatCompletionStreamOptions().includeUsage(true));
  // // return executeStream("/chat/completions", request,
  // ClaudeAiChatCompletionDelta.class);
  // return executeStream("/invoke", request, ClaudeAiChatCompletionDelta.class);
  // }

  // /**
  // * Stream a completion for the given conversation and request parameters.
  // *
  // * <p>Returns a <b>lazily</b> populated stream of delta objects. To simply
  // stream the text chunks
  // * use {@link #streamChatCompletion(String)}
  // *
  // * <p>The stream should be consumed using a try-with-resources block to ensure
  // that the underlying
  // * HTTP connection is closed.
  // *
  // * <p>Example:
  // *
  // * <pre>{@code
  // * try (var stream = client.streamChatCompletionDeltas(request)) {
  // * stream
  // * .peek(delta -> System.out.println(delta.getUsage()))
  // *
  // .map(customer.aireport.service.SAPAICore.claude.model.ClaudeAiChatCompletionDelta::getDeltaContent)
  // * .forEach(System.out::println);
  // * }
  // * }</pre>
  // *
  // * <p>Please keep in mind that using a terminal stream operation like {@link
  // Stream#forEach} will
  // * block until all chunks are consumed. Also, for obvious reasons, invoking
  // {@link
  // * Stream#parallel()} on this stream is not supported.
  // *
  // * @param parameters The prompt, including a list of messages.
  // * @return A stream of message deltas
  // * @throws ClaudeAiClientException if the request fails or if the finish
  // reason is content_filter
  // */
  // @Nonnull
  // public
  // Stream<customer.aireport.service.SAPAICore.claude.model.ClaudeAiChatCompletionDelta>
  // streamChatCompletionDeltas(@Nonnull final ClaudeAiChatCompletionParameters
  // parameters)
  // throws ClaudeAiClientException {
  // // warnIfUnsupportedUsage();
  // parameters.enableStreaming();
  // return executeStream(
  // // "/chat/completions",
  // "/invoke",
  // parameters,
  // customer.aireport.service.SAPAICore.claude.model.ClaudeAiChatCompletionDelta.class);
  // }

  private void warnIfUnsupportedUsage() {
    if (systemPrompt != null) {
      log.warn(
          "Previously set messages will be ignored, set it as an argument of this method instead.");
    }
  }

  @Beta
  @Nonnull
  public Stream<ClaudeAIChatCompletionDelta> streamChatCompletionDeltas(
      @Nonnull final ConverseRequest request) throws ClaudeAiClientException {
    return executeStream("/converse-stream", request, ClaudeAIChatCompletionDelta.class);
  }

  // /**
  // * Get a vector representation of a given request that can be easily consumed
  // by machine learning
  // * models and algorithms using high-level request object.
  // *
  // * @param request the request with input text.
  // * @return the embedding response convenience object
  // * @throws ClaudeAiClientException if the request fails
  // * @see #embedding(EmbeddingsCreateRequest) for full confgurability.
  // * @since 1.4.0
  // */
  // @Beta
  // @Nonnull
  // public ClaudeAiEmbeddingResponse embedding(@Nonnull final
  // ClaudeAiEmbeddingRequest request)
  // throws ClaudeAiClientException {
  // return new
  // ClaudeAiEmbeddingResponse(embedding(request.createEmbeddingsCreateRequest()));
  // }

  // /**
  // * Get a vector representation of a given inputs using low-level request.
  // *
  // * @param request the request with input text.
  // * @return the embedding output
  // * @throws ClaudeAiClientException if the request fails
  // * @see #embedding(ClaudeAiEmbeddingRequest) for conveninece api
  // * @since 1.4.0
  // */
  // @Beta
  // @Nonnull
  // public EmbeddingsCreate200Response embedding(@Nonnull final
  // EmbeddingsCreateRequest request)
  // throws ClaudeAiClientException {
  // return execute("/embeddings", request, EmbeddingsCreate200Response.class);
  // }

  // /**
  // * Get a vector representation of a given input that can be easily consumed by
  // machine learning
  // * models and algorithms.
  // *
  // * @param parameters the input text.
  // * @return the embedding output
  // * @throws ClaudeAiClientException if the request fails
  // */
  // @Nonnull
  // public ClaudeAiEmbeddingOutput embedding(@Nonnull final
  // ClaudeAiEmbeddingParameters parameters)
  // throws ClaudeAiClientException {
  // return execute("/embeddings", parameters, ClaudeAiEmbeddingOutput.class);
  // }

  @Nonnull
  private <T> T execute(
      @Nonnull final String path,
      @Nonnull final Object payload,
      @Nonnull final Class<T> responseType) {
    final var request = new HttpPost(path);
    serializeAndSetHttpEntity(request, payload);
    return executeRequest(request, responseType);
  }


  private void serializeAndSetHttpEntity(
      @Nonnull final BasicClassicHttpRequest request, @Nonnull final Object payload) {
    try {
      final var json = JACKSON.writeValueAsString(payload);
      // Log the payload
      log.info("Claude AI Request Payload: {}", json);

      request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
    } catch (final JsonProcessingException e) {
      throw new ClaudeAiClientException("Failed to serialize request parameters", e);
    }
  }

  @Nonnull
  private <T> T executeRequest(
      final BasicClassicHttpRequest request, @Nonnull final Class<T> responseType) {
    try {
      final var client = ApacheHttpClient5Accessor.getHttpClient(destination);
      return client.execute(
          request,
          new ClientResponseHandler<>(responseType, ClaudeAiError.class, ClaudeAiClientException::new));
    } catch (final IOException e) {
      throw new ClaudeAiClientException("Request to Claude AI model failed", e);
    }
  }

  @Nonnull
  private <D extends StreamedDelta> Stream<D> executeStream(
      @Nonnull final String path,
      @Nonnull final Object payload,
      @Nonnull final Class<D> deltaType) {
    final var request = new HttpPost(path);
    serializeAndSetHttpEntity(request, payload);
    return streamRequest(request, deltaType);
  }

  @Nonnull
  private <D extends StreamedDelta> Stream<D> streamRequest(
      final BasicClassicHttpRequest request, @Nonnull final Class<D> deltaType) {
        JACKSON.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    try {
      final var client = ApacheHttpClient5Accessor.getHttpClient(destination);
      return new ClientStreamingHandler<>(deltaType, ClaudeAiError.class,
          ClaudeAiClientException::new)
          .objectMapper(JACKSON)
          .handleStreamingResponse(client.executeOpen(null, request, null));
    } catch (final IOException e) {
      throw new ClaudeAiClientException("Request to Claude AI model failed", e);
    }
  }
}

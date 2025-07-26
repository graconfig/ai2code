package customer.ai2code.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.config.model.AIModel;
import customer.ai2code.model.ai.config.service.DeepseekServiceConfig;
import customer.ai2code.model.execution.functioncall.FunctionInfo;
import customer.ai2code.model.ai.response.AIResponse;
import customer.ai2code.model.aicore.deepseek.DeepseekChatRequest;
import customer.ai2code.model.aicore.deepseek.DeepseekChatResponse;
import customer.ai2code.model.aicore.deepseek.DeepseekClient;
import customer.ai2code.model.aicore.deepseek.DeepseekMessage;
import customer.ai2code.service.AIService;
import customer.ai2code.service.constant.AIConstants;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.execution.functioncall.FunctionCallProcessor;
import customer.ai2code.service.execution.functioncall.adapter.OpenAIFunctionCallAdapter;
import customer.ai2code.service.handler.factory.AIResponseHandlerFactory;
import customer.ai2code.service.processor.StreamingCompletedProcessor;

/**
 * Deepseek服务实现类（参考SAPOpenAIServiceImpl风格，包含函数调用）
 */
@Service
public class DeepseekAIServiceImpl implements AIService {

    private final FunctionCallProcessor functionCallProcessor;
    private final OpenAIFunctionCallAdapter functionCallAdapter;
    private final AIResponseHandlerFactory responseHandlerFactory;
    private final ObjectMapper objectMapper;

    // 构造函数注入依赖（与参考类保持一致）
    public DeepseekAIServiceImpl(FunctionCallProcessor functionCallProcessor,
                                 OpenAIFunctionCallAdapter functionCallAdapter,
                                 AIResponseHandlerFactory responseHandlerFactory,
                                 ObjectMapper objectMapper) {
        this.functionCallProcessor = functionCallProcessor;
        this.functionCallAdapter = functionCallAdapter;
        this.responseHandlerFactory = responseHandlerFactory;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建Deepseek客户端（参考getAiClientbyModelUsingBTPDestination逻辑）
     */
    private DeepseekClient getDeepseekClient(@Nonnull AIModel model) {
        DeepseekServiceConfig config = (DeepseekServiceConfig) model.parseModelConfigs();
        if (!config.isValid()) {
            throw new BusinessException("Deepseek配置无效，请检查API地址和密钥");
        }
        return new DeepseekClient(config.getApiUrl(), config.getApiKey());
    }

    /**
     * 同步聊天接口（参考SAPOpenAIServiceImpl.chatWithAI）
     */
    @Override
    public String chatWithAI(List<BotMessages> messages, List<PromptTexts> prompts,
                            String content, AIModel model) {
        // 构建请求参数
        DeepseekChatRequest request = buildChatRequest(messages, prompts, content, false, model);
        
        // 调用API
        DeepseekClient client = getDeepseekClient(model);
        DeepseekChatResponse response = client.chatCompletion(request);
        
        // 处理响应（复用响应处理器）
        AIResponse aiResponse = responseHandlerFactory.getHandler(AIConstants.AIServiceType.DEEPSEEK)
                .processResponse(response);
        return aiResponse.getContent();
    }

    /**
     * 流式聊天接口（参考SAPOpenAIServiceImpl.chatWithAIStreaming）
     */
    @Override
    public SseEmitter chatWithAIStreaming(List<BotMessages> messages, List<PromptTexts> prompts,
                                         String content, AIModel model, ExecutorService executor,
                                         StreamingCompletedProcessor processor) {
        // 构建流式请求
        DeepseekChatRequest request = buildChatRequest(messages, prompts, content, true, model);
        DeepseekClient client = getDeepseekClient(model);

        SseEmitter emitter = new SseEmitter(20 * 60 * 1000L); // 30分钟超时
        final StringBuilder fullResponse = new StringBuilder();

        executor.execute(() -> {
            try {
                // 处理流式响应
                client.streamChatCompletion(request)
                        .subscribe(
                                chunk -> {
                                    AIService.send(emitter, chunk); // 复用静态发送方法
                                    fullResponse.append(chunk);
                                },
                                error -> emitter.completeWithError(error),
                                () -> {
                                    if (processor != null) {
                                        processor.process(fullResponse.toString());
                                    }
                                    emitter.complete();
                                }
                        );
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 函数调用接口（参考SAPOpenAIServiceImpl.functionCalling）
     */
    @Override
    public <T extends BotExecution> Object functionCalling(List<BotMessages> messages, List<PromptTexts> prompts,
                                                          T botExecutionInstance, AIModel model) {
        // 1. 提取函数信息
        List<FunctionInfo> functionInfos = functionCallProcessor
                .extractFunctionInfosFromInstance(botExecutionInstance);
        if (functionInfos.isEmpty()) {
            throw new BusinessException("未在Bot实例中找到可执行方法: " + botExecutionInstance.getClass().getSimpleName());
        }

        // 2. 构建包含函数调用的请求参数
        DeepseekChatRequest request = buildFunctionCallRequest(messages, prompts, functionInfos, model);

        // 3. 调用Deepseek API
        DeepseekClient client = getDeepseekClient(model);
        DeepseekChatResponse response = client.chatCompletion(request);

        // 4. 处理响应
        AIResponse aiResponse = responseHandlerFactory.getHandler(AIConstants.AIServiceType.DEEPSEEK)
                .processResponse(response);

        // 5. 执行函数调用（若存在）
        if (containsFunctionCall(response)) {
            try {
                return executeFunctionCall(response, botExecutionInstance);
            } catch (Exception e) {
                throw new BusinessException("函数调用执行失败: " + e.getMessage(), e);
            }
        }

        return aiResponse.getContent();
    }

    /**
     * 构建普通聊天请求（参考参数组装逻辑）
     */
    private DeepseekChatRequest buildChatRequest(List<BotMessages> messages, List<PromptTexts> prompts,
                                                String content, boolean isStream, AIModel model) {
        DeepseekServiceConfig config = (DeepseekServiceConfig) model.parseModelConfigs();
        DeepseekChatRequest request = new DeepseekChatRequest();

        // 设置基础参数
        request.setModel(model.getModelName());
        request.setTemperature(config.getTemperature());
        request.setMax_tokens(config.getMaxTokens());
        request.setStream(isStream);
        request.setEnable_thinking(config.getEnableThinking());

        // 组装消息列表
        List<DeepseekMessage> msgList = new ArrayList<>();
        
        // 添加系统提示（合并为单个system消息）
        StringBuilder promptContent = new StringBuilder();
        prompts.forEach(prompt -> promptContent.append(prompt.getContent()).append("\n"));
        if (promptContent.length() > 0) {
            msgList.add(new DeepseekMessage("system", promptContent.toString().trim()));
        }

        // 添加历史消息
        messages.forEach(msg -> {
            switch (msg.getRole()) {
                case AIConstants.Roles.SYSTEM -> msgList.add(new DeepseekMessage("system", msg.getMessage()));
                case AIConstants.Roles.USER -> msgList.add(new DeepseekMessage("user", msg.getMessage()));
                case AIConstants.Roles.ASSISTANT -> msgList.add(new DeepseekMessage("assistant", msg.getMessage()));
                default -> throw new BusinessException(AIConstants.Messages.UNEXPECTED_ROLE + msg.getRole());
            }
        });

        // 添加当前用户输入
        if (content != null && !content.isBlank()) {
            msgList.add(new DeepseekMessage("user", content));
        }

        request.setMessages(msgList);
        return request;
    }

    /**
     * 构建函数调用请求（参考OpenAI函数调用参数格式）
     */
    private DeepseekChatRequest buildFunctionCallRequest(List<BotMessages> messages, List<PromptTexts> prompts,
                                                       List<FunctionInfo> functionInfos, AIModel model) {
        // 复用普通请求构建逻辑
        DeepseekChatRequest request = buildChatRequest(messages, prompts, null, false, model);
        
        // 转换函数信息为Deepseek工具调用格式
        List<Map<String, Object>> functions = functionCallAdapter.convertToOpenAIFormat(functionInfos);
        
        // 添加函数调用提示（Deepseek通过system消息指定函数调用格式）
        String functionPrompt = "\n可调用的工具函数: " + functions.toString() + 
                               "\n若需要调用工具，请返回JSON格式: {\"name\":\"函数名\",\"parameters\":{...}}";
        
        // 追加到系统消息
        DeepseekMessage systemMsg = request.getMessages().stream()
                .filter(m -> "system".equals(m.getRole()))
                .findFirst()
                .orElse(new DeepseekMessage("system", ""));
        systemMsg.setContent(systemMsg.getContent() + functionPrompt);

        return request;
    }

    /**
     * 检查响应中是否包含函数调用
     */
    private boolean containsFunctionCall(DeepseekChatResponse response) {
        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            return false;
        }
        String content = response.getChoices().get(0).getMessage().getContent();
        return content.contains("\"name\"") && content.contains("\"parameters\"");
    }

    /**
     * 执行函数调用（参考OpenAI的执行逻辑）
     */
    private <T extends BotExecution> Object executeFunctionCall(DeepseekChatResponse response,
                                                              T botExecutionInstance) throws Exception {
        String content = response.getChoices().get(0).getMessage().getContent();
        
        // 解析函数调用信息（Deepseek返回的是JSON字符串）
        Map<String, Object> functionCall = objectMapper.readValue(
                content, new TypeReference<Map<String, Object>>() {});
        
        String functionName = (String) functionCall.get("name");
        String argumentsJson = objectMapper.writeValueAsString(functionCall.get("parameters"));
        
        // 执行函数
        return functionCallProcessor.executeFunctionCallOnInstance(
                functionName, argumentsJson, botExecutionInstance);
    }

    /**
     * 解析Deepseek模型名称（参考resolveOpenAiModel）
     */
    private String resolveDeepseekModel(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return "deepseek-chat"; // 默认模型
        }
        
        return switch (modelName.toLowerCase()) {
            case "deepseek-chat", "chat" -> "deepseek-chat";
            case "deepseek-coder", "coder" -> "deepseek-coder";
            case "deepseek-r1", "r1" -> "deepseek-r1";
            default -> {
                System.out.println("未知模型名: " + modelName + ", 使用默认模型 deepseek-chat");
                yield "deepseek-chat";
            }
        };
    }
}
    
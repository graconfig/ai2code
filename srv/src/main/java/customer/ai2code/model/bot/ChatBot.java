package customer.ai2code.model.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.configservice.BotTypes;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotMessages;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.config.AIModelResolver;
import customer.ai2code.model.ai.config.model.AIModel;
import customer.ai2code.service.AIService;
import customer.ai2code.service.PromptService;
import customer.ai2code.service.constant.AIConstants;
import customer.ai2code.service.impl.GenericCqnService;
// import customer.ai2code.service.impl.rag.RAGExtractionFactoryService;
// import customer.ai2code.service.rag.RAGExtraction;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatBot extends AbstractBot {

    // 服务依赖（通过构造函数注入）
    private PromptService promptService;
    private AIModelResolver aiModelResolver;
    // private RAGExtractionFactoryService ragExtractionFactoryService;

    public ChatBot(BotInstances botInstance, AIModel aiModel, BotTypes botType,
            Locale locale, GenericCqnService genericCqnService,
            PromptService promptService, AIModelResolver aiModelResolver) {
        super(botInstance, aiModel, botType, locale, genericCqnService);
        this.promptService = promptService;
        this.aiModelResolver = aiModelResolver;
    }

    @Override
    public String chat(String content) {
        List<PromptTexts> prompts = new ArrayList<>();
        try {
            // 1. 根据AIModel类型，获取到不同AIService服务
            AIService aiService = aiModelResolver.resolveAIService(aiModel.getModelConfigs());

            // 获取prompts
            List<PromptTexts> retrievedPrompts = promptService.getPrompts(this);
            if (retrievedPrompts != null && !retrievedPrompts.isEmpty()) {
                prompts = new ArrayList<>(retrievedPrompts); // 创建可修改的副本
            }

            // 2. 第一次chat需要保存prompt消息
            // boolean isFirstCall = genericCqnService.isFirstCall(botInstance.getId());
            // if (isFirstCall) {
            // // 3. 使用genericCqnService.getMainTaskId，再获取Prompt
            // // String mainTaskId = genericCqnService.getMainTaskId(botInstance.getId());
            // savePromptMessages(prompts);

            // }

            // 4. 获取历史消息
            List<BotMessages> historyMessages = genericCqnService.getBotMessagesByBotInstanceId(botInstance.getId());

            // String ragContent = ;
            PromptTexts ragPrompt = promptService.getRagAsPrompts(this, content);

            // 6.将用户的聊天内容存储到表中
            genericCqnService.createAndInsertBotMessage(botInstance.getId(), content, ragPrompt.getContent(),
                    AIConstants.Roles.USER);

            if (ragPrompt != null && ragPrompt.getContent() != null && !ragPrompt.getContent().isEmpty()) {
                prompts.add(ragPrompt);
            }

            // 7. 真正调用chat服务
            String response = aiService.chatWithAI(historyMessages, prompts, content, aiModel);

            return response;

        } catch (Exception e) {
            System.err.println("Chat failed for bot: " + botInstance.getId() + ", error: " + e.getMessage());
            throw new BusinessException("Chat failed", e);
        }
    }

    @Override
    public SseEmitter chatInStreaming(String content) {
        List<PromptTexts> prompts = new ArrayList<>();
        try {
            // 1. 根据AIModel类型，获取到不同AIService服务
            AIService aiService = aiModelResolver.resolveAIService(aiModel.getModelConfigs());

            // 获取prompts
            List<PromptTexts> retrievedPrompts = promptService.getPrompts(this);
            if (retrievedPrompts != null && !retrievedPrompts.isEmpty()) {
                prompts = new ArrayList<>(retrievedPrompts); // 创建可修改的副本
            }

            // // 3. 第一次调用需要保存prompt消息
            // boolean isFirstCall = genericCqnService.isFirstCall(botInstance.getId());
            // if (isFirstCall && prompts != null && !prompts.isEmpty()) {
            // savePromptMessages(prompts);
            // }

            // 4. 获取历史消息
            List<BotMessages> historyMessages = genericCqnService.getBotMessagesByBotInstanceId(botInstance.getId());

            // String ragContent = ;
            PromptTexts ragPrompt = promptService.getRagAsPrompts(this, content);

            // 6.将用户的聊天内容存储到表中
            BotMessages userMessage = genericCqnService.createAndInsertBotMessage(botInstance.getId(), content,
                    ragPrompt.getContent(), AIConstants.Roles.USER);

            if (ragPrompt != null && ragPrompt.getContent() != null && !ragPrompt.getContent().isEmpty()) {
                prompts.add(ragPrompt);
            }

            // 创建final变量供lambda使用
            final List<PromptTexts> finalPrompts = prompts;

            // 7. 调用流式聊天服务
            // return aiService.chatWithAIStreaming(historyMessages, prompts, content,
            // aiModel,
            // Executors.newCachedThreadPool(), // executor - 可以后续添加
            // null // streamingCompletionProcessor - 可以后续添加
            // );
            SseEmitter emitter = new SseEmitter(20 * 60 * 1000L); // 设置超时时间为20分钟

            final ExecutorService executor = Executors.newCachedThreadPool();
            final StringBuilder responseBuilder = new StringBuilder();

            executor.execute(() -> {
                try {
                    aiService.chatWithAIStreaming(historyMessages, finalPrompts, content, aiModel)
                            .forEach(delta -> {
                                // 发送每个delta到SSE emitter
                                responseBuilder.append(delta);
                                AIService.send(emitter, delta);
                            });
                    // 完成后关闭emitter
                    // emitter.complete();
                } catch (Exception e) {
                    System.err.println(
                            "Streaming chat failed for bot: " + botInstance.getId() + ", error: " + e.getMessage());
                    emitter.completeWithError(e);
                } finally {
                    BotMessages assistantMessage = genericCqnService.createAndInsertBotMessage(botInstance.getId(),
                            responseBuilder.toString(), AIConstants.Roles.ASSISTANT);
                    // 再以SSE的方式发送BotMessages
                    AIService.send(emitter, userMessage.toJson());

                    AIService.send(emitter, assistantMessage.toJson());

                    // emitter.send(responseBuilder.toString());
                    emitter.complete();
                    executor.shutdown();
                }

            });
            return emitter;

        } catch (Exception e) {
            System.err.println("Streaming chat failed for bot: " + botInstance.getId() + ", error: " + e.getMessage());
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }

    // private

    /**
     * 保存Prompt消息到BotMessages表
     */
    private void savePromptMessages(List<PromptTexts> prompts) {
        for (PromptTexts prompt : prompts) {
            if (prompt.getContent() != null && !prompt.getContent().isEmpty()) {
                // 将Prompt作为system消息保存
                genericCqnService.createAndInsertBotMessage(botInstance.getId(), prompt.getContent(), "system");
            }
        }
    }

    @Override
    public BotInstancesExecuteContext.ReturnType execute() {
        // 实现聊天机器人的执行逻辑
        return null; // 返回执行结果
    }

    @Override
    public Boolean executeAsync() {
        // 实现异步执行逻辑
        return true; // 返回是否成功
    }

    @Override
    public Boolean stop() {
        // 实现停止逻辑
        return true; // 返回是否成功
    }

    @Override
    public Boolean resume() {
        // 实现恢复逻辑
        return true; // 返回是否成功
    }

    @Override
    public Boolean cancel() {
        // 实现取消逻辑
        return true; // 返回是否成功
    }

    // 使用AbstractBot中的实现，不需要在这里重写
    // @Override
    // public BotInstances getBotInstance() {
    // return botInstance;
    // }

    // @Override
    // public AIModel getAiModel() {
    // return aiModel;
    // }

}

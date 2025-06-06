package customer.ai2code.service.impl;

import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.MainService;
import cds.gen.configservice.ConfigService;
import cds.gen.configservice.BotTypes;
import cds.gen.configservice.BotTypes_;
import customer.ai2code.model.AIModel;
import customer.ai2code.model.Bot;
import customer.ai2code.model.ChatBot;
import customer.ai2code.model.FunctionCallingBot;
import customer.ai2code.model.config.AIModelResolver;
import customer.ai2code.model.CodingBot;
import customer.ai2code.service.BotService;
import customer.ai2code.service.ContextService;

// import customer.ai2code.service.AIService;
import com.sap.cds.ql.Select;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import cds.gen.mainservice.BotMessages;

@Service
public class BotServiceImpl implements BotService {

    private final AIModelResolver aiModelResolver;
    private final GenericCqnService genericCqnService;
    // private final AIService aiService;

    // 全局Bot缓存链表
    private final Map<String, Bot> botCache = new ConcurrentHashMap<>();

    public BotServiceImpl(
            AIModelResolver aiModelResolver,
            GenericCqnService genericCqnService) {
        // AIService aiService) {
        this.aiModelResolver = aiModelResolver;
        this.genericCqnService = genericCqnService;
        // this.aiService = aiService;
    }

    @Override
    public Bot getCurrentBot(String botInstanceId) {
        // 先从缓存中查找
        Bot cachedBot = botCache.get(botInstanceId);
        if (cachedBot != null) {
            return cachedBot;
        }

        // 从数据库查询BotInstance
        BotInstances botInstance = genericCqnService.getBotInstanceById(botInstanceId);

        // 查询关联的BotType
        BotTypes botType = genericCqnService.getBotTypeById(botInstance.getTypeId());

        // 获取AI模型
        AIModel aiModel = aiModelResolver.resolveAIModel(botType.getModelId());

        // 根据BotType的functionType创建相应的Bot实例
        Bot bot = createBotInstance(botInstance, botType, aiModel);

        // 放入缓存
        botCache.put(botInstanceId, bot);

        return bot;
    }

    @Override
    public Bot getCurrentBot(String taskId, int sequence) {
        // 根据taskId和sequence查询BotInstance
        BotInstances botInstance = genericCqnService.getBotInstanceByTaskAndSequence(taskId, sequence);
        return getCurrentBot(botInstance.getId());
    }

    @Override
    public String chat(BotInstancesChatCompletionContext context) {
        // 从context的CQN中获取ID
        String botInstanceId = extractIdFromContext(context);

        // context.setResult();
        return chat(botInstanceId, context.getContent());
    }

    @Override
    public String chat(String botInstanceId, String content) {

        Bot bot = getCurrentBot(botInstanceId);

        // 更新状态为RUNNING
        updateBotInstanceStatus(bot, "R");

        try {

            // 2. 判断是否是第一次调用
            // 2.1 第一次调用 获取Prompt
            // 2.2 如果不是第一次调用，获取历史消息

            String response;
            if (bot instanceof ChatBot) {
                response = ((ChatBot) bot).chat(content);
            } else {
                throw new IllegalStateException("Bot is not a ChatBot: " + botInstanceId);
            }

            // 更新状态为SUCCESS
            // updateBotInstanceStatus(bot, "S");

            // 3. 将用户和AI的聊天内容存储到表中

            // 将content和response更新到BotMessages中

            return response;

        } catch (Exception e) {
            // 更新状态为FAILED
            updateBotInstanceStatus(bot, "F");
            throw new RuntimeException("Chat failed for bot: " + botInstanceId, e);
        }
    }

    @Override
    public SseEmitter chatInStreaming(String botInstanceId, String content) {
        Bot bot = getCurrentBot(botInstanceId);

        if (bot instanceof ChatBot) {
            return ((ChatBot) bot).chatInStreaming(content);
        } else {
            throw new IllegalStateException("Bot is not a ChatBot: " + botInstanceId);
        }
    }

    @Override
    public Boolean executeAsync(BotInstancesExecuteContext context) {
        String botInstanceId = extractIdFromContext(context);
        return executeAsync(botInstanceId);
    }

    @Override
    public Boolean executeAsync(String botInstanceId) {
        Bot bot = getCurrentBot(botInstanceId);
        return bot.executeAsync();
    }

    @Override
    public BotInstancesExecuteContext.ReturnType execute(BotInstancesExecuteContext context) {
        String botInstanceId = extractIdFromContext(context);
        return execute(botInstanceId);
    }

    @Override
    public BotInstancesExecuteContext.ReturnType execute(String botInstanceId) {
        Bot bot = getCurrentBot(botInstanceId);

        // 更新状态为RUNNING
        updateBotInstanceStatus(bot, "R");

        try {
            BotInstancesExecuteContext.ReturnType result = bot.execute();

            // 更新状态为SUCCESS
            updateBotInstanceStatus(bot, "S");

            // 更新result字段
            updateBotInstanceResult(bot, result.getResult());

            return result;

        } catch (Exception e) {
            // 更新状态为FAILED
            updateBotInstanceStatus(bot, "F");
            throw new RuntimeException("Execution failed for bot: " + botInstanceId, e);
        }
    }

    private Bot createBotInstance(BotInstances botInstance, BotTypes botType, AIModel aiModel) {
        String functionTypeCode = botType.getFunctionTypeCode();

        switch (functionTypeCode) {
            case "A": // AI Chat Bot
                return new ChatBot(botInstance, aiModel, botType);
            case "F": // Function Calling Bot
                return new FunctionCallingBot(botInstance, aiModel, botType);
            case "C": // Coding Bot
                return new CodingBot(botInstance, aiModel, botType);
            default:
                throw new IllegalArgumentException("Unsupported bot function type: " + functionTypeCode);
        }
    }

    private void updateBotInstanceStatus(Bot bot, String status) {
        BotInstances botInstance = bot.getBotInstance();
        botInstance.setStatusCode(status);
        genericCqnService.updateBotInstance(botInstance);
    }

    private void updateBotInstanceResult(Bot bot, String result) {
        BotInstances botInstance = bot.getBotInstance();
        botInstance.setResult(result);
        genericCqnService.updateBotInstance(botInstance);
    }

    private String extractIdFromContext(BotInstancesChatCompletionContext context) {
        // 从CQN查询中提取ID，需要解析CqnSelect
        return context.getCqn().ref().segments().get(0).id();
    }

    private String extractIdFromContext(BotInstancesExecuteContext context) {
        // 从CQN查询中提取ID，需要解析CqnSelect
        return context.getCqn().ref().segments().get(0).id();
    }

    @Override
    public void adopt(BotMessagesAdoptContext context) {

        // 1.通过上下文context获取 MessageId,botInstanceId
        String messageId = extractMessageIdFromContext(context);
        String botInstanceId = context.getCqn().ref().segments().get(0).id();

        // 2.执行实际的采纳操作
        adopt(botInstanceId, messageId);

        // 3.标记操作完成
        context.setCompleted();
    }

    @Override
    public void adopt(String botInstanceId, String messageId) {

        // 1. 获取Bot实例
        Bot bot = getCurrentBot(botInstanceId);
        BotInstances botInstance = bot.getBotInstance();

        // 2. 查找消息
        BotMessages message = botInstance.getMessages().stream()
                .filter(m -> messageId.equals(m.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // 3. 简单标记消息为已采纳
        message.setRagData("ADOPTED");

        // 4. 更新Bot实例(持久化变更)
        genericCqnService.updateBotInstance(botInstance);

    }

    private String extractMessageIdFromContext(BotMessagesAdoptContext context) {
        // 从CQN查询中提取消息ID
        return context.getCqn().ref().segments().get(0).id();
    }
}
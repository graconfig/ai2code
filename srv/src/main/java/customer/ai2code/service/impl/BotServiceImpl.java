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
// import customer.ai2code.service.AIService;
import com.sap.cds.ql.Select;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class BotServiceImpl implements BotService {

    private final MainService mainService;
    private final ConfigService configService;
    private final EntityService entityService;
    private final AIModelResolver aiModelResolver;
    // private final AIService aiService;

    // 全局Bot缓存链表
    private final Map<String, Bot> botCache = new ConcurrentHashMap<>();

    public BotServiceImpl(MainService mainService,
            ConfigService configService,
            EntityService entityService,
            AIModelResolver aiModelResolver) {
        // AIService aiService) {
        this.mainService = mainService;
        this.configService = configService;
        this.entityService = entityService;
        this.aiModelResolver = aiModelResolver;
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
        var select = Select.from(BotInstances_.class).where(b -> b.ID().eq(botInstanceId));
        BotInstances botInstance = entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found: " + botInstanceId);

        // 查询关联的BotType
        var botTypeSelect = Select.from(BotTypes_.class).where(b -> b.ID().eq(botInstance.getTypeId()));
        BotTypes botType = entityService.selectSingle(configService, botTypeSelect, BotTypes.class,
                "BotType not found: " + botInstance.getTypeId());

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
        var select = Select.from(BotInstances_.class)
                .where(b -> b.task_ID().eq(taskId).and(b.sequence().eq(sequence)));
        BotInstances botInstance = entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found for task: " + taskId + ", sequence: " + sequence);

        return getCurrentBot(botInstance.getId());
    }

    @Override
    public String chat(BotInstancesChatCompletionContext context) {
        // 从context的CQN中获取ID
        String botInstanceId = extractIdFromContext(context);
        return chat(botInstanceId, context.getContent());
    }

    @Override
    public String chat(String botInstanceId, String content) {
        Bot bot = getCurrentBot(botInstanceId);

        // 更新状态为RUNNING
        updateBotInstanceStatus(bot, "R");

        try {
            String response;
            if (bot instanceof ChatBot) {
                response = ((ChatBot) bot).chat(content);
            } else {
                throw new IllegalStateException("Bot is not a ChatBot: " + botInstanceId);
            }

            // 更新状态为SUCCESS
            // updateBotInstanceStatus(bot, "S");

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
        entityService.update(mainService, null, BotInstances_.class, botInstance, true);
    }

    private void updateBotInstanceResult(Bot bot, String result) {
        BotInstances botInstance = bot.getBotInstance();
        botInstance.setResult(result);
        entityService.update(mainService, null, BotInstances_.class, botInstance, true);
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'adopt'");
    }

    @Override
    public void adopt(String botInstanceId, String messageId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'adopt'");
    }
}
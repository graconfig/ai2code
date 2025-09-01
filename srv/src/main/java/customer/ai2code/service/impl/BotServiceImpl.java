package customer.ai2code.service.impl;

import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotInstancesChatCompletionContext;

// import cds.gen.configservice.BotTypes;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.config.AIModelResolver;
import customer.ai2code.model.bot.Bot;
import customer.ai2code.model.bot.ChatBot;
import customer.ai2code.model.tree.TaskBotNode;
import customer.ai2code.service.BotService;
import customer.ai2code.service.ContextService;

import customer.ai2code.service.PromptService;
import customer.ai2code.service.websocket.BotInstanceStatusChangedEvent;

import com.sap.cds.ql.cqn.AnalysisResult;
import com.sap.cds.ql.cqn.CqnAnalyzer;
// 
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// import java.util.Locale;

@Service
public class BotServiceImpl implements BotService {

    // private final AIModelResolver aiModelResolver;
    private final GenericCqnService genericCqnService;
    private final TaskBotCacheManager cacheManager;
    // private final PromptService promptService;
    private final ContextService contextService;
    // private final BotExecutionFactoryService botExecutionFactoryService;
    // 全局Bot缓存链表 - 保留作为备用，主要使用TaskBotCacheManager
    // private final Map<String, Bot> botCache = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;

    public BotServiceImpl(
            AIModelResolver aiModelResolver,
            GenericCqnService genericCqnService,
            TaskBotCacheManager cacheManager,
            PromptService promptService,
            ContextService contextService,
            BotExecutionFactoryService botExecutionFactoryService,
            ApplicationEventPublisher eventPublisher) {
        // this.aiModelResolver = aiModelResolver;
        this.genericCqnService = genericCqnService;
        this.cacheManager = cacheManager;
        // this.promptService = promptService;
        this.contextService = contextService;
        // this.botExecutionFactoryService = botExecutionFactoryService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Bot getCurrentBot(String botInstanceId) {
        // 先从缓存中查找
        Bot cachedBot = cacheManager.getCachedBot(botInstanceId);
        // if (cachedBot != null) {
        //     return cachedBot;
        // }
        return cachedBot;

        // // 从数据库查询BotInstance
        // BotInstances botInstance =
        // genericCqnService.getBotInstanceAndSubtasksById(botInstanceId);
        // // BotInstances botInstance =
        // genericCqnService.getBotInstanceById(botInstanceId);

        // // 查询关联的BotType
        // BotTypes botType = genericCqnService.getBotTypeById(botInstance.getTypeId());

        // // 获取AI模型
        // // AIModel aiModel = aiModelResolver.resolveAIModel(botType.getModelId());

        // // 根据BotType的functionType创建相应的Bot实例
        // Bot bot = cacheManager.createBotInstance(botInstance, botType);

        // // 放入缓存 - 使用新的缓存管理器
        // cacheManager.addBotInstanceNode(bot);

        // return bot;
    }

    @Override
    public Bot getCurrentBot(String taskId, int sequence) {
        // 使用缓存管理器查找
        TaskBotNode botNode = cacheManager.getBotInstanceByTaskAndSequence(taskId, sequence);
        // if (botNode != null) {
        return botNode.getBotObject();
        // }

        // 根据taskId和sequence查询BotInstance
        // BotInstances botInstance =
        // genericCqnService.getBotInstanceByTaskAndSequence(taskId, sequence);
        // return getCurrentBot(botInstance.getId());
    }

    @Override
    public BotMessages chat(BotInstancesChatCompletionContext context) {
        // 从context的CQN中获取ID
        String botInstanceId = extractIdFromContext(context);

        // context.setResult();

        // context.setResult();

        // context.setResult();
        return chat(botInstanceId, context.getContent());

    }

    @Override
    public BotMessages chat(String botInstanceId, String content) {

        Bot bot = getCurrentBot(botInstanceId);

        // 更新状态为RUNNING
        // updateBotInstanceStatus(bot, "R");

        try {
            String response;
            if (bot instanceof ChatBot) {
                response = ((ChatBot) bot).chat(content);
            } else {
                throw new BusinessException("Bot is not a ChatBot: " + botInstanceId);
            }

            // 更新状态为SUCCESS
            // updateBotInstanceStatus(bot, "S");

            // // 3.1 停顿0.5 秒，确保用户消息先插入
            // try {
            // Thread.sleep(500);
            // } catch (InterruptedException e) {
            // Thread.currentThread().interrupt(); // 恢复中断状态
            // throw new BusinessException(AIConstants.Messages.THREAD_INTERRUPTED, e);
            // }

            BotMessages botMessage = genericCqnService.createAndInsertBotMessage(botInstanceId, response, "assistant");

            return botMessage;

        } catch (Exception e) {
            // 更新状态为FAILED
            // updateBotInstanceStatus(bot, "F");
            throw new BusinessException("Chat failed for bot: " + botInstanceId, e);
        }
    }

    @Override
    public SseEmitter chatInStreaming(String botInstanceId, String content) {
        Bot bot = getCurrentBot(botInstanceId);

        if (bot instanceof ChatBot) {
            return ((ChatBot) bot).chatInStreaming(content);
        } else {
            throw new BusinessException("Bot is not a ChatBot: " + botInstanceId);
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
        updateBotInstanceStatus(bot, "RUNNING");

        try {
            BotInstancesExecuteContext.ReturnType result = bot.execute();

            // 更新状态为SUCCESS
            updateBotInstanceStatus(bot, "SUCCESS");

            // 更新result字段
            updateBotInstanceResult(bot, result.getResult());

            // 如果维护了outputContextPath，则更新
            // String outputContextPath = genericCqnService.getOutputContextPathByBotInstanceId(botInstanceId);
            String outputContextPath = bot.getBotType().getOutputContextPath();
            if (outputContextPath != null && outputContextPath.isBlank() != true) {
                // 5. 获取绝对的 outputContextPath
                String absoluteOutputContextPath = contextService.getContextFullPath(bot, outputContextPath);
                // 6. 调用 ContextService 的 upsertContext 方法存储并返回 ContextNodes
                ContextNodes node = contextService.upsertContext(bot, absoluteOutputContextPath,
                        result.getResult(),
                        bot.getBotType().getContextTypeCode());

                // 6.1 将 ContextNode 的 ID 设置到 BotInstance 中
                updateBotInstanceContextNodeId(bot, node.getId());

            }
            //
            // 7. 检查botType的ragOutputContextPath，有维护值的情况下写入一条新的ContextNode
            // String ragOutputContextPath = bot.getBotType().getRagOutputContextPath();
            // if (ragOutputContextPath != null && !ragOutputContextPath.isBlank()) {
            // // 获取绝对的 RAG 输出上下文路径
            // String absoluteRagOutputContextPath =
            // contextService.getContextFullPath(botInstanceId,
            // ragOutputContextPath);
            // // 获取最新一条用户消息的 RAG 数据
            // BotMessages latestUserMessage =
            // genericCqnService.getLatestUserMessage(botInstanceId);
            // // 将RAG输出上下文路径和消息文本存储到新的ContextNode中
            // contextService.upsertContext(botInstanceId, absoluteRagOutputContextPath,
            // latestUserMessage.getRagData(),
            // "text");
            // }

            return result;

        } catch (Exception e) {
            // 更新状态为FAILED
            updateBotInstanceStatus(bot, "FAILED");
            // throw new BusinessException("Execution failed for bot: " + botInstanceId, e);
            // 异常情况下，为了不打断事务提交，使用catch语句后，将exception消息存储至result字段，且返回包含错误消息的result对象·
            BotInstancesExecuteContext.ReturnType exceptionResult = BotInstancesExecuteContext.ReturnType.create();
            exceptionResult.setResult("Execution failed: " + e.getMessage());
            updateBotInstanceResult(bot, exceptionResult.getResult());
            return exceptionResult;
        }
    }

    private String extractIdFromContext(BotInstancesChatCompletionContext context) {
        // 使用CqnAnalyzer类，从CQN查询中提取ID，需要解析CqnSelect
        CqnAnalyzer cqnAnalyzer = CqnAnalyzer.create(context.getModel());
        AnalysisResult result = cqnAnalyzer.analyze(context.getCqn().ref());
        // return result.rootKeys().get("ID").toString();
        return result.targetKeys().get("ID").toString();
    }

    private String extractIdFromContext(BotInstancesExecuteContext context) {
        // 从CQN查询中提取ID，需要解析CqnSelect
        // return context.getCqn().ref().segments().get(0).id();
        CqnAnalyzer cqnAnalyzer = CqnAnalyzer.create(context.getModel());
        AnalysisResult result = cqnAnalyzer.analyze(context.getCqn().ref());
        // return result.rootKeys().get("ID").toString();
        return result.targetKeys().get("ID").toString();
    }

    @Override
    public ContextNodes adopt(BotMessagesAdoptContext context) {

        // 1.通过上下文context获取 MessageId,botInstanceId
        String messageId = extractMessageIdFromContext(context);

        // 2. 查询关联的BotInstance ID
        String botInstanceId = genericCqnService.getBotInstanceIdByMessageId(messageId);
        if (botInstanceId == null) {
            throw new BusinessException("No bot instance associated with message: " + messageId);
        }

        return adopt(botInstanceId, messageId);

    }

    @Override
    public ContextNodes adopt(String botInstanceId, String messageId) {

        // 1. 直接通过 messageId 获取对应的 message
        BotMessages botMessage = genericCqnService.getMessageById(botInstanceId, messageId);
        if (botMessage == null) {
            throw new BusinessException("Message not found: " + messageId);
        }

        String messageText = botMessage.getMessage();

        // 2. 获取 Bot 实例
        Bot bot = getCurrentBot(botInstanceId);

        updateBotInstanceStatus(bot, "RUNNING");

        // 3. 查询 outputContextPath
        // String outputContextPath = genericCqnService.getOutputContextPathByBotInstanceId(botInstanceId);
        String outputContextPath = bot.getBotType().getOutputContextPath();
        if (outputContextPath == null || outputContextPath.isBlank()) {
            updateBotInstanceStatus(bot, "FAILED");
            throw new BusinessException("No outputContextPath configured for botInstance: " + botInstanceId);

        }

        // 4. 查询 taskId
        // String taskId = genericCqnService.getTaskIdByBotInstanceId(botInstanceId);
        // String taskId = genericCqnService.getMainTaskId(botInstanceId);
        // if (taskId == null) {
        // updateBotInstanceStatus(bot, "FAILED");
        // throw new BusinessException("No taskId associated with botInstance: " +
        // botInstanceId);
        // }

        // 5. 获取绝对的 outputContextPath
        String absoluteOutputContextPath = contextService.getContextFullPath(bot, outputContextPath);
        // 6. 调用 ContextService 的 upsertContext 方法存储并返回 ContextNodes
        ContextNodes node = contextService.upsertContext(bot, absoluteOutputContextPath, messageText,
                bot.getBotType().getContextTypeCode());

        // 6.1 将 ContextNode 的 ID 设置到 BotInstance 中
        updateBotInstanceContextNodeId(bot, node.getId());

        // 7. 检查botType的ragOutputContextPath，有维护值的情况下写入一条新的ContextNode
        String ragOutputContextPath = bot.getBotType().getRagOutputContextPath();
        if (ragOutputContextPath != null && !ragOutputContextPath.isBlank()) {
            // 获取绝对的 RAG 输出上下文路径
            String absoluteRagOutputContextPath = contextService.getContextFullPath(bot, ragOutputContextPath);
            // 获取最新一条用户消息的 RAG 数据
            BotMessages latestUserMessage = genericCqnService.getLatestUserMessage(botInstanceId);
            // 将RAG输出上下文路径和消息文本存储到新的ContextNode中
            contextService.upsertContext(bot, absoluteRagOutputContextPath, latestUserMessage.getRagData(),
                    "text");
        }

        updateBotInstanceStatus(bot, "SUCCESS");

        return node;

    }

    private String extractMessageIdFromContext(BotMessagesAdoptContext context) {
        // 从CQN查询中提取消息ID
        // 使用CqnAnalyzer类，从CQN查询中提取ID，需要解析CqnSelect
        CqnAnalyzer cqnAnalyzer = CqnAnalyzer.create(context.getModel());
        AnalysisResult result = cqnAnalyzer.analyze(context.getCqn().ref());
        // return result.rootKeys().get("message_ID").toString();
        return result.targetKeys().get("ID").toString();

        // return context.getCqn().ref().segments().get(0).id();
    }

    // @Override
    // public String getMainTaskId(String botInstanceId) {
    // // 使用缓存管理器获取主任务ID
    // String mainTaskId = cacheManager.getMainTaskId(botInstanceId);
    // if (mainTaskId != null) {
    // return mainTaskId;
    // }

    // // 如果缓存中没有，执行原有逻辑
    // // 1. 通过botInstanceId获取BotInstance
    // BotInstances botInstance =
    // genericCqnService.getBotInstanceById(botInstanceId);

    // // 2. 获取当前任务ID
    // String currentTaskId = botInstance.getTaskId();

    // // 3. 循环查找，直到找到主任务
    // while (currentTaskId != null) {
    // Tasks currentTask = genericCqnService.getTaskById(currentTaskId);

    // // 4. 如果是主任务，返回该任务ID
    // if (currentTask.getIsMain() != null && currentTask.getIsMain()) {
    // return currentTaskId;
    // }

    // // 5. 如果不是主任务，通过botInstanceId找到父任务
    // String parentBotInstanceId = currentTask.getBotInstanceId();

    // if (parentBotInstanceId == null || parentBotInstanceId.isEmpty()) {
    // // 如果没有父BotInstance，说明这可能就是顶层任务
    // // 但不是主任务，这种情况可能是数据错误
    // throw new BusinessException("Found top-level task but it's not marked as
    // main task: " + currentTaskId);
    // }

    // // 6. 获取父BotInstance的任务ID
    // BotInstances parentBotInstance =
    // genericCqnService.getBotInstanceById(parentBotInstanceId);
    // currentTaskId = parentBotInstance.getTaskId();
    // }

    // // 如果遍历完还没找到主任务，抛出异常
    // throw new BusinessException("Main task not found for botInstanceId: " +
    // botInstanceId);
    // }

    /**
     * 更新BotInstance的contextNodeId字段
     */
    private void updateBotInstanceContextNodeId(Bot bot, String contextNodeId) {
        try {
            // 调用Bot接口的方法更新ContextNodeId，并获取更新后的实例
            bot.updateContextNodeId(contextNodeId);
            System.out.println(
                    "Updated contextNodeId for botInstance: " + bot.getBotInstance().getId() + " -> " + contextNodeId);
        } catch (Exception e) {
            throw new BusinessException(
                    "Failed to update contextNodeId for botInstance: " + bot.getBotInstance().getId(), e);
        }
    }

    @Override
    public void updateBotInstanceStatus(Bot bot, String status) {
        String botInstanceId = bot.getBotInstance().getId();
        String oldStatus = bot.getBotInstance().getStatusCode(); // 获取当前状态（旧状态）
        // 使用缓存管理器更新状态
        cacheManager.updateBotStatus(botInstanceId, status);
        // 调用Bot接口的方法更新状态
        bot.updateStatus(status);

        // 发布状态变更事件
        eventPublisher.publishEvent(new BotInstanceStatusChangedEvent(
            this, botInstanceId, oldStatus, status, bot.getBotInstance().getTaskId()
        ));
    }

    private void updateBotInstanceResult(Bot bot, String result) {
        // 调用Bot接口的方法更新结果
        bot.updateResult(result);
    }
}
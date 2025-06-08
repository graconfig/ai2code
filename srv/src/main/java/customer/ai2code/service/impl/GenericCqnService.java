package customer.ai2code.service.impl;

import org.springframework.stereotype.Service;

import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;

import cds.gen.configservice.ConfigService;
import cds.gen.configservice.ModelConfigs;
import cds.gen.configservice.ModelConfigs_;
import cds.gen.configservice.BotTypes;
import cds.gen.configservice.BotTypes_;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessages_;
import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.Tasks_;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.ContextNodes_;
import cds.gen.mainservice.MainService;
import cds.gen.configservice.PromptTexts;
import cds.gen.configservice.PromptTexts_;

import java.util.List;
import java.util.UUID;

@Service
public class GenericCqnService {

    private final MainService mainService;
    private final ConfigService configService;
    private final EntityService entityService;

    private final TaskBotCacheManager cacheManager;

    public GenericCqnService(
            EntityService entityService,
            MainService mainService,
            ConfigService configService,
            TaskBotCacheManager cacheManager) {
        this.entityService = entityService;
        this.mainService = mainService;
        this.configService = configService;
        this.cacheManager = cacheManager;
    }

    // 原有查询方法...
    public ModelConfigs getModelConfig(String modelConfigId) {
        CqnSelect select = Select.from(ModelConfigs_.class)
                .where(m -> m.ID().eq(modelConfigId));
        return entityService.selectSingle(null, select, null, modelConfigId);
    }

    public BotInstances getBotInstanceById(String botInstanceId) {
        var select = Select.from(BotInstances_.class).where(b -> b.ID().eq(botInstanceId));
        return entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found: " + botInstanceId);
    }

    public BotInstances getBotInstanceByTaskAndSequence(String taskId, int sequence) {
        var select = Select.from(BotInstances_.class)
                .where(b -> b.task_ID().eq(taskId).and(b.sequence().eq(sequence)));
        return entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found for task: " + taskId + ", sequence: " + sequence);
    }

    public BotTypes getBotTypeById(String typeId) {
        var select = Select.from(BotTypes_.class).where(b -> b.ID().eq(typeId));
        return entityService.selectSingle(configService, select, BotTypes.class,
                "BotType not found: " + typeId);
    }

    public List<BotTypes> getBotTypesByTaskType(String taskTypeId) {
        var select = Select.from(BotTypes_.class)
                .where(b -> b.taskType_ID().eq(taskTypeId))
                .orderBy(b -> b.sequence().asc());
        return entityService.selectList(configService, select, BotTypes.class);
    }

    public Tasks getTaskById(String taskId) {
        var select = Select.from(Tasks_.class).where(t -> t.ID().eq(taskId));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found: " + taskId);
    }

    public Tasks getTaskByBotInstanceAndSequence(String botInstanceId, int sequence) {
        var select = Select.from(Tasks_.class)
                .where(t -> t.botInstance_ID().eq(botInstanceId).and(t.sequence().eq(sequence)));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found for botInstance: " + botInstanceId + ", sequence: " + sequence);
    }

    // 创建和插入方法 - 包含业务逻辑
    public Tasks createAndInsertMainTask(String name, String description, String taskTypeId) {
        Tasks newTask = Tasks.create();
        newTask.setId(UUID.randomUUID().toString());
        newTask.setName(name);
        newTask.setDescription(description);
        newTask.setIsMain(true);
        newTask.setContextPath("");
        newTask.setSequence(0);
        newTask.setTypeId(taskTypeId);

        entityService.insert(mainService, null, Tasks_.class, newTask, true);
        return newTask;
    }

    public Tasks createAndInsertSubTask(String name, String description, String contextPath,
            int sequence, String botInstanceId, String taskTypeId) {
        Tasks newTask = Tasks.create();
        newTask.setId(UUID.randomUUID().toString());
        newTask.setName(name);
        newTask.setDescription(description);
        newTask.setIsMain(false); // 子任务
        newTask.setContextPath(contextPath);
        newTask.setSequence(sequence);
        newTask.setBotInstanceId(botInstanceId);
        newTask.setTypeId(taskTypeId);

        entityService.insert(mainService, null, Tasks_.class, newTask, true);
        return newTask;
    }

    public BotInstances createAndInsertBotInstance(String taskId, BotTypes botType) {
        BotInstances botInstance = BotInstances.create();
        botInstance.setId(UUID.randomUUID().toString());
        botInstance.setSequence(botType.getSequence());
        botInstance.setTypeId(botType.getId());
        botInstance.setStatusCode("C"); // Created
        botInstance.setTaskId(taskId);

        entityService.insert(mainService, null, BotInstances_.class, botInstance, true);
        return botInstance;
    }

    public ContextNodes createAndInsertContextNode(String taskId, String path, String label,
            String type, String value) {
        ContextNodes contextNode = ContextNodes.create();
        contextNode.setId(UUID.randomUUID().toString());
        contextNode.setTaskId(taskId);
        contextNode.setPath(path);
        contextNode.setLabel(label);
        contextNode.setType(type);
        contextNode.setValue(value);

        entityService.insert(mainService, null, ContextNodes_.class, contextNode, true);
        return contextNode;
    }

    // 更新方法
    public void updateBotInstance(BotInstances botInstance) {
        entityService.update(mainService, null, BotInstances_.class, botInstance, true);
    }

    public void updateBotInstanceStatus(String botInstanceId, String statusCode) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setStatusCode(statusCode);
        updateBotInstance(botInstance);
    }

    public void updateBotInstanceResult(String botInstanceId, String result) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setResult(result);
        updateBotInstance(botInstance);
    }

    public void updateBotInstanceStatusAndResult(String botInstanceId, String statusCode, String result) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setStatusCode(statusCode);
        botInstance.setResult(result);
        updateBotInstance(botInstance);
    }

    public ContextNodes getContextNodeById(String contextNodeId) {
        var select = Select.from(ContextNodes_.class).where(c -> c.ID().eq(contextNodeId));
        return entityService.selectSingle(mainService, select, ContextNodes.class,
                "ContextNode not found: " + contextNodeId);
    }

    // 修改为根据taskId和contextPath查询
    public ContextNodes getContextNodeByTaskAndPath(String taskId, String contextPath) {
        var select = Select.from(ContextNodes_.class)
                .where(c -> c.task_ID().eq(taskId).and(c.path().eq(contextPath)));
        return entityService.selectSingle(mainService, select, ContextNodes.class,
                "ContextNode not found for task: " + taskId + ", path: " + contextPath);
    }

    // 更新ContextNode的业务方法
    public ContextNodes updateContextNodeValue(ContextNodes existingNode, String contextValue) {
        existingNode.setValue(contextValue);
        existingNode.setModifiedAt(java.time.Instant.now());

        entityService.update(mainService, null, ContextNodes_.class, existingNode, true);
        return existingNode;
    }

    // 更新ContextNode的完整信息
    public ContextNodes updateContextNode(ContextNodes existingNode, String label, String type, String contextValue) {
        existingNode.setValue(contextValue);
        existingNode.setLabel(label);
        existingNode.setType(type);
        existingNode.setModifiedAt(java.time.Instant.now());

        entityService.update(mainService, null, ContextNodes_.class, existingNode, true);
        return existingNode;
    }

    // 根据path生成友好的label
    public String generateLabelFromPath(String contextPath) {
        if (contextPath == null || contextPath.isEmpty()) {
            return "Context";
        }

        // 将路径转换为友好的标签
        String[] parts = contextPath.split("\\.");
        String lastPart = parts[parts.length - 1];

        // 将驼峰命名转换为可读格式
        return lastPart.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("_", " ")
                .replaceAll("-", " ")
                .toLowerCase()
                .replaceAll("\\b\\w", "");
    }

    // public void insertContextNode(ContextNodes contextNode) {
    // entityService.insert(mainService, null, ContextNodes_.class, contextNode,
    // true);
    // }

    // 原有的简单插入方法保留，但标记为内部使用
    private void insertTask(Tasks task) {
        entityService.insert(mainService, null, Tasks_.class, task, true);
    }

    private void insertBotInstance(BotInstances botInstance) {
        entityService.insert(mainService, null, BotInstances_.class, botInstance, true);
    }

    private void insertContextNode(ContextNodes contextNode) {
        entityService.insert(mainService, null, ContextNodes_.class, contextNode, true);
    }

    /**
     * 根据BotType ID查询所有PromptTexts
     */
    public List<PromptTexts> getPromptTextsByBotType(String botTypeId) {
        var select = Select.from(PromptTexts_.class)
                .where(p -> p.botType_ID().eq(botTypeId));
        // .orderBy(p -> p.sequence().asc());
        return entityService.selectList(configService, select, PromptTexts.class);
    }

    /**
     * 根据path查询ContextNode（不指定taskId）
     */
    // public ContextNodes getContextNodeByPath(String contextPath) {
    // var select = Select.from(ContextNodes_.class)
    // .where(c -> c.path().eq(contextPath));
    // return entityService.selectSingle(mainService, select, ContextNodes.class,
    // "ContextNode not found for path: " + contextPath);
    // }

    /**
     * 根据botInstanceId获取主任务ID
     */
    public String getMainTaskId(String botInstanceId) {
        // 使用缓存管理器获取主任务ID
        String mainTaskId = cacheManager.getMainTaskId(botInstanceId);
        if (mainTaskId != null) {
            return mainTaskId;
        }

        // 如果缓存中没有，执行原有逻辑
        // 1. 通过botInstanceId获取BotInstance
        BotInstances botInstance = getBotInstanceById(botInstanceId);

        // 2. 获取当前任务ID
        String currentTaskId = botInstance.getTaskId();

        // 3. 循环查找，直到找到主任务
        while (currentTaskId != null) {
            Tasks currentTask = getTaskById(currentTaskId);

            // 4. 如果是主任务，返回该任务ID
            if (currentTask.getIsMain() != null && currentTask.getIsMain()) {
                return currentTaskId;
            }

            // 5. 如果不是主任务，通过botInstanceId找到父任务
            String parentBotInstanceId = currentTask.getBotInstanceId();

            if (parentBotInstanceId == null || parentBotInstanceId.isEmpty()) {
                // 如果没有父BotInstance，说明这可能就是顶层任务
                // 但不是主任务，这种情况可能是数据错误
                throw new IllegalStateException(
                        "Found top-level task but it's not marked as main task: " + currentTaskId);
            }

            // 6. 获取父BotInstance的任务ID
            BotInstances parentBotInstance = getBotInstanceById(parentBotInstanceId);
            currentTaskId = parentBotInstance.getTaskId();
        }

        // 如果遍历完还没找到主任务，抛出异常
        throw new IllegalStateException("Main task not found for botInstanceId: " + botInstanceId);
    }

    // /**
    // * 创建并插入BotMessage
    // */
    // public BotMessages createAndInsertBotMessage(String botInstanceId, String
    // userMessage, String assistantMessage) {
    // BotMessages botMessage = BotMessages.create();
    // botMessage.setId(UUID.randomUUID().toString());
    // botMessage.setBotInstanceId(botInstanceId);
    // botMessage.setRole("user"); // 设置角色
    // botMessage.setMessage(userMessage); // 设置通用message字段

    // // 插入用户消息
    // entityService.insert(mainService, null, BotMessages_.class, botMessage,
    // true);

    // // 如果有AI回复，创建另一条记录
    // if (assistantMessage != null && !assistantMessage.isEmpty()) {
    // BotMessages assistantBotMessage = BotMessages.create();
    // assistantBotMessage.setId(UUID.randomUUID().toString());
    // assistantBotMessage.setBotInstanceId(botInstanceId);
    // assistantBotMessage.setRole("assistant");
    // assistantBotMessage.setMessage(assistantMessage);

    // // 插入AI回复消息
    // entityService.insert(mainService, null, BotMessages_.class,
    // assistantBotMessage, true);

    // // 返回AI回复的消息记录
    // return assistantBotMessage;
    // }

    // // 如果没有AI回复，返回用户消息记录
    // return botMessage;
    // }

    /**
     * 创建并插入单条BotMessage（指定角色）
     */
    public BotMessages createAndInsertBotMessage(String botInstanceId, String message, String role) {
        BotMessages botMessage = BotMessages.create();
        botMessage.setId(UUID.randomUUID().toString());
        botMessage.setBotInstanceId(botInstanceId);
        botMessage.setMessage(message);
        botMessage.setRole(role);

        entityService.insert(mainService, null, BotMessages_.class, botMessage, true);
        return botMessage;
    }

    /**
     * 判断是否是第一次调用
     */
    public boolean isFirstCall(String botInstanceId) {
        try {
            // 查询该Bot实例是否有历史消息
            List<BotMessages> messages = getBotMessagesByBotInstanceId(botInstanceId);
            return messages == null || messages.isEmpty();
        } catch (Exception e) {
            // 如果查询失败，默认认为是第一次调用
            return true;
        }
    }

    /**
     * 根据BotInstance ID查询所有BotMessages
     */
    public List<BotMessages> getBotMessagesByBotInstanceId(String botInstanceId) {
        var select = Select.from(BotMessages_.class)
                .where(b -> b.botInstance_ID().eq(botInstanceId))
                .orderBy(b -> b.createdAt().asc());
        return entityService.selectList(mainService, select, BotMessages.class);
    }

}

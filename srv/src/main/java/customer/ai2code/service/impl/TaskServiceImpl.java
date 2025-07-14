package customer.ai2code.service.impl;

import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.TasksGetHierarchyContext;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.BotType;
import cds.gen.mainservice.CreateTaskWithBotsContext;
import cds.gen.mainservice.TaskType;
import cds.gen.configservice.BotTypes;
import customer.ai2code.model.bot.Bot;
import customer.ai2code.model.task.GenericTask;
import customer.ai2code.model.task.Task;
import customer.ai2code.model.tree.TaskBotNode;
import customer.ai2code.service.BotService;
import customer.ai2code.service.TaskService;
import customer.ai2code.service.ContextService;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.ql.cqn.AnalysisResult;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.services.EventContext;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    private final BotService botService;
    private final GenericCqnService genericCqnService;
    private final ContextService contextService;
    private final TaskBotCacheManager cacheManager;
    private final ObjectMapper objectMapper;

    // 全局Task缓存链表
    // private final Map<String, Task> taskCache = new ConcurrentHashMap<>();

    public TaskServiceImpl(
            BotService botService,
            GenericCqnService genericCqnService,
            ContextService contextService,
            TaskBotCacheManager cacheManager,
            ObjectMapper objectMapper) {

        this.botService = botService;
        this.genericCqnService = genericCqnService;
        this.contextService = contextService;
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
    }

    /*
     * 创建主任务及其以下的Bot实例
     */
    @Override
    public Task createTaskWithBots(String name, String description, String taskTypeId) {
        // 1. 查询TaskType的botTypes
        List<BotTypes> botTypes = genericCqnService.getBotTypesByTaskType(taskTypeId);

        // 2. 创建主Task
        Tasks newTask = genericCqnService.createAndInsertMainTask(name, description, taskTypeId);

        // 3. 为每个BotType创建BotInstance
        for (BotTypes botType : botTypes) {
            genericCqnService.createAndInsertBotInstance(newTask.getId(), botType);
        }

        // 4. 创建基础ContextNodes
        createBasicContextNodes(newTask.getId(), name, description);

        // 5.新建Task对象
        GenericTask task = new GenericTask(newTask);

        // 6. 放入缓存 - 使用新的缓存管理器
        cacheManager.addTaskNode(task, null);

        // 7. 返回新建的Task对象
        return task;
    }

    /**
     * 根据botInstanceId，找到BotType下的TaskType创建子任务，及其以下的Bot实例
     */
    @Override
    public Task createTaskWithBots(String botInstanceId, String name, String description, String contextPath,
            int sequence) {
        // 1. 查询botInstance
        Bot bot = botService.getCurrentBot(botInstanceId);

        // 2. 获取上级BotType
        BotTypes PBotType = bot.getBotType();

        // 3. 获取TaskType
        // TaskType taskType = PBotType.getTaskType();

        // 3.1 获取下属BotType
        List<BotTypes> botTypes = genericCqnService.getBotTypesByTaskType(PBotType.getSubTaskTypeId());

        // 3.2 获取绝对路径
        String absoluteOutputContextPath = contextService.getContextFullPath(botInstanceId, contextPath);
        // 4. 创建Task
        Tasks newTask = genericCqnService.createAndInsertSubTask(name, description, absoluteOutputContextPath,
                sequence, botInstanceId, PBotType.getSubTaskTypeId());

        // 4.1 为SubTask创建ContextNode - description
        // contextService.upsertContextWithMainTaskId(newTask.getId(),
        // absoluteOutputContextPath + ".description",
        // description, "text");
        contextService.upsertContext(botInstanceId, absoluteOutputContextPath + ".description", description, "STRING");

        // 5. 为每个BotType创建BotInstance
        for (BotTypes botType : botTypes) {
            genericCqnService.createAndInsertBotInstance(newTask.getId(), botType);
        }

        // 6.新建Task对象
        Task task = new GenericTask(newTask);

        // 7. 放入缓存 - 使用新的缓存管理器
        cacheManager.addTaskNode(task, botInstanceId);

        // 8.返回新建的Task对象
        return task;
    }

    /**
     * 根据botInstanceId，找到BotType下的TaskType创建子任务，及其以下的Bot实例
     */
    @Override
    public Task createTaskWithBots(String botInstanceId, String name) {
        // 1. 查询Bot
        Bot bot = botService.getCurrentBot(botInstanceId);

        // 2.获取BotType
        BotType botType = bot.getBotInstance().getType();
        TaskType taskType = botType.getTaskType();
        String description = taskType.getDescription();
        String contextPath = botType.getOutputContextPath();

        return createTaskWithBots(botInstanceId, name, description, contextPath, 0);
    }

    /**
     * 创建主任务及其以下的Bot实例
     * 
     * @param context 创建任务的上下文
     * @return 创建的任务
     */
    @Override
    public Task createTaskWithBots(CreateTaskWithBotsContext context) {
        // 创建主任务
        Task task = createTaskWithBots(context.getName(), context.getDescription(), context.getTypeId());

        // 设置context
        // context.setResult(task.getTask());
        return task;
    }

    @Override
    public Task getCurrentTask(String taskId) {
        // 先从缓存中查找 - 使用新的缓存管理器
        Task cachedTask = cacheManager.getCachedTask(taskId);
        if (cachedTask != null) {
            return cachedTask;
        }

        // 从数据库查询
        Tasks taskCDS = genericCqnService.getTaskAndSubBotsById(taskId);
        // Tasks taskCDS = genericCqnService.getTaskById(taskId);
        Task task = new GenericTask(taskCDS);

        // 放入缓存 - 使用新的缓存管理器
        cacheManager.addTaskNode(task, taskCDS.getBotInstanceId());

        return task;
    }

    @Override
    public Task getCurrentTask(String botInstanceId, int sequence) {
        // 使用缓存管理器查找
        TaskBotNode botNode = cacheManager.getBotInstanceByTaskAndSequence(botInstanceId, sequence);
        if (botNode != null && botNode.getParent() != null) {
            TaskBotNode parentTask = botNode.getParent();
            if (parentTask.getType() == TaskBotNode.NodeType.TASK) {
                return parentTask.getTaskObject();
            }
        }

        // 根据botInstanceId和sequence查询Task
        Tasks task = genericCqnService.getTaskByBotInstanceAndSequence(botInstanceId, sequence);
        return getCurrentTask(task.getId());
    }

    @Override
    public String getHierarchy(TasksGetHierarchyContext context) {
        // 从上下文中获取任务ID
        String taskId = extractTaskIdFromContext(context);

        // 调用getHierarchy方法
        return getHierarchy(taskId);
    }

    private String extractTaskIdFromContext(TasksGetHierarchyContext context) {
        // 从CQN查询中提取消息ID
        // 使用CqnAnalyzer类，从CQN查询中提取ID，需要解析CqnSelect
        CqnAnalyzer cqnAnalyzer = CqnAnalyzer.create(context.getModel());
        AnalysisResult result = cqnAnalyzer.analyze(context.getCqn().ref());
        // return result.rootKeys().get("message_ID").toString();
        return result.targetKeys().get("ID").toString();

    }

    @Override
    public String getHierarchy(String taskId) {
        try {
            // 获取根任务
            Task rootTask = getCurrentTask(taskId);
            if (rootTask == null) {
                return "{}";
            }

            // 构建层次结构
            HierarchyNode hierarchyNode = buildHierarchy(rootTask);

            // 使用 ObjectMapper 转换为JSON字符串
            return objectMapper.writeValueAsString(hierarchyNode);

        } catch (Exception e) {
            System.err.println("Error building hierarchy for task: " + taskId + ", error: " + e.getMessage());
            return "{}";
        }
    }

    /**
     * 递归构建任务和Bot的层次结构
     */
    private HierarchyNode buildHierarchy(Task task) {
        Tasks taskCDS = task.getTask();

        // 创建任务节点
        HierarchyNode taskNode = new HierarchyNode();
        taskNode.type = "task";
        taskNode.id = taskCDS.getId();
        taskNode.name = taskCDS.getName();
        taskNode.description = taskCDS.getDescription();
        taskNode.isMain = taskCDS.getIsMain() != null ? taskCDS.getIsMain() : false;
        taskNode.sequence = taskCDS.getSequence() != null ? taskCDS.getSequence() : 0;
        // Tasks 没有 status 字段，跳过设置

        // 获取任务下的BotInstances
        List<BotInstances> botInstances = taskCDS.getBotInstances();
        if (botInstances != null && !botInstances.isEmpty()) {
            taskNode.items = new ArrayList<>();

            for (BotInstances botInstance : botInstances) {
                try {
                    // 获取Bot对象
                    Bot bot = botService.getCurrentBot(botInstance.getId());

                    // 创建Bot节点
                    HierarchyNode botNode = new HierarchyNode();
                    botNode.type = "bot";
                    botNode.id = bot.getBotInstance().getId();
                    botNode.name = bot.getBotType().getName();
                    botNode.description = bot.getBotType().getDescription();
                    botNode.functionType = bot.getBotType().getFunctionTypeCode();
                    botNode.status = bot.getBotInstance().getStatusCode(); // 使用 getStatusCode() 而不是 getStatus()
                    botNode.sequence = bot.getBotInstance().getSequence() != null ? bot.getBotInstance().getSequence() : 0;

                    // 获取Bot下的子任务
                    List<Tasks> subTasks = bot.getBotInstance().getTasks();
                    if (subTasks != null && !subTasks.isEmpty()) {
                        botNode.items = new ArrayList<>();

                        for (Tasks subTaskCDS : subTasks) {
                            try {
                                // 递归处理子任务
                                Task subTask = getCurrentTask(subTaskCDS.getId());
                                HierarchyNode subTaskNode = buildHierarchy(subTask);
                                botNode.items.add(subTaskNode);
                            } catch (Exception e) {
                                System.err.println("Error processing sub-task: " + subTaskCDS.getId() + ", error: "
                                        + e.getMessage());
                            }
                        }
                    }

                    taskNode.items.add(botNode);

                } catch (Exception e) {
                    System.err.println(
                            "Error processing bot instance: " + botInstance.getId() + ", error: " + e.getMessage());
                }
            }
        }

        return taskNode;
    }

    /**
     * 层次结构节点内部类
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HierarchyNode {
        public String type; // "task" 或 "bot"
        public String id;
        public String name;
        public String description;
        public String status;
        public int sequence;

        // 任务特有属性
        public Boolean isMain;

        // Bot特有属性
        public String functionType;

        // 子节点
        public List<HierarchyNode> items;
    }

    private void createBasicContextNodes(String taskId, String name, String description) {
        // 创建description节点
        contextService.upsertContextWithMainTaskId(taskId, "description", description, "STRING");

    }
}
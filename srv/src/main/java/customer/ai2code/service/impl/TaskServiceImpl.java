package customer.ai2code.service.impl;

import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.TasksGetHierarchyContext;
import cds.gen.mainservice.BotInstances;
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

        // 3.新建Task对象
        GenericTask task = new GenericTask(newTask);

        // 4. 放入缓存 - 使用新的缓存管理器
        cacheManager.addTaskNode(task, null);

        // 5. 为每个BotType创建BotInstance
        for (BotTypes botType : botTypes) {
            BotInstances botInstancesNew = genericCqnService.createAndInsertBotInstance(newTask.getId(), botType);
            // 新建Bot对象
            Bot bot = cacheManager.createBotInstance(botInstancesNew, botType);
            // 放入缓存 - 使用新的缓存管理器
            cacheManager.addBotInstanceNode(bot);
        }

        // 6. 创建基础ContextNodes
        createBasicContextNodes(newTask.getId(), name, description);

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
        String absoluteOutputContextPath = contextService.getContextFullPath(bot, contextPath);
        // String absoluteOutputContextPath = contextService.getContextFullPath(botInstanceId, contextPath);
        // 4. 创建Task
        Tasks newTask = genericCqnService.createAndInsertSubTask(name, description, absoluteOutputContextPath,
                sequence, botInstanceId, PBotType.getSubTaskTypeId());

        // 4.1 为SubTask创建ContextNode - description
        // contextService.upsertContextWithMainTaskId(newTask.getId(),
        // absoluteOutputContextPath + ".description",
        // description, "text");
        // 5.新建Task对象
        Task task = new GenericTask(newTask);

        // 6. 放入缓存 - 使用新的缓存管理器
        cacheManager.addTaskNode(task, botInstanceId);

        contextService.upsertContext(bot, absoluteOutputContextPath + ".description", description, "STRING");

        // 7. 为每个BotType创建BotInstance
        for (BotTypes botType : botTypes) {
            BotInstances botInstancesNew = genericCqnService.createAndInsertBotInstance(newTask.getId(), botType);
            // 新建Bot对象
            Bot botNew = cacheManager.createBotInstance(botInstancesNew, botType);
            // 放入缓存 - 使用新的缓存管理器
            cacheManager.addBotInstanceNode(botNew);
        }

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
        return task;
    }

    @Override
    public Task getCurrentTask(String taskId) {
        // 先从缓存中查找 - 使用新的缓存管理器
        TaskBotNode taskNode = cacheManager.getTaskNode(taskId);
        return taskNode.getTaskObject();
        // Task cachedTask = cacheManager.getCachedTask(taskId);
        // if (cachedTask != null) {
        //     return cachedTask;
        // }

        // // 从数据库查询
        // Tasks taskCDS = genericCqnService.getTaskAndSubBotsById(taskId);
        // // Tasks taskCDS = genericCqnService.getTaskById(taskId);
        // Task task = new GenericTask(taskCDS);

        // // 放入缓存 - 使用新的缓存管理器
        // cacheManager.addTaskNode(task, taskCDS.getBotInstanceId());

        // return task;
    }

    @Override
    public Task getCurrentTask(String botInstanceId, int sequence) {
        // 使用缓存管理器查找
        TaskBotNode taskNode = cacheManager.getTaskByBotInstanceAndSequence(botInstanceId, sequence);
        return taskNode.getTaskObject();
        // TaskBotNode botNode =
        // cacheManager.getBotInstanceByTaskAndSequence(botInstanceId, sequence);
        // if (botNode != null && botNode.getParent() != null) {
        // TaskBotNode parentTask = botNode.getParent();
        // if (parentTask.getType() == TaskBotNode.NodeType.TASK) {
        // return parentTask.getTaskObject();
        // }
        // }

        // // 根据botInstanceId和sequence查询Task
        // Tasks task = genericCqnService.getTaskByBotInstanceAndSequence(botInstanceId,
        // sequence);
        // return getCurrentTask(task.getId());
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

    @Override
    public String getMainTaskId(String botInstanceId) {
        // 使用缓存管理器获取主任务ID
        // return cacheManager.getMainTaskId(botInstanceId);
        String mainTaskId = cacheManager.getMainTaskId(botInstanceId);
        if (mainTaskId != null) {
            return mainTaskId;
        }
        // 如果缓存中没有，执行原有逻辑
        return genericCqnService.getMainTaskId(botInstanceId);
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
        // List<BotInstances> botInstances = taskCDS.getBotInstances();
        List<TaskBotNode> bots = cacheManager.getChildren(cacheManager.getTaskNode(task));
        if (bots != null && !bots.isEmpty()) {
            taskNode.items = new ArrayList<>();

            for (TaskBotNode cachedBot : bots) {
                try {
                    // 获取Bot对象
                    // Bot bot = botService.getCurrentBot(botInstance.getId());

                    // 创建Bot节点
                    HierarchyNode botNode = new HierarchyNode();
                    botNode.type = "bot";
                    botNode.id = cachedBot.getId();
                    botNode.name = cachedBot.getBotObject().getBotType().getName();
                    botNode.description = cachedBot.getBotObject().getBotType().getDescription();
                    // botNode.functionType = cachedBot.getBotObject().getBotInstance().getTypeId();
                    botNode.functionType = cachedBot.getBotObject().getBotType().getFunctionTypeCode();
                    botNode.status = cachedBot.getBotStatus(); // 使用 getStatusCode() 而不是 getStatus()
                    botNode.sequence = cachedBot.getBotObject().getBotInstance().getSequence() != null ? cachedBot.getBotObject().getBotInstance().getSequence()
                            : 0;

                    // 获取Bot下的子任务
                    // List<Tasks> subTasks = bot.getBotInstance().getTasks();
                    List<TaskBotNode> subTasks = cacheManager.getChildren(cachedBot);


                    if (subTasks != null && !subTasks.isEmpty()) {
                        botNode.items = new ArrayList<>();

                        for (TaskBotNode cachedTask : subTasks) {
                            try {
                                // 递归处理子任务
                                // Task subTask = getCurrentTask(subTask.getId());
                                HierarchyNode subTaskNode = buildHierarchy(cachedTask.getTaskObject());
                                botNode.items.add(subTaskNode);
                            } catch (Exception e) {
                                System.err.println("Error processing sub-task: " + cachedTask.getId() + ", error: "
                                        + e.getMessage());
                            }
                        }
                    }

                    taskNode.items.add(botNode);

                } catch (Exception e) {
                    System.err.println(
                            "Error processing bot instance: " + cachedBot.getId() + ", error: " + e.getMessage());
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

    @Override
    public boolean deleteOriginalTasks(String botInstanceId) {
        try {
            // 获取Bot节点
            TaskBotNode botNode = cacheManager.getBotInstanceNode(botInstanceId);
            if (botNode == null) {
                System.err.println("Bot节点不存在: " + botInstanceId);
                return false;
            }

            // 递归删除Bot下的所有子任务及其子Bot
            boolean deleteSuccess = deleteSubTasksRecursively(botNode);
            
            if (deleteSuccess) {
                System.out.println("成功删除Bot及其子任务: " + botInstanceId);
            } else {
                System.err.println("删除Bot及其子任务时出现部分失败: " + botInstanceId);
            }
            
            return deleteSuccess;
            
        } catch (Exception e) {
            System.err.println("删除Bot及其子任务失败: " + botInstanceId + ", 错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递归删除Bot节点下的所有子任务及其子Bot
     */
    private boolean deleteSubTasksRecursively(TaskBotNode botNode) {
        boolean allSuccess = true;
        
        try {
            // 获取Bot下的所有子任务
            List<TaskBotNode> subTasks = cacheManager.getChildren(botNode);
            
            if (subTasks != null && !subTasks.isEmpty()) {
                // 创建副本列表避免并发修改异常
                List<TaskBotNode> subTasksCopy = new ArrayList<>(subTasks);
                
                for (TaskBotNode taskNode : subTasksCopy) {
                    if (taskNode.getType() == TaskBotNode.NodeType.TASK) {
                        try {
                            // 递归删除任务下的所有Bot及其子任务
                            boolean taskDeleteSuccess = deleteTaskAndBotsRecursively(taskNode);
                            if (!taskDeleteSuccess) {
                                allSuccess = false;
                                System.err.println("删除子任务失败: " + taskNode.getId());
                            }
                        } catch (Exception e) {
                            allSuccess = false;
                            System.err.println("删除子任务时出错: " + taskNode.getId() + ", 错误: " + e.getMessage());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("获取Bot子任务失败: " + botNode.getId() + ", 错误: " + e.getMessage());
            allSuccess = false;
        }
        
        return allSuccess;
    }

    /**
     * 递归删除任务节点及其下的所有Bot和子任务
     */
    private boolean deleteTaskAndBotsRecursively(TaskBotNode taskNode) {
        boolean allSuccess = true;
        String taskId = taskNode.getId();
        
        try {
            // 获取任务下的所有Bot
            List<TaskBotNode> bots = cacheManager.getChildren(taskNode);
            
            if (bots != null && !bots.isEmpty()) {
                // 创建副本列表避免并发修改异常
                List<TaskBotNode> botsCopy = new ArrayList<>(bots);
                
                for (TaskBotNode botNode : botsCopy) {
                    if (botNode.getType() == TaskBotNode.NodeType.BOT_INSTANCE) {
                        try {
                            // 递归删除Bot下的子任务
                            boolean botDeleteSuccess = deleteSubTasksRecursively(botNode);
                            if (!botDeleteSuccess) {
                                allSuccess = false;
                            }
                            
                            // 从数据库删除Bot实例
                            boolean dbDeleteBot = genericCqnService.deleteBotInstance(botNode.getId());
                            if (!dbDeleteBot) {
                                allSuccess = false;
                                System.err.println("从数据库删除Bot失败: " + botNode.getId());
                            }
                            
                            // 从缓存中删除Bot节点
                            cacheManager.removeNode(botNode.getId());
                            System.out.println("已删除Bot: " + botNode.getId());
                            
                        } catch (Exception e) {
                            allSuccess = false;
                            System.err.println("删除Bot时出错: " + botNode.getId() + ", 错误: " + e.getMessage());
                        }
                    }
                }
            }
            
            // 从数据库删除任务
            boolean dbDeleteTask = genericCqnService.deleteTask(taskId);
            if (!dbDeleteTask) {
                allSuccess = false;
                System.err.println("从数据库删除任务失败: " + taskId);
            }
            
            // 从缓存中删除任务节点
            cacheManager.removeNode(taskId);
            System.out.println("已删除任务: " + taskId);
            
        } catch (Exception e) {
            allSuccess = false;
            System.err.println("删除任务时出错: " + taskId + ", 错误: " + e.getMessage());
        }
        
        return allSuccess;
    }
}
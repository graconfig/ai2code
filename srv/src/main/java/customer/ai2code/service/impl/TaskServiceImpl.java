package customer.ai2code.service.impl;

import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotType;
import cds.gen.mainservice.CreateTaskWithBotsContext;
import cds.gen.mainservice.MainService;
import cds.gen.configservice.ConfigService;
import cds.gen.mainservice.TaskType;
import cds.gen.configservice.BotTypes;
import customer.ai2code.model.Bot;
import customer.ai2code.model.GenericTask;
import customer.ai2code.model.Task;
import customer.ai2code.service.BotService;
import customer.ai2code.service.TaskService;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.impl.EntityService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class TaskServiceImpl implements TaskService {


    private final BotService botService;
    private final GenericCqnService genericCqnService;
    private final ContextService contextService;

    // 全局Task缓存链表
    private final Map<String, Task> taskCache = new ConcurrentHashMap<>();

    public TaskServiceImpl(
            BotService botService,
            GenericCqnService genericCqnService,
            ContextService contextService) {

        this.botService = botService;
        this.genericCqnService = genericCqnService;
        this.contextService = contextService;
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

        // 6. 放入缓存
        taskCache.put(newTask.getId(), task);
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
        BotInstances PBotInstance = bot.getBotInstance();

        // 2. 获取上级BotType
        BotType PBotType = PBotInstance.getType();

        // 3. 获取TaskType
        TaskType taskType = PBotType.getTaskType();

        // 3.1 获取下属BotType
        List<BotTypes> botTypes = genericCqnService.getBotTypesByTaskType(taskType.getId());

        // 4. 创建Task
        Tasks newTask = genericCqnService.createAndInsertSubTask(name, description, contextPath, 
                                                                sequence, botInstanceId, taskType.getId());

        // 5. 为每个BotType创建BotInstance
        for (BotTypes botType : botTypes) {
            genericCqnService.createAndInsertBotInstance(newTask.getId(), botType);
        }

        // 6.新建Task对象
        Task task = new GenericTask(newTask);

        // 7. 放入缓存
        taskCache.put(newTask.getId(), task);

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
        context.setResult(task.getTask());
        return task;
    }

    @Override
    public Task getCurrentTask(String taskId) {
        // 先从缓存中查找
        Task cachedTask = taskCache.get(taskId);
        if (cachedTask != null) {
            return cachedTask;
        }

        // 从数据库查询
        Tasks taskCDS = genericCqnService.getTaskById(taskId);
        Task task = new GenericTask(taskCDS);

        // 放入缓存
        taskCache.put(taskId, task);

        return task;
    }

    @Override
    public Task getCurrentTask(String botInstanceId, int sequence) {
        // 根据botInstanceId和sequence查询Task
        Tasks task = genericCqnService.getTaskByBotInstanceAndSequence(botInstanceId, sequence);
        return getCurrentTask(task.getId());
    }

    private void createBasicContextNodes(String taskId, String name, String description) {
        // 创建description节点
        contextService.upsertContext(taskId, "description", description);
    
    }
}
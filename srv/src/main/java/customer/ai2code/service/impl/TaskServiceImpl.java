package customer.ai2code.service.impl;

import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.Tasks_;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.BotType;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.ContextNodes_;
import cds.gen.mainservice.CreateTaskWithBotsContext;
import cds.gen.mainservice.MainService;
import cds.gen.configservice.ConfigService;
import cds.gen.configservice.TaskTypes;
import cds.gen.configservice.TaskTypes_;
import cds.gen.mainservice.TaskType;
import cds.gen.configservice.BotTypes;
import cds.gen.configservice.BotTypes_;
import customer.ai2code.model.Bot;
import customer.ai2code.model.GenericTask;
import customer.ai2code.model.Task;
import customer.ai2code.service.BotService;
import customer.ai2code.service.TaskService;
import customer.ai2code.service.impl.EntityService;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Insert;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class TaskServiceImpl implements TaskService {

    private final MainService mainService;
    private final ConfigService configService;
    private final EntityService entityService;
    private final BotService botService;

    // 全局Task缓存链表
    private final Map<String, Task> taskCache = new ConcurrentHashMap<>();

    public TaskServiceImpl(MainService mainService,
            ConfigService configService,
            EntityService entityService,
            BotService botService) {
        this.mainService = mainService;
        this.configService = configService;
        this.entityService = entityService;
        this.botService = botService;
    }

    /*
     * 创建主任务及其以下的Bot实例
     */
    @Override
    public Task createTaskWithBots(String name, String description, String taskTypeId) {
        // 1. 查询TaskType的botTypes


        var botTypesSelect = Select.from(BotTypes_.class)
                .where(b -> b.taskType_ID().eq(taskTypeId))
                .orderBy(b -> b.sequence().asc());
        List<BotTypes> botTypes = entityService.selectList(configService, botTypesSelect, BotTypes.class);

        // 2. 创建主Task
        Tasks newTask = Tasks.create();
        newTask.setId(UUID.randomUUID().toString());
        newTask.setName(name);
        newTask.setDescription(description);
        newTask.setIsMain(true);
        newTask.setContextPath("");
        newTask.setSequence(0);
        newTask.setTypeId(taskTypeId);

        entityService.insert(mainService, null, Tasks_.class, newTask, true);

        // 3. 为每个BotType创建BotInstance
        for (BotTypes botType : botTypes) {
            BotInstances botInstance = BotInstances.create();
            botInstance.setId(UUID.randomUUID().toString());
            botInstance.setSequence(botType.getSequence());
            botInstance.setTypeId(botType.getId());
            botInstance.setStatusCode("C"); // Created
            botInstance.setTaskId(newTask.getId());

            entityService.insert(mainService, null, BotInstances_.class, botInstance, true);
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
        var botTypesSelect = Select.from(BotTypes_.class)
                .where(b -> b.taskType_ID().eq(taskType.getId()))
                .orderBy(b -> b.sequence().asc());
        List<BotTypes> botTypes = entityService.selectList(configService, botTypesSelect, BotTypes.class);

        // 4. 创建Task

        Tasks newTask = Tasks.create();
        newTask.setId(UUID.randomUUID().toString());
        newTask.setName(name);
        newTask.setDescription(description);
        newTask.setIsMain(false); // 子任务
        newTask.setContextPath(contextPath);
        newTask.setSequence(sequence);
        newTask.setBotInstanceId(botInstanceId);
        newTask.setTypeId(taskType.getId());

        entityService.insert(mainService, null, Tasks_.class, newTask, true);

        // 5. 为每个BotType创建BotInstance
        for (BotTypes botType : botTypes) {
            BotInstances botInstance = BotInstances.create();
            botInstance.setId(UUID.randomUUID().toString());
            botInstance.setSequence(botType.getSequence());
            botInstance.setTypeId(botType.getId());
            botInstance.setStatusCode("C"); // Created
            botInstance.setTaskId(newTask.getId());

            entityService.insert(mainService, null, BotInstances_.class, botInstance, true);
        }

        // 6.新建Task对象
        Task task = new GenericTask(newTask);

        // 7. 放入缓存
        taskCache.put(newTask.getId(), task);

        // 8.返回新建的Task对象
        return task;
        // return

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
        // TaskTypes taskType = botType.getTaskType();
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
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method
        // 'createTaskWithBots'");
        // return createTaskWithBots(context.getName(), context.getDescription(), context.getTypeId());
        
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
        var select = Select.from(Tasks_.class).where(t -> t.ID().eq(taskId));
        Tasks taskCDS = entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found: " + taskId);
        Task task = new GenericTask(taskCDS);

        // 放入缓存
        taskCache.put(taskId, task);

        return task;
    }

    @Override
    public Task getCurrentTask(String botInstanceId, int sequence) {
        // 根据botInstanceId和sequence查询Task
        var select = Select.from(Tasks_.class)
                .where(t -> t.botInstance_ID().eq(botInstanceId).and(t.sequence().eq(sequence)));
        Tasks task = entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found for botInstance: " + botInstanceId + ", sequence: " + sequence);

        return getCurrentTask(task.getId());
    }

    private void createBasicContextNodes(String taskId, String name, String description) {
        // // 创建name节点
        // ContextNodes nameNode = ContextNodes.create();
        // nameNode.setId(UUID.randomUUID().toString());
        // nameNode.setTaskId(taskId);
        // nameNode.setPath("name");
        // nameNode.setLabel("Task Name");
        // nameNode.setType("string");
        // nameNode.setValue(name);

        // entityService.insert(mainService, null, ContextNodes_.class, nameNode, true);

        // 创建description节点
        ContextNodes descNode = ContextNodes.create();
        descNode.setId(UUID.randomUUID().toString());
        descNode.setTaskId(taskId);
        descNode.setPath("description");
        descNode.setLabel("Task Description");
        descNode.setType("markdown");
        descNode.setValue(description);

        entityService.insert(mainService, null, ContextNodes_.class, descNode, true);
    }

}
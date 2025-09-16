package customer.ai2code.service.impl.execution;

import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.websocket.TaskTreeChangedEvent;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;

import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.execution.TaskCreationParam;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.model.task.Task;
import customer.ai2code.service.BotService;
import customer.ai2code.service.TaskBotDataService;
import customer.ai2code.service.TaskService;

@BotExecutor(name = "Create Tasks Bot Execution", description = "Implementation for creating tasks in the bot execution framework", version = "1.0", enabled = true)
public class CreateTasksBotExecution implements BotExecution {

    // 任务服务实例
    private final TaskService taskService;
    // private final BotService botService;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskBotDataService taskBotDataService;

    public CreateTasksBotExecution(TaskService taskService,
            ApplicationEventPublisher eventPublisher, TaskBotDataService taskBotDataService) {
        this.taskService = taskService;
        // this.botService = botService;
        this.taskBotDataService = taskBotDataService;
        this.eventPublisher = eventPublisher;
    }

    @ExecuteMethod
    public List<String> execute(
            @ExecuteParameter(name = "botInstanceId", description = "Bot Instance") String botInstanceId,
            @ExecuteParameter(name = "taskCreationParams", description = "Array of task parameters") List<TaskCreationParam> taskCreationParams
    /**
     * 1.
     * sequence
     * 2.name
     * 3.description
     * 4.
     * contextPath
     */
    ) {

        List<String> tasks = new ArrayList<>();
        // throw new BusinessException("Unimplemented method 'execute'");
        // 调用BotService.createTaskWithBots(param);
        // if (taskCreationParams == null || taskCreationParams.isEmpty()) {
        if (taskCreationParams == null) {
            throw new BusinessException("Task creation parameters cannot be null");
        }
        if (botInstanceId == null || botInstanceId.isEmpty()) {
            throw new BusinessException("Bot instance ID cannot be null or empty");
        }

        // 遍历任务创建参数，创建任务
        for (TaskCreationParam param : taskCreationParams) {
            if (param.getSequence() == null || param.getName() == null || param.getDescription() == null) {
                throw new BusinessException("Task creation parameters must include sequence, name, and description");
            }
            // 调用任务服务创建任务
            tasks.add(taskService.createTaskWithBots(botInstanceId, param.getName(), param.getDescription(),
                    param.getContextPath(), param.getSequence()).getTask().getId());
        }

        // 发送任务创建事件
        eventPublisher.publishEvent(new TaskTreeChangedEvent(
                this,
                taskService.getMainTaskId(botInstanceId),
                taskService.buildBotHierarchy(taskBotDataService.getBotInstanceNode(botInstanceId))));

        return tasks;
    }
}
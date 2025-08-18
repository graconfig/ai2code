package customer.ai2code.service;

import cds.gen.mainservice.CreateTaskWithBotsContext;
import cds.gen.mainservice.TasksGetHierarchyContext;
import customer.ai2code.model.task.Task;

public interface TaskService {

    public Task createTaskWithBots(CreateTaskWithBotsContext context);

    public Task createTaskWithBots(String name, String description, String taskTypeId);

    public Task createTaskWithBots(String botInstanceId, String name, String description, String contextPath,
            int sequence);

    public Task createTaskWithBots(String botInstanceId, String name);

    /**
     * Get Current Task(with bots) by taskId.
     * 
     * @param taskId
     * @return
     */
    public Task getCurrentTask(String taskId);

    /**
     * Get Current Task(no bots) by botInstanceId(as parent) and sequence.
     * 
     * @param botInstanceId
     * @param sequence
     * @return
     */
    public Task getCurrentTask(String botInstanceId, int sequence);

    public String getHierarchy(String taskId);

    public String getHierarchy(TasksGetHierarchyContext context);

    public String getMainTaskId(String botInstanceId);

    public boolean deleteOriginalTasks(String botInstanceId);
}

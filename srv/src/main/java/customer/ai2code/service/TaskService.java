package customer.ai2code.service;

import customer.ai2code.model.Task;

public interface TaskService {
    public Task createTaskWithBots(String name, String description, String taskTypeId);

    public Task createTaskWithBots(String botInstanceId);

    public Task createTaskWithBots(String botInstanceId, String name);
}

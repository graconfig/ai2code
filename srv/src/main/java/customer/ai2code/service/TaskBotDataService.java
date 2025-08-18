package customer.ai2code.service;

import customer.ai2code.model.bot.Bot;
import customer.ai2code.model.task.Task;
import customer.ai2code.model.tree.TaskBotNode;

/**
 * 任务和Bot数据访问服务接口
 * 提供缓存查询功能，避免循环依赖
 */
public interface TaskBotDataService {
    
    /**
     * 根据Bot实例ID获取主任务ID
     */
    String getMainTaskId(String botInstanceId);
    
    /**
     * 根据Bot实例获取父任务
     */
    Task getParentTaskByBotInstance(String botInstanceId);
    
    /**
     * 获取缓存的Task对象
     */
    Task getCachedTask(String taskId);
    
    /**
     * 获取缓存的Bot对象
     */
    Bot getCachedBot(String botInstanceId);
    
    /**
     * 获取任务节点
     */
    TaskBotNode getTaskNode(String taskId);  
    /**
     * 获取Bot实例节点
     */
    TaskBotNode getBotInstanceNode(String botInstanceId);

    /**
     * 根据任务ID和序列号获取Bot实例节点
     * 用于快速定位特定任务下的Bot实例
     */
    TaskBotNode getBotInstanceByTaskAndSequence(String taskId, int sequence);

    /**
     * 根据Bot实例ID和序列号获取任务节点
     * 用于快速定位特定Bot实例下的任务
     */
    TaskBotNode getTaskByBotInstanceAndSequence(String botInstanceId, int sequence);
}

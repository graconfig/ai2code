package customer.ai2code.service.orchestration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import customer.ai2code.service.impl.TaskBotCacheManager;
import customer.ai2code.service.BotService;
import customer.ai2code.model.tree.TaskBotNode;
import customer.ai2code.model.tree.TaskBotNode.NodeType;
import customer.ai2code.model.bot.Bot;
import customer.ai2code.model.task.Task;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.ContextNodes;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 任务编排服务 - 自动执行框架核心
 * 基于现有TaskBotCacheManager实现轻量级自动执行引擎
 */
@Service
public class TaskOrchestrationService {

    @Autowired
    private TaskBotCacheManager taskBotCacheManager;
    
    @Autowired
    private TaskExecutionLogManager logManager;
    
    @Autowired
    private BotService botService;
    
    // 执行器线程池
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // 运行中的任务实例缓存
    private final Map<String, TaskExecutionContext> runningTasks = new ConcurrentHashMap<>();
    
    /**
     * 启动主任务自动执行
     * @param mainTaskId 主任务ID
     * @return 执行上下文
     */
    @Async
    public CompletableFuture<TaskExecutionResult> startMainTaskExecution(String mainTaskId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 创建执行上下文
                TaskExecutionContext context = new TaskExecutionContext(mainTaskId);
                runningTasks.put(mainTaskId, context);
                
                // 2. 开始执行日志记录
                logManager.startExecution(mainTaskId);
                
                // 3. 获取主任务节点
                TaskBotNode mainTaskNode = taskBotCacheManager.getTaskNode(mainTaskId);
                
                // 4. 递归执行任务树
                TaskExecutionResult result = executeTaskNode(mainTaskNode, context);
                
                // 5. 完成执行
                context.complete();
                logManager.completeExecution(mainTaskId, result);
                
                return result;
                
            } catch (Exception e) {
                logManager.failExecution(mainTaskId, e);
                throw new RuntimeException("Failed to execute main task: " + mainTaskId, e);
            }
        }, executorService);
    }
    
    /**
     * 递归执行任务节点
     */
    private TaskExecutionResult executeTaskNode(TaskBotNode taskNode, TaskExecutionContext context) {
        if (context.isPaused()) {
            return TaskExecutionResult.paused(taskNode.getId());
        }
        
        try {
            TaskExecutionResult result = TaskExecutionResult.success(taskNode.getId());
            
            // 1. 执行当前任务的所有Bot
            List<TaskBotNode> botNodes = taskBotCacheManager.getChildren(taskNode);
            for (TaskBotNode botNode : botNodes) {
                if (botNode.getType() == NodeType.BOT_INSTANCE) {
                    TaskExecutionResult botResult = executeBotNode(botNode, context);
                    result.addBotResult(botResult);
                    
                    // 检查是否有新的子任务生成（自动生成，无需手动添加）
                    List<TaskBotNode> subTaskNodes = taskBotCacheManager.getChildren(botNode);
                    for (TaskBotNode subTaskNode : subTaskNodes) {
                        if (subTaskNode.getType() == NodeType.TASK) {
                            TaskExecutionResult subResult = executeTaskNode(subTaskNode, context);
                            result.addSubTaskResult(subResult);
                        }
                    }
                }
            }
            
            return result;
            
        } catch (Exception e) {
            logManager.logError(taskNode.getId(), e);
            return TaskExecutionResult.failed(taskNode.getId(), e);
        }
    }
    
    /**
     * 执行Bot节点
     */
    private TaskExecutionResult executeBotNode(TaskBotNode botNode, TaskExecutionContext context) {
        if (context.isPaused()) {
            return TaskExecutionResult.paused(botNode.getId());
        }
        
        try {
            Bot bot = botNode.getBotObject();
            String botInstanceId = botNode.getId();
            
            // 记录开始执行
            logManager.startBotExecution(botInstanceId);
            
            String functionType = bot.getBotType().getFunctionTypeCode();
            
            // 根据Bot类型执行不同方法
            if ("A".equals(functionType)) {
                // A类型执行chat方法，然后调用adopt
                return executeChatBot(botInstanceId, context);
            } else if ("F".equals(functionType)) {
                // F类型执行execute方法
                return executeExecutionBot(botInstanceId, context);
            } else {
                // 其他类型暂时跳过
                logManager.logError(botInstanceId, new Exception("Unsupported bot type: " + functionType));
                return TaskExecutionResult.skipped(botInstanceId);
            }
            
        } catch (Exception e) {
            // 更新Bot状态为失败
            taskBotCacheManager.updateBotStatus(botNode.getId(), "F"); // FAILED
            logManager.logError(botNode.getId(), e);
            return TaskExecutionResult.failed(botNode.getId(), e);
        }
    }
    
    /**
     * 执行A类型Bot：chat + adopt
     */
    private TaskExecutionResult executeChatBot(String botInstanceId, TaskExecutionContext context) {
        try {
            // 1. 执行chat方法
            String chatContent = context.getChatContent();
            if (chatContent == null || chatContent.isEmpty()) {
                chatContent = "继续执行任务"; // 默认内容
            }
            
            BotMessages chatResult = botService.chat(botInstanceId, chatContent);
            logManager.logBotChat(botInstanceId, chatResult.getMessage());
            
            // 2. 调用adopt方法
            ContextNodes adoptResult = botService.adopt(botInstanceId, chatResult.getId());
            logManager.logBotExecution(botInstanceId, adoptResult);
            
            // 3. 更新Bot状态
            taskBotCacheManager.updateBotStatus(botInstanceId, "S"); // SUCCESS
            
            return TaskExecutionResult.success(botInstanceId, chatResult.getMessage(), null);
            
        } catch (Exception e) {
            taskBotCacheManager.updateBotStatus(botInstanceId, "F"); // FAILED
            throw e;
        }
    }
    
    /**
     * 执行F类型Bot：execute
     */
    private TaskExecutionResult executeExecutionBot(String botInstanceId, TaskExecutionContext context) {
        try {
            // 执行execute方法
            var executeResult = botService.execute(botInstanceId);
            logManager.logBotExecution(botInstanceId, executeResult);
            
            // 更新Bot状态
            taskBotCacheManager.updateBotStatus(botInstanceId, "S"); // SUCCESS
            
            return TaskExecutionResult.success(botInstanceId, null, executeResult);
            
        } catch (Exception e) {
            taskBotCacheManager.updateBotStatus(botInstanceId, "F"); // FAILED
            throw e;
        }
    }
    
    /**
     * 暂停任务执行
     */
    public void pauseExecution(String mainTaskId) {
        TaskExecutionContext context = runningTasks.get(mainTaskId);
        if (context != null) {
            context.pause();
            logManager.pauseExecution(mainTaskId);
        }
    }
    
    /**
     * 恢复任务执行
     */
    public CompletableFuture<TaskExecutionResult> resumeExecution(String mainTaskId) {
        TaskExecutionContext context = runningTasks.get(mainTaskId);
        if (context != null) {
            context.resume();
            logManager.resumeExecution(mainTaskId);
            
            // 从暂停点继续执行
            return continueExecution(mainTaskId, context);
        }
        
        // 如果没有找到上下文，重新开始
        return startMainTaskExecution(mainTaskId);
    }
    
    /**
     * 从暂停点继续执行
     */
    private CompletableFuture<TaskExecutionResult> continueExecution(String mainTaskId, TaskExecutionContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 获取当前执行点
                TaskBotNode currentNode = context.getCurrentNode();
                if (currentNode == null) {
                    currentNode = taskBotCacheManager.getTaskNode(mainTaskId);
                }
                
                // 继续执行
                return executeTaskNode(currentNode, context);
                
            } catch (Exception e) {
                logManager.failExecution(mainTaskId, e);
                throw new RuntimeException("Failed to resume task execution: " + mainTaskId, e);
            }
        }, executorService);
    }
    
    /**
     * 获取任务执行状态
     */
    public TaskExecutionStatus getExecutionStatus(String mainTaskId) {
        TaskExecutionContext context = runningTasks.get(mainTaskId);
        if (context == null) {
            return TaskExecutionStatus.NOT_STARTED;
        }
        
        return context.getStatus();
    }
    
    /**
     * 获取任务执行日志
     */
    public TaskExecutionLog getExecutionLog(String mainTaskId) {
        return logManager.getExecutionLog(mainTaskId);
    }
    
    /**
     * 清理完成的任务
     */
    public void cleanupCompletedTask(String mainTaskId) {
        TaskExecutionContext context = runningTasks.remove(mainTaskId);
        if (context != null) {
            context.cleanup();
        }
        logManager.archiveExecutionLog(mainTaskId);
    }
    
    public enum TaskExecutionStatus {
        NOT_STARTED, RUNNING, PAUSED, COMPLETED, FAILED, UNKNOWN
    }
}

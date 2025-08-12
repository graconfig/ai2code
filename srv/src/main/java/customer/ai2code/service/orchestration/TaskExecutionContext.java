package customer.ai2code.service.orchestration;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import customer.ai2code.model.tree.TaskBotNode;
import customer.ai2code.model.task.Task;
import cds.gen.mainservice.BotInstancesExecuteContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 任务执行上下文
 * 维护单个主任务的执行状态和上下文信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionContext {
    
    private String mainTaskId;
    private TaskOrchestrationService.TaskExecutionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime pauseTime;
    private LocalDateTime resumeTime;
    private TaskBotNode currentNode;
    private String chatContent;
    
    // 暂停/恢复控制
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    
    // 动态添加的任务队列
    private final ConcurrentLinkedQueue<Task> dynamicTasks = new ConcurrentLinkedQueue<>();
    
    public TaskExecutionContext(String mainTaskId) {
        this.mainTaskId = mainTaskId;
        this.status = TaskOrchestrationService.TaskExecutionStatus.RUNNING;
        this.startTime = LocalDateTime.now();
        this.chatContent = "开始执行任务";
    }
    
    public boolean isPaused() {
        return paused.get();
    }
    
    public void pause() {
        paused.set(true);
        pauseTime = LocalDateTime.now();
        status = TaskOrchestrationService.TaskExecutionStatus.PAUSED;
    }
    
    public void resume() {
        paused.set(false);
        resumeTime = LocalDateTime.now();
        status = TaskOrchestrationService.TaskExecutionStatus.RUNNING;
    }
    
    public void complete() {
        completed.set(true);
        status = TaskOrchestrationService.TaskExecutionStatus.COMPLETED;
    }
    
    public void fail() {
        status = TaskOrchestrationService.TaskExecutionStatus.FAILED;
    }
    
    public boolean isCompleted() {
        return completed.get();
    }
    
    public void addDynamicTask(Task task) {
        dynamicTasks.offer(task);
    }
    
    public Task pollDynamicTask() {
        return dynamicTasks.poll();
    }
    
    public boolean hasDynamicTasks() {
        return !dynamicTasks.isEmpty();
    }
    
    public void updateChatContent(String content) {
        this.chatContent = content;
    }
    
    public void cleanup() {
        dynamicTasks.clear();
        currentNode = null;
    }
}

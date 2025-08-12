package customer.ai2code.service.orchestration;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import cds.gen.mainservice.BotInstancesExecuteContext;

import java.util.List;
import java.util.ArrayList;

/**
 * 任务执行结果
 * 记录单个任务或Bot的执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionResult {
    
    private String nodeId;
    private TaskExecutionStatus status;
    private String chatResult;
    private BotInstancesExecuteContext.ReturnType executeResult;
    private Exception error;
    private long executionTime;
    
    // 子结果集合
    @Builder.Default
    private List<TaskExecutionResult> botResults = new ArrayList<>();
    
    @Builder.Default
    private List<TaskExecutionResult> subTaskResults = new ArrayList<>();
    
    public enum TaskExecutionStatus {
        SUCCESS, FAILED, PAUSED, SKIPPED
    }
    
    // 静态工厂方法
    public static TaskExecutionResult success(String nodeId) {
        return TaskExecutionResult.builder()
            .nodeId(nodeId)
            .status(TaskExecutionStatus.SUCCESS)
            .build();
    }
    
    public static TaskExecutionResult success(String nodeId, String chatResult, BotInstancesExecuteContext.ReturnType executeResult) {
        return TaskExecutionResult.builder()
            .nodeId(nodeId)
            .status(TaskExecutionStatus.SUCCESS)
            .chatResult(chatResult)
            .executeResult(executeResult)
            .build();
    }
    
    public static TaskExecutionResult failed(String nodeId, Exception error) {
        return TaskExecutionResult.builder()
            .nodeId(nodeId)
            .status(TaskExecutionStatus.FAILED)
            .error(error)
            .build();
    }
    
    public static TaskExecutionResult paused(String nodeId) {
        return TaskExecutionResult.builder()
            .nodeId(nodeId)
            .status(TaskExecutionStatus.PAUSED)
            .build();
    }
    
    public static TaskExecutionResult skipped(String nodeId) {
        return TaskExecutionResult.builder()
            .nodeId(nodeId)
            .status(TaskExecutionStatus.SKIPPED)
            .build();
    }
    
    public void addBotResult(TaskExecutionResult botResult) {
        this.botResults.add(botResult);
    }
    
    public void addSubTaskResult(TaskExecutionResult subTaskResult) {
        this.subTaskResults.add(subTaskResult);
    }
    
    public boolean isSuccess() {
        return status == TaskExecutionStatus.SUCCESS;
    }
    
    public boolean isFailed() {
        return status == TaskExecutionStatus.FAILED;
    }
    
    public boolean isPaused() {
        return status == TaskExecutionStatus.PAUSED;
    }
}

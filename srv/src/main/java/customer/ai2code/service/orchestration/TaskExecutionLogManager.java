package customer.ai2code.service.orchestration;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 任务执行日志管理器
 * 负责记录和管理任务执行过程中的所有日志信息
 */
@Slf4j
@Service
public class TaskExecutionLogManager {
    
    // 执行日志缓存：mainTaskId -> TaskExecutionLog
    private final Map<String, TaskExecutionLog> executionLogs = new ConcurrentHashMap<>();
    
    // 步骤日志缓存：mainTaskId -> List<StepLog>
    private final Map<String, List<StepLog>> stepLogs = new ConcurrentHashMap<>();
    
    /**
     * 开始执行记录
     */
    public void startExecution(String mainTaskId) {
        TaskExecutionLog executionLog = TaskExecutionLog.builder()
            .mainTaskId(mainTaskId)
            .status("RUNNING")
            .startTime(LocalDateTime.now())
            .totalSteps(0)
            .completedSteps(0)
            .failedSteps(0)
            .build();
        
        executionLogs.put(mainTaskId, executionLog);
        stepLogs.put(mainTaskId, new ArrayList<>());
        
        log.info("开始执行主任务: {}", mainTaskId);
    }
    
    /**
     * 完成执行记录
     */
    public void completeExecution(String mainTaskId, TaskExecutionResult result) {
        TaskExecutionLog executionLog = executionLogs.get(mainTaskId);
        if (executionLog != null) {
            executionLog.setStatus("COMPLETED");
            executionLog.setEndTime(LocalDateTime.now());
            
            if (executionLog.getStartTime() != null) {
                long duration = java.time.Duration.between(executionLog.getStartTime(), executionLog.getEndTime()).toMillis();
                executionLog.setDuration(duration);
            }
            
            log.info("主任务执行完成: {}, 耗时: {}ms", mainTaskId, executionLog.getDuration());
        }
    }
    
    /**
     * 执行失败记录
     */
    public void failExecution(String mainTaskId, Exception error) {
        TaskExecutionLog executionLog = executionLogs.get(mainTaskId);
        if (executionLog != null) {
            executionLog.setStatus("FAILED");
            executionLog.setEndTime(LocalDateTime.now());
            executionLog.setErrorMessage(error.getMessage());
            
            if (executionLog.getStartTime() != null) {
                long duration = java.time.Duration.between(executionLog.getStartTime(), executionLog.getEndTime()).toMillis();
                executionLog.setDuration(duration);
            }
            
            log.error("主任务执行失败: {}, 错误: {}", mainTaskId, error.getMessage(), error);
        }
    }
    
    /**
     * 暂停执行记录
     */
    public void pauseExecution(String mainTaskId) {
        TaskExecutionLog executionLog = executionLogs.get(mainTaskId);
        if (executionLog != null) {
            executionLog.setStatus("PAUSED");
            log.info("主任务执行暂停: {}", mainTaskId);
        }
    }
    
    /**
     * 恢复执行记录
     */
    public void resumeExecution(String mainTaskId) {
        TaskExecutionLog executionLog = executionLogs.get(mainTaskId);
        if (executionLog != null) {
            executionLog.setStatus("RUNNING");
            log.info("主任务执行恢复: {}", mainTaskId);
        }
    }
    
    /**
     * 开始Bot执行记录
     */
    public void startBotExecution(String botInstanceId) {
        log.info("开始执行Bot: {}", botInstanceId);
    }
    
    /**
     * Bot聊天日志记录
     */
    public void logBotChat(String botInstanceId, String chatResult) {
        log.info("Bot {} 聊天结果: {}", botInstanceId, 
            chatResult != null && chatResult.length() > 100 ? 
            chatResult.substring(0, 100) + "..." : chatResult);
    }
    
    /**
     * Bot执行日志记录
     */
    public void logBotExecution(String botInstanceId, Object executeResult) {
        log.info("Bot {} 执行结果: {}", botInstanceId, executeResult);
    }
    
    /**
     * 错误日志记录
     */
    public void logError(String nodeId, Exception error) {
        log.error("节点 {} 执行出错: {}", nodeId, error.getMessage(), error);
    }
    
    /**
     * 动态任务添加日志
     */
    public void logDynamicTaskAddition(String mainTaskId, String newTaskId) {
        log.info("主任务 {} 动态添加子任务: {}", mainTaskId, newTaskId);
    }
    
    /**
     * 获取执行日志
     */
    public TaskExecutionLog getExecutionLog(String mainTaskId) {
        return executionLogs.get(mainTaskId);
    }
    
    /**
     * 归档执行日志
     */
    public void archiveExecutionLog(String mainTaskId) {
        TaskExecutionLog executionLog = executionLogs.remove(mainTaskId);
        List<StepLog> steps = stepLogs.remove(mainTaskId);
        
        if (executionLog != null) {
            // 这里可以实现日志持久化到数据库
            log.info("归档主任务执行日志: {}", mainTaskId);
        }
    }
    
    /**
     * 步骤日志内部类
     */
    public static class StepLog {
        @SuppressWarnings("unused")
        private String stepId;
        @SuppressWarnings("unused")
        private String stepType; // TASK, BOT, CHAT, EXECUTE
        @SuppressWarnings("unused")
        private LocalDateTime startTime;
        @SuppressWarnings("unused")
        private LocalDateTime endTime;
        @SuppressWarnings("unused")
        private String status;
        @SuppressWarnings("unused")
        private String result;
        @SuppressWarnings("unused")
        private String errorMessage;
        
        // getter/setter 省略
    }
}

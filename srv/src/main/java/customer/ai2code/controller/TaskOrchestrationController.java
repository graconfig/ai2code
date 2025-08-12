package customer.ai2code.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import customer.ai2code.service.orchestration.TaskOrchestrationService;
import customer.ai2code.service.orchestration.TaskExecutionResult;
import customer.ai2code.service.orchestration.TaskExecutionLog;

import java.util.concurrent.CompletableFuture;

/**
 * 任务编排控制器
 * 提供任务自动执行的REST API
 */
@RestController
@RequestMapping("/api/orchestration")
public class TaskOrchestrationController {

    @Autowired
    private TaskOrchestrationService orchestrationService;

    /**
     * 启动主任务自动执行
     */
    @PostMapping("/tasks/{mainTaskId}/start")
    public CompletableFuture<TaskExecutionResult> startExecution(@PathVariable String mainTaskId) {
        return orchestrationService.startMainTaskExecution(mainTaskId);
    }

    /**
     * 暂停任务执行
     */
    @PostMapping("/tasks/{mainTaskId}/pause")
    public void pauseExecution(@PathVariable String mainTaskId) {
        orchestrationService.pauseExecution(mainTaskId);
    }

    /**
     * 恢复任务执行
     */
    @PostMapping("/tasks/{mainTaskId}/resume")
    public CompletableFuture<TaskExecutionResult> resumeExecution(@PathVariable String mainTaskId) {
        return orchestrationService.resumeExecution(mainTaskId);
    }

    /**
     * 获取任务执行状态
     */
    @GetMapping("/tasks/{mainTaskId}/status")
    public TaskOrchestrationService.TaskExecutionStatus getStatus(@PathVariable String mainTaskId) {
        return orchestrationService.getExecutionStatus(mainTaskId);
    }

    /**
     * 获取任务执行日志
     */
    @GetMapping("/tasks/{mainTaskId}/logs")
    public TaskExecutionLog getLogs(@PathVariable String mainTaskId) {
        return orchestrationService.getExecutionLog(mainTaskId);
    }

    /**
     * 清理完成的任务
     */
    @DeleteMapping("/tasks/{mainTaskId}")
    public void cleanupTask(@PathVariable String mainTaskId) {
        orchestrationService.cleanupCompletedTask(mainTaskId);
    }
}

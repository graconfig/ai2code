package customer.ai2code.service.batch;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

/**
 * Spring Batch任务编排控制器
 * 提供基于Spring Batch的任务自动执行REST API
 */
@RestController
@RequestMapping("/api/batch")
public class BatchTaskOrchestrationController {

    @Autowired
    private BatchTaskOrchestrationService batchOrchestrationService;

    /**
     * 启动主任务自动执行
     */
    @PostMapping("/tasks/{mainTaskId}/start")
    public CompletableFuture<JobExecution> startExecution(@PathVariable String mainTaskId) {
        return batchOrchestrationService.startMainTaskExecution(mainTaskId);
    }

    /**
     * 暂停任务执行
     */
    @PostMapping("/tasks/{mainTaskId}/pause")
    public ResponseEntity<Map<String, String>> pauseExecution(@PathVariable String mainTaskId) {
        try {
            batchOrchestrationService.pauseExecution(mainTaskId);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "任务暂停成功");
            response.put("taskId", mainTaskId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "暂停任务失败: " + e.getMessage());
            errorResponse.put("taskId", mainTaskId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 停止任务执行
     */
    @PostMapping("/tasks/{mainTaskId}/stop")
    public void stopExecution(@PathVariable String mainTaskId) {
        batchOrchestrationService.stopExecution(mainTaskId);
    }

    /**
     * 重启任务执行
     */
    @PostMapping("/tasks/{mainTaskId}/restart")
    public CompletableFuture<JobExecution> restartExecution(@PathVariable String mainTaskId) {
        return batchOrchestrationService.restartExecution(mainTaskId);
    }

    /**
     * 获取任务执行状态
     */
    @GetMapping("/tasks/{mainTaskId}/status")
    public BatchStatus getStatus(@PathVariable String mainTaskId) {
        return batchOrchestrationService.getExecutionStatus(mainTaskId);
    }

    /**
     * 获取任务执行详情
     */
    @GetMapping("/tasks/{mainTaskId}/execution")
    public JobExecution getJobExecution(@PathVariable String mainTaskId) {
        return batchOrchestrationService.getJobExecution(mainTaskId);
    }

    /**
     * 清理完成的任务
     */
    @DeleteMapping("/tasks/{mainTaskId}")
    public void cleanupTask(@PathVariable String mainTaskId) {
        batchOrchestrationService.cleanupCompletedTask(mainTaskId);
    }
}

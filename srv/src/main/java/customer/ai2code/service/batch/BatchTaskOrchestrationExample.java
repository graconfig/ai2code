package customer.ai2code.service.batch;

import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Spring Batch任务编排示例
 * 展示如何使用Spring Batch框架进行任务执行
 */
@Service
public class BatchTaskOrchestrationExample {

    @Autowired
    private BatchTaskOrchestrationService batchOrchestrationService;

    /**
     * 示例：启动任务自动执行
     */
    public void exampleBatchExecution(String mainTaskId) {
        // 1. 启动主任务自动执行
        CompletableFuture<JobExecution> future = 
            batchOrchestrationService.startMainTaskExecution(mainTaskId);
        
        // 2. 异步处理结果
        future.thenAccept(jobExecution -> {
            System.out.println("Job启动成功:");
            System.out.println("- Job ID: " + jobExecution.getId());
            System.out.println("- Job Name: " + jobExecution.getJobInstance().getJobName());
            System.out.println("- Status: " + jobExecution.getStatus());
            System.out.println("- Start Time: " + jobExecution.getStartTime());
        }).exceptionally(throwable -> {
            System.err.println("Job启动失败: " + throwable.getMessage());
            return null;
        });
        
        // 3. 可以在需要时停止
        // batchOrchestrationService.stopExecution(mainTaskId);
        
        // 4. 或者重启执行
        // batchOrchestrationService.restartExecution(mainTaskId);
    }

    /**
     * 示例：监控任务执行状态
     */
    public void exampleBatchMonitoring(String mainTaskId) {
        // 获取执行状态
        var status = batchOrchestrationService.getExecutionStatus(mainTaskId);
        System.out.println("Batch任务状态: " + status);
        
        // 获取Job执行详情
        JobExecution jobExecution = batchOrchestrationService.getJobExecution(mainTaskId);
        if (jobExecution != null) {
            System.out.println("Job执行详情:");
            System.out.println("- 执行时长: " + 
                (jobExecution.getEndTime() != null && jobExecution.getStartTime() != null ?
                java.time.Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis() : "进行中") + "ms");
            System.out.println("- Step数量: " + jobExecution.getStepExecutions().size());
            System.out.println("- 失败异常数: " + jobExecution.getAllFailureExceptions().size());
        }
    }
}

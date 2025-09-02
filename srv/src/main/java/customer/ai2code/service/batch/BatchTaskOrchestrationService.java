package customer.ai2code.service.batch;

import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import customer.ai2code.service.impl.TaskBotCacheManager;
import customer.ai2code.model.tree.TaskBotNode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Spring Batch任务编排服务
 * 使用Spring Batch框架实现任务自动执行
 */
@Service
public class BatchTaskOrchestrationService {

    @Autowired
    @Qualifier("asyncJobLauncher")
    private JobLauncher jobLauncher;
    
    @Autowired
    private TaskBotCacheManager taskBotCacheManager;
    
    @Autowired
    private BatchStepFlowConfiguration stepFlowConfiguration;
    
    // 运行中的Job实例缓存
    private final Map<String, JobExecution> runningJobs = new ConcurrentHashMap<>();
    
    /**
     * 启动主任务自动执行
     * @param mainTaskId 主任务ID
     * @return JobExecution 执行实例
     */
    public CompletableFuture<JobExecution> startMainTaskExecution(String mainTaskId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 获取主任务节点
                TaskBotNode mainTaskNode = taskBotCacheManager.getTaskNode(mainTaskId);
                
                // 2. 构建动态Job (支持运行时添加步骤)
                Job job = buildDynamicTaskExecutionJob(mainTaskNode);
                
                // 3. 准备Job参数
                JobParameters jobParameters = new JobParametersBuilder()
                    .addString("mainTaskId", mainTaskId)
                    .addLong("startTime", System.currentTimeMillis())
                    .addString("executionType", "start")
                    .toJobParameters();
                
                // 4. 启动Job执行
                JobExecution jobExecution = jobLauncher.run(job, jobParameters);
                
                // 5. 缓存运行实例
                runningJobs.put(mainTaskId, jobExecution);
                
                return jobExecution;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to start main task execution: " + mainTaskId, e);
            }
        });
    }
    
    /**
     * 构建动态任务执行Job (支持运行时动态添加步骤)
     * 使用配置类中的Step和Flow组件
     */
    private Job buildDynamicTaskExecutionJob(TaskBotNode mainTaskNode) {
        // 使用Flow配置类构建完整的Job结构 - 支持ChatBot Step和Function Call Bot Flow
        return stepFlowConfiguration.buildMainTaskJob(mainTaskNode.getId(), mainTaskNode);
    }
    
    /**
     * 暂停任务执行
     */
    public void pauseExecution(String mainTaskId) {
        JobExecution jobExecution = runningJobs.get(mainTaskId);
        if (jobExecution != null && jobExecution.isRunning()) {
            System.out.println("暂停任务执行: " + mainTaskId);
            jobExecution.setStatus(BatchStatus.STOPPING);
            
            // 标记当前正在执行的Bot状态为暂停
            // markCurrentBotsAsPaused(jobExecution);
        }
    }
    
    /**
     * 标记当前正在执行的Bot为暂停状态
     */
    private void markCurrentBotsAsPaused(JobExecution jobExecution) {
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if (stepExecution.getStatus() == BatchStatus.STARTED) {
                String stepName = stepExecution.getStepName();
                String botInstanceId = extractBotInstanceIdFromStepName(stepName);
                if (botInstanceId != null) {
                    // 标记为暂停状态
                    taskBotCacheManager.updateBotStatus(botInstanceId, "P"); // P表示暂停
                    System.out.println("标记Bot为暂停状态: " + botInstanceId);
                }
            }
        }
    }

    /**
     * 停止任务执行
     */
    public void stopExecution(String mainTaskId) {
        JobExecution jobExecution = runningJobs.get(mainTaskId);
        if (jobExecution != null && jobExecution.isRunning()) {
            System.out.println("停止任务执行: " + mainTaskId);
            jobExecution.setStatus(BatchStatus.STOPPING);
        }
    }
    
    /**
     * 智能重启任务执行 - 从失败或暂停的step继续执行
     */
    public CompletableFuture<JobExecution> restartExecution(String mainTaskId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JobExecution lastExecution = runningJobs.get(mainTaskId);
                
                if (lastExecution != null) {
                    BatchStatus lastStatus = lastExecution.getStatus();
                    
                    // 检查是否可以重启
                    if (lastStatus == BatchStatus.FAILED || 
                        lastStatus == BatchStatus.STOPPED || 
                        lastStatus == BatchStatus.STOPPING) {
                        
                        System.out.println("从" + lastStatus + "状态重启任务: " + mainTaskId);
                        
                        // 分析上次执行的失败点
                        analyzeFailurePoints(lastExecution);
                        
                        // 创建重启Job参数，包含上次执行的上下文
                        JobParameters restartJobParameters = new JobParametersBuilder()
                            .addString("mainTaskId", mainTaskId)
                            .addLong("restartTime", System.currentTimeMillis())
                            .addString("executionType", "restart")
                            .addLong("lastExecutionId", lastExecution.getId())
                            .toJobParameters();
                        
                        // 构建动态Job，会自动跳过已完成的Bot
                        TaskBotNode mainTaskNode = taskBotCacheManager.getTaskNode(mainTaskId);
                        Job restartJob = buildDynamicTaskExecutionJob(mainTaskNode);
                        
                        // 启动重启执行
                        JobExecution newExecution = jobLauncher.run(restartJob, restartJobParameters);
                        
                        // 更新缓存
                        runningJobs.put(mainTaskId, newExecution);
                        
                        return newExecution;
                        
                    } else if (lastStatus == BatchStatus.COMPLETED) {
                        System.out.println("任务已完成，无需重启: " + mainTaskId);
                        return lastExecution;
                        
                    } else if (lastStatus == BatchStatus.STARTED || lastStatus == BatchStatus.STARTING) {
                        System.out.println("任务正在运行中，无需重启: " + mainTaskId);
                        return lastExecution;
                    }
                }
                
                // 如果没有找到上次执行或状态不支持重启，重新开始
                System.out.println("未找到可重启的执行记录，重新开始任务: " + mainTaskId);
                return startMainTaskExecution(mainTaskId).get();
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to restart task execution: " + mainTaskId, e);
            }
        });
    }
    
    /**
     * 分析失败点，用于重启时的智能恢复
     */
    private void analyzeFailurePoints(JobExecution lastExecution) {
        System.out.println("分析上次执行的失败点...");
        
        // 遍历所有Step执行记录
        for (StepExecution stepExecution : lastExecution.getStepExecutions()) {
            BatchStatus stepStatus = stepExecution.getStatus();
            String stepName = stepExecution.getStepName();
            
            if (stepStatus == BatchStatus.FAILED) {
                System.out.println("发现失败步骤: " + stepName + ", 退出描述: " + stepExecution.getExitStatus().getExitDescription());
                
                // 提取Bot实例ID并重置状态
                if (stepName.contains("botStep-")) {
                    String botInstanceId = extractBotInstanceIdFromStepName(stepName);
                    if (botInstanceId != null) {
                        // 重置失败Bot的状态，以便重启时重新执行
                        taskBotCacheManager.updateBotStatus(botInstanceId, "I"); // I表示初始状态
                        System.out.println("重置失败Bot状态: " + botInstanceId);
                    }
                }
            } else if (stepStatus == BatchStatus.COMPLETED) {
                System.out.println("已完成步骤: " + stepName + " (重启时将跳过)");
            }
        }
    }
    
    /**
     * 从步骤名称中提取Bot实例ID
     */
    private String extractBotInstanceIdFromStepName(String stepName) {
        try {
            // 步骤名称格式: "botStep-{botInstanceId}-{sequence}"
            if (stepName.startsWith("botStep-")) {
                String[] parts = stepName.split("-");
                if (parts.length >= 2) {
                    return parts[1]; // 提取botInstanceId部分
                }
            }
        } catch (Exception e) {
            System.err.println("提取Bot实例ID失败: " + stepName + ", " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取任务执行状态
     */
    public BatchStatus getExecutionStatus(String mainTaskId) {
        JobExecution jobExecution = runningJobs.get(mainTaskId);
        return jobExecution != null ? jobExecution.getStatus() : BatchStatus.UNKNOWN;
    }
    
    /**
     * 获取任务执行信息
     */
    public JobExecution getJobExecution(String mainTaskId) {
        return runningJobs.get(mainTaskId);
    }
    
    /**
     * 清理完成的任务
     */
    public void cleanupCompletedTask(String mainTaskId) {
        runningJobs.remove(mainTaskId);
    }
}

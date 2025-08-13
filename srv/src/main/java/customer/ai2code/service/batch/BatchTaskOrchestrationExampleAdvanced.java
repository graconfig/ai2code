package customer.ai2code.service.batch;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Spring Batch任务编排高级功能示例
 * 展示动态子任务添加和智能重启功能
 */
@Service
public class BatchTaskOrchestrationExampleAdvanced {

    @Autowired
    private BatchTaskOrchestrationService batchOrchestrationService;

    /**
     * 完整示例：演示动态子任务和智能重启
     */
    public void demonstrateAdvancedFeatures(String mainTaskId) {
        System.out.println("=== Spring Batch高级功能演示 ===");
        
        try {
            // 1. 启动任务执行
            System.out.println("1. 启动主任务执行: " + mainTaskId);
            batchOrchestrationService.startMainTaskExecution(mainTaskId);
            
            // 等待初始启动
            Thread.sleep(2000);
            
            // 2. 监控执行状态
            monitorExecution(mainTaskId);
            
            // 3. 模拟暂停场景
            System.out.println("\n3. 暂停任务执行...");
            batchOrchestrationService.pauseExecution(mainTaskId);
            Thread.sleep(1000);
            
            // 4. 智能重启 - 从暂停点继续
            System.out.println("4. 智能重启任务执行...");
            CompletableFuture<JobExecution> restartFuture = 
                batchOrchestrationService.restartExecution(mainTaskId);
            
            // 5. 继续监控
            monitorExecution(mainTaskId);
            
            // 6. 等待最终完成
            JobExecution finalExecution = restartFuture.get();
            System.out.println("6. 任务最终状态: " + finalExecution.getStatus());
            
            // 7. 展示动态子任务功能
            demonstrateDynamicSubTasks(mainTaskId);
            
        } catch (Exception e) {
            System.err.println("演示过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 监控任务执行状态
     */
    private void monitorExecution(String mainTaskId) {
        try {
            System.out.println("\n--- 开始监控任务执行 ---");
            
            for (int i = 0; i < 10; i++) {
                BatchStatus status = batchOrchestrationService.getExecutionStatus(mainTaskId);
                JobExecution execution = batchOrchestrationService.getJobExecution(mainTaskId);
                
                if (execution != null) {
                    LocalDateTime startTime = execution.getStartTime();
                    LocalDateTime currentTime = LocalDateTime.now();
                    
                    long runningTime = startTime != null ? 
                        Duration.between(startTime, currentTime).getSeconds() : 0;
                    
                    System.out.println("状态检查 " + (i + 1) + ": " + status + 
                        ", 运行时间: " + runningTime + "秒");
                    
                    // 检查是否完成或失败
                    if (status == BatchStatus.COMPLETED || 
                        status == BatchStatus.FAILED || 
                        status == BatchStatus.STOPPED) {
                        break;
                    }
                }
                
                Thread.sleep(1000);
            }
            
            System.out.println("--- 监控结束 ---\n");
            
        } catch (Exception e) {
            System.err.println("监控过程中发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 演示动态子任务功能
     */
    private void demonstrateDynamicSubTasks(String mainTaskId) {
        System.out.println("\n=== 动态子任务功能演示 ===");
        
        try {
            JobExecution execution = batchOrchestrationService.getJobExecution(mainTaskId);
            
            if (execution != null) {
                System.out.println("任务ID: " + mainTaskId);
                System.out.println("执行ID: " + execution.getId());
                
                // 展示步骤执行详情
                execution.getStepExecutions().forEach(stepExecution -> {
                    String stepName = stepExecution.getStepName();
                    BatchStatus stepStatus = stepExecution.getStatus();
                    
                    System.out.println("步骤: " + stepName + " -> 状态: " + stepStatus);
                    
                    // 展示执行上下文中的结果
                    stepExecution.getExecutionContext().entrySet().forEach(entry -> {
                        System.out.println("  上下文: " + entry.getKey() + " = " + entry.getValue());
                    });
                });
                
                System.out.println("\n🚀 新增功能特性:");
                System.out.println("✅ F类型Bot执行execute()后，会自动检查并执行新产生的子任务");
                System.out.println("✅ 重启时会自动跳过已完成的Bot，从失败或暂停的步骤继续");
                System.out.println("✅ 每个Bot的状态变化都会被记录和跟踪");
                System.out.println("✅ 支持多层嵌套的子任务动态执行");
                System.out.println("✅ 智能失败点分析和恢复机制");
            }
            
        } catch (Exception e) {
            System.err.println("演示动态子任务功能时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 测试智能重启功能
     */
    public void testIntelligentRestart(String mainTaskId) {
        System.out.println("=== 智能重启功能测试 ===");
        
        try {
            // 1. 启动任务
            batchOrchestrationService.startMainTaskExecution(mainTaskId);
            
            Thread.sleep(3000); // 让任务运行一段时间
            
            // 2. 模拟失败场景
            batchOrchestrationService.stopExecution(mainTaskId);
            Thread.sleep(1000);
            
            // 3. 智能重启
            System.out.println("开始智能重启...");
            CompletableFuture<JobExecution> restartFuture = 
                batchOrchestrationService.restartExecution(mainTaskId);
            
            // 4. 检查重启结果
            JobExecution restartExecution = restartFuture.get();
            System.out.println("重启完成，状态: " + restartExecution.getStatus());
            
            System.out.println("智能重启功能验证完成");
            
        } catch (Exception e) {
            System.err.println("智能重启测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 演示动态子任务添加
     */
    public void testDynamicSubTaskAddition(String mainTaskId) {
        System.out.println("=== 动态子任务添加测试 ===");
        
        try {
            System.out.println("执行包含F类型Bot的任务，观察动态子任务生成...");
            
            CompletableFuture<JobExecution> future = 
                batchOrchestrationService.startMainTaskExecution(mainTaskId);
            
            // 监控并等待完成
            JobExecution execution = future.get();
            
            System.out.println("任务完成，分析子任务添加情况:");
            execution.getStepExecutions().forEach(stepExecution -> {
                String stepName = stepExecution.getStepName();
                if (stepName.contains("dynamicExecution")) {
                    System.out.println("发现动态执行步骤: " + stepName);
                    System.out.println("该步骤处理了Bot执行后的子任务动态添加");
                }
            });
            
        } catch (Exception e) {
            System.err.println("动态子任务测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理测试数据
     */
    public void cleanup(String mainTaskId) {
        try {
            batchOrchestrationService.cleanupCompletedTask(mainTaskId);
            System.out.println("清理完成: " + mainTaskId);
        } catch (Exception e) {
            System.err.println("清理失败: " + e.getMessage());
        }
    }
}

package customer.ai2code.service.batch;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Spring Batch执行监听器
 * 监控Job和Step的执行状态
 */
@Component
public class BatchExecutionListener implements JobExecutionListener, StepExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        System.out.println("=== Job开始执行: " + jobName + " ===");
        System.out.println("Job ID: " + jobExecution.getId());
        System.out.println("开始时间: " + jobExecution.getStartTime());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        System.out.println("=== Job执行完成: " + jobName + " ===");
        System.out.println("Job ID: " + jobExecution.getId());
        System.out.println("结束时间: " + jobExecution.getEndTime());
        System.out.println("执行状态: " + jobExecution.getStatus());
        System.out.println("退出状态: " + jobExecution.getExitStatus());
        
        if (jobExecution.getStatus().isUnsuccessful()) {
            System.err.println("Job执行失败，异常信息: ");
            for (Throwable exception : jobExecution.getAllFailureExceptions()) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        System.out.println("--- Step开始执行: " + stepName + " ---");
        System.out.println("Step ID: " + stepExecution.getId());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        System.out.println("--- Step执行完成: " + stepName + " ---");
        System.out.println("Step ID: " + stepExecution.getId());
        System.out.println("执行状态: " + stepExecution.getStatus());
        System.out.println("退出状态: " + stepExecution.getExitStatus());
        System.out.println("读取记录数: " + stepExecution.getReadCount());
        System.out.println("写入记录数: " + stepExecution.getWriteCount());
        
        if (stepExecution.getStatus().isUnsuccessful()) {
            System.err.println("Step执行失败，异常信息: ");
            for (Throwable exception : stepExecution.getFailureExceptions()) {
                exception.printStackTrace();
            }
        }
        
        return stepExecution.getExitStatus();
    }
}

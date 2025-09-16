package customer.ai2code.service.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Spring Batch配置类
 * 配置Job Repository、Job Launcher等核心组件
 */
@Configuration
@EnableBatchProcessing(dataSourceRef = "ds-db", transactionManagerRef = "tx-db")
public class BatchConfiguration {

    /**
     * 配置异步Job Launcher
     */
    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository,
            TaskExecutor taskExecutor) {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(taskExecutor);
        return jobLauncher;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("spring_batch");
    }

}

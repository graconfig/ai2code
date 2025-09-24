package customer.ai2code.service.batch;

import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring Batch配置类
 * 扩展DefaultBatchConfiguration并重写getDataSource()方法
 * 以适配SAP CAP框架的DataSource bean命名规则
 */
@Configuration
public class BatchConfiguration extends DefaultBatchConfiguration {

    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;

    public BatchConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    /**
     * 重写getDataSource()方法，提供SAP CAP环境中的DataSource
     * 这样Spring Batch就不会期望找到名为'dataSource'的bean
     */
    @Override
    protected DataSource getDataSource() {
        return this.dataSource;
    }

    /**
     * 重写getTransactionManager()方法，提供SAP CAP环境中的TransactionManager
     */
    @Override
    protected PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }

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

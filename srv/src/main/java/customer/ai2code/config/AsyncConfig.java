package customer.ai2code.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步执行配置
 * 为任务编排服务提供异步执行能力
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskOrchestrationExecutor")
    public Executor taskOrchestrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("TaskOrchestration-");
        executor.initialize();
        return executor;
    }
}

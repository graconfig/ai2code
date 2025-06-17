package customer.ai2code.service.execution.functioncall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 测试应用程序配置
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "customer.ai2code.service.execution.functioncall",
    "customer.ai2code.service.impl",
    "customer.ai2code.model"
})
public class TestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
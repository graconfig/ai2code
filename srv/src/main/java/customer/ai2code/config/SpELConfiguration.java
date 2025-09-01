package customer.ai2code.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * SpEL (Spring Expression Language) 配置
 */
@Configuration
public class SpELConfiguration {

    /**
     * SpEL表达式解析器
     */
    @Bean
    public ExpressionParser expressionParser() {
        return new SpelExpressionParser();
    }

    /**
     * 标准SpEL评估上下文
     */
    @Bean
    public StandardEvaluationContext standardEvaluationContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 注册常用的静态方法和函数
        // context.registerFunction("isEmpty", String.class.getDeclaredMethod("isEmpty"));
        
        return context;
    }
}

package customer.ai2code.service.variable;

/**
 * 变量解析器接口
 */
public interface VariableResolver {
    
    /**
     * 检查是否支持解析该变量类型
     */
    boolean supports(String variableExpression);
    
    /**
     * 解析变量并返回值
     */
    String resolve(String variableExpression, VariableContext context);
    
    /**
     * 获取解析器优先级，数字越小优先级越高
     */
    int getPriority();
}
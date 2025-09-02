package customer.ai2code.service.execution;

import org.springframework.stereotype.Service;
import customer.ai2code.service.variable.VariableParsingService;
import customer.ai2code.service.variable.VariableContext;
import customer.ai2code.model.bot.Bot;
import customer.ai2code.util.SpELVariableHelper;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Bot执行条件评估服务
 * 基于VariableParsingService和SpEL实现executeCondition的条件判断
 */
@Service
public class ExecuteConditionEvaluationService {

    private final VariableParsingService variableParsingService;
    private final ExpressionParser expressionParser;
    private final SpELVariableHelper spelVariableHelper;

    // ✨ 改为依赖注入，使用SpELConfiguration中配置的Bean
    public ExecuteConditionEvaluationService(
            VariableParsingService variableParsingService,
            ExpressionParser expressionParser,
            SpELVariableHelper spelVariableHelper) {
        this.variableParsingService = variableParsingService;
        this.expressionParser = expressionParser; // 使用注入的Bean
        this.spelVariableHelper = spelVariableHelper;
    }

    /**
     * 评估Bot的执行条件
     * @param executeCondition 执行条件表达式
     * @param bot Bot实例
     * @return 是否应该执行该Bot
     */
    public boolean evaluateCondition(String executeCondition, Bot bot) {
        // 如果没有执行条件，默认执行
        if (executeCondition == null || executeCondition.trim().isEmpty()) {
            return true;
        }

        try {
            // 1. 构建变量解析上下文
            // VariableContext context = buildVariableContext(bot);

            // 2. 解析条件中的变量
            String resolvedCondition = variableParsingService.parseVariables(executeCondition, bot.getVariableContext());

            // 3. 构建SpEL评估上下文并转换表达式
            StandardEvaluationContext spelContext = new StandardEvaluationContext();
            // spelContext.setVariable("botInstanceId", context.getBotInstanceId());
            // spelContext.setVariable("mainTaskId", context.getMainTaskId());
            
            // 🔧 使用新方法：注入变量并获取转换后的表达式
            String transformedExpression = spelVariableHelper.injectVariablesAndTransform(spelContext, resolvedCondition);

            // 4. 使用转换后的表达式进行SpEL评估
            Expression expression = expressionParser.parseExpression(transformedExpression);
            Object result = expression.getValue(spelContext);

            // 5. 转换为布尔值
            return convertToBoolean(result);

        } catch (Exception e) {
            System.err.println("Failed to evaluate execute condition: " + executeCondition + 
                             " for bot: " + bot.getBotInstance().getId() + ", error: " + e.getMessage());
            // 条件评估失败时，默认不执行（安全策略）
            return false;
        }
    }

    // /**
    //  * 评估Bot的执行条件（基于Bot实例ID）
    //  * @param executeCondition 执行条件表达式
    //  * @param botInstanceId Bot实例ID
    //  * @return 是否应该执行该Bot
    //  */
    // public boolean evaluateCondition(String executeCondition, String botInstanceId) {



        // if (executeCondition == null || executeCondition.trim().isEmpty()) {
        //     return true;
        // }

        // try {
        //     // 构建基本的变量上下文
        //     VariableContext context = VariableContext.builder()
        //             .botInstanceId(botInstanceId)
        //             .build();

        //     // 解析条件中的变量
        //     String resolvedCondition = variableParsingService.parseVariables(executeCondition, context);

        //     // 🔧 构建SpEL评估上下文并转换表达式
        //     StandardEvaluationContext spelContext = new StandardEvaluationContext();
        //     spelContext.setVariable("botInstanceId", context.getBotInstanceId());
        //     spelContext.setVariable("mainTaskId", context.getMainTaskId());
            
        //     String transformedExpression = spelVariableHelper.injectVariablesAndTransform(spelContext, resolvedCondition);

        //     // 使用转换后的表达式进行SpEL评估
        //     Expression expression = expressionParser.parseExpression(transformedExpression);
        //     Object result = expression.getValue(spelContext);

        //     return convertToBoolean(result);

        // } catch (Exception e) {
        //     System.err.println("Failed to evaluate execute condition: " + executeCondition + 
        //                      " for botInstanceId: " + botInstanceId + ", error: " + e.getMessage());
        //     return false;
        // }
    // }

    /**
     * 构建变量解析上下文（复用PromptServiceImpl的逻辑）
     */
    // private VariableContext buildVariableContext(Bot bot) {
    //     // 使用PromptServiceImpl中已有的buildVariableContext逻辑
    //     // 这里通过反射调用私有方法，或者将该方法公开
    //     return VariableContext.builder()
    //             .botInstanceId(bot.getBotInstance().getId())
    //             .mainTaskId
    //             .currentInstance(bot.getBotInstance())
    //             .build();
    // }

    /**
     * 创建SpEL评估上下文（已弃用，保留用于向后兼容）
     */
    // @SuppressWarnings("unused")
    // @Deprecated
    // private StandardEvaluationContext createSpelContext(VariableContext variableContext, String resolvedCondition) {
    //     StandardEvaluationContext context = new StandardEvaluationContext();

    //     // 添加基本变量到SpEL上下文
    //     context.setVariable("botInstanceId", variableContext.getBotInstanceId());
    //     context.setVariable("mainTaskId", variableContext.getMainTaskId());

    //     // ✨ 使用注入的SpELVariableHelper实例注入复杂变量（JSON对象、数组等）
    //     spelVariableHelper.injectVariables(context, resolvedCondition);

    //     return context;
    // }

    /**
     * 将结果转换为布尔值
     */
    private boolean convertToBoolean(Object result) {
        if (result == null) {
            return false;
        }

        if (result instanceof Boolean) {
            return (Boolean) result;
        }

        if (result instanceof String) {
            String str = (String) result;
            return !str.isEmpty() && !"false".equalsIgnoreCase(str) && !"0".equals(str);
        }

        if (result instanceof Number) {
            return ((Number) result).doubleValue() != 0.0;
        }

        // 其他类型默认为true（存在即为真）
        return true;
    }
}

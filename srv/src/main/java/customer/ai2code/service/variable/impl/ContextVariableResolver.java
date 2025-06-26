package customer.ai2code.service.variable.impl;

import org.springframework.stereotype.Component;

import cds.gen.mainservice.ContextNodes;
import customer.ai2code.service.variable.VariableContext;
import customer.ai2code.service.variable.VariableResolver;
import customer.ai2code.service.impl.GenericCqnService;

/**
 * Context 和 SubContext 变量解析器
 */
@Component
public class ContextVariableResolver implements VariableResolver {

    private final GenericCqnService genericCqnService;

    public ContextVariableResolver(GenericCqnService genericCqnService) {
        this.genericCqnService = genericCqnService;
    }

    @Override
    public boolean supports(String variableExpression) {
        return variableExpression.startsWith("Context:") || variableExpression.startsWith("SubContext:");
    }

    @Override
    public String resolve(String variableExpression, VariableContext context) {
        try {
            String contextPath = resolveContextPath(variableExpression, context);
            String taskId = context.getMainTaskId(); // 统一使用mainTaskId
            
            ContextNodes contextNode = genericCqnService.getContextNodeByTaskAndPath(taskId, contextPath);
            return contextNode != null ? contextNode.getValue() : "";
            
        } catch (Exception e) {
            System.err.println("Failed to resolve context variable: " + variableExpression + ", error: " + e.getMessage());
            return "";
        }
    }

    @Override
    public int getPriority() {
        return 1; // 高优先级
    }

    /**
     * 解析上下文路径
     */
    private String resolveContextPath(String variableExpression, VariableContext context) {
        if (variableExpression.startsWith("SubContext:")) {
            // SubContext: 相对路径引用
            String relativePath = variableExpression.replace("SubContext:", "");
            return resolveSubContextPath(context.getBotInstanceId(), relativePath);
        } else if (variableExpression.startsWith("Context:")) {
            // Context: 绝对路径引用
            return variableExpression.replace("Context:", "");
        }
        return variableExpression;
    }

    /**
     * 处理SubContext相对路径
     */
    private String resolveSubContextPath(String botInstanceId, String relativePath) {
        try {
            // 获取当前BotInstance对应的Task的上下文路径
            cds.gen.mainservice.Tasks task = genericCqnService.getParentTaskByBotInstance(botInstanceId);
            String taskContextPath = task.getContextPath();

            // 如果任务上下文路径为空或null，直接返回相对路径
            if (taskContextPath == null || taskContextPath.isEmpty()) {
                return relativePath;
            }

            // 拼接路径，避免重复的点号
            if (taskContextPath.endsWith(".") || relativePath.startsWith(".")) {
                return taskContextPath + relativePath;
            } else {
                return taskContextPath + "." + relativePath;
            }
        } catch (Exception e) {
            return relativePath;
        }
    }
}
package customer.ai2code.service.variable.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.Tasks;
import customer.ai2code.service.variable.VariableContext;
import customer.ai2code.service.variable.VariableResolver;
import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.service.TaskBotDataService;

/**
 * Context 和 SubContext 变量解析器
 */
@Component
public class ContextVariableResolver implements VariableResolver {

    private final GenericCqnService genericCqnService;

    private final ObjectMapper objectMapper;

    // private final TaskBotDataService taskBotDataService;

    public ContextVariableResolver(GenericCqnService genericCqnService, ObjectMapper objectMapper
    // ,TaskBotDataService taskBotDataService
            ) {
        this.genericCqnService = genericCqnService;
        this.objectMapper = objectMapper;
        // this.taskBotDataService = taskBotDataService;
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

            // 判断 contextPath 是否包含 [*]
            if (contextPath.contains("[*]")) {
                // 获取多个 ContextNodes
                List<ContextNodes> contextNodes = genericCqnService.getContextNodesByTaskAndPathPattern(taskId,
                        contextPath);

                // 如果只有一个节点，直接返回其值；如果多个节点，返回数组格式
                if (contextNodes == null || contextNodes.isEmpty()) {
                    return "";

                } else {
                    // 多个节点，构建 JSON 数组
                    return objectMapper.writeValueAsString(contextNodes);
                }
            } else {
                // 获取单个 ContextNode
                ContextNodes contextNode = genericCqnService.getContextNodeByTaskAndPath(taskId, contextPath);

                return contextNode != null ? contextNode.toJson() : "";
            }

        } catch (Exception e) {
            System.err.println(
                    "Failed to resolve context variable: " + variableExpression + ", error: " + e.getMessage());
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
            // 获取当前BotInstance对应的Task的上下文路径，使用缓存优化
            // Tasks task = taskBotDataService.getParentTaskByBotInstance(botInstanceId).getTask();
            Tasks task = genericCqnService.getParentTaskByBotInstance(botInstanceId);
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
package customer.ai2code.service.impl.execution;

import java.util.List;

import cds.gen.mainservice.ContextNodes;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.execution.BotExecution;

/**
 * 假定AI帮我们把ContextNodes.value字段更新成最新的CDS内容，且整理好形成List<ContextNodes>的传参结构，我们需要用这个数据集直接更新ContextNodes。
 */
@BotExecutor(name = "Split CDS and Update Context Execution", description = "Implementation for splitting CDS and updating context in the bot execution framework", version = "1.0", enabled = true)
public class SplitCDSandUpdateContextExecution implements BotExecution {

    private final ContextService contextService;

    public SplitCDSandUpdateContextExecution(ContextService contextService) {
        // 默认构造函数
        this.contextService = contextService;
    }

    @ExecuteMethod
    public String execute(
            @ExecuteParameter(name = "contextNodesOfCds", description = "Context Nodes of CDS") List<ContextNodes> contextNodesOfCds) {

        // 调用上下文服务进行CDS拆分和上下文更新
        // return contextService.splitCDSAndUpdateContext(botInstanceId, contextPath);
        contextNodesOfCds.forEach(contextNode -> {
            // 添加调试信息
            System.out.println("Processing contextNode: " + contextNode.getClass().getName());
            System.out.println("ContextNode toString: " + contextNode.toString());
            
            // 使用增强的方法安全地获取各个字段的值
            String taskId = safeGetStringFromProxy(contextNode, "taskId");
            String path = safeGetStringFromProxy(contextNode, "path");
            String value = safeGetStringFromProxy(contextNode, "value");
            String type = safeGetStringFromProxy(contextNode, "type");
            
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("CDS cannot be null or empty");
            }
            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException("Context path cannot be null or empty");
            }
            if (taskId == null || taskId.isEmpty()) {
                throw new IllegalArgumentException("Main task ID cannot be null or empty");
            }

            contextService.upsertContextWithMainTaskId(taskId, path, value, type);
        });
        return "CDS split and context updated successfully";
    }
    
    /**
     * 安全地获取字符串值，处理可能的代理对象或包装对象
     */
    private String safeGetString(Object value, String fieldName) {
        if (value == null) {
            System.out.println(fieldName + " is null from getter method");
            return null;
        }
        
        String result = value.toString();
        System.out.println(fieldName + " value: '" + result + "' (type: " + value.getClass().getName() + ")");
        
        // 如果是 "null" 字符串，则视为 null
        if ("null".equals(result)) {
            return null;
        }
        
        return result;
    }
    
    /**
     * 从 CDS 代理对象中安全地获取字段值，支持多种访问方式
     */
    private String safeGetStringFromProxy(ContextNodes contextNode, String fieldName) {
        // 1. 首先尝试 getter 方法
        Object value = null;
        try {
            switch (fieldName) {
                case "taskId":
                    value = contextNode.getTaskId();
                    break;
                case "path":
                    value = contextNode.getPath();
                    break;
                case "value":
                    value = contextNode.getValue();
                    break;
                case "type":
                    value = contextNode.getType();
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error calling getter for " + fieldName + ": " + e.getMessage());
        }
        
        // 2. 如果 getter 返回 null，尝试从 toString() 解析
        if (value == null) {
            System.out.println(fieldName + " is null from getter, trying to parse from toString()");
            // String toStringValue = contextNode.toString();
            value = contextNode.get(fieldName);
        }
        
        return safeGetString(value, fieldName);
    }

}

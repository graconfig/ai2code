package customer.ai2code.service.impl.execution;

import java.util.List;

import cds.gen.mainservice.ContextNodes;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.execution.BotExecution;

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
            if (contextNode.getValue() == null || contextNode.getValue().isEmpty()) {
                throw new IllegalArgumentException("CDS cannot be null or empty");
            }
            if (contextNode.getPath() == null || contextNode.getPath().isEmpty()) {
                throw new IllegalArgumentException("Context path cannot be null or empty");
            }
            if (contextNode.getTaskId() == null || contextNode.getTaskId().isEmpty()) {
                throw new IllegalArgumentException("Main task ID cannot be null or empty");
            }

            contextService.upsertContextWithMainTaskId(contextNode.getTaskId(), contextNode.getPath(),
                    contextNode.getValue(), contextNode.getType());
        });
        return "CDS split and context updated successfully";

    }
}

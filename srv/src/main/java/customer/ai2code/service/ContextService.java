package customer.ai2code.service;

import java.util.List;
import java.util.Map;

import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.TasksGetContextHierarchyContext;

public interface ContextService {

    /**
     * 将上下文节点列表构建为层次结构
     */
    public String buildContextAsHierarchy(String mainTaskId);

    public String buildContextAsHierarchy(TasksGetContextHierarchyContext context);

    /**
     * 获取完整的上下文路径（处理SubContext相对路径）
     */
    public String getContextFullPath(String botInstanceId, String subPath);

    /**
     * 创建或更新上下文节点
     */
    public ContextNodes upsertContext(String botInstanceId, String contextPath, String contextValue,
            String contextType);

    public ContextNodes upsertContextWithMainTaskId(String mainTaskId, String contextPath, String contextValue,
            String contextType);

    /**
     * 获取上下文节点
     */
    public ContextNodes getContextNode(String contextNodeId);

    /**
     * 批量更新/创建上下文节点
     */
    public List<ContextNodes> upsertContextBatch(String botInstanceId, Map<String, String> contextPathValueMap,
            String contextType);

    /**
     * 根据路径模式查询上下文节点（支持数组语法）
     */
    public List<ContextNodes> getContextNodesByPattern(String botInstanceId, String pathPattern);

    public void updateAdditionInfo(String botInstanceId,String contextPath,String additionalInfo);

}

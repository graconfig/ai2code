package customer.ai2code.service;

import java.util.List;
import java.util.Map;

import com.sap.cds.Result;

import cds.gen.mainservice.ContextNodes;

public interface ContextService {
    public List<Map<String, Object>> buildContextAsHierarchy(List<ContextNodes> contextNodes);

    public String getContextFullPath(String subPathPrefix, String subPath);

    public Result upsertContext(
            String botInstanceId,
            String taskId,
            // Integer sequence,
            // String contextPath,
            String contextValue);
    public ContextNodes getContextNode(
            String contextNodeId);
}

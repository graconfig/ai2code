package customer.ai2code.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;


import cds.gen.mainservice.ContextNodes;
import customer.ai2code.service.ContextService;

@Service
public class ContextServiceImpl implements ContextService {

    private final GenericCqnService genericCqnService;

    public ContextServiceImpl(GenericCqnService genericCqnService) {
        this.genericCqnService = genericCqnService;
    }

    @Override
    public List<Map<String, Object>> buildContextAsHierarchy(List<ContextNodes> contextNodes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'buildContextAsHierarchy'");
    }

    @Override
    public String getContextFullPath(String subPathPrefix, String subPath) {
        // if (subPathPrefix == null || subPathPrefix.isEmpty()) {
        //     return subPath;
        // }
        // if (subPath == null || subPath.isEmpty()) {
        //     return subPathPrefix;
        // }
        // return subPathPrefix + "." + subPath;
        throw new UnsupportedOperationException("No need to implement 'getContextFullPath' in this service");
    }

    @Override
    public ContextNodes upsertContext(String taskId, String contextPath, String contextValue) {
        try {
            // 1. 查询是否已存在相同taskId和contextPath的记录
            ContextNodes existingNode = null;
            try {
                existingNode = genericCqnService.getContextNodeByTaskAndPath(taskId, contextPath);
            } catch (Exception e) {
                // 如果没找到，existingNode保持为null
            }

            if (existingNode != null) {
                // 2. 如果存在，更新现有记录
                return genericCqnService.updateContextNodeValue(existingNode, contextValue);
            } else {
                // 3. 如果不存在，创建新记录
                return genericCqnService.createAndInsertContextNode(taskId,
                        contextPath,
                        genericCqnService.generateLabelFromPath(contextPath),
                        "text",
                        contextValue);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to upsert context node for taskId: " + taskId +
                    ", contextPath: " + contextPath, e);
        }
    }

    @Override
    public ContextNodes getContextNode(String contextNodeId) {
        return genericCqnService.getContextNodeById(contextNodeId);
    }

    /**
     * 重载方法，支持指定更多参数
     */
    public ContextNodes upsertContextNode(String taskId, String contextPath, String label,
            String type, String contextValue) {
        try {
            // 查询是否已存在相同taskId和contextPath的记录
            ContextNodes existingNode = null;
            try {
                existingNode = genericCqnService.getContextNodeByTaskAndPath(taskId, contextPath);
            } catch (Exception e) {
                // 如果没找到，existingNode保持为null
            }

            if (existingNode != null) {
                // 更新现有记录
                return genericCqnService.updateContextNode(existingNode, label, type, contextValue);
            } else {
                // 创建新记录
                return genericCqnService.createAndInsertContextNode(taskId, contextPath, label, type, contextValue);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to upsert context node for taskId: " + taskId +
                    ", contextPath: " + contextPath, e);
        }
    }
}

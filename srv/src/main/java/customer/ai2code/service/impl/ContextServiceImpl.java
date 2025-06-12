package customer.ai2code.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.Tasks;
import customer.ai2code.exception.BusinessException;
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
        throw new BusinessException("Unimplemented method 'buildContextAsHierarchy'");
    }

    @Override
    public String getContextFullPath(String botInstanceId, String subPath) {
        if (subPath.contains("SubContext:")) {
            // 处理SubContext:的情况，需要获取任务的上下文路径并拼接
            //获取上级任务的上下文路径
            Tasks task = genericCqnService.getParentTaskByBotInstance(botInstanceId);
            // Tasks task = genericCqnService.getTaskByBotInstance(botInstanceId);
            String taskContextPath = task.getContextPath();
            String relativePath = subPath.replace("SubContext:", "");

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
        } else if (subPath.contains("Context:")) {
            // 处理Context:的情况，直接去掉Context:前缀
            return subPath.replace("Context:", "");
        } else {
            // 如果都不包含，直接返回原路径
            return subPath;
        }
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
            throw new BusinessException("Failed to upsert context node for taskId: " + taskId +
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
            throw new BusinessException("Failed to upsert context node for taskId: " + taskId +
                    ", contextPath: " + contextPath, e);
        }
    }
}

package customer.ai2code.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.ql.cqn.AnalysisResult;
import com.sap.cds.ql.cqn.CqnAnalyzer;

import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.TasksGetContextHierarchyContext;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.bot.Bot;
import customer.ai2code.model.tree.ContextTreeNode;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.websocket.ContextNodeChangedEvent;
import customer.ai2code.service.TaskBotDataService;
import customer.ai2code.util.PathParser;

@Service
public class ContextServiceImpl implements ContextService {

    private final GenericCqnService genericCqnService;
    private final ObjectMapper objectMapper;
    private final PathParser pathParser;
    // private final TaskBotDataService taskBotDataService;
    private final ApplicationEventPublisher eventPublisher;

    public ContextServiceImpl(  GenericCqnService genericCqnService, 
                                ObjectMapper objectMapper, 
                                PathParser pathParser
    // ,TaskBotDataService taskBotDataService
    ,
                                ApplicationEventPublisher eventPublisher) {
        this.genericCqnService = genericCqnService;
        this.objectMapper = objectMapper;
        this.pathParser = pathParser;
        // this.taskBotDataService = taskBotDataService;
        this.eventPublisher = eventPublisher;
    }

    // @Override
    // public List<Map<String, Object>> buildContextAsHierarchy(List<ContextNodes>
    // contextNodes) {
    // Map<String, Map<String, Object>> nodeMap = new HashMap<>();
    // List<Map<String, Object>> rootNodes = new ArrayList<>();
    //
    // // 第一步：创建所有节点对象
    // for (ContextNodes node : contextNodes) {
    // Map<String, Object> treeNode = new HashMap<>();
    // treeNode.put("id", node.getId());
    // treeNode.put("path", node.getPath());
    // treeNode.put("label", node.getLabel());
    // treeNode.put("type", node.getType());
    // treeNode.put("value", node.getValue());
    // treeNode.put("children", new ArrayList<Map<String, Object>>());
    // treeNode.put("taskId", node.getTask() != null ? node.getTask().getId() :
    // node.getTaskId());
    //
    // nodeMap.put(node.getPath(), treeNode);
    // }
    //
    // // 第二步：建立父子关系
    // for (ContextNodes node : contextNodes) {
    // String parentPath = getParentPath(node.getPath());
    //
    // if (parentPath != null && nodeMap.containsKey(parentPath)) {
    // // 有父节点，添加到父节点的children中
    // Map<String, Object> parentNode = nodeMap.get(parentPath);
    // @SuppressWarnings("unchecked")
    // List<Map<String, Object>> children = (List<Map<String, Object>>)
    // parentNode.get("children");
    // children.add(nodeMap.get(node.getPath()));
    // } else {
    // // 没有父节点，是根节点
    // rootNodes.add(nodeMap.get(node.getPath()));
    // }
    // }
    //
    // return rootNodes;
    // }
    private String extractIdFromContext(TasksGetContextHierarchyContext context) {
        // 使用CqnAnalyzer类，从CQN查询中提取ID，需要解析CqnSelect
        CqnAnalyzer cqnAnalyzer = CqnAnalyzer.create(context.getModel());
        AnalysisResult result = cqnAnalyzer.analyze(context.getCqn().ref());
        // return result.rootKeys().get("ID").toString();
        return result.targetKeys().get("ID").toString();
    }

    @Override
    public String buildContextAsHierarchy(TasksGetContextHierarchyContext context) {
        String mainTaskId = extractIdFromContext(context);
        return buildContextAsHierarchy(mainTaskId);
    }

    // 重写buildContextAsHierarchy - context tree
    @Override
    public String buildContextAsHierarchy(String mainTaskId) {
        List<ContextNodes> contextNodes = genericCqnService.getContextNodesByMainTaskId(mainTaskId);

        if (contextNodes == null || contextNodes.isEmpty()) {
            return "[]"; // 返回空的JSON数组表示没有上下文节点
        }

        // 1. 创建所有节点，包括中间虚拟节点
        Map<String, ContextTreeNode> nodeMap = new HashMap<>();

        // 首先创建所有实际的上下文节点
        for (ContextNodes node : contextNodes) {
            ContextTreeNode treeNode = new ContextTreeNode(node);
            nodeMap.put(node.getPath(), treeNode);
        }

        // 然后创建所有必要的中间虚拟节点
        for (ContextNodes node : contextNodes) {
            createIntermediateNodes(node.getPath(), nodeMap);
        }

        // 2. 构建树结构
        for (ContextTreeNode node : nodeMap.values()) {
            String parentPath = pathParser.getImmediateParentPath(node.getPath());
            if (parentPath != null) {
                ContextTreeNode parentNode = nodeMap.get(parentPath);
                if (parentNode != null) {
                    parentNode.addChild(node);
                }
            }
        }

        // 3. 提取根节点
        List<ContextTreeNode> rootNodes = nodeMap.values().stream()
                .filter(node -> node.getParent() == null)
                .collect(Collectors.toList());

        // 4. 转换为JSON格式返回
        try {
            return objectMapper.writeValueAsString(rootNodes);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to convert context tree to JSON", e);
        }
    }

    /**
     * 创建中间虚拟节点
     * 例如：对于路径 "subtask[0].cView"，需要创建虚拟节点 "subtask" 和 "subtask[0]"
     */
    private void createIntermediateNodes(String path, Map<String, ContextTreeNode> nodeMap) {
        List<String> pathHierarchy = pathParser.getPathHierarchy(path);

        for (String intermediatePath : pathHierarchy) {
            if (!nodeMap.containsKey(intermediatePath)) {
                // 创建虚拟节点
                ContextNodes virtualNode = createVirtualContextNode(intermediatePath);
                ContextTreeNode virtualTreeNode = new ContextTreeNode(virtualNode);
                nodeMap.put(intermediatePath, virtualTreeNode);
            }
        }
    }

    /**
     * 创建虚拟上下文节点
     */
    private ContextNodes createVirtualContextNode(String path) {
        ContextNodes virtualNode = ContextNodes.create();
        virtualNode.setPath(path);
        virtualNode.setLabel(pathParser.generateLabelFromPath(path));
        virtualNode.setType("virtual");
        virtualNode.setValue(null);
        return virtualNode;
    }

    @Override
    public String getContextFullPath(Bot bot, String subPath) {
        if (subPath.startsWith("SubContext:")) {
            // 处理SubContext:的情况，需要获取任务的上下文路径并拼接
            Tasks task = genericCqnService.getParentTaskByBotInstance(bot.getBotInstance().getId());
            // Tasks task =
            // cacheManager.getParentTaskByBotInstance(bot.getBotInstance().getId()).getTask();
            // bot
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
        } else if (subPath.startsWith("Context:")) {
            // 处理Context:的情况，直接去掉Context:前缀
            return subPath.replace("Context:", "");
        } else {
            // 如果都不包含，直接返回原路径
            return subPath;
        }
    }

    @Override
    public ContextNodes upsertContext(Bot bot, String contextPath, String contextValue,
            String contextType) {
        // try {
        // 1. 通过botInstanceId获取mainTaskId，使用缓存优化
        // String mainTaskId =
        // taskBotDataService.getMainTaskId(bot.getBotInstance().getId());
        String mainTaskId = genericCqnService.getMainTaskId(bot.getBotInstance().getId());

        ContextNodes node = upsertContextWithMainTaskId(mainTaskId, contextPath, contextValue, contextType);
        // 发布上下文节点变更事件（包含创建/更新的详细信息）
        eventPublisher.publishEvent(new ContextNodeChangedEvent(
            this, 
            node.getId(), 
            // 若为更新，需获取旧值；若为创建，旧值为null
            (node.getValue() != null ? node.getValue() : null), 
            contextValue, 
            mainTaskId, 
            node.getPath()
        ));

        return node;
        
        // return upsertContextWithMainTaskId(mainTaskId, contextPath, contextValue, contextType);
        // // 2. 查询是否已存在相同mainTaskId和contextPath的记录
        // ContextNodes existingNode = null;
        // try {
        // existingNode = genericCqnService.getContextNodeByTaskAndPath(mainTaskId,
        // contextPath);
        // } catch (Exception e) {
        // // 如果没找到，existingNode保持为null
        // }

        // if (existingNode != null) {
        // // 3. 如果存在，更新现有记录
        // return genericCqnService.updateContextNodeValue(existingNode, contextValue);
        // } else {
        // // 4. 如果不存在，创建新记录
        // return genericCqnService.createAndInsertContextNode(mainTaskId,
        // contextPath,
        // generateLabelFromPath(contextPath),
        // "text",
        // contextValue);
        // }

        // } catch (Exception e) {
        // throw new BusinessException("Failed to upsert context node for botInstanceId:
        // " + botInstanceId +
        // ", contextPath: " + contextPath, e);
        // }
    }

    @Override
    public ContextNodes upsertContextWithMainTaskId(String mainTaskId, String contextPath, String contextValue,
            String contextType) {
        // return upsertContext(botInstanceId, contextPath, null);
        String contextValueInString = contextValue != null ? contextValue.toString() : null;
        try {
            // 1. 通过botInstanceId获取mainTaskId
            // String mainTaskId = genericCqnService.getMainTaskId(botInstanceId);

            // 2. 查询是否已存在相同mainTaskId和contextPath的记录
            ContextNodes existingNode = null;
            try {
                existingNode = genericCqnService.getContextNodeByTaskAndPath(mainTaskId, contextPath);
            } catch (Exception e) {
                // 如果没找到，existingNode保持为null
            }

            if (existingNode != null) {
                // 3. 如果存在，更新现有记录
                return genericCqnService.updateContextNodeValue(existingNode, contextValueInString);
            } else {
                // 4. 如果不存在，创建新记录
                return genericCqnService.createAndInsertContextNode(mainTaskId,
                        contextPath,
                        pathParser.generateLabelFromPath(contextPath),
                        contextType,
                        contextValueInString);
            }

        } catch (Exception e) {
            throw new BusinessException("Failed to upsert context node for mainTaskId: " + mainTaskId +
                    ", contextPath: " + contextPath, e);
        }
    }

    @Override
    public ContextNodes getContextNode(String contextNodeId) {
        return genericCqnService.getContextNodeById(contextNodeId);
    }

    @Override
    public List<ContextNodes> upsertContextBatch(Bot bot, Map<String, String> contextPathValueMap,
            String contextType) {
        List<ContextNodes> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : contextPathValueMap.entrySet()) {
            try {
                ContextNodes node = upsertContext(bot, entry.getKey(), entry.getValue(), contextType);
                results.add(node);
            } catch (Exception e) {
                System.err.println("Failed to upsert context node: " + entry.getKey() + ", error: " + e.getMessage());
            }
        }

        return results;
    }

    @Override
    public List<ContextNodes> getContextNodesByPattern(Bot bot, String pathPattern) {
        try {
            // String mainTaskId = taskBotDataService.getMainTaskId(bot.getBotInstance().getId());
            String mainTaskId = genericCqnService.getMainTaskId(bot.getBotInstance().getId());

            // 处理 [-1] 语法，转换为 SQL LIKE 查询
            if (pathPattern.contains("[-1]")) {
                // String likePattern = pathPattern.replace("[-1]", "[%]") + "%";
                // TODO: 实现模式匹配查询
                return List.of();
            } else {
                // 普通路径查询
                ContextNodes node = genericCqnService.getContextNodeByTaskAndPath(mainTaskId, pathPattern);
                return node != null ? List.of(node) : List.of();
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void updateAdditionInfo(Bot bot, String contextPath, String additionalInfo) {
        // String mainTaskId = taskBotDataService.getMainTaskId(bot.getBotInstance().getId());
        String mainTaskId = genericCqnService.getMainTaskId(bot.getBotInstance().getId());
        genericCqnService.updateContextNodeAdditionalInfo(mainTaskId, contextPath, additionalInfo);
    }
}

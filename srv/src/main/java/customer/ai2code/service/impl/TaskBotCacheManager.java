package customer.ai2code.service.impl;

import customer.ai2code.model.tree.TaskBotNode;
import customer.ai2code.model.tree.TaskBotNode.NodeType;
import customer.ai2code.service.PromptService;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.bot.Bot;
import customer.ai2code.model.bot.ChatBot;
import customer.ai2code.model.bot.CodingBot;
import customer.ai2code.model.bot.FunctionCallingBot;
import customer.ai2code.model.config.AIModel;
import customer.ai2code.model.config.AIModelResolver;
import customer.ai2code.model.task.GenericTask;
import customer.ai2code.model.task.Task;

import org.apache.poi.ss.formula.functions.T;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import cds.gen.configservice.BotTypes;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务-Bot统一缓存管理器
 * 使用树形结构管理Task和BotInstance的层级关系
 */
@Service
public class TaskBotCacheManager {

    // 全局节点缓存：nodeId -> TaskBotNode
    private final Map<String, TaskBotNode> nodeCache = new ConcurrentHashMap<>();

    // 根节点缓存：mainTaskId -> rootNode
    private final Map<String, TaskBotNode> rootCache = new ConcurrentHashMap<>();

    private final GenericCqnService genericCqnService;
    private final AIModelResolver aiModelResolver;
    private final PromptService promptService;
    // private final RAGExtractionFactoryService ragExtractionFactoryService;
    private final BotExecutionFactoryService botExecutionFactoryService;

    public TaskBotCacheManager(GenericCqnService genericCqnService,
            AIModelResolver aiModelResolver,
            PromptService promptService,
            BotExecutionFactoryService botExecutionFactoryService) {
        this.genericCqnService = genericCqnService;
        this.aiModelResolver = aiModelResolver;
        this.promptService = promptService;
        this.botExecutionFactoryService = botExecutionFactoryService;
    }

    /**
     * 添加任务节点
     */
    public TaskBotNode addTaskNode(Task task, String parentBotInstanceId) {
        TaskBotNode taskNode = new TaskBotNode(task);

        nodeCache.put(task.getTask().getId(), taskNode);

        // 如果是主任务，添加到根缓存
        if (task.getTask().getIsMain() != null && task.getTask().getIsMain()) {
            rootCache.put(task.getTask().getId(), taskNode);
        } else if (parentBotInstanceId != null) {
            // 如果有父Bot实例，建立父子关系
            TaskBotNode parentNode = nodeCache.get(parentBotInstanceId);
            if (parentNode != null) {
                parentNode.addChild(taskNode);
            }
        }

        return taskNode;
    }

    /**
     * 添加Bot实例节点
     */
    public TaskBotNode addBotInstanceNode(Bot bot) {
        TaskBotNode botNode = new TaskBotNode(bot);

        nodeCache.put(bot.getBotInstance().getId(), botNode);

        // 建立与任务的父子关系
        String taskId = bot.getBotInstance().getTaskId();
        if (taskId != null) {
            TaskBotNode taskNode = nodeCache.get(taskId);
            if (taskNode != null) {
                taskNode.addChild(botNode);
            }
        }

        return botNode;
    }

    /**
     * 获取节点
     */
    public TaskBotNode getNode(String nodeId) {
        return nodeCache.get(nodeId);
    }

    /**
     * 获取任务节点
     */
    public TaskBotNode getTaskNode(String taskId) {
        TaskBotNode node = nodeCache.get(taskId);
        if (node == null) {
            Task task = createTaskInstance(taskId);
            node = getTaskNode(task);
        }
        return node;
        // if (node != null && node.getType() == TaskBotNode.NodeType.TASK) {
        // return node;
        // }
        // return null;
    }

    public TaskBotNode getTaskNode(Task task) {
        return getTaskNode(task.getTask().getId());
    }

    /**
     * 获取Bot实例节点
     */
    public TaskBotNode getBotInstanceNode(String botInstanceId) {
        TaskBotNode node = nodeCache.get(botInstanceId);
        if (node == null) {
            Bot bot = createBotInstance(botInstanceId);
            node = getBotInstanceNode(bot);
        }
        return node;
        // if (node != null && node.getType() == TaskBotNode.NodeType.BOT_INSTANCE) {
        // return node;
        // }
        // return null;
    }

    public TaskBotNode getBotInstanceNode(Bot bot) {
        return getBotInstanceNode(bot.getBotInstance().getId());
    }

    /**
     * 根据Bot实例ID获取主任务ID
     */
    public String getMainTaskId(String botInstanceId) {
        TaskBotNode botNode = getBotInstanceNode(botInstanceId);
        // if (botNode != null) {
        // return botNode.getMainTaskId();
        TaskBotNode rootNode = getRoot(botNode);
        // }
        if (rootNode.getType() == NodeType.TASK && rootNode.getTaskObject() != null) {
            if (rootNode.getTaskObject().getTask().getIsMain() != null
                    && rootNode.getTaskObject().getTask().getIsMain()) {
                return rootNode.getId();
            }
        }
        // throw new BusinessException("Root node is not a main task");
        return null;
    }

    public TaskBotNode getRoot(TaskBotNode current) {
        // TaskBotNode current = this;
        while (getParent(current) != null) {
            current = getParent(current);
        }
        return current;
    }

    /**
     * 根据任务ID和序列号查找Bot实例
     */
    public TaskBotNode getBotInstanceByTaskAndSequence(String taskId, int sequence) {
        TaskBotNode taskNode = getTaskNode(taskId);
        // if (taskNode != null) {
        TaskBotNode botNode = taskNode.findBotBySequence(sequence);
        if (botNode == null) {
            // return botNode;
            BotInstances botInstances = genericCqnService.getBotInstanceByTaskAndSequence(taskId, sequence);
            BotTypes botType = genericCqnService.getBotTypeById(botInstances.getTypeId());
            Bot bot = createBotInstance(botInstances, botType);
            // // botNode = addBotInstanceNode(bot);
            botNode = getBotInstanceNode(bot);
            // botNode = getBotInstanceNode(taskNode.getTaskId());
        }
        // }
        return botNode;
    }

    public TaskBotNode getTaskByBotInstanceAndSequence(String botInstanceId, int sequence) {
        TaskBotNode botNode = getBotInstanceNode(botInstanceId);
        TaskBotNode taskNode = botNode.findTaskBySequence(sequence);
        if (taskNode == null) {
            // 如果没有找到，尝试通过Bot实例ID获取主任务ID
            Tasks taskInstance = genericCqnService.getTaskByBotInstanceAndSequence(botInstanceId, sequence);
            Task task = createTaskInstance(taskInstance.getId());
            taskNode = getTaskNode(task);
        }
        return taskNode;
        // if (botNode != null) {
        // TaskBotNode parentNode = botNode.getParent();
        // if (parentNode != null && parentNode.getType() == TaskBotNode.NodeType.TASK)
        // {
        // return parentNode;
        // }
        // }
        // return null;
    }

    public List<TaskBotNode> getChildren(TaskBotNode current) {
        // return new ArrayList<>(nodeCache.values());
        List<TaskBotNode> children = current.getChildren();
        if (children == null || children.isEmpty()) {
            List<TaskBotNode> newChildren = new ArrayList<>();
            if (current.getType() == NodeType.TASK) {
                // 如果是任务节点，尝试获取子Bot实例
                // children = current.getTaskObject().getChildren();
                genericCqnService.getTaskAndSubBotsById(current.getTaskObject().getTask().getId())
                        .getBotInstances()
                        .forEach(botInstance -> {
                            TaskBotNode botNode = getBotInstanceNode(botInstance.getId());
                            if (botNode != null) {
                                newChildren.add(botNode);
                            }
                        });
            } else if (current.getType() == NodeType.BOT_INSTANCE) {
                // 如果是Bot实例节点，尝试获取子任务
                // children = current.getBotObject().getChildren();
                genericCqnService.getBotInstanceAndSubtasksById(current.getBotObject().getBotInstance().getId())
                        .getTasks()
                        .forEach(task -> {
                            TaskBotNode taskNode = getTaskNode(task.getId());
                            if (taskNode != null) {
                                newChildren.add(taskNode);
                            }
                        });
            }
            return newChildren;
        }
        return children;
    }

    /**
     * 获取缓存的Task对象
     */
    public Task getCachedTask(String taskId) {
        TaskBotNode node = getTaskNode(taskId);
        return node != null ? node.getTaskObject() : null;
    }

    /**
     * 获取缓存的Bot对象
     */
    public Bot getCachedBot(String botInstanceId) {
        TaskBotNode node = getBotInstanceNode(botInstanceId);
        return node != null ? node.getBotObject() : null;
    }

    /**
     * 更新Bot状态
     */
    public void updateBotStatus(String botInstanceId, String statusCode) {
        TaskBotNode node = getBotInstanceNode(botInstanceId);
        if (node != null) {
            node.updateBotStatus(statusCode);
        }
    }

    /**
     * 移除节点及其子树
     */
    public void removeNode(String nodeId) {
        TaskBotNode node = nodeCache.remove(nodeId);
        if (node != null) {
            // 递归移除子节点
            removeSubtree(node);

            // 从父节点中移除
            if (node.getParent() != null) {
                node.getParent().removeChild(nodeId);
            }

            // 如果是根节点，从根缓存中移除
            if (node.isMainTask()) {
                rootCache.remove(node.getId());
            }
        }
    }

    private void removeSubtree(TaskBotNode node) {
        for (TaskBotNode child : node.getChildren()) {
            nodeCache.remove(child.getId());
            removeSubtree(child);
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        nodeCache.clear();
        rootCache.clear();
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        stats.put("totalNodes", nodeCache.size());
        stats.put("rootNodes", rootCache.size());

        long taskNodes = nodeCache.values().stream()
                .filter(node -> node.getType() == TaskBotNode.NodeType.TASK)
                .count();
        long botNodes = nodeCache.values().stream()
                .filter(node -> node.getType() == TaskBotNode.NodeType.BOT_INSTANCE)
                .count();

        stats.put("taskNodes", (int) taskNodes);
        stats.put("botInstanceNodes", (int) botNodes);

        return stats;
    }

    public Bot createBotInstance(String botInstanceId) {
        // BotInstances botInstance, BotTypes botType
        // Bot bot;
        BotInstances botInstance = genericCqnService.getBotInstanceById(botInstanceId);
        // BotInstances botInstance =
        // genericCqnService.getBotInstanceAndSubtasksById(botInstanceId);
        BotTypes botType = genericCqnService.getBotTypeById(botInstance.getTypeId());
        return createBotInstance(botInstance, botType);
    }

    public Bot createBotInstance(BotInstances botInstance, BotTypes botType) {
        // BotTypes botType = genericCqnService.getBotTypeById(botInstance.getTypeId());
        Bot bot;
        Locale locale = LocaleContextHolder.getLocale();
        String functionTypeCode = botType.getFunctionTypeCode();
        AIModel aiModel = aiModelResolver.resolveAIModel(botType.getModelId());

        switch (functionTypeCode) {
            case "A": // AI Chat Bot
                bot = new ChatBot(botInstance, aiModel, botType, locale, genericCqnService, promptService,
                        aiModelResolver);
                break;
            case "F": // Function Calling Bot
                bot = new FunctionCallingBot(botInstance, aiModel, botType, locale, genericCqnService, promptService,
                        aiModelResolver, botExecutionFactoryService);
                break;
            case "C": // Coding Bot
                bot = new CodingBot(botInstance, aiModel, botType, locale, genericCqnService);
                break;
            default:
                throw new BusinessException("Unsupported bot function type: " + functionTypeCode);
        }
        addBotInstanceNode(bot);
        return bot;
    }

    public Task createTaskInstance(String taskId) {
        Task task;
        // Tasks taskInstance = genericCqnService.getTaskAndSubBotsById(taskId);
        Tasks taskInstance = genericCqnService.getTaskById(taskId);
        task = new GenericTask(taskInstance);
        // return new GenericTask(task);
        addTaskNode(task, taskInstance.getBotInstanceId()); // 添加至缓存
        return task;
    }

    public Task getParentTaskByBotInstance(String botInstanceId) {
        TaskBotNode botNode = getBotInstanceNode(botInstanceId);

        TaskBotNode parentNode = getParent(botNode);
        // if (parentNode == null) {

        // parentNode =
        // getTaskNode(botNode.getBotObject().getBotInstance().getTaskId());
        // }
        return parentNode != null && parentNode.getType() == TaskBotNode.NodeType.TASK
                ? parentNode.getTaskObject()
                : null;
        // if (botNode != null) {
        // TaskBotNode parentNode = botNode.getParent();
        // if (parentNode != null && parentNode.getType() == TaskBotNode.NodeType.TASK)
        // {
        // return parentNode.getTaskObject();
        // }
        // }
        // return null;
    }

    public TaskBotNode getParent(TaskBotNode current) {
        if (current.getParent() != null) {
            return current.getParent();
        } else {
            // 如果没有父节点，返回根节点
            if (current.getType() == TaskBotNode.NodeType.TASK) {
                // return getRoot(current);
                // Node(current.)
                return getBotInstanceNode(current.getTaskObject().getTask().getBotInstanceId());
            } else {
                return getTaskNode(current.getBotObject().getBotInstance().getTaskId());
            }
        }
    }
}
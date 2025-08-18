package customer.ai2code.model.tree;

import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.bot.Bot;
import customer.ai2code.model.task.Task;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * 任务-Bot树形节点
 * 表示Task和BotInstance的层级关系
 */
public class TaskBotNode {
    
    // 节点类型
    public enum NodeType {
        TASK,           // 任务节点
        BOT_INSTANCE    // Bot实例节点
    }
    
    private final String id;
    private final NodeType type;
    
    // 树形关系
    private TaskBotNode parent;
    private final List<TaskBotNode> children = new ArrayList<>();
    
    // 业务对象 - 只存储Task或Bot
    private Task taskObject;
    private Bot botObject;
    
    // 快速查找子节点的映射
    private final Map<String, TaskBotNode> childrenMap = new HashMap<>();
    
    // 构造函数 - 用于Task节点
    public TaskBotNode(Task task) {
        this.id = task.getTask().getId();
        this.type = NodeType.TASK;
        this.taskObject = task;
        this.botObject = null;
    }
    
    // 构造函数 - 用于Bot节点
    public TaskBotNode(Bot bot) {
        this.id = bot.getBotInstance().getId();
        this.type = NodeType.BOT_INSTANCE;
        this.taskObject = null;
        this.botObject = bot;
    }
    
    // 添加子节点
    public void addChild(TaskBotNode child) {
        children.add(child);
        childrenMap.put(child.getId(), child);
        child.setParent(this);
    }
    
    // 移除子节点
    public void removeChild(String childId) {
        TaskBotNode child = childrenMap.remove(childId);
        if (child != null) {
            children.remove(child);
            child.setParent(null);
        }
    }
    
    // 查找子节点
    public TaskBotNode findChild(String childId) {
        return childrenMap.get(childId);
    }
    
    // 递归查找后代节点
    public TaskBotNode findDescendant(String nodeId) {
        if (this.id.equals(nodeId)) {
            return this;
        }
        
        for (TaskBotNode child : children) {
            TaskBotNode found = child.findDescendant(nodeId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
    
    // // 获取根节点（主任务）
    // public TaskBotNode getRoot() {
    //     TaskBotNode current = this;
    //     while (current.parent != null) {
    //         current = current.parent;
    //     }
    //     return current;
    // }
    
    // // 获取主任务ID
    // public String getMainTaskId() {
    //     TaskBotNode root = getRoot();
    //     if (root.type == NodeType.TASK && root.taskObject != null) {
    //         if (root.taskObject.getTask().getIsMain() != null && root.taskObject.getTask().getIsMain()) {
    //             return root.id;
    //         }
    //     }
    //     // throw new BusinessException("Root node is not a main task");
    //     return null;
    // }
    
    // 查找指定序列的Bot实例
    public TaskBotNode findBotBySequence(int sequence) {
        for (TaskBotNode child : children) {
            if (child.type == NodeType.BOT_INSTANCE && child.botObject != null) {
                Integer botSequence = child.botObject.getBotInstance().getSequence();
                if (botSequence != null && botSequence == sequence) {
                    return child;
                }
            }
        }
        return null;
    }

    // 查找指定序列的Task实例
    public TaskBotNode findTaskBySequence(int sequence) {
        for (TaskBotNode child : children) {
            if (child.type == NodeType.TASK && child.taskObject != null) {
                Integer taskSequence = child.taskObject.getTask().getSequence();
                if (taskSequence != null && taskSequence == sequence) {
                    return child;
                }
            }
        }
        return null;
    }
    
    // 获取任务ID（适用于Bot节点）
    public String getTaskId() {
        if (type == NodeType.BOT_INSTANCE && botObject != null) {
            return botObject.getBotInstance().getTaskId();
        } else if (type == NodeType.TASK && taskObject != null) {
            return taskObject.getTask().getId();
        }
        return null;
    }
    
    // 获取Bot实例的类型ID
    public String getBotTypeId() {
        if (type == NodeType.BOT_INSTANCE && botObject != null) {
            return botObject.getBotInstance().getTypeId();
        }
        return null;
    }
    
    // 获取任务的类型ID
    public String getTaskTypeId() {
        if (type == NodeType.TASK && taskObject != null) {
            return taskObject.getTask().getTypeId();
        }
        return null;
    }
    
    // 检查是否是主任务
    public boolean isMainTask() {
        if (type == NodeType.TASK && taskObject != null) {
            Boolean isMain = taskObject.getTask().getIsMain();
            return isMain != null && isMain;
        }
        return false;
    }
    
    // 获取Bot实例的状态
    public String getBotStatus() {
        if (type == NodeType.BOT_INSTANCE && botObject != null) {
            return botObject.getBotInstance().getStatusCode();
        }
        return null;
    }
    
    // 更新Bot实例的状态
    public void updateBotStatus(String statusCode) {
        if (type == NodeType.BOT_INSTANCE && botObject != null) {
            botObject.getBotInstance().setStatusCode(statusCode);
        }
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public NodeType getType() { return type; }
    public TaskBotNode getParent() { return parent; }
    public List<TaskBotNode> getChildren() { return children; }
    public Task getTaskObject() { return taskObject; }
    public Bot getBotObject() { return botObject; }
    
    public void setParent(TaskBotNode parent) { this.parent = parent; }
    public void setTaskObject(Task taskObject) { 
        if (type != NodeType.TASK) {
            throw new BusinessException("Cannot set task object on non-task node");
        }
        this.taskObject = taskObject; 
    }
    public void setBotObject(Bot botObject) { 
        if (type != NodeType.BOT_INSTANCE) {
            throw new BusinessException("Cannot set bot object on non-bot node");
        }
        this.botObject = botObject; 
    }
    
    // 类型安全的数据访问
    public Task getTask() {
        if (type != NodeType.TASK) {
            throw new BusinessException("Node is not a task node");
        }
        return taskObject;
    }
    
    public Bot getBot() {
        if (type != NodeType.BOT_INSTANCE) {
            throw new BusinessException("Node is not a bot instance node");
        }
        return botObject;
    }
    
    @Override
    public String toString() {
        String typeName = type == NodeType.TASK ? "Task" : "BotInstance";
        String name = "";
        if (type == NodeType.TASK && taskObject != null) {
            name = taskObject.getTask().getName();
        } else if (type == NodeType.BOT_INSTANCE && botObject != null) {
            name = "Bot[" + botObject.getBotInstance().getSequence() + "]";
        }
        return typeName + "(" + id + "): " + name;
    }
}
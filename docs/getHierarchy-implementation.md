# getHierarchy 方法实现说明

## 概述

`getHierarchy` 方法已成功实现在 `TaskServiceImpl` 类中，用于递归构建以指定任务为根节点的完整层次结构，并以 JSON 格式字符串返回。该方法展示了任务（Task）和机器人实例（BotInstance）之间的完整层次关系。

## 方法签名

```java
public String getHierarchy(String taskId)
```

## 实现特性

### 1. 递归结构构建
- **任务层级**: Task → BotInstances → Sub-Tasks → Sub-BotInstances...
- **双向关系**: 
  - `Tasks.getBotInstances()` 获取任务下的机器人实例
  - `BotInstances.getTasks()` 获取机器人实例下的子任务

### 2. JSON 输出格式
```json
{
  "type": "task",
  "id": "task-id",
  "name": "任务名称",
  "description": "任务描述",
  "sequence": 0,
  "isMain": true,
  "items": [
    {
      "type": "bot",
      "id": "bot-instance-id",
      "name": "机器人名称",
      "description": "机器人描述",
      "functionType": "A",
      "status": "SUCCESS",
      "sequence": 1,
      "items": [
        {
          "type": "task",
          "id": "sub-task-id",
          "name": "子任务名称",
          "description": "子任务描述",
          "sequence": 0,
          "isMain": false,
          "items": []
        }
      ]
    }
  ]
}
```

### 3. 节点类型和属性

#### Task 节点属性
- `type`: 固定为 "task"
- `id`: 任务ID
- `name`: 任务名称
- `description`: 任务描述 (可选)
- `sequence`: 序列号
- `isMain`: 是否为主任务
- `items`: 子节点数组 (BotInstance 节点)

#### Bot 节点属性
- `type`: 固定为 "bot"
- `id`: 机器人实例ID
- `name`: 机器人类型名称
- `description`: 机器人描述 (可选)
- `functionType`: 功能类型码 ("A"=AI聊天, "F"=功能调用, "C"=编码)
- `status`: 状态码 (如 "RUNNING", "SUCCESS", "FAILED")
- `sequence`: 序列号
- `items`: 子节点数组 (Sub-Task 节点)

## 使用场景

### 1. 任务管理界面展示
```java
String hierarchyJson = taskService.getHierarchy(mainTaskId);
// 将 JSON 传送到前端，用于树形结构展示
```

### 2. 任务执行状态监控
```java
String hierarchy = taskService.getHierarchy(taskId);
if (hierarchy.contains("\"status\":\"RUNNING\"")) {
    // 有组件正在运行
} else if (hierarchy.contains("\"status\":\"FAILED\"")) {
    // 有组件执行失败
}
```

### 3. 任务复杂度分析
```java
String hierarchy = taskService.getHierarchy(taskId);
int taskCount = countOccurrences(hierarchy, "\"type\":\"task\"");
int botCount = countOccurrences(hierarchy, "\"type\":\"bot\"");
System.out.println("任务包含 " + taskCount + " 个子任务和 " + botCount + " 个机器人");
```

### 4. 调试和故障排查
```java
// 输出完整的任务层次结构，用于调试
String hierarchy = taskService.getHierarchy(problemTaskId);
logger.debug("Task hierarchy: {}", hierarchy);
```

## 核心实现逻辑

### 1. 递归构建流程
```java
private HierarchyNode buildHierarchy(Task task) {
    // 1. 创建任务节点
    HierarchyNode taskNode = createTaskNode(task);
    
    // 2. 获取任务下的所有 BotInstances
    List<BotInstances> botInstances = task.getTask().getBotInstances();
    
    // 3. 为每个 BotInstance 创建节点
    for (BotInstances botInstance : botInstances) {
        HierarchyNode botNode = createBotNode(botInstance);
        
        // 4. 获取 BotInstance 下的子任务
        List<Tasks> subTasks = botInstance.getTasks();
        
        // 5. 递归处理子任务
        for (Tasks subTask : subTasks) {
            HierarchyNode subTaskNode = buildHierarchy(getCurrentTask(subTask.getId()));
            botNode.items.add(subTaskNode);
        }
        
        taskNode.items.add(botNode);
    }
    
    return taskNode;
}
```

### 2. JSON 序列化
- **手动构建**: 避免依赖外部JSON库
- **字符转义**: 处理特殊字符，确保JSON格式正确
- **空值处理**: 优雅处理 null 值和空集合

### 3. 错误处理
- **异常捕获**: 在递归过程中捕获异常，避免整个构建过程失败
- **部分失败容忍**: 单个节点失败不影响其他节点的构建
- **空结果处理**: 任务不存在时返回空JSON对象 `{}`

## 性能考虑

### 1. 缓存利用
- 使用 `getCurrentTask()` 和 `getCurrentBot()` 方法，充分利用现有缓存机制
- 避免重复数据库查询

### 2. 递归深度控制
- 理论上支持无限深度的层次结构
- 实际深度受任务设计复杂度限制

### 3. 内存使用
- 构建过程中会创建临时的 `HierarchyNode` 对象
- 最终只返回JSON字符串，临时对象会被垃圾回收

## 最佳实践

### 1. 错误处理
```java
try {
    String hierarchy = taskService.getHierarchy(taskId);
    if ("{}".equals(hierarchy)) {
        // 任务不存在或无权限访问
        handleTaskNotFound(taskId);
    } else {
        // 正常处理层次结构
        processHierarchy(hierarchy);
    }
} catch (Exception e) {
    logger.error("Failed to get task hierarchy for: " + taskId, e);
    // 处理异常情况
}
```

### 2. JSON 解析
如果需要进一步处理JSON结果，建议使用专业的JSON库：
```java
String hierarchyJson = taskService.getHierarchy(taskId);
ObjectMapper mapper = new ObjectMapper();
JsonNode root = mapper.readTree(hierarchyJson);
// 进行JSON操作
```

### 3. 大型任务处理
对于非常复杂的任务层次结构，考虑：
- 分页或分层加载
- 异步处理
- 结果缓存

## 扩展建议

### 1. 添加过滤功能
```java
public String getHierarchy(String taskId, String statusFilter, String typeFilter) {
    // 只返回特定状态或类型的节点
}
```

### 2. 支持不同输出格式
```java
public String getHierarchyAsXML(String taskId) {
    // 返回XML格式的层次结构
}
```

### 3. 添加统计信息
```java
public HierarchyStatistics getHierarchyStatistics(String taskId) {
    // 返回层次结构的统计信息（深度、节点数量等）
}
```

## 测试建议

### 1. 单元测试
- 测试简单的单任务层次结构
- 测试多层嵌套的复杂结构
- 测试异常情况处理

### 2. 集成测试
- 在真实环境中测试完整的任务执行流程
- 验证层次结构的准确性

### 3. 性能测试
- 测试大型任务层次结构的构建性能
- 验证内存使用情况

## 已知限制

1. **循环引用**: 目前没有检测循环引用，理论上可能导致无限递归
2. **大数据量**: 非常大的层次结构可能导致性能问题
3. **并发安全**: 在高并发环境下可能需要额外的同步机制

## 更新日志

- **已实现**: 基本的递归层次结构构建
- **已实现**: JSON格式输出
- **已实现**: 错误处理和异常容忍
- **已文档化**: 完整的使用示例和最佳实践

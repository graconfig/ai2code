# Path Parser Refactoring Documentation

## 重构目标
将路径相关的方法从 `ContextServiceImpl` 移动到 `PathParser` 工具类中，实现更好的代码组织和复用性。

## 重构内容

### 移动到 PathParser 的方法

1. **getPathHierarchy(String path)**
   - 功能：获取路径的所有层级
   - 例如："subtask[0].cView" -> ["subtask", "subtask[0]"]
   - 用于创建中间虚拟节点

2. **getImmediateParentPath(String path)**
   - 功能：获取直接父路径
   - 例如："subtask[0].cView" -> "subtask[0]"
   - 用于构建树形结构的父子关系

3. **generateLabelFromPath(String path)**
   - 功能：从路径生成显示标签
   - 例如："subtask[0].cView" -> "CView"
   - 支持移除数组索引并首字母大写

### PathParser 完整功能

现在 `PathParser` 类提供以下完整的路径操作功能：

```java
@Component
public class PathParser {
    // 原有方法
    public String[] parsePath(String path)           // 解析路径为段数组
    public String getParentPath(String path)         // 获取父路径
    
    // 新增方法
    public List<String> getPathHierarchy(String path)      // 获取路径的所有层级
    public String getImmediateParentPath(String path)      // 获取直接父路径
    public String generateLabelFromPath(String path)       // 生成标签
}
```

### ContextServiceImpl 简化

移除了所有路径处理逻辑，现在专注于业务逻辑：

```java
@Service
public class ContextServiceImpl implements ContextService {
    private final GenericCqnService genericCqnService;
    private final ObjectMapper objectMapper;
    private final PathParser pathParser;  // 依赖注入 PathParser
    
    // 使用 pathParser 处理所有路径相关操作
    private void createIntermediateNodes(String path, Map<String, ContextTreeNode> nodeMap) {
        List<String> pathHierarchy = pathParser.getPathHierarchy(path);
        // ...
    }
    
    private ContextNodes createVirtualContextNode(String path) {
        // ...
        virtualNode.setLabel(pathParser.generateLabelFromPath(path));
        // ...
    }
}
```

## 重构优势

### 1. **单一职责原则**
- `PathParser`：专门处理路径解析和操作
- `ContextServiceImpl`：专门处理上下文业务逻辑

### 2. **代码复用性**
- 路径相关方法可以在其他服务中复用
- 避免重复实现路径处理逻辑

### 3. **更好的测试性**
- 可以独立测试路径解析逻辑
- 业务逻辑测试更加专注

### 4. **维护性提升**
- 路径处理逻辑集中在一个类中
- 修改路径解析规则只需修改一个地方

## 影响的文件

1. **PathParser.java** - 新增了3个路径处理方法
2. **ContextServiceImpl.java** - 移除路径处理方法，添加 PathParser 依赖
3. **ContextHierarchyTest.java** - 更新构造函数以包含 PathParser

## 路径处理示例

### 输入路径: "subtask[0].cView"

1. **getPathHierarchy()** 返回:
   ```
   ["subtask", "subtask[0]"]
   ```

2. **getImmediateParentPath()** 返回:
   ```
   "subtask[0]"
   ```

3. **generateLabelFromPath()** 返回:
   ```
   "CView"  (移除了数组索引并首字母大写)
   ```

### 结果层级结构:
```
subtask (虚拟节点)
  └── subtask[0] (虚拟节点)
      └── subtask[0].cView (实际节点)
```

## 向后兼容性
- 所有公共 API 保持不变
- 内部实现重构，外部调用无影响
- JSON 输出格式完全一致

## 使用说明
在需要路径处理的其他服务中，只需注入 `PathParser`：

```java
@Service
public class AnotherService {
    private final PathParser pathParser;
    
    public AnotherService(PathParser pathParser) {
        this.pathParser = pathParser;
    }
    
    public void someMethod() {
        List<String> hierarchy = pathParser.getPathHierarchy("some.path[0].field");
        // 使用层级信息...
    }
}
```

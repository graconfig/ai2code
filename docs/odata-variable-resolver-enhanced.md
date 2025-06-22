# Enhanced OData Variable Resolver

这是一个增强的 OData 变量解析器，使用 SAP CAP 的 CdsModel 来提供更好的 OData URL 解析和查询功能。

## 功能特性

### 1. 支持的 OData 查询场景

#### 1.1 查询整个实体集合
```
OData:BotInstances
OData:BotInstances?$select=ID,status_code&$filter=status_code eq 'RUNNING'&$top=10
```

#### 1.2 查询单个实体
```
OData:BotInstances('bot-123')
OData:BotInstances(ID='bot-123')
```

#### 1.3 访问实体属性
```
OData:BotInstances('bot-123')/status_code
OData:Tasks('task-456')/name
```

#### 1.4 导航属性查询
```
OData:BotInstances('bot-123')/messages
OData:BotInstances('bot-123')/task
```

#### 1.5 复杂导航路径
```
OData:BotInstances('bot-123')/task/name
OData:Tasks('task-456')/botInstances/messages
OData:BotInstances/b631b9de-24ba-439c-afb3-f6a8002ddc9c/type/outputContextPath
```

#### 1.6 复合键实体
```
OData:ContextNodes(taskId='task-123',path='context.result')
```

### 2. 支持的查询选项

- **$select**: 选择特定字段
  ```
  OData:BotInstances?$select=ID,status_code,sequence
  ```

- **$filter**: 过滤条件
  ```
  OData:BotInstances?$filter=status_code eq 'RUNNING'
  OData:Tasks?$filter=isMain eq true and sequence gt 5
  ```

- **$top**: 限制返回记录数
  ```
  OData:BotInstances?$top=10
  ```

- **$skip**: 跳过记录数
  ```
  OData:BotInstances?$skip=20
  ```


### 3. 组合查询示例

```
OData:BotInstances?$select=ID,status_code,sequence&$filter=status_code ne 'FAILED'&$orderby=sequence asc&$top=20&$skip=10&$expand=task
```

## 实现细节

### CdsModel 集成

解析器使用 SAP CAP 的 `CdsModel` 来：

1. **实体验证**: 验证实体名称是否存在于模型中
2. **字段验证**: 验证字段名称和类型
3. **类型转换**: 根据 CDS 类型定义自动转换键值
4. **导航属性验证**: 验证导航路径的正确性


### Select语句构建
1. 通过问号将path和query分开
2. 将path通过/符号分割成若干段，从最右侧开始往左侧依次解析。
3. 最右侧段是一层导航属性/属性的情况
    - 将这个段放在column中
    - 往左一位/两位查找EntitySet和Key 作为from表
4. 最右侧段是多层下的导航属性/属性的情况
    - 将这个段放在column中
    - 往左一位/两位查找到的NavigationProperty和Key 作为from表，接着往左递归查找将找到的所有EntitySet/NavigationProperty和Key作为where条件
        - 找到的第一个NavigationProperty(作为from表)，在这个情况下是往左第二个EntitySet/Navigationproperty的navigationProperty，需要通过CdsModel反查找转换成EntitySet。
        - 往左找到第一个带key的NavigationProperty为止，使用CdsModel通过from里的EntitySet，反找到这个NavigationProperty作为from Entity的NavigationProperty，作为where条件添加，
    - 示例： 一对多导航：BotInstances 通过NavigationProperty tasks关联Tasks表，Tasks表通过NavigationProperty messages 关联Messages表；反过来Messages表通过task关联到Tasks表，Tasks表通过botInstance关联到BotInstances表。一对一导航：BotInstances-> type(BotTypes)-> model(BotModels)；BotModels-> type(BotTypes)-> botInstance(BotInstances)。
        - 获取导航属性 /BotInstances/bot-123/task/task-123/messages, 解析成Select语句为：Select.from("BotMessages").where(a -> a.task.ID().eq("task-123"));
        - 获取单个导航属性 /BotInstances/bot-123/task/task-123/messages/message-1，解析成Select语句为：Select.from("BotMessages").where(a -> a.ID().eq("message-1"));
        - 获取字段 /BotInstances/bot-123/tasks/task-123/messages/message-1/content,解析成Select语句为: Select.from("BotMessages").column(["content"]).where(a -> a.ID().eq("message-1"));
        - 获取一对一导航属性 /BotInstances/bot-123/type/model,解析成Select语句为 Select.from("BotModels").where(a -> a.type().botInstance().ID().eq("bot-123")); 或者解析成 Select.from("BotTypes").column(["model"]).where(a -> a.botInstance().ID().eq("bot-123"));
        - 获取一对一导航属性下的字段 /BotInstances/bot123/type/model/field1，解析成Select语句为 Select.from("BotModels").column(["field1"]).where(a -> a.type().botInstance().ID().eq("bot-123")); 

5. query中filter字段，默认只作用与最左侧第一个segment。不影响字段个数和行数的query不解析，比如orderby这种。不解析aggregation这类复杂语句。


## ExecuteCondition 使用指南

### 概述
ExecuteCondition是Bot执行的条件表达式，使用SpEL (Spring Expression Language) 语法，支持变量解析和复杂条件判断。

### 集成到Spring Batch

在Spring Batch的执行流程中，executeCondition会在每个Bot执行前进行评估：

```java
// 在BatchStepFlowConfiguration中的集成
String executeCondition = bot.getBotType().getExecuteCondition();
if (executeCondition != null && !executeCondition.trim().isEmpty()) {
    boolean shouldExecute = executeConditionEvaluationService.evaluateCondition(executeCondition, bot);
    if (!shouldExecute) {
        // 跳过Bot执行，记录跳过原因
        System.out.println("🚫 跳过Bot执行，条件不满足: " + botInstanceId);
        return RepeatStatus.FINISHED;
    }
}
```

### 条件表达式语法

#### 1. 简单布尔条件
```sql
-- 在数据库中配置BotType.executeCondition字段
UPDATE BotType SET executeCondition = 'true' WHERE id = 'bot-001';
UPDATE BotType SET executeCondition = 'false' WHERE id = 'bot-002';
```

#### 2. 变量条件
```sql
-- 基于Context变量（绝对路径）
UPDATE BotType SET executeCondition = '{{Context:user.status}} == ''READY''' WHERE id = 'bot-003';

-- 基于SubContext变量（相对路径）
UPDATE BotType SET executeCondition = '{{SubContext:processStatus}} == ''COMPLETED''' WHERE id = 'bot-004';

-- 基于Instance变量
UPDATE BotType SET executeCondition = '{{Instance:sequence}} > 1 && {{Instance:autoRun}} == true' WHERE id = 'bot-005';

-- 基于OData查询
UPDATE BotType SET executeCondition = '{{OData:/Tasks/{{Instance:ID}}/status}} != ''FAILED''' WHERE id = 'bot-006';
```

#### 3. 复杂条件组合
```sql
-- 逻辑AND条件
UPDATE BotType SET executeCondition = '{{Context:user.role}} == ''ADMIN'' && {{SubContext:confidence}} > 0.8' WHERE id = 'bot-007';

-- 逻辑OR条件
UPDATE BotType SET executeCondition = '{{Context:status}} == ''READY'' || {{Context:status}} == ''PENDING''' WHERE id = 'bot-008';

-- 数值比较
UPDATE BotType SET executeCondition = '{{SubContext:errorCount}} < 3 && {{SubContext:retryCount}} <= 5' WHERE id = 'bot-009';

-- JSON对象属性访问
UPDATE BotType SET executeCondition = '{{Context:userProfile}}.age >= 18 && {{Context:settings}}.autoApprove == true' WHERE id = 'bot-010';
```

#### 4. 高级表达式
```sql
-- 混合引用
UPDATE BotType SET executeCondition = '{{OData:/BotInstances/{{Instance:ID}}/type/functionType}} == ''AI_CHAT''' WHERE id = 'bot-011';

-- 复杂JSON对象条件
UPDATE BotType SET executeCondition = '{{Context:config}}.features.enabled == true && {{SubContext:data}}.quality > 0.9' WHERE id = 'bot-012';
```

### 实际应用场景

#### 场景1：基于前一个Bot的执行结果
```sql
-- 只有当前一个Bot成功时才执行
UPDATE BotType SET executeCondition = '{{SubContext:previousBotResult}} == ''SUCCESS''' WHERE botType = 'DataValidationBot';
```

#### 场景2：基于用户权限控制
```sql
-- 只有管理员或有权限的用户才能执行
UPDATE BotType SET executeCondition = '{{Context:user.role}} == ''ADMIN'' || {{Context:user.permissions}}.contains(''EXECUTE_BOT'')' WHERE botType = 'AdminBot';

-- 只在特定时间执行
UPDATE BotType SET executeCondition = 'T(java.time.LocalDateTime).now().getHour() >= 9 && T(java.time.LocalDateTime).now().getHour() <= 17' WHERE botType = 'BusinessHoursBot';
```

#### 场景3：基于数据状态
```sql
-- 只有当数据准备就绪时执行
UPDATE BotType SET executeCondition = '{{SubContext:dataQuality.completeness}} >= 0.9 && {{SubContext:dataQuality.accuracy}} > 0.95' WHERE botType = 'DataProcessingBot';

-- 只有当错误数量在阈值内时执行
UPDATE BotType SET executeCondition = '{{SubContext:errorCount}} < 10 && {{Context:retryCount}} <= 3' WHERE botType = 'ErrorHandlingBot';
```

#### 场景4：基于配置和状态
```sql
-- 只有开启自动运行时执行
UPDATE BotType SET executeCondition = '{{Instance:autoRun}} == true && {{Context:environment}} == ''PROD''' WHERE botType = 'AutomationBot';

-- 基于优先级和前置条件执行
UPDATE BotType SET executeCondition = '{{Context:priority}} >= 3 && {{SubContext:prerequisiteCompleted}} == true' WHERE botType = 'HighPriorityBot';
```

#### 场景5：基于OData查询结果
```sql
-- 基于相关实例状态执行
UPDATE BotType SET executeCondition = '{{OData:/BotInstances/{{Instance:parentTaskId}}/status}} == ''SUCCESS''' WHERE botType = 'DependentBot';

-- 基于动态数据查询执行
UPDATE BotType SET executeCondition = '{{OData:/Tasks/{{Instance:ID}}/contextNodes}} != null && {{Context:ready}} == true' WHERE botType = 'ContextAwareBot';
```

### Spring Batch执行日志

执行时的日志输出示例：
```
✅ Bot执行条件满足: bot-001 -> 条件: {{Context:user.status}} == 'READY'
🚫 跳过Bot执行，条件不满足: bot-002 -> 条件: {{SubContext:errorCount}} >= 5
✅ Function Bot执行条件满足: bot-003 -> 条件: {{Instance:autoRun}} == true && {{Context:priority}} > 5
✅ OData条件执行成功: bot-004 -> 条件: {{OData:/Tasks/{{Instance:ID}}/status}} != 'FAILED'
```

### 测试条件表达式

可以使用ExecuteConditionEvaluationService的测试方法：
```java
@Autowired
private ExecuteConditionEvaluationService executeConditionService;

public void testCondition() {
    String condition = "{{Context:user.status}} == 'READY'";
    String testResult = executeConditionService.testCondition(condition, bot);
    System.out.println("测试结果: " + testResult);
}
```

### 变量上下文说明

支持的变量类型（必须使用{{}}包围）：
- `{{Context:path}}` - 绝对路径引用Task context中的任意节点
- `{{SubContext:path}}` - 相对路径引用当前任务context节点下的内容
- `{{Instance:属性名}}` - 引用当前实例的属性(如ID、status、result、type等)
- `{{OData:/路径}}` - 直接查询OData数据，支持混合引用

变量解析通过现有的VariableParsingService完成，支持JSON对象属性访问和复杂嵌套结构。

### 注意事项

1. **语法规范**：变量引用必须使用{{}}包围，字符串值需要使用单引号包围
2. **错误处理**：条件表达式语法错误会导致默认执行（返回true）
3. **空值处理**：空的executeCondition字段表示无条件执行
4. **调试支持**：可以通过日志查看条件评估过程和结果

### 配置建议

1. 使用明确的变量名和条件逻辑
2. 为复杂条件添加注释（在description字段中）
3. 定期测试条件表达式的正确性
4. 监控跳过执行的Bot数量，避免过度限制

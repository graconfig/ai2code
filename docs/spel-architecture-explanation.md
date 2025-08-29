# SpEL架构设计说明

## 🔧 三个关键类的作用和关系

### 1. **ExecuteConditionEvaluationService** - 核心服务类
**职责**：Bot执行条件的评估入口
```java
@Service
public class ExecuteConditionEvaluationService {
    // 核心方法：评估Bot是否应该执行
    public boolean evaluateCondition(String executeCondition, Bot bot)
}
```

### 2. **SpELConfiguration** - Spring配置类
**职责**：提供SpEL相关的Bean配置
```java
@Configuration
public class SpELConfiguration {
    @Bean
    public ExpressionParser expressionParser() {
        return new SpelExpressionParser(); // 统一的表达式解析器
    }
    
    @Bean 
    public StandardEvaluationContext standardEvaluationContext() {
        // 预配置的评估上下文
    }
}
```

### 3. **SpELVariableHelper** - 工具类
**职责**：处理复杂变量注入（JSON对象、数组等）
```java
public class SpELVariableHelper {
    // 将解析后的复杂变量注入到SpEL上下文
    public static void injectVariables(StandardEvaluationContext context, String resolvedCondition)
}
```

---

## 🔄 完整的执行流程

```
1. executeCondition获取
   ↓
2. VariableParsingService解析变量
   ↓ 
3. SpELVariableHelper注入复杂变量
   ↓
4. ExpressionParser评估条件
   ↓
5. 返回布尔结果
```

### 详细流程示例：

**输入条件**：
```sql
executeCondition = "{{Context:userProfile}}.age >= 18 && {{Context:settings}}.autoRun == true"
```

**Step 1 - VariableParsingService解析**：
```java
// 解析后变成：
"{'name':'张三','age':25,'city':'北京'}.age >= 18 && {'autoRun':true,'theme':'dark'}.autoRun == true"
```

**Step 2 - SpELVariableHelper处理**：
```java
// 自动提取JSON并注入变量
context.setVariable("jsonVar0", Map.of("name", "张三", "age", 25, "city", "北京"));
context.setVariable("jsonVar1", Map.of("autoRun", true, "theme", "dark"));

// 条件变成可执行的SpEL：
"#jsonVar0.age >= 18 && #jsonVar1.autoRun == true"
```

**Step 3 - ExpressionParser评估**：
```java
Expression expr = expressionParser.parseExpression("#jsonVar0.age >= 18 && #jsonVar1.autoRun == true");
Boolean result = expr.getValue(context, Boolean.class); // true
```

---

## 💡 设计优势分析

### **1. 为什么需要SpELConfiguration？**

❌ **直接new方式（当前）**：
```java
this.expressionParser = new SpelExpressionParser(); // 每个服务都创建新实例
```

✅ **依赖注入方式（推荐）**：
```java
// 全局单例，可统一配置
@Autowired
private ExpressionParser expressionParser;
```

**优势**：
- **性能**：单例模式，避免重复创建
- **配置统一**：可以在SpELConfiguration中统一配置安全策略
- **扩展性**：可以注册自定义函数和方法

### **2. 为什么需要SpELVariableHelper？**

**处理复杂变量场景**：
```java
// 简单变量 - 直接setVariable即可
context.setVariable("botId", "bot-123");

// 复杂JSON变量 - 需要SpELVariableHelper
String jsonData = "{'users':[{'name':'张三','age':25},{'name':'李四','age':30}],'config':{'enabled':true}}";
SpELVariableHelper.injectVariables(context, jsonData);
// 自动解析并注入为可用的变量
```

### **3. 为什么需要ExecuteConditionEvaluationService？**

**业务封装**：
- 整合VariableParsingService（变量解析）
- 整合SpELVariableHelper（复杂变量处理）
- 整合ExpressionParser（条件评估）
- 提供统一的条件评估API

---

## 🎯 实际使用场景对比

### **场景1：简单条件**
```sql
executeCondition = "true"
```
- ✅ 直接评估，不需要复杂处理
- SpELVariableHelper：不涉及
- SpELConfiguration：基础解析器即可

### **场景2：变量条件**  
```sql
executeCondition = "{{Instance:sequence}} > 1"
```
- VariableParsingService：`{{Instance:sequence}}` → `2`
- 最终条件：`"2 > 1"`
- SpELVariableHelper：不需要
- 评估结果：`true`

### **场景3：复杂JSON条件**
```sql
executeCondition = "{{Context:userProfile}}.age >= 18 && {{Context:userProfile}}.city == 'Beijing'"
```
- VariableParsingService：解析为JSON字符串
- SpELVariableHelper：提取JSON并注入变量
- 最终条件：`"#jsonVar0.age >= 18 && #jsonVar0.city == 'Beijing'"`
- 评估结果：基于实际数据

---

## 🔧 优化建议

### **1. 当前架构改进**
```java
// 修改ExecuteConditionEvaluationService构造函数
public ExecuteConditionEvaluationService(
    VariableParsingService variableParsingService,
    ExpressionParser expressionParser) { // 依赖注入
    this.variableParsingService = variableParsingService;
    this.expressionParser = expressionParser;
}
```

### **2. SpELConfiguration增强**
```java
@Configuration
public class SpELConfiguration {
    @Bean
    public ExpressionParser expressionParser() {
        SpelExpressionParser parser = new SpelExpressionParser();
        // 可以配置解析器选项
        return parser;
    }
    
    @Bean
    public StandardEvaluationContext evaluationContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();
        // 注册常用函数
        context.registerFunction("isEmpty", 
            StringUtils.class.getDeclaredMethod("isEmpty", String.class));
        return context;
    }
}
```

### **3. SpELVariableHelper使用场景扩展**
```java
// 支持更多复杂数据类型
SpELVariableHelper.injectVariables(context, resolvedCondition);
// 自动处理：
// - JSON对象：{"name":"value"}
// - JSON数组：[1,2,3]  
// - 嵌套结构：{"users":[{"id":1}]}
```

---

## 📊 总结

| 类名 | 职责 | 当前状态 | 建议 |
|------|------|----------|------|
| ExecuteConditionEvaluationService | 条件评估入口 | ✅ 已使用 | 改为依赖注入 |
| SpELConfiguration | SpEL配置管理 | ⚠️ 未充分利用 | 依赖注入ExpressionParser |
| SpELVariableHelper | 复杂变量注入 | ✅ 已集成 | 扩展支持更多数据类型 |

这样的架构设计使得executeCondition功能既强大又灵活，能够处理从简单布尔值到复杂JSON数据的各种条件表达式！

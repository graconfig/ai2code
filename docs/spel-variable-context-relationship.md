# StandardEvaluationContext变量名与SpEL表达式关系详解

## 🎯 核心关系：变量名 → SpEL表达式访问

### **基本语法**
```java
// 设置变量
context.setVariable("variableName", value);

// SpEL表达式中访问
String spelExpression = "#variableName.property";
```

---

## 📋 SpELVariableHelper中的实际应用

### **1. JSON对象注入示例**

**输入条件**：
```sql
executeCondition = "{{Context:userProfile}}.age >= 18"
```

**VariableParsingService解析后**：
```java
resolvedCondition = "{'name':'张三','age':25,'city':'北京'}.age >= 18"
```

**SpELVariableHelper处理过程**：
```java
private void injectJsonObjects(StandardEvaluationContext context, String text) {
    // 1. 正则匹配找到JSON: {'name':'张三','age':25,'city':'北京'}
    String jsonStr = "{'name':'张三','age':25,'city':'北京'}";
    
    // 2. 解析为Map对象
    Map<String, Object> jsonObject = {
        "name": "张三",
        "age": 25,
        "city": "北京"
    };
    
    // 3. 注入变量到上下文
    context.setVariable("jsonObject0", jsonObject);  // 整个对象
    context.setVariable("name", "张三");              // 各个属性
    context.setVariable("age", 25);
    context.setVariable("city", "北京");
}
```

**可能的SpEL表达式访问方式**：
```java
// 方式1: 通过整个对象访问
"#jsonObject0.age >= 18"     // true

// 方式2: 直接访问属性变量
"#age >= 18"                 // true

// 方式3: 复杂表达式
"#jsonObject0.name.equals('张三') && #age >= 18"  // true
```

---

## ⚠️ 当前实现的问题分析

### **问题1: 变量名冲突**
```java
// 如果有多个JSON对象都有相同属性名（基于正确语法）
resolvedCondition = "{'name':'张三','age':25}.age >= 18 && {'name':'公司A','age':10}.age >= 5"

// 当前实现会导致变量覆盖
context.setVariable("name", "张三");    // 第一次设置
context.setVariable("name", "公司A");   // 第二次覆盖了第一次！
context.setVariable("age", 25);        // 第一次设置
context.setVariable("age", 10);        // 第二次覆盖了第一次！
```

### **问题2: SpEL表达式无法直接使用**
```java
// 原始条件中的表达式
"{'name':'张三','age':25}.age >= 18"

// 注入变量后，这个表达式依然无法执行，因为：
// 1. JSON字符串 {'name':'张三','age':25} 不是有效的SpEL语法
// 2. 需要转换为变量引用形式
```

---

## 🔧 改进方案

### **方案1: 智能表达式替换**
```java
public void injectVariables(StandardEvaluationContext context, String resolvedCondition) {
    String processedCondition = resolvedCondition;
    
    // 1. 找到JSON对象并替换为变量引用
    Matcher matcher = JSON_OBJECT_PATTERN.matcher(resolvedCondition);
    int index = 0;
    
    while (matcher.find()) {
        String jsonStr = matcher.group();
        String varName = "jsonObject" + index;
        
        // 注入变量
        Map<String, Object> jsonObject = objectMapper.readValue(jsonStr, Map.class);
        context.setVariable(varName, jsonObject);
        
        // 替换表达式中的JSON字符串为变量引用
        processedCondition = processedCondition.replace(jsonStr, "#" + varName);
        index++;
    }
    
    // 最终SpEL表达式: "#jsonObject0.age >= 18"
    return processedCondition;
}
```

### **方案2: 命名空间隔离**
```java
private void injectJsonObjects(StandardEvaluationContext context, String text) {
    Matcher matcher = JSON_OBJECT_PATTERN.matcher(text);
    int index = 0;
    
    while (matcher.find()) {
        String jsonStr = matcher.group();
        Map<String, Object> jsonObject = objectMapper.readValue(jsonStr, Map.class);
        
        // 使用唯一的命名空间
        String namespace = "json" + index;
        context.setVariable(namespace, jsonObject);
        
        // 避免属性名冲突，使用命名空间前缀
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            context.setVariable(namespace + "_" + entry.getKey(), entry.getValue());
        }
        
        index++;
    }
}

// 访问方式：
// #json0.age >= 18
// #json0_age >= 18  (备选方式)
```

---

## 💡 实际使用示例

### **当前代码的变量访问**
```java
// 假设resolvedCondition = "{'name':'张三','age':25}.age >= 18"

// SpELVariableHelper注入后的变量：
context.variables = {
    "jsonObject0": {"name":"张三", "age":25},
    "name": "张三",
    "age": 25
}

// 可能的SpEL表达式：
"#jsonObject0.age >= 18"  // ✅ 正确
"#age >= 18"              // ✅ 正确，但可能有冲突风险
"{'name':'张三','age':25}.age >= 18"  // ❌ 错误，无效SpEL语法
```

### **建议的改进访问**
```java
// 改进后的变量注入：
context.variables = {
    "userProfile": {"name":"张三", "age":25},  // 语义化命名
    "settings": {"theme":"dark", "autoSave":true}
}

// 清晰的SpEL表达式：
"#userProfile.age >= 18 && #settings.autoSave == true"
```

---

## 🎯 总结建议

### **当前SpELVariableHelper的局限性**：
1. **表达式转换不完整** - 注入变量但不更新表达式
2. **变量名冲突风险** - 多个JSON对象的同名属性会覆盖
3. **语义不清晰** - jsonObject0、jsonObject1 等名称无意义

### **改进方向**：
1. **返回处理后的表达式** - 将JSON字符串替换为变量引用
2. **智能命名策略** - 根据变量来源生成语义化名称
3. **冲突检测** - 避免变量名覆盖
4. **表达式验证** - 确保生成的SpEL表达式语法正确

这样可以让executeCondition的条件表达式更加可靠和易于理解！

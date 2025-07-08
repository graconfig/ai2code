# ChatBot RAG 功能使用指南

## 概述

本文档介绍如何在 ChatBot 中使用 RAG (Retrieval-Augmented Generation) 功能。RAG 功能通过 `RAGExtractionFactoryService` 来动态创建不同类型的 RAG 提取器，增强聊天机器人的回答质量。

## 核心组件

### 1. RAGExtractionFactoryService
- **位置**: `customer.ai2code.service.impl.rag.RAGExtractionFactoryService`
- **功能**: 工厂服务，负责创建和管理 RAG 提取器实例
- **特性**:
  - 支持类名解析（简单类名和完整类名）
  - 自动依赖注入
  - Spring 容器集成
  - 多包路径搜索

### 2. ChatBot RAG 增强功能
- **新增方法**: `chatWithRAG()`
- **功能**: 结合 RAG 提取的内容进行增强聊天
- **参数**:
  - `content`: 用户输入内容
  - `ragExtractorClassName`: RAG 提取器类名
  - `ragSource`: RAG 数据源
  - `ragTopK`: 返回的 Top-K 结果数量
  - `threshold`: 相似度阈值

## 可用的 RAG 提取器

### 1. RAGCDSViewExtractorImpl
- **用途**: 从 CDS 视图中提取相关信息
- **适用场景**: 视图查询、数据模型相关问题

### 2. RAGJoinConditionExtractorImpl
- **用途**: 提取表连接条件信息
- **适用场景**: 数据关联、表关系查询

### 3. RAGViewFieldExtractorImpl
- **用途**: 提取视图字段信息
- **适用场景**: 字段查询、数据结构相关问题

## 使用示例

### 基本使用
```java
// 1. 创建 ChatBot 实例
ChatBot chatBot = new ChatBot(
    botInstance, 
    aiModel, 
    botType, 
    Locale.CHINESE,
    genericCqnService,
    promptService,
    aiModelResolver,
    ragExtractionFactoryService
);

// 2. 使用 RAG 增强聊天
String response = chatBot.chatWithRAG(
    "如何查询用户订单信息？",        // 用户输入
    "RAGCDSViewExtractorImpl",     // RAG 提取器
    "scenario_views",              // 数据源
    5,                            // Top-K
    0.75                          // 相似度阈值
);
```

### 动态选择 RAG 提取器
```java
public String chatWithDynamicRAG(String userInput, String extractorType, ...) {
    String extractorClassName;
    String ragSource;
    int topK;
    double threshold;
    
    switch (extractorType.toLowerCase()) {
        case "view":
            extractorClassName = "RAGCDSViewExtractorImpl";
            ragSource = "scenario_views";
            topK = 5;
            threshold = 0.75;
            break;
        case "join":
            extractorClassName = "RAGJoinConditionExtractorImpl";
            ragSource = "join_conditions";
            topK = 3;
            threshold = 0.8;
            break;
        case "field":
            extractorClassName = "RAGViewFieldExtractorImpl";
            ragSource = "view_fields";
            topK = 7;
            threshold = 0.7;
            break;
        default:
            return chatBot.chat(userInput); // 普通聊天
    }
    
    return chatBot.chatWithRAG(userInput, extractorClassName, ragSource, topK, threshold);
}
```

### 检查可用的 RAG 提取器
```java
// 获取所有可用的 RAG 提取器
List<String> availableExtractors = chatBot.getAvailableRAGExtractors();

// 检查特定提取器是否可用
boolean isAvailable = chatBot.isRAGExtractorAvailable("RAGCDSViewExtractorImpl");
```

## 参数配置指南

### TopK 值建议
- **视图查询**: 3-5 个结果
- **字段查询**: 5-10 个结果
- **连接条件**: 2-3 个结果

### 相似度阈值建议
- **高精度要求**: 0.8-0.9
- **平衡精度和召回**: 0.7-0.8
- **高召回要求**: 0.6-0.7

### 数据源配置
根据不同的 RAG 提取器选择合适的数据源：
- `scenario_views`: CDS 视图场景
- `join_conditions`: 表连接条件
- `view_fields`: 视图字段信息

## 错误处理

### 自动回退机制
如果 RAG 增强失败，系统会自动回退到普通聊天模式：
```java
try {
    // RAG 增强聊天
    return chatWithRAG(...);
} catch (Exception e) {
    // 自动回退到普通聊天
    return chat(content);
}
```

### 提取器验证
在使用前可以验证提取器是否可用：
```java
if (chatBot.isRAGExtractorAvailable(extractorClassName)) {
    // 使用 RAG 增强
    return chatBot.chatWithRAG(...);
} else {
    // 使用普通聊天
    return chatBot.chat(content);
}
```

## 最佳实践

1. **选择合适的提取器**: 根据用户问题类型选择最相关的 RAG 提取器
2. **调优参数**: 根据实际效果调整 TopK 和相似度阈值
3. **缓存策略**: 考虑对频繁使用的 RAG 结果进行缓存
4. **监控性能**: 监控 RAG 提取的响应时间和准确性
5. **错误恢复**: 始终提供回退机制确保系统稳定性

## 扩展开发

### 添加新的 RAG 提取器
1. 实现 `RAGExtraction` 接口
2. 添加 `@RAGExtractor` 注解
3. 在工厂服务的搜索包路径中确保可被发现
4. 更新 `getAvailableRAGExtractors()` 方法

### 自定义提示词模板
可以通过修改 `buildEnhancedPrompt()` 方法来自定义 RAG 内容的集成方式。

## 注意事项

1. 确保所有依赖服务都已正确注入
2. RAG 提取器类必须在指定的包路径中
3. 数据源必须在系统中存在并可访问
4. 建议在生产环境中进行充分的性能测试

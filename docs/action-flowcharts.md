# CAP Actions 流程图

## 1. BotInstances.execute() Action 流程图

```mermaid
flowchart TD
    A[开始: execute action] --> B[MainServiceBotInstancesExecuteHandler.on]
    B --> C[获取 BotInstancesExecuteContext]
    C --> C2[BotService.execute]
    C2 --> D[BotService.getCurrentBot]
    D --> E{Bot 类型判断}
    
    E -->|ChatBot| F[ChatBot.execute]
    E -->|FunctionCallingBot| G[FunctionCallingBot.execute]
    E -->|CodingBot| H[CodingBot.execute]
    
    F --> I[AIModel.execute]
    G --> J[AIModel.execute + function call工具调用]
    H --> K[AIModel.execute + 代码生成]
    
    I --> L[设置执行结果]
    J --> L[设置执行结果]
    K --> L[设置执行结果]

    L --> L1[BotService.saveContext将结果保存到ContextNode表中]
    
    L --> M[MainServiceBotInstancesExecuteHandler.after]
    M --> N[返回执行结果]
    N --> O[结束]
```

## 2. BotInstances.chatCompletion() Action 流程图

```mermaid
flowchart TD
    A[开始: chatCompletion action] --> B[MainServiceBotInstancesChatCompletionHandler.before]
    B --> C[获取 BotInstancesChatCompletionContext]
    C --> D[获取聊天内容]
    C --> D1[获取历史聊天记录]
    C --> D2[获取提示词]
        
    D --> G[BotService.chat]
    D1 --> G
    D2 --> G
    
    G --> I[Bot.chat]
    I --> I1[AIModelResolver.resolveAIService通过AIModel注入不同aiService]
    I1 --> J[AIService.chatCompletion]

    J --> O[设置响应结果]

    O --> P[BotService.saveMessage将用户消息和AI消息保存至表中]
    P --> Q[返回响应]
    Q --> R[结束]
```

## 3. BotMessages.adopt() Action 流程图

```mermaid
flowchart TD
    A[开始: adopt action] --> B[MainServiceBotMessagesAdoptHandler.before]
    B --> C[获取 BotMessagesAdoptContext]
    C --> D[获取消息ID]
    D --> E[BotService.adoptMessage]
    E --> F[查询 BotMessages]
    F --> G{消息是否存在}
    
    G -->|否| H[返回空列表]
    G -->|是| I[提取消息内容]
    
    I --> J[创建 ContextNodes]
    J --> K[设置节点类型和内容]
    K --> L[保存 ContextNodes]
    L --> M[返回 ContextNodes 列表]
    
    H --> N[MainServiceBotMessagesAdoptHandler.after]
    M --> N
    N --> O[设置采纳结果]
    O --> P[结束]
```

## 4. createTaskWithBots() Action 流程图

```mermaid
flowchart TD
    A[开始: createTaskWithBots action] --> B[MainServiceCreateTaskWithBotsHandler.before]
    B --> C[获取 CreateTaskWithBotsContext]
    C --> D[获取任务信息]
    D --> E[TaskService.createTaskWithBots]
    E --> F[创建 Tasks 实体]
    F --> G[保存 Tasks 到数据库]
    G --> H[查询 TaskType 配置]
    H --> I{是否有关联的 BotTypes}
    
    I -->|否| J[任务创建完成]
    I -->|是| K[遍历 BotTypes]
    
    K --> L[创建 BotInstances]
    L --> M[设置 Bot 配置]
    M --> N[设置执行序列]
    N --> O[保存 BotInstances]
    O --> P{还有其他 BotTypes?}
    
    P -->|是| K
    P -->|否| Q[所有 Bot 实例创建完成]
    
    J --> R[MainServiceCreateTaskWithBotsHandler.after]
    Q --> R
    R --> S[返回 Tasks 实体]
    S --> T[结束]
```

## 5. 整体系统交互流程图

```mermaid
flowchart TD
    A[CAP Framework] --> B[Action Handler]
    B --> C[Service Layer]
    C --> D[Entity Service]
    C --> E[AI Service]
    C --> F[Config Service]
    
    D --> G[Database]
    E --> H[AI Model APIs]
    F --> I[Configuration Data]
    
    B --> J[Context Processing]
    J --> K[Before Handler]
    K --> L[Main Handler]
    L --> M[After Handler]
    
    M --> N[Response]
    
    subgraph "AI Integration"
        E --> O[Claude AI]
        E --> P[OpenAI]
        E --> Q[Custom Models]
    end
    
    subgraph "Data Persistence"
        D --> R[Tasks]
        D --> S[BotInstances]
        D --> T[BotMessages]
        D --> U[ContextNodes]
    end
```

## 6. Bot 执行生命周期流程图

```mermaid
flowchart TD
    A[Bot 初始化] --> B[加载配置]
    B --> C[选择 AI 模型]
    C --> D[准备输入上下文]
    D --> E{Bot 类型}
    
    E -->|Chat| F[聊天对话处理]
    E -->|Function| G[函数调用处理]
    E -->|Coding| H[代码生成处理]
    
    F --> I[发送到 AI 模型]
    G --> J[解析工具规范]
    H --> K[代码模板处理]
    
    J --> L[执行工具调用]
    L --> M[整合工具结果]
    M --> I
    
    K --> I
    I --> N[接收 AI 响应]
    N --> O[后处理响应]
    O --> P{需要流式输出?}
    
    P -->|是| Q[SSE 流式推送]
    P -->|否| R[直接返回结果]
    
    Q --> S[完成]
    R --> S
```
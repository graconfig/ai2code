# 程序规定
项目名称
ai2code


# 实现效果
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Task Object Page（头部/主属性区）                      │
├─────────────────────────────────────────────────────────────────────────────┤
│    左栏（树形导航）         │   中栏（context节点编辑/显示）      │  右栏（AI/chat + code result）   │
│────────────────────────────┼───────────────────────────────────┼──────────────────────────────────│
│• Context树（Tab1/区块）     │ • 当前选中 context 节点内容         │ ┌─────── AI Chat ─────────────┐  │
│  - 总体需求（markdown）     │   - markdown: 富文本/MD编辑        │ │  user: ...                  │  │
│  - 用户需求（markdown）     │   - 代码/配置: code editor         │ │  AI  : ...                  │  │
│  - 报表层次（String）       │   - 长文本: TextArea               │ │  markdown/code block         │  │
│    └─ 报表层1,2...          │   - 支持只读/可编辑切换            │ │  ...                 采用   │  │
│                            │                                   │ └─────────────────────────────┘  │
│────────────────────────────┼                                   |──────────────────────────────────│
│• Task树（Tab2/区块）        │                                   │ ┌────── Code Result 区 ──────┐  │
│  - 主Task                  │                                   │ │ •                           │  │
│    ├─ botInstance(1)       │                                   │ │ • 执行按钮                  │  │
│    │   └─ Task(1)          │                                   │ │ • 运行结果 / 日志           │  │
│    │       └─ botInstance  │                                   │ └─────────────────────────────┘  │
│    ├─ botInstance(2)       │                                   │                                  │
│    ├─ botInstance(3)...    │                                   │                                  │
│    └─ ...                  │                                   │                                  │
│────────────────────────────┴───────────────────────────────────┴──────────────────────────────────│
│  左侧Tab/区块可切换 Context 树 与 Task树，支持增删改查，节点图标区分类型。            │
│  中栏动态渲染选中 context 节点内容，支持多编辑模式。                                 │
│  右栏分 AI/chat、Code Result 区，支持 chat 历史、代码运行日志、执行按钮等交互。         │
└─────────────────────────────────────────────────────────────────────────────┘
```

# 📦 CDS设计

---
## 📁 /db/orchestration-model.cds  
```
using {
    cuid,
    managed
} from '@sap/cds/common';

using {
    ai.orchestration.config.TaskType          as TaskType,
    ai.orchestration.config.BotType           as BotType,
    ai.orchestration.config.BotInstanceStatus as BotInstanceStatus
} from './orchestration-config-model';

namespace ai.orchestration;

/** Task entity, supports multi-level sub-tasks, recorded in context. */
entity Task : cuid, managed {

    name         : String(100);
    description  : String;
    contextPath  : String(1000); // Context path for the task, e.g., datasource.children[3]
    sequence     : Integer; // Execution order for sub-tasks
    isMain       : Boolean default true; // Redundant flag to mark main tasks
    botInstance  : Association to BotInstance;
    type         : Association to TaskType;
    botInstances : Composition of many BotInstance
                    on botInstances.task = $self;
    contextNodes : Composition of many ContextNode
                       on contextNodes.task = $self; // All context nodes under this task
}

/** Single context node, flattened structure to support tree reconstruction */
entity ContextNode : cuid, managed {
    path  : String(1000); // Unique path, e.g., a.b.c[0].d
    label : String(200); // Node label
    type  : String(50); // Type (e.g., text, markdown, code, object, array)
    value : LargeString; // Node value/content
    task  : Association to Task; // Parent task
    //readonly  : Boolean default false; // Optional: whether the node is read-only
    // Extendable: sorting, validation, metadata, etc.
}

/** Bot execution instance */
entity BotInstance : cuid, managed {
    sequence : Integer;
    result   : LargeString;
    type     : Association to BotType;
    status   : Association to BotInstanceStatus default 'C';
    task     : Association to Task;
    tasks    : Composition of many Task
                   on tasks.botInstance = $self; // Sub-tasks
    messages : Composition of many BotMessage
                on messages.botInstance = $self;
}

/** Bot message entity, records human-AI/system conversations */
entity BotMessage : cuid, managed {
    role        : String(20); // 'user' | 'assistant' | 'system'
    message     : LargeString;
    ragData     : LargeString; // RAG result data (optional)
    botInstance : Association to BotInstance;
}

```

## 📁  /db/orchestration-config-model.cds
```
using {
  cuid,
  managed,
  sap.common.CodeList
} from '@sap/cds/common';

namespace ai.orchestration.config;

/** Task types, such as field design, API mapping, etc. */
entity TaskType : cuid, managed {
  name        : String(100);
  description : String;
  autoRun     : Boolean default false;
  isMain      : Boolean default true;
  botTypes    : Composition of many BotType
                  on botTypes.taskType = $self;
}

/** BotType: bot type, with contextType field (enum reference) */
entity BotType : cuid, managed {
  taskType            : Association to TaskType;
  sequence            : Integer;
  name                : String(50);
  description         : String;
  functionType        : Association to BotFunctionType default 'AI_CHAT';
  autoRun             : Boolean default false;
  executionCondition  : String(1000);
  model               : Association to ModelConfig;
  prompts             : Composition of many PromptText
                          on prompts.botType = $self;
  outputContextPath   : String(1000); // Output path, can be array[-1]; relative in subTask, absolute in main task
  contextType         : Association to ContextType; // New: Output context data type (enum)
  isRAGEnabled        : Boolean default false;
  //ragFunction       : Association to RagFunction;
  ragClass            : String(100); // Replaces ragFunction
  ragSource           : String(100);
  ragTopK             : Integer;
  implementationClass : String(100); // For C and F types
  //subTaskContextPath: String(1000); // Must include array, e.g., datasource.children[-1].content
  //subTaskType       : Association to TaskType;
}

/** AI model configuration */
entity ModelConfig : cuid, managed {
  name       : String(100);
  provider   : String(50);
  modelName  : String(100);
  parameters : LargeString;
}

/** Bot prompt templates, support multilingual and multiple templates */
entity PromptText : cuid, managed {
  botType : Association to BotType;
  lang    : Association to Languages; // Association to enable value help
  name    : String(100);
  content : LargeString;
}

/** Bot execution status enumeration */
entity BotInstanceStatus : CodeList {
  key code : String enum {
        CREATED   = 'CREATED';
        RUNNING   = 'RUNNING';
        SUCCESS   = 'SUCCESS';
        FAILED    = 'FAILED';
        SKIPPED   = 'SKIPPED';
        CANCELLED = 'CANCELLED';
      };
}

/** Bot function type enumeration */
entity BotFunctionType : CodeList {
  key code : String enum {
        AI_CHAT        = 'AI CHAT';
        FUNCTION_CALL = 'FUNCTION CALL';
        CODE          = 'CODE';
      //SUBTASK_GENERATOR = 'SUBTASK_GENERATOR'; replaced by FUNCTION_CALL
      };
}

entity Languages : CodeList {
  key code : String enum {
        EN = 'English';
        ZH = 'Chinese';
        DE = 'German';
        JA = 'Japanese';
        ID = 'Indonesian';
      };
}

/** ContextType: type of content in the context node (strongly typed) */
entity ContextType : CodeList {
  key code : String enum {
        string   = 'STRING';   // Plain text
        markdown = 'MARKDOWN'; // Markdown document
        code     = 'CODE';     // Code snippet
        json     = 'JSON';     // JSON structure
      //object  = 'OBJECT';    // Object
      //array   = 'ARRAY';     // Array
      //table   = 'TABLE';     // Table
      //image   = 'IMAGE';     // Image (base64 or URL)
      };
}

```

## srv/orchestration-service.cds

```
using ai.orchestration as db from '../db/orchestration-model';
using ai.orchestration.config as config from '../db/orchestration-config-model';

service MainService {
    entity Tasks        as projection on db.Task;
    entity ContextNodes as projection on db.ContextNode;
    entity TaskType as projection on config.TaskType;


    //entity SubTasks      as projection on db.SubTask;
    entity BotInstances as projection on db.BotInstance
        actions {
            action execute() returns {
                result : String;
                tasks  : array of UUID;
            };
            action chatCompletion(content: LargeString) returns BotMessages;
        }

    entity BotMessages  as projection on db.BotMessage
        actions {
            action adopt() returns ContextNodes;
        }

    // Unbound actions
    action createTaskWithBots(name : String,
                              description : String,
                              typeId : UUID) returns Tasks;


}
```

## srv/orchestration-config-service.cds
```
using ai.orchestration.config as cfg from '../db/orchestration-config-model';

service ConfigService {
  entity TaskTypes            as projection on cfg.TaskType;
  //entity TaskBotSequences     as projection on cfg.TaskBotSequence;
  entity BotTypes             as projection on cfg.BotType;
  entity ModelConfigs         as projection on cfg.ModelConfig;
  entity PromptTexts          as projection on cfg.PromptText;
  //entity FunctionCalls        as projection on cfg.FunctionCall;
  entity BotInstanceStatuses  as projection on cfg.BotInstanceStatus;
  entity BotFunctionTypes     as projection on cfg.BotFunctionType;
  //entity RagFunctions         as projection on cfg.RagFunction;
  entity ContextTypes         as projection on cfg.ContextType;
  entity Languages             as projection on cfg.Languages;
}


annotate ConfigService.TaskTypes with @odata.draft.enabled ;
```

## 可以直接用 CDS 标准服务的典型用法

| 功能描述                       | 方法 | 接口路径                                 |
|------------------------------|------|------------------------------------------|
| 查询 Task 列表               | GET  | /Tasks                                   |
| 查询某 Task 的所有 BotInstance | GET  | /Tasks(ID)/botInstances                  |
| 查询 Task 的所有 ContextNode | GET  | /Tasks(ID)/contextNodes                  |
| 查询 ContextNode 详情        | GET  | /ContextNodes(ID)                        |
| 更新 ContextNode             | PATCH| /ContextNodes(ID)                        |
| 查询 BotInstance 的消息      | GET  | /BotInstances(ID)/messages               |
| 查询 BotInstance 下的任务    | GET  | /BotInstances(ID)/tasks                  |


## 实体标准接口举例（前端可直接使用，无需 action）
- 查询 context Node：GET /Tasks('...')/contextNodes
- 保存 context 编辑：PATCH /ContextNodes('...')
- 查询 botInstance 树：GET /Tasks('...')/botInstances
- 查询消息对话：GET /BotInstances('...')/messages
## 需自定义的 action

| 功能描述                                | 接口名称            | 说明                                                                 |
|---------------------------------------|---------------------|----------------------------------------------------------------------|
| 创建 Task 并生成 BotInstances         | createTaskWithBots  | 1. 弹出窗口输入 `taskType`、`name`、`description`<br>2. 建 Task 时同时创建 BotInstances |
| 发送对话消息                           | chatCompletion      | 将用户消息发送给 AI，AI 返回消息，将用户消息和 AI 消息共同存储至 BotMessages 表 |
| 流式对话消息服务                       | chatStreaming       | 以流式方式返回 AI 消息                                               |
| BotInstance 执行（Function Call/代码） | execute  | 类型为 F 和 C 的需要执行；S 类型被 F 取代                            |
| 采用 AI 回复并写回 ContextNode         | adopt     | AI 回复内容写入 ContextNode，同时更新 BotInstance 的 Status 字段     |


---
# 后端逻辑
## 手动执行
| action                              | 参数                   | 功能                                                           |
|-------------------------------------|------------------------|----------------------------------------------------------------|
| createTaskWithBots                  |  1. 传入：<br>  a. name : String,<br>  b. description : String,<br>  c. typeId : UUID<br>2. 传出:<br>  a. tasks: Tasks| 1. 根据typeId查询ConfigService中TaskTypes表中条目以及他的botTypes<br>2. 创建一条目MainService.Tasks。<br>a. 将isMain设置为true<br>b. Name<br>c. Description<br>d. contextPath设置为空<br>e. sequence设置为空或0<br>f. Type 设置为typeId查找到的TaskType<br>3. 根据ConfigService.TaskTypes.botTypes的条目数，创建相应条目数的MainService.BotInstances。<br>a. Sequence 设置为 ConfigService.BotTypes.sequence<br>b. Type 设置为ConfigService.BotTypes<br>c. status设置为BotInstanceStatus.code.C (Created)<br>4. (自动执行)将name、description和第二步得到的Tasks.Id，创建两条目MainService.ContextNodes。<br>a. 第一条<br>a. path设置为name<br>b. 第二条<br>a. path设置为description<br>5. 将创建的Tasks条目返回给前端                         |
| BotInstances/execute                | 1. 传入<br>a. Bound action自带参数<br>2. 传出<br>a. result: String(单纯code运行返回的结果)<br>b. tasks(如果是functioncall类型的Bot，执行分解任务的操作，返回的是taskId的数组)                     | 正常流程(同步)：<br>1. 将BotInstances的status字段设置为R(Running)。<br>F类型BotInstance:<br>a. 取到维护的BotType.prompts<br>b. 调用AI Function call，调用维护的implementationClass维护的类中execute方法。返回结果存储到BotInstances.result字段中。同时通过维护的outputContextPath写到ContextNodes条目中。<br>c. 将BotInstances.Status设置为S(Success)。<br>C类型BotInstance:<br>a. 执行implementationClass维护的Class中execute方法<br>b. 结果存储在BotInstances.result字段中。<br>c. 将BotInstances.Status设置为S(Success)。<br>错误处理：<br>1. 将BotInstances的status字段设置为F(Failed)。|
| BotInstances/chatCompletion         | 1. 传入<br>a. Bound action自带参数<br>b. content: LargeString<br>2. 传出<br>a. LargeString   | 正常流程: a. 将BotInstances的status字段设置为R(Running)。<br> b. 判断是否第一次对话，第一次对话content字段 作为本次用户对话内容，取BotTypes.prompts作为system消息 组成消息发给AI模型。<br> c. 不是第一次对话，content字段 作为本次用户对话内容，再根据BotInstances.messages取到历史记录消息 组成消息发给AI模型。<br> d. 第一次对话，将system消息、user消息和assistant消息存入BotMessages表中。非第一次对话，将user消息和assistant消息存入BotMessages表中。<br> e. 返回assistant消息。<br> 错误处理: a. 将BotInstances的status字段设置为F(Failed)。|
| BotMessages/adopt                   | 1. 传入<br>a. Bound action自带参数<br>2. 传出<br>a. array of ContextNodes                     | 1. 获取当前BotMessages条目。<br>2. 根据BotMessages.botInstance获取到BotInstances条目。<br>3. 根据BotInstances.type获取BotTypes条目。<br>4. 将这条消息内容存储到ContextNodes条目中。<br>a. Path: 根据BotTypes设置的outputContextPath<br>b. label:<br>c. type: 根据BotTypes设置的contextType<br>d. Value: BotMessages.message/根据AI function call转成相应的格式。<br>5. 将BotInstances的status字段设置为S(Success)。<br>6. 返回ContextNodes条目。<br>错误处理：<br>a. 将BotInstances的status字段设置为F(Failed)。               |
| /api/chat/Streaming<br>Restful协议SSE流式接口。| 传参<br>1. 传入<br>a. botInstanceId<br>b. content<br>2. 传出<br>a. SseEmitter对象      | 1. 判断是否第一次对话，第一次对话content字段 作为本次用户对话内容，取BotTypes.prompts作为system消息 组成消息发给AI模型。<br>2. 不是第一次对话，content字段 作为本次用户对话内容，再根据BotInstances.messages取到历史记录消息 组成消息发给AI模型。<br>3. 启动异步线程接收AI返回的流式消息内容：<br>a. 每次AI返回消息段，立刻通过SseEmitter.send()返回消息给前端。<br>b. 在SseEmitter结束的时候启动存储消息的动作：第一次对话，将system消息、user消息和assistant消息存入BotMessages表中。非第一次对话，将user消息和assistant消息存入BotMessages表中。<br>错误处理：<br>1. 将BotInstances的status字段设置为F(Failed)。|


## 自动执行
考虑引入Spring-AI或Langchain4j。


## 类定义
### Model

#### CDS auto generated POJO Model:

1. BotInstances
```java
@CdsName("MainService.BotInstances")
@Generated(
    value = "cds-maven-plugin",
    date = "2025-06-12T12:28:49.432640500Z",
    comments = "com.sap.cds:cds-maven-plugin:3.10.1 / com.sap.cds:cds4j-api:3.10.1"
)
public interface BotInstances extends CdsData {
  String ID = "ID";

  String CREATED_AT = "createdAt";

  String CREATED_BY = "createdBy";

  String MODIFIED_AT = "modifiedAt";

  String MODIFIED_BY = "modifiedBy";

  String SEQUENCE = "sequence";

  String RESULT = "result";

  String TYPE = "type";

  String TYPE_ID = "type_ID";

  String STATUS = "status";

  String STATUS_CODE = "status_code";

  String TASK = "task";

  String TASK_ID = "task_ID";

  String TASKS = "tasks";

  String MESSAGES = "messages";

  @CdsName(ID)
  String getId();

  @CdsName(ID)
  void setId(String id);

  Instant getCreatedAt();

  void setCreatedAt(Instant createdAt);

  /**
   * Canonical user ID
   */
  String getCreatedBy();

  /**
   * Canonical user ID
   */
  void setCreatedBy(String createdBy);

  Instant getModifiedAt();

  void setModifiedAt(Instant modifiedAt);

  /**
   * Canonical user ID
   */
  String getModifiedBy();

  /**
   * Canonical user ID
   */
  void setModifiedBy(String modifiedBy);

  Integer getSequence();

  void setSequence(Integer sequence);

  String getResult();

  void setResult(String result);

  BotType getType();

  void setType(Map<String, ?> type);

  @CdsName(TYPE_ID)
  String getTypeId();

  @CdsName(TYPE_ID)
  void setTypeId(String typeId);

  BotInstanceStatus getStatus();

  void setStatus(Map<String, ?> status);

  @CdsName(STATUS_CODE)
  String getStatusCode();

  @CdsName(STATUS_CODE)
  void setStatusCode(String statusCode);

  Tasks getTask();

  void setTask(Map<String, ?> task);

  @CdsName(TASK_ID)
  String getTaskId();

  @CdsName(TASK_ID)
  void setTaskId(String taskId);

  List<Tasks> getTasks();

  void setTasks(List<? extends Map<String, ?>> tasks);

  List<BotMessages> getMessages();

  void setMessages(List<? extends Map<String, ?>> messages);

  BotInstances_ ref();

  static BotInstances create() {
    return Struct.create(BotInstances.class);
  }

  static BotInstances of(Map<String, Object> map) {
    return Struct.access(map).as(BotInstances.class);
  }

  static BotInstances create(String id) {
    Map<String, Object> keys = new HashMap<>();
    keys.put(ID, id);
    return Struct.access(keys).as(BotInstances.class);
  }
}

```

2. Tasks

```java
@CdsName("MainService.Tasks")
@Generated(
    value = "cds-maven-plugin",
    date = "2025-06-12T12:28:49.432640500Z",
    comments = "com.sap.cds:cds-maven-plugin:3.10.1 / com.sap.cds:cds4j-api:3.10.1"
)
public interface Tasks extends CdsData {
  String ID = "ID";

  String CREATED_AT = "createdAt";

  String CREATED_BY = "createdBy";

  String MODIFIED_AT = "modifiedAt";

  String MODIFIED_BY = "modifiedBy";

  String NAME = "name";

  String DESCRIPTION = "description";

  String CONTEXT_PATH = "contextPath";

  String SEQUENCE = "sequence";

  String IS_MAIN = "isMain";

  String BOT_INSTANCE = "botInstance";

  String BOT_INSTANCE_ID = "botInstance_ID";

  String TYPE = "type";

  String TYPE_ID = "type_ID";

  String BOT_INSTANCES = "botInstances";

  String CONTEXT_NODES = "contextNodes";

  @CdsName(ID)
  String getId();

  @CdsName(ID)
  void setId(String id);

  Instant getCreatedAt();

  void setCreatedAt(Instant createdAt);

  /**
   * Canonical user ID
   */
  String getCreatedBy();

  /**
   * Canonical user ID
   */
  void setCreatedBy(String createdBy);

  Instant getModifiedAt();

  void setModifiedAt(Instant modifiedAt);

  /**
   * Canonical user ID
   */
  String getModifiedBy();

  /**
   * Canonical user ID
   */
  void setModifiedBy(String modifiedBy);

  String getName();

  void setName(String name);

  String getDescription();

  void setDescription(String description);

  String getContextPath();

  void setContextPath(String contextPath);

  Integer getSequence();

  void setSequence(Integer sequence);

  Boolean getIsMain();

  void setIsMain(Boolean isMain);

  BotInstances getBotInstance();

  void setBotInstance(Map<String, ?> botInstance);

  @CdsName(BOT_INSTANCE_ID)
  String getBotInstanceId();

  @CdsName(BOT_INSTANCE_ID)
  void setBotInstanceId(String botInstanceId);

  TaskType getType();

  void setType(Map<String, ?> type);

  @CdsName(TYPE_ID)
  String getTypeId();

  @CdsName(TYPE_ID)
  void setTypeId(String typeId);

  List<BotInstances> getBotInstances();

  void setBotInstances(List<? extends Map<String, ?>> botInstances);

  List<ContextNodes> getContextNodes();

  void setContextNodes(List<? extends Map<String, ?>> contextNodes);

  Tasks_ ref();

  static Tasks create() {
    return Struct.create(Tasks.class);
  }

  static Tasks of(Map<String, Object> map) {
    return Struct.access(map).as(Tasks.class);
  }

  static Tasks create(String id) {
    Map<String, Object> keys = new HashMap<>();
    keys.put(ID, id);
    return Struct.access(keys).as(Tasks.class);
  }
}
```

#### AI Config Models:

1. AIModel:
```Java
package customer.ai2code.model;

import cds.gen.configservice.ModelConfigs;
// import cds.gen.configservice.ModelConfigs;
// import cds.gen.configservice.ModelConfigs;
import customer.ai2code.model.config.AIServiceConfig;
import customer.ai2code.service.AIService;

public interface AIModel {
    // public ModelConfigs modelConfigs;

    // private Map<String, E extends Object> modelParametersMap;
    public String getModelName();
    // public Map<String, Object> getModelParametersMap();
    public AIServiceConfig parseModelConfigs();

    public ModelConfigs getModelConfigs();
}
```

2. SAPAICoreOpenAIgpt4o

```java
package customer.ai2code.model;

import cds.gen.configservice.ModelConfigs;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.config.AIServiceConfig;
import customer.ai2code.model.config.SAPAICoreConfig;
import customer.ai2code.service.AIService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import static com.sap.ai.sdk.core.JacksonConfiguration.getDefaultObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SAPAICoreOpenAIgpt4o implements AIModel {

    private ModelConfigs modelConfigs;

    @Override
    public String getModelName() {
        return modelConfigs.getModelName();
    }

    @Override
    public AIServiceConfig parseModelConfigs() {
        try {
            ObjectMapper mapper = getDefaultObjectMapper();
            
            // Check if parameters is a String (JSON) or already an Object
            Object parameters = modelConfigs.getParameters();
            if (parameters instanceof String) {
                // Parse JSON string to SAPAICoreConfig
                return mapper.readValue((String) parameters, SAPAICoreConfig.class);
            } else {
                // Convert Object to SAPAICoreConfig
                return mapper.convertValue(parameters, SAPAICoreConfig.class);
            }
        } catch (Exception e) {
            throw new BusinessException("Failed to parse model configuration", e);
        }
    }
}


```

3. StreamRequestVO

```java
String botInstanceId;
String Content;
```


#### Bot & Task Model

1. Bot

```java
public BotInstancesExecuteContext.ReturnType execute();

public Boolean executeAsync();

public Boolean stop();

public Boolean resume();

public Boolean cancel();

public SseEmitter chatInStreaming(String content);

public String chat(String content);

public BotInstances getBotInstance();

public AIModel getAiModel();
```

2. ChatBot: 具有chatCompletion和chatInStreaming的Bot实施类，BotType=AI_CHAT,参考已经实现的Java代码。
  - 由BotService.chat调用
  - 调用AIService.chat方法
    - 根据AIModel类型，获取到不同AIService服务实现
    - 第一次chat需要保存prompt消息
    - 使用genericCqnService.getMainTaskId，再获取Prompt (PromptService中用到ContextService，Context都是以MainTaskId存储的)
    - 不是第一次调用，获取历史消息
    - 真正调用AIService.chat服务
3. FunctionCallingBot: 具有execute的Bot实施类，BotType=FUNCTION_CALL。
  - 由BotService.execute调用
  - 调用AIService.functionCalling方法
    - 根据AIModel类型，获取到不同AIService服务实现
    - 根据BotType获取prompt
    - 根据BotType获取implementionClass
    - 新建implementationClass的实例，作为参数传入AIService.functionCalling方法
    - 调用AIService.functionCalling方法

4. CodingBot: 具有execute的Bot实施类，BotType=CODE。

5. Task: Task对象类，包含Tasks CDS POJO对象 
```java
public Tasks getTask();
```


### Service
#### AI Service

1. AIService: AI相关的服务类
```java
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import customer.ai2code.model.AIModel;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.processor.StreamingCompletedProcessor;

public interface AIService {
        public String chatWithAI(
                        List<BotMessages> messages,
                        List<PromptTexts> prompts,
                        String content,
                        AIModel model);

        public SseEmitter chatWithAIStreaming(
                        List<BotMessages> messages,
                        List<PromptTexts> prompts,
                        String content,
                        AIModel model,
                        ExecutorService executor,
                        StreamingCompletedProcessor streamingCompletionProcessor);

        public <T extends BotExecution> String functionCalling(
                        List<BotMessages> messages,
                        List<PromptTexts> prompts,
                        // FunctionCalls functionCall,
                        Class<T> botExecutClazz,
                        AIModel model);

        /**
         * Send a chunk to the emitter
         *
         * @param emitter The emitter to send the chunk to
         * @param chunk   The chunk to send
         */
        public static void send(@Nonnull final SseEmitter emitter, @Nonnull final String chunk) {
                try {
                        emitter.send(chunk);
                } catch (final IOException e) {
                        emitter.completeWithError(e);
                }
        }
```
2. SAPOpenAIServiceImpl:  SAP AICore OpenAI服务类

3. SAPClaudeAIServiceImpl SAP AICore ClaudeAI服务类



#### 通用Service

1. BotService: Bot相关服务

execute 的步骤：
a. 获取当前Bot对象
b. 更新Bot状态
c. bot执行
d. 更新bot状态
e. 更新BotInstance的Result字段

```java
package customer.ai2code.service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.ContextNodes;
import customer.ai2code.model.Bot;

public interface BotService {

    
    public Bot getCurrentBot(String botInstanceId);

    public Bot getCurrentBot(String taskId, int sequence);

    public BotMessages chat(BotInstancesChatCompletionContext context);
    public BotMessages chat(String botInstanceId, String content);

    public SseEmitter chatInStreaming(String botInstanceId, String content);


    public Boolean executeAsync(BotInstancesExecuteContext context);
    public Boolean executeAsync(String botInstanceId);

    public BotInstancesExecuteContext.ReturnType execute(BotInstancesExecuteContext context);
    public BotInstancesExecuteContext.ReturnType execute(String botInstanceId);

    public ContextNodes adopt(BotMessagesAdoptContext context);

    public ContextNodes adopt(String botInstanceId, String messageId);
}

```

2. TaskService: Task相关服务

```java
package customer.ai2code.service;

import cds.gen.mainservice.CreateTaskWithBotsContext;
import customer.ai2code.model.Task;

public interface TaskService {

    public Task createTaskWithBots(CreateTaskWithBotsContext context);

    public Task createTaskWithBots(String name, String description, String taskTypeId);

    public Task createTaskWithBots(String botInstanceId, String name, String description, String contextPath, int sequence);

    public Task createTaskWithBots(String botInstanceId, String name);

    public Task getCurrentTask(String taskId);

    public Task getCurrentTask(String botInstanceId, int sequence);
}


```

3. ContextService: Context上下文相关服务

```java
package customer.ai2code.service;

import java.util.List;
import java.util.Map;

import cds.gen.mainservice.ContextNodes;

public interface ContextService {
    public List<Map<String, Object>> buildContextAsHierarchy(List<ContextNodes> contextNodes);

    public String getContextFullPath(String botInstanceId, String subPath);

    public ContextNodes upsertContext(
        //     String botInstanceId,
            String taskId,
            // Integer sequence,
            String contextPath,
            String contextValue);
    public ContextNodes getContextNode(
            String contextNodeId);
}

```

4. PromptService: 提示词相关服务

```java
package customer.ai2code.service;

import java.util.List;

import cds.gen.configservice.PromptTexts;

public interface PromptService {
    /**
     * 
     * @param prompt     提示词
     * @param mainTaskId 主任务ID
     * @return
     */
    public String parse(PromptTexts prompt, String mainTaskId, String botInstanceId);

    /**
     * 
     * @param botTypeId
     * @param mainTaskId
     * @return
     */
    public List<PromptTexts> getPrompts(String botTypeId, String mainTaskId, String botInstanceId);
}

```


#### Lifecycle Managements 
首先定义一个基于Spring Bean的Bot/Task的链式/树状全局链表。实现BotService/TaskService的以下方法：
1. 从链表中查询Bot/Task(getCurrentBot/getCurrentTask)，如果没有则从存储的Bot/Task表中取到记录并实例化，再将Bot/Task放到链表中。
2. CreateTaskWithBots方法新建的Task和Bots除了在表中记录，还需要将Task和Bot放到全局链表中，供所有用户查询和使用。


1. TaskBotNode: 任务-Bot树形节点，表示Task和BotInstance的层级关系

```java
package customer.ai2code.model.tree;

import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.Bot;
import customer.ai2code.model.Task;

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
    
    // 获取根节点（主任务）
    public TaskBotNode getRoot() {
        TaskBotNode current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        return current;
    }
    
    // 获取主任务ID
    public String getMainTaskId() {
        TaskBotNode root = getRoot();
        if (root.type == NodeType.TASK && root.taskObject != null) {
            if (root.taskObject.getTask().getIsMain() != null && root.taskObject.getTask().getIsMain()) {
                return root.id;
            }
        }
        // throw new BusinessException("Root node is not a main task");
        return null;
    }
    
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
```

2. TaskBotCacheManager: 任务-Bot统一缓存管理器，使用树形结构管理Task和BotInstance的层级关系

```java
package customer.ai2code.service.impl;

import customer.ai2code.model.tree.TaskBotNode;
import customer.ai2code.model.Bot;
import customer.ai2code.model.Task;

import org.springframework.stereotype.Service;

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
        if (node != null && node.getType() == TaskBotNode.NodeType.TASK) {
            return node;
        }
        return null;
    }
    
    /**
     * 获取Bot实例节点
     */
    public TaskBotNode getBotInstanceNode(String botInstanceId) {
        TaskBotNode node = nodeCache.get(botInstanceId);
        if (node != null && node.getType() == TaskBotNode.NodeType.BOT_INSTANCE) {
            return node;
        }
        return null;
    }
    
    /**
     * 根据Bot实例ID获取主任务ID
     */
    public String getMainTaskId(String botInstanceId) {
        TaskBotNode botNode = getBotInstanceNode(botInstanceId);
        if (botNode != null) {
            return botNode.getMainTaskId();
        }
        return null;
    }
    
    /**
     * 根据任务ID和序列号查找Bot实例
     */
    public TaskBotNode getBotInstanceByTaskAndSequence(String taskId, int sequence) {
        TaskBotNode taskNode = getTaskNode(taskId);
        if (taskNode != null) {
            return taskNode.findBotBySequence(sequence);
        }
        return null;
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
}
```





### Bot Execution
Bot执行模型的相关类

#### Interface
1. BotExecution: Bot执行接口，所有Bot执行的动作类必须实现这个接口
```java
package customer.ai2code.service.execution;

// import customer.ai2code.service.execution.annotation.BotExecutor;

// @BotExecutor(name = "Execute Bot Interface", 
//              description = "Marker interface for bot execution implementations", 
//              version = "1.0", 
//              enabled = true)
public interface BotExecution {
    // 空接口，通过注解处理器强制要求实现类必须有execute方法
}
```

#### Annotation

1. BotExecutor: execute类注解

```java
package customer.ai2code.service.execution.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BotExecutor {
    String name();
    String description() default "";
    String version() default "1.0";
    boolean enabled() default true;
}
```

2. ExecuteMethod: execute方法的注解

```java
package customer.ai2code.service.execution.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecuteMethod {
    String operation() default "";
    // int timeout() default 30;
    boolean logExecution() default true;
}
```

3. ExecuteParameter: execute方法的参数注解

```java
// code\srv\src\main\java\customer\ai2code\service\execution\Parameter.java
package customer.ai2code.service.execution.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecuteParameter {
    // String name();
    boolean required() default false;
    String description() ;
}
```

#### 实现类

1. CreateTasksBotExecution: 创建SubTask的execute类

传入参数 botInstanceId, 

```java
package customer.ai2code.service.impl;

import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.execution.annotation.BotExecutor;
import customer.ai2code.service.execution.annotation.ExecuteMethod;
import customer.ai2code.service.execution.annotation.ExecuteParameter;

import java.util.ArrayList;
import java.util.List;

import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.Task;
import customer.ai2code.model.TaskCreationParam;
import customer.ai2code.service.TaskService;

@BotExecutor(name = "Create Tasks Bot Execution", description = "Implementation for creating tasks in the bot execution framework", version = "1.0", enabled = true)
public class CreateTasksBotExecution implements BotExecution {

    public Create

    @ExecuteMethod
    public List<Task> execute(@ExecuteParameter(description = "Bot Instance") String botInstanceId, 
                         @ExecuteParameter(description = "Array of task parameters") List<TaskCreationParam> taskCreationParam, /** 1. sequence 2.name 3.description 4. contextPath */) {

        List<Task> tasks = new ArrayList<>();
        // throw new BusinessException("Unimplemented method 'execute'");
        // 调用BotService.createTaskWithBots(param);
        if (taskCreationParam == null || taskCreationParam.isEmpty()) {
            throw new BusinessException("Task creation parameters cannot be null or empty");
        }
        if (botInstanceId == null || botInstanceId.isEmpty()) {
            throw new BusinessException("Bot instance ID cannot be null or empty");
        }

        // 遍历任务创建参数，创建任务
        for (TaskCreationParam param : taskCreationParam) {
            if (param.getSequence() == null || param.getName() == null || param.getDescription() == null) {
                throw new BusinessException("Task creation parameters must include sequence, name, and description");
            }
            // 调用任务服务创建任务
            tasks.add(taskService.createTaskWithBots(botInstanceId, param.getName(), param.getDescription(), param.getContextPath(), param.getSequence()));
        }
        return tasks;
    }
}
```

2. DeployCDSExecutionImpl: 部署CDS的execute类

### RAG Execution

#### Interface

1. RAGExtrator: RAG提取对象

```java
package customer.ai2code.service.execution;

public interface RAGExtractor {
    public String extract(String RAGSource, int RAGTopK);
}

```



## Function Call 功能架构

### 技术架构概览

Function Call 功能基于注解驱动的架构设计，支持 AI 模型调用 Java 方法执行具体的业务逻辑。整体架构包含以下核心组件：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Function Call 架构图                                │
├─────────────────────────────────────────────────────────────────────────────┤
│   AI Model (OpenAI)                                                         │
│   ↓ Function Call Request                                                    │
│   SAPOpenAIServiceImpl.functionCalling()                                    │
│   ↓                                                                          │
│   ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐          │
│   │ FunctionCall    │    │ OpenAIFunction  │    │ FunctionCall    │          │
│   │ Processor       │    │ CallAdapter     │    │ Execution       │          │
│   │                 │    │                 │    │                 │          │
│   │ • 函数信息提取   │    │ • OpenAI格式转换 │    │ • 参数类型转换   │          │
│   │ • 参数验证      │    │ • JSON Schema   │    │ • 方法执行       │          │
│   │ • 方法调用      │    │ • 参数映射      │    │ • 结果返回       │          │
│   └─────────────────┘    └─────────────────┘    └─────────────────┘          │
│   ↓                                                                          │
│   BotExecution 实现类 (如 CreateTasksBotExecution)                           │
│   ↓                                                                          │
│   业务服务层 (TaskService, etc.)                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 核心组件详解

#### 1. 注解系统

**@BotExecutor**: 类级注解，标识 BotExecution 实现类
```java
@BotExecutor(
    name = "Create Tasks Bot Execution", 
    description = "Implementation for creating tasks", 
    version = "1.0", 
    enabled = true
)
```

**@ExecuteMethod**: 方法级注解，标识可被 AI 调用的方法
```java
@ExecuteMethod(
    operation = "Create Tasks Bot Execution_execute",
    description = "Create multiple tasks",
    logExecution = true
)
```

**@ExecuteParameter**: 参数级注解，定义参数元数据
```java
@ExecuteParameter(
    name = "taskCreationParam",  // 必须与实际参数名一致
    description = "Array of task creation parameters", 
    required = true
)
```

#### 2. FunctionCallProcessor

**核心功能**:
- 扫描带有 `@BotExecutor` 注解的类
- 提取带有 `@ExecuteMethod` 注解的方法信息
- 解析方法参数的类型信息和注解元数据
- 执行函数调用并处理参数类型转换

**关键方法**:
```java
// 提取函数信息
public List<FunctionInfo> extractFunctionInfosFromInstance(T botExecutionInstance)

// 执行函数调用
public Object executeFunctionCallOnInstance(String functionName, String argumentsJson, T botExecutionInstance)

// 参数类型转换
private Object convertArgumentToParameterType(Object argumentValue, Parameter parameter)
```

#### 3. OpenAIFunctionCallAdapter

**职责**: 将通用的函数信息转换为 OpenAI Function Calling 所需的 JSON Schema 格式

**转换流程**:
1. 提取方法名和描述
2. 分析参数类型，生成 JSON Schema
3. 处理复杂类型（List、自定义对象等）
4. 构建 OpenAI 兼容的函数定义

**示例输出**:
```json
{
  "name": "Create Tasks Bot Execution_execute",
  "description": "Create multiple tasks based on provided parameters",
  "parameters": {
    "type": "object",
    "properties": {
      "botInstanceId": {
        "type": "string",
        "description": "Bot Instance ID for task creation"
      },
      "taskCreationParam": {
        "type": "array",
        "description": "Array of task creation parameters",
        "items": {
          "type": "object",
          "properties": {
            "sequence": {"type": "integer"},
            "name": {"type": "string"},
            "description": {"type": "string"},
            "contextPath": {"type": "string"}
          }
        }
      }
    },
    "required": ["taskCreationParam"]
  }
}
```

#### 4. SAPOpenAIServiceImpl Function Calling 集成

**执行流程**:
```java
@Override
public <T extends BotExecution> Object functionCalling(
    List<BotMessages> messages, 
    List<PromptTexts> prompts,
    T botExecutionInstance,
    AIModel model) {
    
    // 1. 提取函数信息
    List<FunctionInfo> functionInfos = functionCallProcessor
        .extractFunctionInfosFromInstance(botExecutionInstance);
    
    // 2. 转换为 OpenAI 格式
    List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
        .convertToOpenAIFormat(functionInfos);
    
    // 3. 构建请求参数并调用 AI
    OpenAiChatCompletionOutput rawResult = aiClient.chatCompletion(params);
    
    // 4. 检查并执行函数调用
    if (containsFunctionCall(rawResult)) {
        return executeFunctionCall(rawResult, botExecutionInstance);
    }
    
    return responseContent;
}
```

### 类型转换系统

#### 支持的类型转换

1. **基本类型**: String, Integer, Boolean, Double 等
2. **集合类型**: List\<T\>，支持泛型类型推断
3. **复杂对象**: 使用 Jackson ObjectMapper 进行转换
4. **嵌套结构**: 支持多层嵌套的对象结构

#### 转换策略

```java
private Object convertArgumentToParameterType(Object argumentValue, Parameter parameter) {
    // 1. 处理 List 类型 - 提取泛型参数并逐个转换元素
    if (List.class.isAssignableFrom(parameterType)) {
        return convertToList(argumentValue, genericType);
    }
    
    // 2. 处理基本类型 - 直接类型转换
    if (parameterType == String.class) {
        return argumentValue.toString();
    }
    
    // 3. 处理复杂对象 - 使用 Jackson 转换
    return objectMapper.convertValue(argumentValue, parameterType);
}
```

### 编译时验证

通过注解处理器 `BotExecutionProcessor` 在编译时验证：

1. **类验证**: 实现 BotExecution 接口的类必须有 @BotExecutor 注解
2. **方法验证**: execute 方法必须有 @ExecuteMethod 注解
3. **参数验证**: 方法参数必须有 @ExecuteParameter 注解
4. **名称一致性**: @ExecuteParameter 的 name 属性必须与实际参数名一致

### 实现示例

#### CreateTasksBotExecution 示例

```java
@BotExecutor(name = "Create Tasks Bot Execution", description = "Implementation for creating tasks", version = "1.0", enabled = true)
public class CreateTasksBotExecution implements BotExecution {

    private final TaskService taskService;

    public CreateTasksBotExecution(TaskService taskService) {
        this.taskService = taskService;
    }

    @ExecuteMethod(
        operation = "Create Tasks Bot Execution_execute",
        description = "Create multiple tasks based on provided parameters",
        logExecution = true
    )
    public List<Task> execute(
            @ExecuteParameter(
                name = "botInstanceId",
                description = "Bot Instance ID for task creation", 
                required = false
            ) 
            String botInstanceId,
            
            @ExecuteParameter(
                name = "taskCreationParam",
                description = "Array of task creation parameters", 
                required = true
            ) 
            List<TaskCreationParam> taskCreationParam) {
        
        // 业务逻辑验证
        if (taskCreationParam == null || taskCreationParam.isEmpty()) {
            throw new BusinessException("Task creation parameters cannot be empty");
        }
        
        if (botInstanceId == null || botInstanceId.trim().isEmpty()) {
            throw new BusinessException("Bot instance ID cannot be null or empty");
        }
        
        // 执行任务创建
        List<Task> createdTasks = new ArrayList<>();
        for (TaskCreationParam param : taskCreationParam) {
            Task createdTask = taskService.createTaskWithBots(
                botInstanceId,
                param.getName(),
                param.getDescription(),
                param.getContextPath(),
                param.getSequence()
            );
            createdTasks.add(createdTask);
        }
        
        return createdTasks;
    }
}
```

### AI Service 集成更新

在 AIService 接口中添加 Function Calling 支持：

```java
public interface AIService {
    // 新增 Function Calling 方法
    public <T extends BotExecution> Object functionCalling(
        List<BotMessages> messages,
        List<PromptTexts> prompts,
        T botExecutionInstance,
        AIModel model);
        
    // 现有方法保持不变...
}
```

### Bot Service 集成更新

FunctionCallingBot 类更新：

```java
public class FunctionCallingBot implements Bot {
    
    @Override
    public BotInstancesExecuteContext.ReturnType execute() {
        // 1. 获取 BotType 配置
        BotType botType = getBotType();
        
        // 2. 根据 implementationClass 创建实例
        String implementationClass = botType.getImplementationClass();
        BotExecution botExecution = createBotExecutionInstance(implementationClass);
        
        // 3. 获取历史消息和提示词
        List<BotMessages> messages = getHistoryMessages();
        List<PromptTexts> prompts = getPrompts();
        
        // 4. 调用 AI Function Calling
        Object result = aiService.functionCalling(messages, prompts, botExecution, getAiModel());
        
        // 5. 处理结果并返回
        return processExecutionResult(result);
    }
    
    private BotExecution createBotExecutionInstance(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (BotExecution) applicationContext.getBean(clazz);
        } catch (Exception e) {
            throw new BusinessException("Failed to create bot execution instance: " + className, e);
        }
    }
}
```

### 配置更新

在 BotType 配置中需要添加：

1. **functionType**: 设置为 'FUNCTION_CALL'
2. **implementationClass**: 指定 BotExecution 实现类的完整类名，如 `customer.ai2code.service.impl.CreateTasksBotExecution`
3. **contextType**: 设置输出的上下文类型
4. **outputContextPath**: 设置结果写入的上下文路径

### 错误处理

Function Call 功能包含完整的错误处理机制：

1. **编译时错误**: 注解处理器验证配置正确性
2. **运行时错误**: 参数转换、方法调用异常处理
3. **业务逻辑错误**: BotExecution 实现类中的业务验证
4. **AI 调用错误**: OpenAI API 调用异常处理

所有错误都会被包装为 `BusinessException` 并提供详细的错误信息，便于调试和问题定位。



# 前端开发步骤
1. 利用fiori elements开发config维护界面，均为list-object
  1. Task Type
    1. Bot Type
      1. Prompt Text
  2. Model 
2. 利用fiori elements开发runtime界面, 均为list-object
  1. list为task(isMain=true)，包含创建按钮
  2. Task object
    1. Context Node list
      1. Context Node详细object页
    2. botInstance list
      1. botInstance 详细object页
        1. Task list  ---> Task object
        2. Message List （可以创建）
        3. Code excute按钮和结果
3. 重写runtime界面，只保留一个task list和一个task object
  1. 开发Context Node树
  2. 开发task-botInstance-task. 树
  3. 开发Context Node 编辑和显示
  4. 开发botIntance显示和执行
    1. 对话界面
    2. code和functionCall执行界面
Context树的前端组装
📌 前端 context 树的组装与交互设计（基于 CDS Entity ContextNode）

---
1️⃣ 后端数据模型回顾（CDS）
entity ContextNode : cuid, managed {
  task      : Association to Task;        // 所属任务
  path      : String(1000);              // 唯一路径，如 a.b.c[0].d
  label     : String(200);               // 节点名称
  type      : String(50);                // 类型（text, markdown, code, object, array等）
  value     : LargeString;               // 节点值/内容
  // 可扩展：排序、校验、元数据等
}

---
2️⃣ 前端数据拉取与结构组装
拉取 context 节点
- 调用标准 OData API（CDS 自动支持）：
GET /Tasks('<taskId>')/contextNodes
- 得到 flat 列表，每条含 path、label、type、value 等字段
组装 context 树算法
- 核心思想：用 path 字段递归分层，将所有节点嵌套成一棵树，便于前端树形组件（如 antd Tree、Fiori Tree、ElementTree）使用。
- 算法关键：
  - 对 path 用正则拆分为多级（兼容 a.b[0].c）。
  - 每一级用对象/Map 递归挂载。
  - 叶子节点为实际可编辑节点，携带 value/type/label。
代码示例（TypeScript/JavaScript）
function buildContextTree(flatList) {
  const root = [];
  const nodeMap = {};
  // 首先创建所有节点对象（便于引用与children组装）
  flatList.forEach(node => {
    node.children = [];
    nodeMap[node.path] = node;
  });
  // 遍历，将每个节点插入其父级children中
  flatList.forEach(node => {
    const parentPath = getParentPath(node.path);
    if (parentPath && nodeMap[parentPath]) {
      nodeMap[parentPath].children.push(node);
    } else {
      root.push(node); // 没有父级的为顶级节点
    }
  });
  return root; // 返回树形结构
}
// 获取父节点path工具函数，如 a.b[0].c -> a.b[0]
function getParentPath(path) {
  const lastDot = path.lastIndexOf(".");
  if (lastDot > 0) return path.substring(0, lastDot);
  return null;
}

---
3️⃣ 前端 UI 展示与交互
树形结构绑定
- 将上述树形数组直接传给任意支持 children 递归的树形组件。
- 例（antd Tree）：
treeData = [{ key, label, type, value, children: [...] }]
节点编辑与保存
- 用户点击节点，右/中区弹出编辑器（根据 type 渲染文本、markdown、代码、表格等不同控件）。
- 保存时调用 PATCH API：
PATCH /ContextNodes('<contextNodeId>') { value: 'new value' }
- 支持批量/只读标识等 UI 状态控制。
新增/删除 context 节点
- 新增时：
POST /ContextNodes，带 path、label、type、初始 value
- 删除时：
DELETE /ContextNodes('<id>')
- 前端需根据 path 自动生成唯一标识并管理树结构。

---
4️⃣ 支持高级场景
- 数组处理
  - path 如 fields[0].name，前端根据 path 的 [n] 拆分，渲染为树状数组。
- 批量更新/拖拽排序
  - 通过修改 path 或排序字段，实现树节点的批量更新。
- 类型高亮与校验
  - type 可用以区分不同节点样式与可编辑性。

---
5️⃣ 总结流程
1. 前端加载 contextNodes（flat list）→ 按 path 递归组装树形结构
2. 展示为树形 UI，支持展开、折叠、编辑、批量
3. 单节点编辑直接 PATCH，新增/删除用 POST/DELETE
4. 支持类型渲染、只读标识、校验等扩展
5. 适用于 Fiori/React/Vue/ElementUI/Antd 等所有现代前端技术栈

---
当然，下面是去掉 SubTask 后，前端 Task / BotInstance / Task ... 递归树结构的最新组装方法说明，适用于你的简化模型：

---
🟢 基于 Task / BotInstance 递归的树结构组装方法
1️⃣ 数据结构复习（CDS 简要）
- Task
  - 字段：ID, type, name, contextPath, isMain, ...
  - 关联：botInstances : Composition of many BotInstance on botInstances.task = $self
- BotInstance
  - 字段：ID, type, task, status, result, ...
  - 关联：tasks : Composition of many Task on tasks.botInstance = $self  // 下一级 Task 递归
树结构层级为：
 Task（根/主任务）
 └─ botInstance[]
  └─ Task[]
   └─ botInstance[]
    └─ Task[]
     └─ ...

---
---
4️⃣ 总结
- 树型结构为 Task（children: botInstances[]），每个 botInstance 再有 children: Task[]，无限递归。
- 前端只需按 Task→botInstance→Task→botInstance... 递归 fetch 组装。
- 不需要 SubTask，结构更简洁、性能优、易维护。

---
# Prompt/AI 脚本 context 引用变量
## Prompt中引用格式
以{{}}包围表达式的形式引用变量。
## ContextNode型引用变量
为方便灵活引用 context 数据，在 Prompt、代码模板、AI 推理中统一采用以下变量前缀：
暂时无法在飞书文档外展示此内容
- Context:path：绝对路径引用 Task context 中任意节点
- SubContext:path：相对路径引用当前任务 context 节点下的内容（平台自动补全为绝对 path）
平台在解析时自动将 SubContext:x 替换为 Task.contextPath + '.' + x，Context:x 保持绝对路径

## 引用实例变量
引用当前实例的属性，支持BotInstances和Tasks两张表的引用，仅允许BotInstances和Tasks两个实例表。用法：

Instance前缀后跟上属性名称，属性名称来源是CDS Entity属性，区分大小写。
- Instance:ID
- Instance:result
- Instance:type

解析方式：
1. 判断当前的实例是Bot还是Task。
2. 通过cds.gen目录自动生成的CDS Entity Java POJO类，解析到对应的getter方法
3. 通过cds model reflection类解析。

## 引用OData Read/Query作为变量
支持在Prompt中直接使用OData Read/Query查询的方式将想要的值引用进来。用法：

OData:/BotInstances/b631b9de-24ba-439c-afb3-f6a8002ddc9c/type/outputContextPath

## 混合引用
支持在引用表达式中嵌入引用表达式。用法:
嵌入表达式需要使用{{}}符号包括，用以区别。
示例：

{{OData:/BotInstances/{{Instance:ID}}/type/outputContextPath}}

---
5️⃣ 常见场景示例
- Prompt 输入变量
SubContext:inputText → 实际读取 datasource.children[2].inputText
- AI 结果写回
 outputContextPath = result
 实际写回：datasource.children[2].result
- 多输出情况
 outputContextPath = [ "result", "summary" ]
 写回：datasource.children[2].result、datasource.children[2].summary
- 全局引用
Context:projectOwner → 读取顶层 projectOwner 字段

---
6️⃣ 批量操作和数组
- 若需处理数组整体，接口传参/AI输出可用 [-1] 语法
 例：SubContext:fields[-1] 表示当前子任务所有 fields
- 后端批量读取实际用 SQL LIKE 'datasource.children[2].fields[%]%' 匹配所有子节点

---
7️⃣ 扩展与建议
- 平台支持 path 变量自动插值、批量展开，便于复杂自动化处理
- 统一所有前端、后端、AI 逻辑中的 context 引用规则，确保一致性和可维护性
- 建议在 Prompt 配置、API 文档、代码模板中始终采用 Context/SubContext 约定

---
8️⃣ 总结
- path 是 context 节点唯一定位符
- 子任务 context 路径（contextPath）与 botType 输出路径（outputContextPath）组合后生成实际写回/读取路径
- Prompt/AI 统一用 Context:x 和 SubContext:x 变量占位符，平台自动解析
- 支持复杂嵌套和批量操作，适应多种智能编排场景

# 示例
TASK 多层报表
        bot 获取报表初步需求 aichat      outputContextPath: report.requirement
        bot 获取层次        aifuncall    outputContextPath: report.level[-1].content
        bot 生成取层次内容子任务    TaskContextPath: report.level[-1].content
                Task：取一层的内容   ContextPath: report.level[0]
                        bot ai 取数据源
                        bot ai 取字段
                        bot ai 取关系关系
                        bot ai CDS
                Task：取一层的内容  ContextPath: report.level[1]
                        bot ai 取数据源   outputContextPath:     datasource
                        bot ai 取字段
                        bot ai 取关系关系
                        bot ai CDS           outputContextPath:     CDS
                Task：取一层的内容   ContextPath: report.level[2]
                        bot ai 取数据源
                        bot ai 取字段
                        bot ai 取关系关系
                        bot ai CDS
        bot Code 调用Odata生成CDS Project  read: report.level[-1].CDS, output：report.CDSCreateResult
        bot AI UTcase


report.level[0].content = "取采购订单头"
report.level[1].content = "取采购订单行"
report.level[2].content = "取采购订单交货历史"


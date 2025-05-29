程序规定
项目名称
ai2code


实现效果
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

📦 CDS设计

---
📁 /db/orchestration-model.cds  
```
using {
  cuid,
  managed
} from '@sap/cds/common';

using {
  ai.orchestration.config.TaskType as TaskType,
  ai.orchestration.config.BotType as BotType,
  ai.orchestration.config.BotInstanceStatus as BotInstanceStatus
} from './orchestration-config-model';

namespace ai.orchestration;

/* 任务实体，支持多级子任务，记录在context中。 */
entity Task : cuid, managed {
  botInstance  : Association to BotInstance;
  type         : Association to TaskType;
  name         : String(100);
  description  : String;
  contextPath  : String(1000);    //Task需要指定的Context路径 例：datasource.children[3]
  sequence     : Integer;        // SubTask需要指定的执行顺序
  isMain       : Boolean default true; //冗余存储
  botInstances : Composition of many BotInstance on botInstances.task = $self;
  contextNodes : Composition of many ContextNode on contextNodes.task = $self; //任务下所有context节点
}

/* 单个context节点，采用扁平结构，支持树结构自动还原 */
entity ContextNode : cuid, managed {
  task      : Association to Task;        // 所属任务
  path      : String(1000);              // 唯一路径，如 a.b.c[0].d
  label     : String(200);               // 节点名称
  type      : String(50);                // 类型（text, markdown, code, object, array等）
  value     : LargeString;               // 节点值/内容
  //readonly  : Boolean default false;     // 是否只读（可选）
}

/* 单次Bot执行实例。 */
entity BotInstance : cuid, managed {
  sequence  : Integer;
  result    : LargeString;
  type      : Association to BotType;
  status    : Association to BotInstanceStatus default 'C';
  task      : Association to Task;    //上级
  tasks     : Composition of many Task on tasks.botInstance = $self; //下级
  messages  : Composition of many BotMessage on messages.botInstance = $self; 
}

/* Bot消息实体，记录人与AI/系统的对话消息。 */
entity BotMessage : cuid, managed {
  role        : String(20);       // 'user' | 'assistant' | 'system'
  message     : LargeString;
  ragData     : LargeString; 
  botInstance : Association to BotInstance;
}
```

📁  /db/orchestration-config-model.cds
```
using {
  cuid,
  managed,
  sap.common.CodeList
} from '@sap/cds/common';

namespace ai.orchestration.config;

/* 任务类型，如字段设计、API映射等。 */
entity TaskType : cuid, managed {
  name        : String(100);
  description : String;
  autoRun     : Boolean default false;
  isMain      : Boolean default true;
  botTypes    : Composition of many BotType
                  on botTypes.taskType = $self;
}

/* 任务类型下的Bot执行顺序定义。 */
//entity TaskBotSequence : cuid, managed {
//  taskType : Association to TaskType;
//sequence : Integer;
//botType  : Association to BotType;
//remarks  : String;
// 可扩展唯一性断言: @assert.unique: ['taskType', 'sequence']
//}

/* BotType: bot类型，增加contextType字段（枚举引用） */
entity BotType : cuid, managed {
  taskType            : Association to TaskType;
  sequence            : Integer;
  name                : String(50);
  description         : String;
  functionType        : Association to BotFunctionType default 'A'; //A F C S
  autoRun             : Boolean default false;
  executionCondition  : String(1000);
  model               : Association to ModelConfig;
  prompts             : Composition of many PromptText
                          on prompts.botType = $self;
  functionCalls       : Composition of many FunctionCall
                          on functionCalls.botType = $self;
  outputContextPath   : String(1000); // 输出内容写回路径, 可以是数组[-1], 在subTask中是相对路径；在主task中是绝对路径
  contextType         : Association to ContextType; // 新增：输出内容的数据类型（枚举）
  isRAGEnabled        : Boolean default false;
  //ragFunction         : Association to RagFunction;
  ragClass            : String(100); //代替ragFunction
  ragSource           : String(100);
  ragTopK             : Integer;
  implementationClass : String(100); //C和F适用
//subTaskContextPath  : String(1000);   // 约定必须包含数组，例如datasource.children[-1].content，数组实例会写入subTask的contextPath, 例如datasource.children[3]
//subTaskType         : Association to TaskType;
}

/* AI模型配置 */
entity ModelConfig : cuid, managed {
  name       : String(100);
  provider   : String(50);
  modelName  : String(100);
  parameters : LargeString;
}

/* Bot提示词模板，支持多语言多模板。 */
entity PromptText : cuid, managed {
  botType : Association to BotType;
  lang    : String(5);
  name    : String(100);
  content : LargeString;
}

entity FunctionCall : cuid, managed {
  botType : Association to BotType;
  name    : String(100);
  description : String(500);
  parameters : LargeString; // FunctionCall参数定义 parameters: { "type": "object", "properties": { "param1": { "type": "string" }, "param2": { "type": "integer" } } } / inputSchema: { "type": "object", "properties": { "param1": { "type": "string" }, "param2": { "type": "integer" } } }
}

/* Bot执行状态枚举 */
entity BotInstanceStatus : CodeList {
  key code : String enum {
        C = 'CREATED';
        R = 'RUNNING';
        S = 'SUCCESS';
        F = 'FAILED';
        K = 'SKIPPED';
        X = 'CANCELLED';
      };
}

/* Bot功能类型枚举 */
entity BotFunctionType : CodeList {
  key code : String enum {
        A = 'AI';
        F = 'FUNCTION_CALL';
        C = 'CODE';
      //S = 'SUBTASK_GENERATOR';  用Function_call代替
      };
}

/* ContextType: 上下文节点内容类型（强类型约束） */
entity ContextType : CodeList {
  key code : String enum {
        string = 'STRING'; // 普通文本
        markdown = 'MARKDOWN'; // Markdown文档
        code = 'CODE'; // 代码片段
        json = 'JSON'; // JSON结构
      //object   = 'OBJECT';    // 对象
      //array    = 'ARRAY';     // 数组
      //table    = 'TABLE';     // 表格
      //image    = 'IMAGE';     // 图片(base64或URL)
      };
}

/* RAG功能类型枚举 */
entity RagFunction : CodeList {
  key code : String enum {
        V = 'Vector';
        T = 'Table';
        X = '';
      };
}
```

srv/orchestration-service.cds

```
using ai.orchestration as db from '../db/orchestration-model';

service MainService {
    entity Tasks        as projection on db.Task;
    entity ContextNodes as projection on db.ContextNode;

    //entity SubTasks      as projection on db.SubTask;
    entity BotInstances as projection on db.BotInstance
        actions {
            action execute() returns {
                result : String;
                tasks  : array of UUID;
            };
            action executeAsync() return Boolean;
            
            action chatCompletion(content: LargeString) returns LargeString;
        }

    entity BotMessages  as projection on db.BotMessage
        actions {
            action adopt() returns array of ContextNodes;
        }

    // Unbound actions
    action createTaskWithBots(name : String,
                              description : String,
                              typeId : UUID) returns Tasks;

}
```

srv/orchestration-config-service.cds
```
using ai.orchestration.config as cfg from '../db/orchestration-config-model';

service ConfigService {
  entity TaskTypes            as projection on cfg.TaskType;
  //entity TaskBotSequences     as projection on cfg.TaskBotSequence;
  entity BotTypes             as projection on cfg.BotType;
  entity ModelConfigs         as projection on cfg.ModelConfig;
  entity PromptTexts          as projection on cfg.PromptText;
  entity FunctionCalls        as projection on cfg.FunctionCall;
  entity BotInstanceStatuses  as projection on cfg.BotInstanceStatus;
  entity BotFunctionTypes     as projection on cfg.BotFunctionType;
  entity RagFunctions         as projection on cfg.RagFunction;
  entity ContextTypes         as projection on cfg.ContextType;
}
```

可以直接用 CDS 标准服务的典型用法
暂时无法在飞书文档外展示此内容

实体标准接口举例（前端可直接使用，无需 action）
- 查询 context Node：GET /Tasks('...')/contextNodes
- 保存 context 编辑：PATCH /ContextNodes('...')
- 查询 botInstance 树：GET /Tasks('...')/botInstances
- 查询消息对话：GET /BotInstances('...')/messages
需自定义的 action
必须自定义 action 的场景（标准服务不覆盖的业务流）


暂时无法在飞书文档外展示此内容

---
后端逻辑
手动执行
action
参数
功能
createTaskWithBots
1. 传入：
  1. name : String,
  2. description : String,
  3. typeId : UUID
2. 传出:
  1. tasks: Tasks

1. 根据typeId查询ConfigService中TaskTypes表中条目以及他的botTypes
2. 创建一条目MainService.Tasks。
  1. 将isMain设置为true
  2. Name
  3. Description
  4. contextPath设置为空
  5. sequence设置为空或0
  6. Type 设置为typeId查找到的TaskType
3. 根据ConfigService.TaskTypes.botTypes的条目数，创建相应条目数的MainService.BotInstances。
  1. Sequence 设置为 ConfigService.BotTypes.sequence
  2. Type 设置为ConfigService.BotTypes
  3. status设置为BotInstanceStatus.code.C (Created)
4. (自动执行)将name、description和第二步得到的Tasks.Id，创建两条目MainService.ContextNodes。
  1. 第一条
    1. path设置为name
  2. 第二条
    1. path设置为description
5. 将创建的Tasks条目返回给前端
BotInstances/execute
1. 传入
  1. Bound action自带参数
2. 传出
  1. result: String(单纯code运行返回的结果)
  2. tasks(如果是functioncall类型的Bot，执行分解任务的操作，返回的是taskId的数组)
正常流程(同步)：
1. 将BotInstances的status字段设置为R(Running)。
- F类型BotInstance
  - 取到维护的BotType.prompts
  - 调用AI Function call，调用维护的implementationClass维护的类中execute方法。返回结果存储到BotInstances.result字段中。同时通过维护的outputContextPath写到ContextNodes条目中。
  - 将BotInstances.Status设置为S(Success)。
- C类型BotInstance
  - 执行implementationClass维护的Class中execute方法
  - 结果存储在BotInstances.result字段中。
  - 将BotInstances.Status设置为S(Success)。
错误处理：
1. 将BotInstances的status字段设置为F(Failed)。
BotInstances/chatCompletion
1. 传入
  1. Bound action自带参数
  2. content: LargeString
2. 传出
  1. LargeString
正常流程:
1. 将BotInstances的status字段设置为R(Running)。
2. 判断是否第一次对话，第一次对话content字段 作为本次用户对话内容，取BotTypes.prompts作为system消息 组成消息发给AI模型。
3. 不是第一次对话，content字段 作为本次用户对话内容，再根据BotInstances.messages取到历史记录消息 组成消息发给AI模型。
4. 第一次对话，将system消息、user消息和assistant消息存入BotMessages表中。非第一次对话，将user消息和assistant消息存入BotMessages表中。
5. 返回assistant消息。
错误处理：
1. 将BotInstances的status字段设置为F(Failed)。

BotMessages/adopt
1. 传入
  1. Bound action自带参数
2. 传出
  1. array of ContextNodes
1. 获取当前BotMessages条目。
2. 根据BotMessages.botInstance获取到BotInstances条目。
3. 根据BotInstances.type获取BotTypes条目。
4. 将这条消息内容存储到ContextNodes条目中。
  1. Path:  根据BotTypes设置的outputContextPath
  2. label:
  3. type: 根据BotTypes设置的contextType
  4. Value: BotMessages.message/根据AI function call转成相应的格式。
5. 将BotInstances的status字段设置为S(Success)。
6. 返回ContextNodes条目。
错误处理：
1. 将BotInstances的status字段设置为F(Failed)。

/api/chat/Streaming
Restful协议SSE流式接口。
传参
1. 传入
  1. botInstanceId
  2. content
2. 传出
  1. SseEmitter对象

1. 判断是否第一次对话，第一次对话content字段 作为本次用户对话内容，取BotTypes.prompts作为system消息 组成消息发给AI模型。
2. 不是第一次对话，content字段 作为本次用户对话内容，再根据BotInstances.messages取到历史记录消息 组成消息发给AI模型。
3. 启动异步线程接收AI返回的流式消息内容：
  1. 每次AI返回消息段，立刻通过SseEmitter.send()返回消息给前端。
  2. 在SseEmitter结束的时候启动存储消息的动作：第一次对话，将system消息、user消息和assistant消息存入BotMessages表中。非第一次对话，将user消息和assistant消息存入BotMessages表中。
错误处理：
1. 将BotInstances的status字段设置为F(Failed)。

自动执行
考虑引入Spring-AI或Langchain4j。


类定义
Model
AI Config Models:
名称
类型
描述
属性
方法
AIModel

Interface

modelConfig: ModelConfigs类
Parameters : Map对象

public String getModelName()
public  Map<String,E extends Object> parseParameters() //string转到Map对象
OpenAIGPT35Model
Class
OpenAI 3.5模型
实现AIModel
实现AIModel
OpenAIGPT4OModel
Class
OpenAI 4O模型
实现AIModel
实现AIModel
ClaudeAI35SonnetModel
Class
Claude 3.5 sonnet
实现AIModel
实现AIModel
ClaudeAI37SonnetModel
Class
Claude 3.7 sonnet
实现AIModel
实现AIModel
StreamRequestVO
Class
Streaming传入的时候的payload类
botInstanceId: String
Content: String
无
Bot & Task Model
名称
类型
描述
属性
方法
Bot

Interface
Bot接口
ExecutorService executor;

public BotInstancesExecuteContext.ReturnType execute();
public Boolean executeAsync();
public Boolean stop();
public Boolean resume();
Public Boolean cancel();

ChatBot
Class
对话型Bot

public String chat(String content);
public SseEmitter chatInStreaming(String content);
FunctionCallingBot
Class
FunctionCalling型Bot


CodingBot
Class
代码执行类型Bot


Task
Interface
Task接口(暂时不用实现)












Service
AI Service
名称
类型
描述
属性
方法
AIService

Interface

AI服务的接口


public String chatWithAI(
List<BotMessages> messages,
List<PromptTexts> prompts,
String content
)
public SseEmitter chatWithAIStreaming(
List<BotMessages> messages,
List<PromptTexts> prompts,
String content
)
public <E extends BotExecution> String functionCalling(
List<BotMessages> messages,
List<PromptTexts> prompts,
FunctionCallingBot bot 
) 

SAPOpenAIServiceImpl
Class
SAP AICore OpenAI服务类


SAPClaudeAIServiceImpl
Class
SAP AICore ClaudeAI服务类


通用Service
名称
类型
描述
属性
方法
BotService

Interface

Bot相关服务


    public Bot getCurrentBot(String botInstanceId);

    public Bot getCurrentBot(String taskId, Integer sequence);

    public Boolean executeAsync(BotInstancesExecuteContext context);
    public Boolean executeAsync(String botInstanceId);

    public BotInstancesExecuteContext.ReturnType execute(BotInstancesExecuteContext context);
    public BotInstancesExecuteContext.ReturnType execute(String botInstanceId);
TaskService
Interface

Task相关服务

    public Task createTaskWithBots(String name, String description, String taskTypeId);

    public Task createTaskWithBots(String botInstanceId);

    public Task createTaskWithBots(String botInstanceId, String name);
ContextService
Interface

Context相关服务
 

    public List<Map<String, Object>> buildContextAsHierarchy(List<ContextNodes> contextNodes);

    public String getContextFullPath(String subPathPrefix, String subPath);

    public Result upsertContext(
            String botInstanceId,
            String taskId,
            // Integer sequence,
            // String contextPath,
            String contextValue);
PromptService
Interface
提示词相关服务

public String parse(PromptTexts prompt,ContextNodes contextNode,String contextPath);
EntityService
Class
CqnService相关服务

selectSingle(CqnService)
selectList(CqnService)
insert()
batchInsert()
update()
delete()






名称
类型
描述
属性
方法

BotExecution

Interface
Bot对象执行接口

public String execute(Map<String, Object> parameters);

CreateSubTaskExecutionImpl
Class




DeployCDSExectionImpl
Class




RAGExtractor
Interface
RAG提取对象
 

public String extract(String RAGSource, int RAGTopK)



前端开发步骤
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
2️⃣ 前端组装递归树算法
A. 后端 API 数据拉取方式
- 拉取根 Task（或所有主 Task）：
GET /Tasks?$filter=isMain eq true
- 拉取某 Task 下所有 botInstances：
GET /Tasks('<taskId>')/botInstances
- 拉取某 BotInstance 下所有子 Task：
GET /BotInstances('<botInstanceId>')/tasks
B. 递归组装树的伪代码示例（TypeScript/JS）
// 递归函数：从Task开始组装
async function buildTaskTree(taskId) {
  // 1. 拉取当前 Task
  const task = await api.get(
/Tasks(${taskId})
);
  // 2. 拉取当前 Task 下的所有 botInstances
  const botInstances = await api.get(
/Tasks(${taskId})/botInstances
);
  // 3. 对每个 botInstance 递归拉取其 tasks
  task.children = await Promise.all(botInstances.map(async (bot) => {
    // 对每个 botInstance，递归其下所有 task
    const childTasks = await api.get(
/BotInstances(${bot.ID})/tasks
);
    bot.children = await Promise.all(childTasks.map(childTask => buildTaskTree(childTask.ID)));
    return bot; // bot节点，children为其下Task[]
  }));
  return task;
}
- 最终树结构：
  - 根为 Task，每个 Task 下有 children = [botInstance...]
  - 每个 botInstance 下有 children = [Task...]
  - 无限递归嵌套，适配任意层级智能编排树

---
C. 适配树形 UI 组件的数据结构（示例）
{
  id: task.ID,
  label: task.name,
  type: 'task',
  children: [
    {
      id: bot.ID,
      label: bot.type, // 可用 type 名称或 description
      type: 'botInstance',
      children: [
        // 子 Task 节点
      ]
    }
  ]
}
- 树节点可递归 children 字段适配 antd-tree, element-tree, fiori-tree, vue-tree 等所有主流树控件。

---
3️⃣ 性能建议与扩展
- 建议懒加载（点击节点时再拉下一级），避免初始递归全部加载造成性能压力。
- 可缓存已加载节点，减少重复请求。
- 支持拖拽/批量/节点状态高亮，可用 children 数组灵活拓展。

---
4️⃣ 总结
- 树型结构为 Task（children: botInstances[]），每个 botInstance 再有 children: Task[]，无限递归。
- 前端只需按 Task→botInstance→Task→botInstance... 递归 fetch 组装。
- 不需要 SubTask，结构更简洁、性能优、易维护。

---
4️⃣ Prompt/AI 脚本 context 引用变量
为方便灵活引用 context 数据，在 Prompt、代码模板、AI 推理中统一采用以下变量前缀：
暂时无法在飞书文档外展示此内容
- Context:path：绝对路径引用 Task context 中任意节点
- SubContext:path：相对路径引用当前任务 context 节点下的内容（平台自动补全为绝对 path）
平台在解析时自动将 SubContext:x 替换为 Task.contextPath + '.' + x，Context:x 保持绝对路径

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
构思
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


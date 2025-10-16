# AI2Code 项目介绍

## 📋 项目概述

AI2Code 是一个基于 SAP BTP (Business Technology Platform) 的智能任务编排与代码生成平台。该项目通过 AI 驱动的多 Bot 协作机制，实现了任务的自动化分解、执行和代码生成，特别适用于企业级应用开发场景。

**核心价值**：
- 🤖 AI 驱动的任务自动化编排
- 🌲 支持多层级任务树形结构
- 💬 智能对话式交互
- 🔧 Function Calling 自动代码执行
- 📊 RAG 增强的知识检索
- 🔄 Spring Batch 批处理支持

---

## 🖥️ 用户界面设计

### 三栏式任务编排界面

AI2Code 采用创新的三栏式界面设计，为用户提供直观高效的任务管理和 AI 交互体验：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Task Object Page（任务对象页）                         │
├─────────────────────────────────────────────────────────────────────────────┤
│    左栏（树形导航）         │   中栏（内容编辑）         │  右栏（AI 交互）           │
│────────────────────────────┼──────────────────────────┼──────────────────────────│
│• Context 树（Tab1）         │ • 当前选中节点内容        │ ┌───── AI Chat ─────────┐ │
│  - 总体需求（markdown）     │   - Markdown 编辑器      │ │ user: 创建用户管理模块  │ │
│  - 用户需求（markdown）     │   - 代码编辑器           │ │ AI  : 我会帮你分解...   │ │
│  - 数据结构（json）         │   - 富文本编辑           │ │                        │ │
│    └─ 字段列表[0]          │   - 只读/可编辑切换      │ │ [采用] 按钮             │ │
│       └─ 字段列表[1]       │                         │ └────────────────────────┘ │
│                           │                         │                            │
│────────────────────────────┼                         │────────────────────────────│
│• Task 树（Tab2）           │                         │ ┌──── Code Result ──────┐ │
│  - 主任务                  │                         │ │ • 执行按钮             │ │
│    ├─ Bot1 (AI_CHAT) ✓     │                         │ │ • 运行日志             │ │
│    │   └─ 子任务1          │                         │ │ • 执行结果             │ │
│    │       ├─ Bot1.1 🔄    │                         │ │ • 状态监控             │ │
│    │       └─ Bot1.2       │                         │ └────────────────────────┘ │
│    ├─ Bot2 (FUNCTION) ⏸   │                         │                            │
│    └─ Bot3 (CODE)         │                         │                            │
│         └─ 子任务3         │                         │                            │
└────────────────────────────┴──────────────────────────┴────────────────────────────┘

图例：✓=成功  🔄=运行中  ⏸=暂停  ❌=失败
```

### 界面功能详解

#### 左栏 - 树形导航区

**Context 树（Tab1）**：
- 📁 **层次化展示**：以树形结构展示任务的所有上下文节点
- 🔍 **路径导航**：支持 `a.b[0].c` 格式的复杂路径
- 🏷️ **类型标识**：图标区分 STRING、MARKDOWN、CODE、JSON 等类型
- ➕ **动态管理**：支持节点的增删改查操作

**Task 树（Tab2）**：
- 📊 **执行视图**：展示 Task 和 BotInstance 的递归树形结构
- 🎯 **状态可视化**：实时显示每个 Bot 的执行状态
  - ✅ SUCCESS（成功）
  - 🔄 RUNNING（运行中）
  - ⏸️ CREATED（已创建）
  - ❌ FAILED（失败）
  - ⏭️ SKIPPED（跳过）
- 📝 **序号排序**：按 sequence 字段排序展示执行顺序
- 🔗 **关联导航**：点击节点可在中栏查看关联的 Context

#### 中栏 - 内容编辑区

**动态编辑器**：根据选中节点的 `type` 字段渲染不同编辑器

1. **MARKDOWN 类型**：
   - 富文本/Markdown 编辑器
   - 支持实时预览
   - 适合需求文档、说明文本

2. **CODE 类型**：
   - 代码编辑器（Monaco Editor）
   - 语法高亮
   - 适合代码片段、配置文件

3. **JSON 类型**：
   - JSON 编辑器
   - 格式验证
   - 适合结构化数据

4. **STRING 类型**：
   - 文本域（TextArea）
   - 适合简单文本

**编辑功能**：
- 💾 **自动保存**：编辑后通过 PATCH API 更新 ContextNode
- 🔒 **只读模式**：支持只读/可编辑状态切换
- 📋 **版本历史**：通过 `modifiedAt` 字段追踪修改历史

#### 右栏 - AI 交互区

**上半部 - AI Chat**：
- 💬 **智能对话**：与 AI Bot 进行多轮对话
- 📜 **历史记录**：展示完整的对话历史（BotMessages）
- 🎨 **富文本显示**：支持 Markdown 和代码块渲染
- ✅ **采用按钮**：点击后调用 `adopt` action，将 AI 回复写入 ContextNode
- 🔄 **流式响应**：支持 SSE 流式显示 AI 回复（chatStreaming）

**下半部 - Code Result**：
- ▶️ **执行控制**：执行 Function Call Bot 或 Code Bot
- 📊 **运行日志**：实时显示执行日志和错误信息
- ✅ **执行结果**：展示 Bot 执行的返回结果
- 📈 **状态监控**：显示当前 Bot 的执行状态

### 交互流程示例

#### 场景：创建用户管理模块

1. **用户新建任务**（任务创建）
   - 创建新任务，输入任务描述："创建用户管理模块，包含增删改查功能"
   - 系统自动在 Context 树中生成 `requirement` 节点（MARKDOWN 类型）
   - 任务描述自动保存到 requirement 节点中

2. **AI 对话分析**（右栏 AI Chat）
   - 点击 Bot1（AI_CHAT 类型）
   - 在右栏输入："请帮我分解这个需求"
   - AI 返回分解后的子任务列表

3. **采用 AI 建议**（右栏 → 中栏）
   - 点击 AI 回复下方的"采用"按钮
   - 系统调用 `adopt` action
   - AI 回复自动写入 Context 节点 `subtasks`（JSON 类型）

4. **自动创建子任务**（左栏 Task 树）
   - Bot2（FUNCTION_CALL 类型）自动执行
   - 调用 `CreateTasksBotExecution`
   - 在 Task 树中自动生成多个子任务节点

5. **代码生成执行**（右栏 Code Result）
   - 点击子任务下的 Bot（CODE 类型）
   - 点击右栏"执行"按钮
   - 实时显示代码生成进度和结果

### 技术实现

#### Context 树组装算法

**存储结构**：

| 字段 | 示例值 | 说明 |
|------|--------|------|
| ID | UUID | 唯一标识 |
| path | `subtask[0].cdsView.fields[2].name` | 节点路径，支持数组索引 |
| label | `Field Name` | 显示名称 |
| type | `STRING` | 数据类型（STRING/MARKDOWN/CODE/JSON） |
| value | `userName` | 节点内容 |

**树形重构流程**：

```
扁平化节点列表
    ↓
1. 解析路径层级
   例如：subtask[0].cdsView.fields[2].name
   拆分为：["subtask", "subtask[0]", "subtask[0].cdsView", ...]
    ↓
2. 创建虚拟中间节点
   为每个层级路径创建节点（如果不存在）
    ↓
3. 建立父子关系
   根据路径确定父节点并添加到 children 数组
    ↓
4. 返回根节点列表
   过滤出没有父节点的根节点
```

#### Task 树递归查询

**节点数据结构**：

| 字段 | Task 节点 | Bot 节点 |
|------|-----------|----------|
| type | `task` | `bot` |
| id | 任务 ID | Bot 实例 ID |
| name | 任务名称 | Bot 类型名称 |
| status | - | CREATED/RUNNING/SUCCESS/FAILED/SKIPPED |
| sequence | 执行序号 | 执行序号 |
| children | BotInstance 列表 | 子 Task 列表 |

**递归构建流程**：

```
获取主任务
    ↓
创建任务节点（包含 ID、name、sequence）
    ↓
遍历任务的 BotInstances
    ├─ 创建 Bot 节点（包含状态、类型信息）
    │   ↓
    │  遍历 Bot 的子任务
    │   └─ 递归调用构建流程 ←┐
    │                        │
    └─ 添加到任务节点的 children 列表
        ↓
返回 JSON 格式的树形结构
```

**前端渲染策略**：

| 渲染元素 | 说明 |
|----------|------|
| **图标** | 根据节点类型（task/bot）和状态显示不同图标 |
| **标签** | 显示节点名称和序号 |
| **状态徽章** | Bot 节点显示当前执行状态 |
| **递归渲染** | 对 children 数组递归调用渲染函数 |
| **交互事件** | 点击节点触发中栏内容更新 |

### 界面优势

1. **📊 信息密度高**：三栏设计在一屏内展示完整工作流程
2. **🔄 实时交互**：左右栏联动，操作结果即时反馈
3. **🎯 上下文清晰**：始终可见当前 Task、Bot 和 Context 关系
4. **✨ 灵活编辑**：根据数据类型智能选择编辑器
5. **🤖 AI 集成**：无缝集成 AI 对话和结果采用
6. **📈 状态可视**：实时显示任务执行状态和进度

---

## 🏗️ 系统架构

### 1. 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           AI2Code 系统架构                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐                │
│  │  Frontend   │      │   Gateway   │      │   Backend   │                │
│  │  (Fiori)    │◄────►│   (OData)   │◄────►│   (Java)    │                │
│  └─────────────┘      └─────────────┘      └─────────────┘                │
│        │                                           │                        │
│        │                                           ▼                        │
│        │                              ┌────────────────────────┐            │
│        │                              │   Service Layer        │            │
│        │                              ├────────────────────────┤            │
│        │                              │ • TaskService          │            │
│        │                              │ • BotService           │            │
│        │                              │ • ContextService       │            │
│        │                              │ • AIService            │            │
│        │                              │ • PromptService        │            │
│        │                              └────────────────────────┘            │
│        │                                           │                        │
│        │                                           ▼                        │
│        │                              ┌────────────────────────┐            │
│        │                              │   Core Components      │            │
│        │                              ├────────────────────────┤            │
│        │                              │ • Bot Execution Engine │            │
│        │                              │ • Function Call System │            │
│        │                              │ • RAG Extraction       │            │
│        │                              │ • Variable Resolver    │            │
│        │                              │ • SpEL Evaluator       │            │
│        │                              └────────────────────────┘            │
│        │                                           │                        │
│        └───────────────────────────────────────────┼────────────────────┐  │
│                                                    │                    │  │
│        ┌───────────────────────────────────────────┼────────────────┐   │  │
│        ▼                                           ▼                ▼   ▼  │
│  ┌──────────┐                           ┌──────────────┐    ┌──────────┐  │
│  │ SAP HANA │                           │  SAP AI Core │    │ External │  │
│  │   DB     │                           │  (OpenAI/    │    │  SAP     │  │
│  │          │                           │   Claude)    │    │  Systems │  │
│  └──────────┘                           └──────────────┘    └──────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2. 技术栈

#### 后端技术栈
- **核心框架**：
  - Spring Boot 3.4.5
  - SAP Cloud Application Programming Model (CAP) 3.10.1
  - Java 21
  
- **AI 集成**：
  - SAP AI SDK 1.6.0
  - OpenAI Integration
  - Claude AI Integration
  - Orchestration Service
  
- **批处理**：
  - Spring Batch
  - 任务编排与自动执行
  
- **数据库**：
  - SAP HANA Cloud
  - H2 (开发环境)
  
- **其他组件**：
  - Lombok
  - Apache POI (Excel 处理)
  - WebSocket (实时通信)

#### 前端技术栈
- **框架**：SAP Fiori Elements + UI5 freestyle
- **数据协议**：OData V4
- **树形组件**：自定义递归树组件
- **编辑器**：
  - Monaco Editor（代码编辑）
  - Markdown Editor（富文本编辑）
  - JSON Editor（结构化数据）


### 3. 数据模型架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         核心实体关系图                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐                                                      │
│  │   TaskType   │                                                      │
│  │  (配置表)     │                                                      │
│  └──────┬───────┘                                                      │
│         │ 1:N                                                          │
│         ▼                                                              │
│  ┌──────────────┐        ┌──────────────┐                             │
│  │   BotType    │───────►│ ModelConfig  │                             │
│  │  (配置表)     │  N:1   │              │                             │
│  └──────┬───────┘        └──────────────┘                             │
│         │ 1:N                                                          │
│         ▼                                                              │
│  ┌──────────────┐                                                      │
│  │ PromptText   │                                                      │
│  │  (配置表)     │                                                      │
│  └──────────────┘                                                      │
│                                                                         │
│  ┌──────────────┐                                                      │
│  │     Task     │◄──────┐                                              │
│  │  (运行时)     │       │                                              │
│  └──────┬───────┘       │ 递归                                          │
│         │ 1:N           │ 关系                                          │
│         ▼               │                                              │
│  ┌──────────────┐       │                                              │
│  │ BotInstance  │       │                                              │
│  │  (运行时)     │       │                                              │
│  └──────┬───────┘       │                                              │
│         │ 1:N           │                                              │
│         ├───────────────┘                                              │
│         │                                                              │
│         ├─────────────┐                                                │
│         │ 1:N         │ 1:N                                            │
│         ▼             ▼                                                │
│  ┌──────────┐   ┌─────────────┐                                        │
│  │BotMessage│   │ContextNode  │                                        │
│  │(运行时)   │   │  (运行时)    │                                        │
│  └──────────┘   └─────────────┘                                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**核心实体说明**：

1. **Task（任务）**：
   - 支持多层级树形结构（主任务 → BotInstance → 子任务 → ...）
   - 每个任务关联多个 BotInstance
   - 通过 contextPath 管理上下文路径

2. **BotInstance（Bot 实例）**：
   - 任务执行的基本单元
   - 三种类型：AI_CHAT、FUNCTION_CALL、CODE
   - 支持状态管理（CREATED、RUNNING、SUCCESS、FAILED、SKIPPED、CANCELLED）

3. **ContextNode（上下文节点）**：
   - 扁平化存储，通过 path 字段支持树形重构
   - 支持多种类型：STRING、MARKDOWN、CODE、JSON
   - 用于存储任务的输入输出数据

4. **BotMessage（对话消息）**：
   - 记录用户与 AI 的对话历史
   - 支持 user、assistant、system 三种角色
   - 支持 RAG 增强数据

---

## 🚀 核心功能

### 1. 任务编排系统

#### 三栏式用户界面
```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Task Object Page（任务对象页）                         │
├─────────────────────────────────────────────────────────────────────────┤
│  左栏（树形导航）    │   中栏（内容编辑）      │  右栏（AI 交互）         │
├──────────────────┼───────────────────┼──────────────────────────────┤
│• Context 树      │ • 当前节点内容      │ ┌───── AI Chat ──────────┐ │
│  - 总体需求      │   - Markdown编辑   │ │ user: 创建用户模块...   │ │
│  - 用户需求      │   - 代码编辑器     │ │ AI  : 我会帮你分解...   │ │
│  - 数据结构      │   - 富文本编辑     │ │                        │ │
│                  │                   │ └────────────────────────┘ │
│──────────────────│                   │──────────────────────────────│
│• Task 树         │                   │ ┌──── Code Result ───────┐ │
│  - 主任务        │                   │ │ • 执行按钮              │ │
│    ├─ Bot1       │                   │ │ • 运行日志              │ │
│    │   └─ 子任务1 │                   │ │ • 执行结果              │ │
│    ├─ Bot2       │                   │ └────────────────────────┘ │
│    └─ Bot3       │                   │                            │
└──────────────────┴───────────────────┴──────────────────────────────┘
```

#### 功能特性
- ✅ 创建主任务时自动生成配置的 BotInstances
- ✅ 支持任务的多层级递归结构
- ✅ Context 节点树形展示与编辑
- ✅ 实时 AI 对话交互
- ✅ 代码执行结果查看

### 2. 智能 Bot 与 Function Calling 体系

#### 总体能力
- ✅ 多 Bot 协同：AI_CHAT、FUNCTION_CALL、CODE 三种 Bot 协作完成需求采集、函数执行和代码生成
- ✅ Function Calling 自动化：注解驱动的函数调用体系将 AI 指令映射为可执行的 Java 方法
- ✅ 生命周期治理：TaskBotCacheManager 统一维护 Task/Bot 树形缓存，保障状态一致性与性能

#### Bot 类型概览

| 类型 | 主要作用 | 核心能力 |
|------|----------|----------|
| **AI Chat Bot** | 多轮对话收集需求与上下文 | 流式响应、对话历史管理、RAG 增强 |
| **Function Call Bot** | AI 触发 Java 方法执行 | 注解驱动元数据、自动参数映射、复杂对象支持 |
| **Code Bot** | 执行预定义代码逻辑 | 动态类加载、结果回写、同步/异步执行 |

#### Function Calling 自动化能力

| 功能分类 | 功能名称 | 实现类 | 主要价值 |
|----------|----------|--------|----------|
| 任务管理 | Create Tasks | CreateTasksBotExecution | AI 自动分解任务并生成子任务 |
| 上下文管理 | Split CDS and Update Context | SplitCDSandUpdateContextExecution | 拆分 AI 生成的 CDS 并更新上下文树 |
| 知识提取 | Extract View Details | ExtractViewDetailExecution | 提取 CDS 字段与连接条件支撑 RAG |
| 代码生成 | Call Remote OData (CDS) | CreateCdsOdataBotExecution | 调用 S/4HANA 服务生成 CDS 视图 |
| 代码生成 | Call Remote OData (Service) | CreateServiceDefinitionBotExecution | 自动生成并发布服务定义 |
| 代码生成 | Call GenBdef OData | CreateGenBdefOdataBotExecution | 生成 RAP 行为定义（BDEF） |
| 代码生成 | Call Genclas OData | CreateGenclasOdataBotExecution | 生成 ABAP 业务逻辑类 |
| 代码生成 | Call Genclasseo OData | CreateGenclasseoOdataBotExecution | 生成 ABAP 类（增强选项） |
| 代码生成 | Call Gen Table OData | CreateGenTableOdataBotExecution | 生成数据库表定义 |

#### 典型自动化场景

| 场景 | Function Calling 调用链 | 说明 |
|------|-------------------------|------|
| **全栈代码生成** | AI 对话 → Create Tasks → Call Gen Table → Call Remote OData → Split CDS | 从需求采集到代码落地的端到端自动化流程 |
| **知识库增强问答** | AI 对话 → Extract View Details → RAG 检索 | 将结构化知识注入回答，提升答案准确度 |
| **自动化开发流程** | Create Tasks → Call GenBdef → Call Genclas → Call Service | 自动生成完整的 RAP 应用骨架 |

#### 注解体系与运行流程

| 注解 | 级别 | 用途 | 示例 |
|------|------|------|------|
| `@BotExecutor` | 类 | 声明 Bot 执行器元数据 | `name="Create Tasks"` |
| `@ExecuteMethod` | 方法 | 暴露可调用方法 | `operation="create_tasks"` |
| `@ExecuteParameter` | 参数 | 定义参数名称、描述与必填项 | `name="tasks", required=true` |

```
AI 模型 → Function Call Request
   ↓
FunctionCallProcessor（扫描注解，提取元数据）
   ↓
OpenAIFunctionCallAdapter（转换为 OpenAI Schema）
   ↓
AI 选择函数并返回参数
   ↓
参数类型转换（JSON → Java Object）
   ↓
反射调用 @ExecuteMethod 方法
   ↓
结果写入 ContextNode
```



#### 端到端自动化工作流示例

```
用户需求："创建一个订单管理的 Fiori 应用"
   ↓
Bot 1 (AI_CHAT): 理解需求，收集细节
   ↓
Bot 2 (FUNCTION_CALL): Create Tasks
   └─ 创建子任务：数据模型、业务逻辑、服务暴露
      ↓
Bot 3 (FUNCTION_CALL): Call Gen Table OData
   └─ 生成订单表、订单项表
      ↓
Bot 4 (FUNCTION_CALL): Call Remote OData (CDS)
   └─ 生成 CDS 视图
      ↓
Bot 5 (FUNCTION_CALL): Call GenBdef OData
   └─ 生成行为定义
      ↓
Bot 6 (FUNCTION_CALL): Call Genclas OData
   └─ 生成业务逻辑类
      ↓
Bot 7 (FUNCTION_CALL): Call Remote OData (Service)
   └─ 生成并发布 OData 服务
      ↓
Bot 8 (FUNCTION_CALL): Split CDS and Update Context
   └─ 拆分代码并更新上下文树
      ↓
完成：完整的 RAP 应用自动生成
```

#### 生命周期管理与缓存

TaskBotCacheManager 负责维护任务与 Bot 的树形结构，实现高效的查找与状态同步。

```
┌──────────────────────────────────────────────────────────┐
│           TaskBotNode 树形缓存结构                         │
├──────────────────────────────────────────────────────────┤
│  MainTask (root)                                         │
│    ├─ BotInstance1                                       │
│    │    └─ SubTask1                                      │
│    │         ├─ BotInstance1.1                           │
│    │         └─ BotInstance1.2                           │
│    ├─ BotInstance2                                       │
│    └─ BotInstance3                                       │
│         └─ SubTask3                                      │
│              └─ BotInstance3.1                           │
└──────────────────────────────────────────────────────────┘
```

| 能力 | 说明 |
|------|------|
| **统一缓存** | 内存中维护 Task/Bot 树形结构，提高访问性能 |
| **懒加载** | 首次访问命中数据库，后续命中缓存 |
| **状态同步** | 缓存与数据库状态自动双向同步 |
| **节点定位** | 支持通过 taskId、botInstanceId 快速检索 |
| **树遍历** | 提供前序、后序、层序等遍历 API |

---

### 3. RAG 知识增强系统

#### 总览
- ✅ 工厂模式动态装配提取器，按需扩展领域知识
- ✅ 多源知识检索（视图、字段、连接条件）提升回答质量
- ✅ Top-K 与阈值可配置，兼顾召回率与精准度

#### 提取器类型与工厂能力

| 提取器 | 主要功能 | 适用场景 |
|--------|----------|----------|
| **RAGCDSViewExtractorImpl** | 提取 CDS 视图元数据 | 视图查询、数据模型分析 |
| **RAGJoinConditionExtractorImpl** | 提取表连接条件 | 关系建模、数据关联 |
| **RAGViewFieldExtractorImpl** | 提取视图字段信息 | 字段说明、结构理解 |

| 工厂能力 | 说明 |
|----------|------|
| 动态创建 | 支持简单类名/完整类名解析并实例化提取器 |
| 多包扫描 | 自动扫描配置包路径，无需手工注册 |
| Spring 注入 | 提供依赖注入与生命周期托管 |
| 实例缓存 | 常用提取器缓存复用，降低创建开销 |

#### 对话流程

```
用户输入问题
   ↓
RAGExtractionFactory 选择提取器
   ↓
提取器按 topK/threshold 检索结构化知识
   ↓
拼装包含原问题与检索内容的增强提示词
   ↓
AI 生成回答并返回给用户
```

#### 配置项与模式对比

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| **extractorClass** | 提取器类名 | RAGCDSViewExtractorImpl |
| **ragSource** | 数据源标识 | scenario_views / view_fields |
| **topK** | 返回结果数量 | 3-10（按场景调优） |
| **threshold** | 相似度阈值 | 0.7-0.9（越高越精准） |

---

### 4. 条件执行与变量解析系统

#### 变量类型与语法

| 变量类型 | 语法 | 示例 | 用途 |
|---------|------|------|------|
| **Context** | `{{Context:path}}` | `{{Context:userProfile.age}}` | 访问任务上下文（绝对路径） |
| **SubContext** | `{{SubContext:path}}` | `{{SubContext:result.confidence}}` | 访问子任务上下文（相对路径） |
| **Instance** | `{{Instance:property}}` | `{{Instance:sequence}}` | 访问当前 Bot 实例属性 |
| **OData** | `{{OData:/Entity/key/prop}}` | `{{OData:/Tasks/{{Instance:ID}}/status}}` | 动态查询后端数据 |

#### 条件表达式示例

| 场景 | 表达式 | 说明 |
|------|--------|------|
| 简单条件 | `true` | 始终执行 |
| 状态检查 | `{{Context:user.status}} == 'READY'` | 根据上下文状态控制执行 |
| 数值比较 | `{{Instance:sequence}} > 1` | 跳过首个 Bot |
| 组合条件 | `{{Context:priority}} >= 5 && {{SubContext:errors}} == 0` | 多条件组合判断 |
| JSON 属性 | `{{Context:userProfile}}.age >= 18` | 解析 JSON 对象属性 |
| OData 查询 | `{{OData:/Tasks/{{Instance:ID}}/status}} != 'FAILED'` | 动态拉取后端状态 |

#### 条件评估流程

```
Bot 执行前
   ↓
读取 executeCondition
   ↓
VariableParsingService 解析变量
   ↓
SpELVariableHelper 注入复杂对象
   ↓
SpEL ExpressionParser 评估表达式
   ↓
true → 执行 Bot / false → 标记为 SKIPPED
```

#### 变量解析引擎
- **VariableContext**：统一管理 Context、SubContext、Instance 和 OData 变量源
- **VariableParsingService**：解析 `{{变量类型:路径}}` 语法，支持嵌套与递归
- **SpELVariableHelper**：将解析出的 JSON/对象注入 SpEL 上下文
- **ExecuteConditionEvaluationService**：协调解析与评估，返回最终布尔结果

#### OData URL 解析与 SpEL 转换
1. 解析 URL 组件，识别实体集、键值与导航属性
2. 基于 SAP CAP CQN API 构建查询语句并执行
3. 将查询结果注入变量上下文，转换为 SpEL 可识别的形式
4. 替换表达式中的 JSON 字符串为 `#jsonVarX` 变量，完成最终评估

#### 能力优势
- ✅ SpEL 全语法支持，可表达复杂业务规则
- ✅ 多变量源统一解析，减少手工代码
- ✅ 自动完成 JSON 与类型转换，保障类型安全
- ✅ OData 深度集成，直接利用后端实时数据

---

### 5. Spring Batch 自动化编排

#### 核心能力

| 功能 | 说明 |
|------|------|
| **任务自动编排** | 按 sequence 顺序执行 BotInstances |
| **条件化执行** | 基于 executeCondition 决定是否执行某个 Bot |
| **递归任务处理** | 支持 Task → BotInstance → SubTask 的递归编排 |
| **失败重试机制** | 可配置重试次数与退避策略 |
| **执行日志记录** | 全链路记录执行状态与上下文 |

#### 执行流程

| 步骤 | 说明 |
|------|------|
| 1️⃣ **任务启动** | 读取设置为 autoRun = true 的主任务 |
| 2️⃣ **实例遍历** | 按 sequence 顺序遍历 BotInstances |
| 3️⃣ **条件评估** | 根据 executeCondition 判定是否执行 |
| 4️⃣ **类型执行** | 根据 Bot 类型调用 chatCompletion / function calling / code execution |
| 5️⃣ **结果记录** | 将执行结果写入 ContextNode |
| 6️⃣ **递归调度** | 若生成子任务，递归执行上述流程 |

---

### 6. 多 AI 模型支持

| 支持项 | 说明 |
|--------|------|
| **OpenAI 系列** | SAP AI Core OpenAI（GPT-4o、GPT-4 Turbo 等） |
| **Claude 系列** | SAP AI Core Claude（Claude 3.5 Sonnet 等） |
| **模型配置** | 通过 ModelConfig 实体集中管理模型参数、温度、最大 Token | 
| **提示词模板** | PromptText 支持多语言提示词模板与版本管理 |

---

## 📦 项目结构

| 目录 | 说明 | 主要内容 |
|------|------|----------|
| **db/** | CDS 数据模型 | orchestration-model.cds（核心运行时）<br>orchestration-config-model.cds（配置）<br>orchestration-rag-model.cds（RAG）<br>data/（初始化数据） |
| **srv/** | 后端服务 | orchestration-service.cds（主服务定义）<br>orchestration-config-service.cds（配置服务）<br>src/main/java/customer/ai2code/ |
| **srv/src/.../handlers/** | CDS 事件处理器 | 处理 CAP 框架的 OData 事件 |
| **srv/src/.../model/** | 业务模型 | Bot.java、Task.java、AIModel.java<br>tree/（树形结构） |
| **srv/src/.../service/** | 业务服务 | AIService、BotService、TaskService、ContextService<br>execution/（BotExecution、FunctionCallProcessor、annotation） |
| **srv/src/.../batch/** | Spring Batch | BatchStepFlowConfiguration.java |
| **srv/src/.../config/** | 配置类 | SpELConfiguration.java |
| **app/** | 前端应用 | config（配置管理）<br>runtime（运行时管理）<br>ragcdsviews（RAG 视图）<br>taskfree（任务管理） |
| **docs/** | 项目文档 | guidance.md（开发指南）<br>spel-architecture-explanation.md<br>executeCondition-usage-guide.md<br>chatbot-rag-usage-guide.md |

---

## 📚 使用场景

### 场景 1：自动化代码生成
| 步骤 | Bot 类型 | 功能 |
|------|----------|------|
| 1 | 主任务 | 创建类型为"代码生成"的主任务，输入任务描述 |
| 2 | 系统自动 | 自动在 Context 中生成 requirement 节点保存需求 |
| 3 | AI_CHAT | 收集用户需求，生成详细需求文档 |
| 4 | FUNCTION_CALL | 分解为多个子任务（设计、开发、测试） |
| 5 | CODE | 子任务 Bot 生成具体代码文件 |
| 6 | 汇总 | 结果存储到 ContextNode 并展示 |

### 场景 2：知识库问答
| 步骤 | Bot 类型 | 功能 |
|------|----------|------|
| 1 | 主任务 | 创建类型为"知识查询"的主任务 |
| 2 | AI_CHAT + RAG | 基于 CDS 视图、字段等知识库回答问题 |
| 3 | 存储 | 结果存储到 ContextNode |

### 场景 3：批量数据处理
| 步骤 | 说明 |
|------|------|
| 1 | 创建主任务，设置 autoRun = true |
| 2 | 配置多个 Bot，每个处理不同的数据转换步骤 |
| 3 | Spring Batch 自动编排执行 |
| 4 | 通过 executeCondition 条件化跳过不需要的步骤 |

---

## 🚀 快速开始

### 环境要求
- Java 21
- Maven 3.6.3+
- Node.js (LTS 版本)
- SAP BTP 账号（Cloud Foundry）

### 本地运行

#### 方式一：使用 VS Code Debug
1. 登录 BTP Cloud Foundry 环境：`cf login -a https://api.cf.jp10.hana.ondemand.com --sso-passcode XXXX`
2. 选择 Org 和 Space
3. 使用 VS Code 的 Java/Spring Boot Debug 工具运行

#### 方式二：使用 Maven 命令
1. 登录 BTP Cloud Foundry 环境
2. 绑定服务：`cds bind --exec '--' node ./writecfenv.js`
3. 运行应用：`mvn spring-boot:run`

### 访问应用
- **主服务**：http://localhost:8080
- **OData 服务**：http://localhost:8080/odata/v4/MainService
- **H2 控制台**：http://localhost:8080/h2-console（开发环境）

---

## 🛠️ 开发指南

### 创建新的 Bot Execution

**实现步骤**：
1. 实现 `BotExecution` 接口
2. 在类上添加 `@BotExecutor` 注解
3. 在执行方法上添加 `@ExecuteMethod` 注解
4. 为参数添加 `@ExecuteParameter` 注解指定参数来源
### 创建新的 RAG 提取器

**实现步骤**：
1. 实现 `RAGExtraction` 接口
2. 在类上添加 `@RAGExtractor` 注解
3. 实现 `extract()` 方法提取知识
4. 在 ChatBot 中配置使用该提取器

### 配置新的任务类型

**配置步骤**：

| 步骤 | 操作 | 说明 |
|------|------|------|
| 1 | 创建 TaskType | 在 TaskType 表中创建新类型记录 |
| 2 | 关联 BotType | 配置该任务类型需要的 Bot 类型 |
| 3 | Bot 配置 | 为每个 BotType 配置执行参数 |

**Bot 执行参数**：

| 参数 | 说明 |
|------|------|
| **sequence** | 执行顺序（数字，从小到大） |
| **functionType** | Bot 类型（AI_CHAT/FUNCTION_CALL/CODE） |
| **autoRun** | 是否自动执行（true/false） |
| **executeCondition** | SpEL 执行条件表达式 |
| **model** | AI 模型配置 |
| **prompts** | 提示词模板 |
| **implementationClass** | Java 实现类全限定名 |

---


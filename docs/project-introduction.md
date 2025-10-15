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

1. **用户输入需求**（左栏 Context 树）
   - 在 Context 树中创建节点 `requirement`（MARKDOWN 类型）
   - 中栏编辑器中输入："创建用户管理模块，包含增删改查功能"

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

#### 前端技术栈
- **框架**：SAP Fiori Elements + UI5 freestyle
- **数据协议**：OData V4
- **树形组件**：自定义递归树组件
- **编辑器**：
  - Monaco Editor（代码编辑）
  - Markdown Editor（富文本编辑）
  - JSON Editor（结构化数据）

#### 后端 API 支持

**标准 OData 操作**：

| API 路径 | 说明 |
|----------|------|
| `GET /Tasks('{taskId}')/contextNodes` | 获取 Context 树数据 |
| `GET /Tasks('{taskId}')/botInstances` | 获取 Task 树数据 |
| `GET /Tasks('{taskId}')/botInstances?$expand=tasks($expand=botInstances)` | 获取完整任务树（包含子任务） |
| `PATCH /ContextNodes('{nodeId}')` | 更新 Context 节点 |
| `GET /BotInstances('{botId}')/messages` | 获取对话历史 |

**自定义 Actions**：

| API 路径 | 方法 | 说明 |
|----------|------|------|
| `/BotInstances('{botId}')/chatCompletion` | POST | AI 对话 |
| `/api/chat/streaming` | POST | SSE 流式对话 |
| `/BotInstances('{botId}')/execute` | POST | 执行 Bot |
| `/BotMessages('{messageId}')/adopt` | POST | 采用 AI 回复 |

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
- SAP Fiori Elements
- UI5
- OData V4

#### 开发工具
- Maven 3.6.3+
- Node.js (CDS 工具链)
- VS Code

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

### 2. 智能 Bot 系统

#### Function Calling 功能全景

**AI2Code 通过 Function Calling 实现了 9 大自动化能力**：

| 序号 | 功能分类 | 具体能力 | 业务价值 |
|------|----------|----------|----------|
| 1 | **任务管理** | Create Tasks（创建子任务） | AI 自动分解复杂任务为多个可执行子任务 |
| 2 | **上下文管理** | Split CDS and Update Context（CDS 拆分） | 自动将 AI 生成的代码拆分并存储到上下文树 |
| 3 | **知识提取** | Extract View Details（提取视图详情） | 从 CDS 视图提取字段和连接条件，增强 RAG |
| 4 | **CDS 生成** | Call Remote OData (CDS)（生成 CDS 视图） | 调用 S4 系统自动生成 CDS 视图定义 |
| 5 | **服务生成** | Call Remote OData (Service)（生成服务） | 调用 S4 系统自动生成服务定义 |
| 6 | **BDEF 生成** | Call GenBdef OData（生成行为定义） | 自动生成 RAP 行为定义（BDEF） |
| 7 | **类生成** | Call Genclas OData（生成类） | 自动生成 ABAP 业务逻辑类 |
| 8 | **类生成增强** | Call Genclasseo OData（生成类增强版） | 自动生成 ABAP 类（更多配置选项） |
| 9 | **表生成** | Call Gen Table OData（生成表） | 自动生成数据库表定义 |

**典型自动化场景**：

1. **全栈代码生成**：用户描述需求 → AI 调用 Create Tasks 分解 → 依次调用 Table/CDS/BDEF/Class/Service 生成 → 完整应用自动创建
2. **智能代码重构**：AI 生成优化后的 CDS → Split CDS 自动拆分 → 更新到上下文树 → 用户审核
3. **知识库增强问答**：用户询问视图 → Extract View Details 提取 → RAG 检索相关知识 → AI 生成精准答案

#### Bot 类型

**1. AI Chat Bot（对话型 Bot）**
- 功能：与用户进行智能对话，收集需求和信息
- 特性：
  - 支持流式响应（SSE）
  - 多轮对话历史管理
  - 系统提示词模板化
  - RAG 增强回答
  
**2. Function Call Bot（函数调用型 Bot）**
- 功能：通过 AI Function Calling 自动执行业务逻辑
- 特性：
  - 注解驱动的函数定义（@BotExecutor、@ExecuteMethod、@ExecuteParameter）
  - 自动参数类型转换
  - 支持复杂对象和数组参数
  - 编译时验证函数签名

**Function Calling 已实现功能清单**：

| 功能名称 | 功能说明 | 主要用途 |
|----------|----------|----------|
| **Create Tasks** | 创建子任务 | AI 根据用户需求自动分解任务，创建多个子任务进行协作处理 |
| **Split CDS and Update Context** | CDS 拆分与上下文更新 | 将 AI 生成的 CDS 视图代码拆分成多个节点并更新到上下文树 |
| **Extract View Details** | 提取视图详细信息 | 从 CDS 视图中提取字段和连接条件信息，支持知识库查询 |
| **Call Remote OData (CDS)** | 调用远程 CDS OData | 调用 S4/HANA 系统的 ZSRVD_GENDDLS 服务生成 CDS 视图 |
| **Call Remote OData (Service)** | 调用远程服务定义 OData | 调用 S4/HANA 系统的 ZSRVD_GENSRVD 服务生成服务定义 |
| **Call GenBdef OData** | 生成行为定义 | 调用 ZSRVD_GENBDEF 服务生成 BDEF（Behavior Definition） |
| **Call Genclas OData** | 生成类定义 | 调用 SRVD_GENCLAS 服务生成 ABAP 类 |
| **Call Genclasseo OData** | 生成类（增强版） | 调用 SRVD_GENCLAS 服务生成 ABAP 类（支持更多选项） |
| **Call Gen Table OData** | 生成数据库表 | 调用远程服务生成数据库表定义 |

**典型应用场景示例**：

| 场景 | Function Calling 调用链 | 说明 |
|------|-------------------------|------|
| **代码生成工作流** | AI 对话 → Create Tasks → Call Remote OData → Split CDS | 用户描述需求 → AI 创建子任务 → 调用 S4 服务生成代码 → 拆分并存储到上下文 |
| **知识库查询增强** | AI 对话 → Extract View Details → RAG 检索 | 用户询问 CDS 视图 → 提取详细信息 → 增强 AI 回答 |
| **自动化开发流程** | Create Tasks → Call GenBdef → Call Genclas → Call Service | 自动生成完整的 RAP 应用（BDEF + Class + Service） |
  
**3. Code Bot（代码执行型 Bot）**
- 功能：直接执行预定义的代码逻辑
- 特性：
  - 动态类加载
  - 执行结果自动记录
  - 支持同步和异步执行

#### Bot 生命周期管理

**TaskBotCacheManager 树形缓存结构**：

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

**生命周期管理功能**：

| 功能 | 说明 |
|------|------|
| **实例缓存** | 在内存中维护 Task 和 Bot 的树形缓存，提高访问性能 |
| **懒加载** | 首次访问时从数据库加载，后续访问直接从缓存获取 |
| **状态同步** | 自动同步内存缓存与数据库状态 |
| **节点查找** | 支持通过 taskId、botInstanceId 快速定位节点 |
| **树遍历** | 提供前序、后序、层序遍历方法 |

### 3. Function Calling 功能

#### 注解系统

**三层注解结构**：

| 注解 | 级别 | 用途 | 示例 |
|------|------|------|------|
| `@BotExecutor` | 类 | 标识执行类 | `name="Create Tasks"` |
| `@ExecuteMethod` | 方法 | 标识可调用方法 | `operation="create_tasks"` |
| `@ExecuteParameter` | 参数 | 定义参数元数据 | `name="tasks", required=true` |

**注解配置示例**：

```
类级别：@BotExecutor
  ├─ name: 执行器名称
  ├─ description: 功能描述
  ├─ version: 版本号
  └─ enabled: 是否启用
     ↓
方法级别：@ExecuteMethod
  ├─ operation: 操作名称（对应 OpenAI function name）
  ├─ description: 方法描述
  └─ logExecution: 是否记录日志
     ↓
参数级别：@ExecuteParameter
  ├─ name: 参数名（必须与实际参数名一致）
  ├─ description: 参数说明
  └─ required: 是否必填
```

#### 执行流程
1. **函数信息提取**：FunctionCallProcessor 扫描注解并提取元数据
2. **OpenAI 格式转换**：OpenAIFunctionCallAdapter 生成 JSON Schema
3. **AI 函数调用**：AI 选择并调用合适的函数
4. **参数转换**：自动将 JSON 参数转换为 Java 对象
5. **方法执行**：反射调用目标方法
6. **结果返回**：将执行结果写入 ContextNode

#### 已实现的 Function Calling 能力矩阵

| 能力类别 | 功能名称 | 实现类 | 主要应用 |
|----------|----------|--------|----------|
| **任务编排** | Create Tasks | CreateTasksBotExecution | 任务分解、子任务创建 |
| **上下文管理** | Split CDS and Update Context | SplitCDSandUpdateContextExecution | CDS 代码拆分、上下文更新 |
| **知识提取** | Extract View Details | ExtractViewDetailExecution | 视图字段、连接条件提取 |
| **代码生成** | Call Remote OData (CDS) | CreateCdsOdataBotExecution | 生成 CDS 视图定义 |
| **代码生成** | Call Remote OData (Service) | CreateServiceDefinitionBotExecution | 生成服务定义 |
| **代码生成** | Call GenBdef OData | CreateGenBdefOdataBotExecution | 生成行为定义（BDEF） |
| **代码生成** | Call Genclas OData | CreateGenclasOdataBotExecution | 生成 ABAP 类 |
| **代码生成** | Call Genclasseo OData | CreateGenclasseoOdataBotExecution | 生成 ABAP 类（增强版） |
| **代码生成** | Call Gen Table OData | CreateGenTableOdataBotExecution | 生成数据库表定义 |

**端到端自动化工作流示例**：

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

### 4. RAG（检索增强生成）系统

#### RAG 提取器类型

**1. RAGCDSViewExtractorImpl**
- 用途：从 CDS 视图中提取相关信息
- 场景：视图查询、数据模型相关问题

**2. RAGJoinConditionExtractorImpl**
- 用途：提取表连接条件信息
- 场景：数据关联、表关系查询

**3. RAGViewFieldExtractorImpl**
- 用途：提取视图字段信息
- 场景：字段查询、数据结构相关问题

#### RAG 增强对话流程

```
用户输入问题
    ↓
RAG 提取器从知识库检索（Top-K + 阈值过滤）
    ↓
将检索结果附加到提示词
    ↓
调用 AI 模型生成回答
    ↓
返回增强后的回答
```

**配置参数说明**：

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| **extractorClass** | RAG 提取器类名 | RAGCDSViewExtractorImpl |
| **ragSource** | 数据源标识 | scenario_views / view_fields |
| **topK** | 返回结果数量 | 3-10（根据场景调整） |
| **threshold** | 相似度阈值 | 0.7-0.9（越高越精确） |

### 5. 条件执行系统（ExecuteCondition）

#### SpEL 表达式支持

Bot 可以配置执行条件，只有满足条件时才会执行。

**支持的变量类型**：

| 变量类型 | 格式 | 示例 | 说明 |
|---------|------|------|------|
| **Context** | `{{Context:path}}` | `{{Context:user.status}}` | 访问任务上下文（绝对路径） |
| **SubContext** | `{{SubContext:path}}` | `{{SubContext:result.confidence}}` | 访问子任务上下文（相对路径） |
| **Instance** | `{{Instance:property}}` | `{{Instance:sequence}}` | 访问当前实例属性 |
| **OData** | `{{OData:/EntitySet/key/property}}` | `{{OData:/Tasks/task-123/name}}` | 动态查询数据库 |

**条件表达式示例**：

| 场景 | 表达式 | 说明 |
|------|--------|------|
| 简单条件 | `true` | 始终执行 |
| 状态检查 | `{{Context:user.status}} == 'READY'` | 检查用户状态 |
| 数值比较 | `{{Instance:sequence}} > 1` | 跳过第一个 Bot |
| 组合条件 | `{{Context:priority}} >= 5 && {{SubContext:errors}} == 0` | 多条件组合 |
| JSON 属性 | `{{Context:userProfile}}.age >= 18` | 访问对象属性 |
| OData 查询 | `{{OData:/Tasks/{{Instance:ID}}/status}} != 'FAILED'` | 动态查询状态 |

#### 执行流程
```
Bot执行前 
  ↓
获取 executeCondition
  ↓
VariableParsingService 解析变量
  ↓
SpEL 表达式评估
  ↓
true → 执行 Bot
false → 跳过 Bot（SKIPPED）
```

### 6. Spring Batch 自动化编排

#### 批处理流程

| 步骤 | 说明 |
|------|------|
| **1. 读取任务** | 读取主任务（autoRun = true） |
| **2. 遍历实例** | 按 sequence 排序遍历 BotInstances |
| **3. 条件评估** | 评估 executeCondition：<br>• true → 执行 Bot<br>• false → 跳过 Bot |
| **4. 类型执行** | 根据 Bot 类型执行：<br>• AI_CHAT → chatCompletion<br>• FUNCTION_CALL → function calling<br>• CODE → execute implementation |
| **5. 结果记录** | 将执行结果记录到 ContextNode |
| **6. 递归调度** | 如果产生子任务，递归执行整个流程 |

### 7. 变量解析系统

#### VariableContext
统一的变量上下文，支持：
- Context 变量（任务上下文）
- Instance 变量（实例属性）
- OData 查询（动态数据查询）

#### VariableParsingService
- 解析 `{{变量类型:路径}}` 格式的变量
- 支持嵌套变量（如 `{{OData:/Tasks/{{Instance:ID}}/name}}`）
- 支持 JSON 路径解析
- 缓存机制提升性能

---

## 🔧 技术亮点

### 1. 注解驱动的 Function Calling 系统

#### 核心特性
- ✅ **编译时验证**：通过注解处理器在编译阶段验证函数签名
- ✅ **自动 Schema 生成**：OpenAIFunctionCallAdapter 自动生成 OpenAI JSON Schema
- ✅ **智能类型转换**：支持基本类型、复杂对象、List 泛型等自动转换
- ✅ **反射执行**：FunctionCallProcessor 通过反射调用目标方法

#### 技术架构
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

#### 关键组件
- **@BotExecutor**：标识 BotExecution 实现类
- **@ExecuteMethod**：标识可被 AI 调用的方法
- **@ExecuteParameter**：定义参数元数据
- **FunctionCallProcessor**：函数信息提取和执行
- **OpenAIFunctionCallAdapter**：OpenAI 格式适配

#### 实现方式

**开发步骤**：

1. **定义执行类**：实现 `BotExecution` 接口
2. **添加类注解**：`@BotExecutor` 标识执行器
3. **添加方法注解**：`@ExecuteMethod` 标识可调用方法
4. **添加参数注解**：`@ExecuteParameter` 定义参数元数据
5. **配置 BotType**：在数据库中配置 `implementationClass` 字段

**执行效果**：AI 根据对话内容自动选择并调用相应的 Java 方法，实现智能任务分解和执行。

### 2. 扁平化 Context 存储与树形重构

#### 存储策略

| 策略 | 说明 | 优势 |
|------|------|------|
| **扁平化存储** | 所有节点存储为单条记录 | 灵活、易扩展、无需预定义结构 |
| **path 标识** | 通过 `path` 字段唯一定位节点 | 支持复杂路径如 `a.b[0].c` |
| **树形重构** | 前端动态组装树形结构 | 按需加载、性能优化 |
| **类型多样** | 支持多种数据类型 | STRING/MARKDOWN/CODE/JSON |

#### ContextNode 数据结构

| 字段 | 类型 | 示例 | 说明 |
|------|------|------|------|
| path | String(1000) | `subtask[0].cdsView.fields[2].name` | 节点路径（支持数组索引） |
| label | String(200) | `Field Name` | 显示标签 |
| type | String(50) | `STRING` / `MARKDOWN` / `CODE` / `JSON` | 数据类型 |
| value | LargeString | 实际内容 | 节点值 |

#### PathParser 工具类功能

| 方法 | 输入 | 输出 | 用途 |
|------|------|------|------|
| `parsePath()` | `subtask[0].cView` | `["subtask[0]", "cView"]` | 解析路径为段数组 |
| `getParentPath()` | `subtask[0].cView` | `subtask[0]` | 获取父路径 |
| `getPathHierarchy()` | `subtask[0].cView` | `["subtask", "subtask[0]"]` | 获取所有层级 |
| `generateLabelFromPath()` | `subtask[0].cView` | `CView` | 生成显示标签 |

#### 树形重构流程

```
扁平化节点列表
    ↓
1. 解析每个节点的路径层级
    ↓
2. 创建虚拟中间节点（如 subtask、subtask[0]）
    ↓
3. 建立父子关系
    ↓
4. 返回树形 JSON 结构
```

#### 优势

| 优势 | 说明 |
|------|------|
| **灵活存储** | 无需预定义树形结构，扁平化存储 |
| **动态扩展** | 可任意添加新路径节点 |
| **类型多样** | 支持 STRING、MARKDOWN、CODE、JSON 等类型 |
| **前端友好** | 自动生成树形 JSON 供 UI 展示 |

### 3. RAG（检索增强生成）知识增强系统

#### 工厂模式架构

**RAGExtractionFactoryService 功能**：

| 功能 | 说明 |
|------|------|
| **动态创建** | 根据类名动态创建 RAG 提取器实例 |
| **类名支持** | 支持简单类名和完整类名 |
| **自动扫描** | 自动扫描多包路径查找提取器 |
| **依赖注入** | Spring 依赖注入支持 |
| **实例缓存** | 缓存已创建实例提升性能 |

#### RAG 提取器类型
1. **RAGCDSViewExtractorImpl**
   - 从 CDS 视图元数据中提取相关信息
   - 适用场景：视图查询、数据模型问题

2. **RAGJoinConditionExtractorImpl**
   - 提取表连接条件信息
   - 适用场景：数据关联、表关系查询

3. **RAGViewFieldExtractorImpl**
   - 提取视图字段信息
   - 适用场景：字段查询、数据结构问题

#### ChatBot RAG 集成流程

```
用户提问
    ↓
ChatBot.chatWithRAG() 被调用
    ↓
1. RAGExtractionFactory 创建提取器实例
    ↓
2. 提取器从知识库检索相关内容（Top-K + 阈值）
    ↓
3. 构建增强提示词（原问题 + RAG 内容）
    ↓
4. 调用 AI 模型生成回答
    ↓
5. 返回增强后的回答
```

**RAG 工作模式对比**：

| 模式 | 方法 | 使用 RAG | 回答质量 | 适用场景 |
|------|------|---------|---------|---------|
| **标准对话** | `chat()` | ❌ | 基于模型知识 | 一般性问题 |
| **RAG 增强** | `chatWithRAG()` | ✅ | 基于项目知识库 | 专业领域问题 |

#### 配置参数

| 参数 | 类型 | 推荐值 | 说明 |
|------|------|--------|------|
| extractorClass | String | `RAGCDSViewExtractorImpl` | RAG 提取器类名 |
| ragSource | String | `scenario_views` | 数据源标识 |
| topK | Integer | 3-10 | 返回结果数量 |
| threshold | Double | 0.7-0.9 | 相似度阈值（越高越精确） |

#### 优势
- ✅ **动态扩展**：通过类名字符串动态创建提取器
- ✅ **多数据源**：支持 CDS 视图、字段、连接条件等
- ✅ **可配置**：Top-K 和阈值可调
- ✅ **容错机制**：提取失败自动回退到普通对话

### 4. SpEL 条件表达式与变量解析系统

#### 条件执行架构
```
Bot 执行前
   ↓
读取 executeCondition（如：{{Context:user.age}} >= 18）
   ↓
VariableParsingService 解析变量
   ↓
SpELVariableHelper 注入复杂变量（JSON 对象等）
   ↓
SpEL ExpressionParser 评估条件
   ↓
true → 执行 Bot
false → 跳过 Bot（状态：SKIPPED）
```

#### 变量解析系统

**四种变量类型**：

| 变量类型 | 语法 | 示例 | 用途 |
|---------|------|------|------|
| **Context** | `{{Context:path}}` | `{{Context:userProfile.age}}` | 访问任务上下文（绝对路径） |
| **SubContext** | `{{SubContext:path}}` | `{{SubContext:result.confidence}}` | 访问子任务上下文（相对路径） |
| **Instance** | `{{Instance:property}}` | `{{Instance:sequence}}` | 访问当前实例属性 |
| **OData** | `{{OData:/Entity/key/prop}}` | `{{OData:/Tasks/task-123/name}}` | 动态查询数据库 |

#### OData URL 解析器

**解析流程**：
        // 1. 解析 URL：/BotInstances/bot-123/type/model/field1
        // 2. 识别实体集、键、导航属性
        // 3. 构建 CQN Select 语句
        // 4. 执行查询并返回结果
    }
}
```

**支持的 OData 查询**：
- 实体集合：`OData:/BotInstances`
- 单个实体：`OData:/BotInstances('bot-123')`
- 属性访问：`OData:/Tasks('task-456')/name`
- 导航属性：`OData:/BotInstances('bot-123')/task/name`
- 复杂路径：`OData:/BotInstances/b631b9de.../type/outputContextPath`
- 查询参数：`OData:/BotInstances?$filter=status eq 'RUNNING'&$top=10`

#### SpEL 表达式转换

```
OData URL 输入
    ↓
1. 解析 URL 组件
   - 识别实体集（EntitySet）
   - 提取键值（Key）
   - 解析导航属性（Navigation Property）
   - 提取查询参数（$filter, $select, $top 等）
    ↓
2. 构建 CQN 查询
   - 使用 SAP CAP CQN API
   - 处理导航路径（一对一、一对多）
   - 应用过滤条件
    ↓
3. 执行查询并返回结果
```

**支持的 OData 查询**：

| 查询类型 | 示例 | 说明 |
|---------|------|------|
| 实体集合 | `/BotInstances` | 查询所有实例 |
| 单个实体 | `/BotInstances('bot-123')` | 通过 ID 查询 |
| 属性访问 | `/Tasks('task-456')/name` | 获取特定属性 |
| 导航属性 | `/BotInstances('bot-123')/task` | 一对一导航 |
| 复杂路径 | `/BotInstances/bot-123/type/outputContextPath` | 多层导航 |
| 查询参数 | `/BotInstances?$filter=status eq 'RUNNING'` | 条件过滤 |

#### SpEL 表达式转换

**转换必要性**：变量解析后的 JSON 字符串需要转换为有效的 SpEL 表达式

**三步转换流程**：

| 步骤 | 输入 | 输出 | 说明 |
|------|------|------|------|
| **1. 变量解析** | `{{Context:userProfile}}.age >= 18` | `{'name':'张三','age':25}.age >= 18` | VariableParsingService 解析 |
| **2. 变量注入** | JSON 字符串 | `jsonVar0 = {"name":"张三", "age":25}` | SpELVariableHelper 注入变量 |
| **3. 表达式转换** | 原表达式 | `#jsonVar0.age >= 18` | 替换 JSON 为变量引用 |
| **4. SpEL 评估** | SpEL 表达式 | `true` | ExpressionParser 执行 |

#### 核心组件

| 组件 | 职责 | 特性 |
|------|------|------|
| **ExecuteConditionEvaluationService** | 条件评估统一入口 | 整合所有解析和评估步骤 |
| **SpELConfiguration** | SpEL Bean 配置 | 单例模式、统一配置 |
| **SpELVariableHelper** | 复杂变量处理 | JSON 注入、表达式转换 |
| **VariableParsingService** | 变量解析 | 支持嵌套、递归解析 |

#### 优势
- ✅ **强大表达式**：SpEL 完整语法支持
- ✅ **多变量源**：Context、Instance、OData 统一访问
- ✅ **嵌套解析**：支持变量内嵌套变量
- ✅ **JSON 支持**：自动处理 JSON 对象属性访问
- ✅ **OData 集成**：动态查询数据库数据
- ✅ **类型安全**：自动类型转换和验证

### 5. Spring Batch 自动化编排

**批处理功能**：

| 功能 | 说明 |
|------|------|
| **任务自动编排** | 按 sequence 顺序自动执行 BotInstances |
| **条件化执行** | 基于 executeCondition 决定是否执行 |
| **递归子任务处理** | 支持 Task → BotInstance → SubTask 递归执行 |
| **失败重试机制** | 可配置重试次数和策略 |
| **执行日志记录** | 完整的执行历史和状态追踪 |

**执行流程**：

| 步骤 | 说明 |
|------|------|
| 1️⃣ **任务启动** | 读取设置为 autoRun = true 的主任务 |
| 2️⃣ **实例遍历** | 按 sequence 顺序遍历 BotInstances |
| 3️⃣ **条件评估** | 评估 executeCondition 表达式<br>• true → 执行 Bot<br>• false → 标记为 SKIPPED 跳过 |
| 4️⃣ **类型执行** | 根据 Bot 类型执行不同逻辑<br>• AI_CHAT → chatCompletion<br>• FUNCTION_CALL → function calling<br>• CODE → execute implementation |
| 5️⃣ **结果记录** | 将执行结果存储到 ContextNode |
| 6️⃣ **递归调度** | 如果产生子任务，递归执行整个流程 |

### 6. 生命周期管理

**TaskBotCacheManager 功能**：

| 功能 | 说明 |
|------|------|
| **统一缓存** | 内存中维护 Task 和 Bot 的树形结构 |
| **TreeNode 结构** | TaskBotNode 支持父子关系和树遍历 |
| **实例查找** | 快速查找和更新实例状态 |
| **双重管理** | 内存缓存和数据库持久化同步 |

### 7. 多 AI 模型支持

| 支持项 | 说明 |
|--------|------|
| **OpenAI 系列** | SAP AI Core OpenAI（GPT-4o、GPT-4-turbo 等） |
| **Claude 系列** | SAP AI Core Claude（Claude 3.5 Sonnet 等） |
| **模型配置** | 通过 ModelConfig 实体集中管理模型参数 |
| **提示词模板** | PromptText 支持多语言提示词模板化 |

---

## 📦 项目结构

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

## 📚 使用场景

### 场景 1：自动化代码生成
| 步骤 | Bot 类型 | 功能 |
|------|----------|------|
| 1 | 主任务 | 创建类型为"代码生成"的主任务 |
| 2 | AI_CHAT | 收集用户需求，生成详细需求文档 |
| 3 | FUNCTION_CALL | 分解为多个子任务（设计、开发、测试） |
| 4 | CODE | 子任务 Bot 生成具体代码文件 |
| 5 | 汇总 | 结果存储到 ContextNode 并展示 |

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

## 🎯 未来规划

- [ ] 支持更多 AI 模型（如 Google Gemini）
- [ ] 增强前端三栏式交互界面
- [ ] 可视化任务编排设计器
- [ ] 执行条件表达式可视化编辑器
- [ ] 更多的内置 Bot Execution 实现
- [ ] 性能优化和缓存策略
- [ ] 多租户支持
- [ ] 实时协作功能

---

## 📞 联系与支持

如有问题或建议，请通过以下方式联系：
- 项目仓库：[GitHub - graconfig/ai2code](https://github.com/graconfig/ai2code)
- 文档中心：`/docs` 目录

---

**版本**：1.0.0  
**最后更新**：2025-01-15

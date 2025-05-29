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

/* 任务实体，支持多级子任务，记录在context中。 */
entity Task : cuid, managed {

    name         : String(100);
    description  : String;
    contextPath  : String(1000); //Task需要指定的Context路径 例：datasource.children[3]
    sequence     : Integer; // SubTask需要指定的执行顺序
    isMain       : Boolean default true; //冗余存储
    botInstance  : Association to BotInstance;
    type         : Association to TaskType;
    botInstances : Composition of many BotInstance
                    on botInstances.task = $self;
    contextNodes : Composition of many ContextNode
                       on contextNodes.task = $self; //任务下所有context节点
}

/* 单个context节点，采用扁平结构，支持树结构自动还原 */
entity ContextNode : cuid, managed {
    path  : String(1000); // 唯一路径，如 a.b.c[0].d
    label : String(200); // 节点名称
    type  : String(50); // 类型（text, markdown, code, object, array等）
    value : LargeString; // 节点值/内容
    task  : Association to Task; // 所属任务
//readonly  : Boolean default false;     // 是否只读（可选）
// 可扩展：排序、校验、元数据等
}


/* 单次Bot执行实例。 */
entity BotInstance : cuid, managed {
    sequence : Integer;
    result   : LargeString;
    type     : Association to BotType;
    status   : Association to BotInstanceStatus default 'C';
    task     : Association to Task;
    tasks    : Composition of many Task
                   on tasks.botInstance = $self; //下一级的
    messages : Composition of many BotMessage
                on messages.botInstance = $self;
}

/* Bot消息实体，记录人与AI/系统的对话消息。 */
entity BotMessage : cuid, managed {
    role        : String(20); // 'user' | 'assistant' | 'system'
    message : LargeString;
    ragData     : LargeString; //??
    botInstance : Association to BotInstance;
}

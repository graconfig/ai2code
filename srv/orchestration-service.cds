using ai.orchestration as db from '../db/orchestration-model';
using ai.orchestration.config as config from '../db/orchestration-config-model';

service MainService {
    entity Tasks        as projection on db.Task;
    entity ContextNodes as projection on db.ContextNode;
    entity TaskType as projection on config.TaskType;

    @readonly
    entity TaskHierarchyView as projection on db.TaskHierarchyView;

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

annotate MainService.TaskHierarchyView with @Aggregation.RecursiveHierarchy #TaskHierarchyView : {
  ParentNavigationProperty : parent, // navigates to a node's parent
  NodeProperty             : ID, // identifies a node, usually the key
};

extend MainService.TaskHierarchyView with @(
    // The columns expected by Fiori to be present in hierarchy entities
    Hierarchy.RecursiveHierarchy #GenresHierarchy          : {
        LimitedDescendantCount: LimitedDescendantCount,
        DistanceFromRoot      : DistanceFromRoot,
        DrillState            : DrillState,
        LimitedRank           : LimitedRank
    },
    // Disallow filtering on these properties from Fiori UIs
    Capabilities.FilterRestrictions.NonFilterableProperties: [
        'LimitedDescendantCount',
        'DistanceFromRoot',
        'DrillState',
        'LimitedRank'
    ],
    // Disallow sorting on these properties from Fiori UIs
    Capabilities.SortRestrictions.NonSortableProperties    : [
        'LimitedDescendantCount',
        'DistanceFromRoot',
        'DrillState',
        'LimitedRank'
    ],
)
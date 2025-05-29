using MainService as service from '../../srv/orchestration-service';

 @odata.draft.enabled
annotate service.Tasks with @(
    UI.LineItem : [
        {
            $Type : 'UI.DataField',
            Label : 'name',
            Value : name,
        },
        {
            $Type : 'UI.DataField',
            Label : 'description',
            Value : description,
        },
        {
            $Type : 'UI.DataField',
            Label : 'contextPath',
            Value : contextPath,
        },
        {
            $Type : 'UI.DataField',
            Label : 'sequence',
            Value : sequence,
        },
        {
            $Type : 'UI.DataField',
            Label : 'isMain',
            Value : isMain,
        },
    ]
);

annotate service.BotInstances with @(
    UI.LineItem : [
        {
            $Type : 'UI.DataField',
            Label : 'ID',
            Value : ID,
        },
        {
            $Type : 'UI.DataField',
            Label : 'sequence',
            Value : sequence,
        },
        {
            $Type : 'UI.DataField',
            Label : 'result',
            Value : result,
        },
        {
            $Type : 'UI.DataField',
            Label : 'status_code',
            Value : status_code,
        },
    ]
);

annotate service.BotMessages with @(
    UI.LineItem : [
        {
            $Type : 'UI.DataField',
            Label : 'role',
            Value : role,
        },
        {
            $Type : 'UI.DataField',
            Label : 'message',
            Value : message,
        },
        {
            $Type : 'UI.DataField',
            Label : 'ragData',
            Value : ragData,
        },
    ]
);
annotate service.BotMessages with @(
    UI.FieldGroup #GeneratedGroup1 : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Label : 'role',
                Value : role,
            },
            {
                $Type : 'UI.DataField',
                Label : 'message',
                Value : message,
            },
            {
                $Type : 'UI.DataField',
                Label : 'ragData',
                Value : ragData,
            },
        ],
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'GeneratedFacet1',
            Label : 'General Information',
            Target : '@UI.FieldGroup#GeneratedGroup1',
        },
    ]
);

annotate service.BotInstances with @(
    UI.FieldGroup #GeneratedGroup1 : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Label : 'ID',
                Value : ID,
            },
            {
                $Type : 'UI.DataField',
                Label : 'sequence',
                Value : sequence,
            },
            {
                $Type : 'UI.DataField',
                Label : 'result',
                Value : result,
            },
            {
                $Type : 'UI.DataField',
                Label : 'status_code',
                Value : status_code,
            },
        ],
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'GeneratedFacet1',
            Label : 'General Information',
            Target : '@UI.FieldGroup#GeneratedGroup1',
        },
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'Task',
            Label : 'Task',
            Target : 'tasks/@UI.LineItem',
        },
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'BotMessages',
            Label : 'Bot Messages',
            Target : 'messages/@UI.LineItem',
        }
        
    ]
);

annotate service.ContextNodes with @(
    UI.LineItem : [
        {
            $Type : 'UI.DataField',
            Label : 'path',
            Value : path,
        },
        {
            $Type : 'UI.DataField',
            Label : 'label',
            Value : label,
        },
        {
            $Type : 'UI.DataField',
            Label : 'type',
            Value : type,
        },
        {
            $Type : 'UI.DataField',
            Label : 'value',
            Value : value,
        },
    ]
);
annotate service.ContextNodes with @(
    UI.FieldGroup #GeneratedGroup1 : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Label : 'path',
                Value : path,
            },
            {
                $Type : 'UI.DataField',
                Label : 'label',
                Value : label,
            },
            {
                $Type : 'UI.DataField',
                Label : 'type',
                Value : type,
            },
            {
                $Type : 'UI.DataField',
                Label : 'value',
                Value : value,
            },
        ],
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'GeneratedFacet1',
            Label : 'General Information',
            Target : '@UI.FieldGroup#GeneratedGroup1',
        },
    ]
);


annotate service.Tasks with @(
    UI.FieldGroup #GeneratedGroup1 : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Label : 'name',
                Value : name,
            },
            {
                $Type : 'UI.DataField',
                Label : 'description',
                Value : description,
            },
            {
                $Type : 'UI.DataField',
                Label : 'contextPath',
                Value : contextPath,
            },
            {
                $Type : 'UI.DataField',
                Label : 'sequence',
                Value : sequence,
            },
            {
                $Type : 'UI.DataField',
                Label : 'isMain',
                Value : isMain,
            },
        ],
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'GeneratedFacet1',
            Label : 'General Information',
            Target : '@UI.FieldGroup#GeneratedGroup1',
        },
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'ContextNodes',
            Label : 'Context Nodes',
            Target : 'contextNodes/@UI.LineItem',
        },
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'BotInstance',
            Label : 'Bot Instance',
            Target : 'botInstances/@UI.LineItem',
        },
    ]
);

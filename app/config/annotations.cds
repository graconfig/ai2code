using ConfigService as service from '../../srv/orchestration-config-service';
using from '../../db/orchestration-config-model';

annotate service.TaskTypes with @odata.draft.enabled;

annotate service.TaskTypes with @(
    UI.FieldGroup #GeneratedGroup : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Label : '{i18n>TaskName}',
                Value : name,
            },
            {
                $Type : 'UI.DataField',
                Label : '{i18n>TaskDescription}',
                Value : description,
            },
            {
                $Type : 'UI.DataField',
                Label : '{i18n>AutoRun}',
                Value : autoRun,
            },
            {
                $Type : 'UI.DataField',
                Label : '{i18n>IsMain}',
                Value : isMain,
            },
        ],
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'GeneratedFacet1',
            Label : '{i18n>TaskInfo}',
            Target : '@UI.FieldGroup#GeneratedGroup',
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>BotTypes}',
            ID : 'BotTypes',
            Target : 'botTypes/@UI.LineItem#BotTypes',
        },
    ],
    UI.LineItem : [
        {
            $Type : 'UI.DataField',
            Label : '{i18n>TaskName}',
            Value : name,
        },
        {
            $Type : 'UI.DataField',
            Label : '{i18n>TaskDescription}',
            Value : description,
        },
        {
            $Type : 'UI.DataField',
            Label : '{i18n>AutoRun}',
            Value : autoRun,
        },
        {
            $Type : 'UI.DataField',
            Label : '{i18n>IsMain}',
            Value : isMain,
        },
    ],
    UI.HeaderInfo : {
        Title : {
            $Type : 'UI.DataField',
            Value : name,
        },
        TypeName : '',
        TypeNamePlural : '',
        Description : {
            $Type : 'UI.DataField',
            Value : description,
        },
    },
);

annotate service.BotTypes with @(
    UI.LineItem #BotTypes : [
        {
            $Type : 'UI.DataField',
            Value : sequence,
            Label : '{i18n>Sequence}',
        },
        {
            $Type : 'UI.DataField',
            Value : name,
            Label : '{i18n>Name}',
        },
        {
            $Type : 'UI.DataField',
            Value : description,
            Label : '{i18n>Description}',
        },
        {
            $Type : 'UI.DataField',
            Value : functionType_code,
            Label : '{i18n>FunctionType}',
        },
        {
            $Type : 'UI.DataField',
            Value : autoRun,
            Label : '{i18n>AutoRun}',
        },
        {
            $Type : 'UI.DataField',
            Value : executionCondition,
            Label : '{i18n>ExecutionCondition}',
        },
        {
            $Type : 'UI.DataField',
            Value : model_ID,
            Label : '{i18n>Model}',
        },
        {
            $Type : 'UI.DataField',
            Value : outputContextPath,
            Label : '{i18n>OutputContextpath}',
        },
        {
            $Type : 'UI.DataField',
            Value : contextType_code,
            Label : '{i18n>ContextType}',
        },
        {
            $Type : 'UI.DataField',
            Value : isRAGEnabled,
            Label : '{i18n>IsRagenabled}',
        },
        {
            $Type : 'UI.DataField',
            Value : ragClass,
            Label : '{i18n>RagClass}',
        },
        {
            $Type : 'UI.DataField',
            Value : ragSource,
            Label : '{i18n>RagSource}',
        },
        {
            $Type : 'UI.DataField',
            Value : ragTopK,
            Label : '{i18n>RagTopk}',
        },
        {
            $Type : 'UI.DataField',
            Value : implementationClass,
            Label : '{i18n>ImplementationClass}',
        },
    ],
    UI.HeaderInfo : {
        Title : {
            $Type : 'UI.DataField',
            Value : name,
        },
        TypeName : '',
        TypeNamePlural : '',
        Description : {
            $Type : 'UI.DataField',
            Value : description,
        },
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            Label : 'Bot Type',
            ID : 'BotType',
            Target : '@UI.FieldGroup#BotType',
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : 'Prompts',
            ID : 'Prompts',
            Target : 'prompts/@UI.LineItem#Prompts',
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : 'FunctionCalls',
            ID : 'FunctionCalls',
            Target : 'functionCalls/@UI.LineItem#FunctionCalls',
        },
    ],
    UI.FieldGroup #BotType : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Value : autoRun,
                Label : '{i18n>AutoRun}',
            },
            {
                $Type : 'UI.DataField',
                Value : contextType_code,
                Label : '{i18n>ContextType}',
            },
            {
                $Type : 'UI.DataField',
                Value : description,
                Label : '{i18n>Description}',
            },
            {
                $Type : 'UI.DataField',
                Value : executionCondition,
                Label : '{i18n>ExecutionCondition}',
            },
            {
                $Type : 'UI.DataField',
                Value : functionType_code,
                Label : '{i18n>FunctionType}',
            },
            {
                $Type : 'UI.DataField',
                Value : implementationClass,
                Label : '{i18n>ImplementationClass}',
            },
            {
                $Type : 'UI.DataField',
                Value : isRAGEnabled,
                Label : '{i18n>IsRagenabled}',
            },
            {
                $Type : 'UI.DataField',
                Value : model_ID,
                Label : '{i18n>Model}',
            },
            {
                $Type : 'UI.DataField',
                Value : name,
                Label : '{i18n>Name}',
            },
            {
                $Type : 'UI.DataField',
                Value : outputContextPath,
                Label : '{i18n>OutputContextpath}',
            },
            {
                $Type : 'UI.DataField',
                Value : ragClass,
                Label : '{i18n>RagClass}',
            },
            {
                $Type : 'UI.DataField',
                Value : ragSource,
                Label : '{i18n>RagSource}',
            },
            {
                $Type : 'UI.DataField',
                Value : ragTopK,
                Label : '{i18n>RagTopk}',
            },
            {
                $Type : 'UI.DataField',
                Value : sequence,
                Label : '{i18n>Sequence}',
            },
        ],
    },
);

annotate service.BotTypes with {
    functionType @(
        Common.Text : functionType.descr,
        Common.ValueList : {
            $Type : 'Common.ValueListType',
            CollectionPath : 'BotFunctionTypes',
            Parameters : [
                {
                    $Type : 'Common.ValueListParameterInOut',
                    LocalDataProperty : functionType_code,
                    ValueListProperty : 'code',
                },
                {
                    $Type : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty : 'name',
                },
            ],
        },
        Common.ValueListWithFixedValues : false,
    )
};

annotate service.BotFunctionTypes with {
    code @Common.Text : descr
};

annotate service.BotTypes with {
    model @(
        Common.Text : model.modelName,
        Common.ValueList : {
            $Type : 'Common.ValueListType',
            CollectionPath : 'ModelConfigs',
            Parameters : [
                {
                    $Type : 'Common.ValueListParameterInOut',
                    LocalDataProperty : model_ID,
                    ValueListProperty : 'ID',
                },
                {
                    $Type : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty : 'name',
                },
                {
                    $Type : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty : 'provider',
                },
            ],
        },
        Common.ValueListWithFixedValues : false,
    )
};

annotate service.ModelConfigs with {
    ID @Common.Text : modelName
};

annotate service.BotTypes with {
    contextType @(
        Common.Text : contextType.descr,
        Common.ValueList : {
            $Type : 'Common.ValueListType',
            CollectionPath : 'ContextTypes',
            Parameters : [
                {
                    $Type : 'Common.ValueListParameterInOut',
                    LocalDataProperty : contextType_code,
                    ValueListProperty : 'code',
                },
                {
                    $Type : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty : 'name',
                },
            ],
        },
        Common.ValueListWithFixedValues : false,
    )
};

annotate service.ContextTypes with {
    code @Common.Text : descr
};

annotate service.PromptTexts with @(
    UI.LineItem #Prompts : [
        {
            $Type : 'UI.DataField',
            Value : lang,
            Label : '{i18n>Language}',
        },
        {
            $Type : 'UI.DataField',
            Value : name,
            Label : '{i18n>Name}',
        },
        {
            $Type : 'UI.DataField',
            Value : content,
            Label : '{i18n>Content}',
        },
    ],
    UI.HeaderInfo : {
        Title : {
            $Type : 'UI.DataField',
            Value : botType.description,
        },
        TypeName : '',
        TypeNamePlural : '',
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            Label : 'Prompt Info',
            ID : 'PromptInfo',
            Target : '@UI.FieldGroup#PromptInfo',
        },
    ],
    UI.FieldGroup #PromptInfo : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Value : lang,
                Label : '{i18n>Language}',
            },
            {
                $Type : 'UI.DataField',
                Value : name,
                Label : '{i18n>Name}',
            },
            {
                $Type : 'UI.DataField',
                Value : content,
                Label : '{i18n>Content}',
            },
        ],
    },
);

annotate service.FunctionCall with @(
    UI.LineItem #FunctionCalls : [
        {
            $Type : 'UI.DataField',
            Value : name,
            Label : '{i18n>Name}',
        },
        {
            $Type : 'UI.DataField',
            Value : description,
            Label : '{i18n>Description}',
        },
        {
            $Type : 'UI.DataField',
            Value : parameters,
            Label : '{i18n>Parameters}',
        },
    ],
    UI.HeaderInfo : {
        Title : {
            $Type : 'UI.DataField',
            Value : botType.description,
        },
        TypeName : '',
        TypeNamePlural : '',
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            Label : 'Function Call',
            ID : 'FunctionCall',
            Target : '@UI.FieldGroup#FunctionCall',
        },
    ],
    UI.FieldGroup #FunctionCall : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Value : name,
                Label : '{i18n>Name}',
            },
            {
                $Type : 'UI.DataField',
                Value : description,
                Label : '{i18n>Description}',
            },
            {
                $Type : 'UI.DataField',
                Value : parameters,
                Label : '{i18n>Parameters}',
            },
        ],
    },
);

annotate service.PromptTexts with {
    content @UI.MultiLineText : true
};

annotate service.FunctionCall with {
    parameters @UI.MultiLineText : true
};


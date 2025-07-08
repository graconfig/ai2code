using ConfigService as service from '../../srv/orchestration-config-service';
using from '../../db/orchestration-config-model';

annotate service.TaskTypes with @odata.draft.enabled;

annotate service.TaskTypes with @(
    UI.FieldGroup #GeneratedGroup: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Label: '{i18n>TaskTypeName}',
                Value: name,
            },
            {
                $Type: 'UI.DataField',
                Label: '{i18n>TaskTypeDescription}',
                Value: description,
            },
            {
                $Type: 'UI.DataField',
                Label: '{i18n>AutoRun}',
                Value: autoRun,
            },
            {
                $Type: 'UI.DataField',
                Label: '{i18n>IsMain}',
                Value: isMain,
            },
        ],
    },
    UI.Facets                    : [
        {
            $Type : 'UI.ReferenceFacet',
            ID    : 'GeneratedFacet1',
            Label : '{i18n>TaskTypeInfo}',
            Target: '@UI.FieldGroup#GeneratedGroup',
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : '{i18n>BotTypes}',
            ID    : 'BotTypes',
            Target: 'botTypes/@UI.PresentationVariant#BotTypes',
        },
    ],
    UI.LineItem                  : [
        {
            $Type: 'UI.DataField',
            Label: '{i18n>TaskTypeName}',
            Value: name,
        },
        {
            $Type: 'UI.DataField',
            Label: '{i18n>TaskTypeDescription}',
            Value: description,
        },
        {
            $Type: 'UI.DataField',
            Label: '{i18n>AutoRun}',
            Value: autoRun,
        },
        {
            $Type: 'UI.DataField',
            Label: '{i18n>IsMain}',
            Value: isMain,
        },
    ],
    UI.HeaderInfo                : {
        Title         : {
            $Type: 'UI.DataField',
            Value: name,
        },
        TypeName      : '',
        TypeNamePlural: '',
        Description   : {
            $Type: 'UI.DataField',
            Value: description,
        },
    },
);

annotate service.BotTypes with @(
    UI.LineItem #BotTypes : [
        {
            $Type: 'UI.DataField',
            Value: sequence,
            Label: '{i18n>Sequence}',
        },
        {
            $Type: 'UI.DataField',
            Value: name,
            Label: '{i18n>Name}',
        },
        {
            $Type: 'UI.DataField',
            Value: description,
            Label: '{i18n>Description}',
        },
        {
            $Type: 'UI.DataField',
            Value: functionType_code,
            Label: '{i18n>FunctionType}',
        },
        {
            $Type: 'UI.DataField',
            Value: autoRun,
            Label: '{i18n>AutoRun}',
        },
        {
            $Type: 'UI.DataField',
            Value: executionCondition,
            Label: '{i18n>ExecutionCondition}',
        },
        {
            $Type: 'UI.DataField',
            Value: model_ID,
            Label: '{i18n>Model}',
        },
        {
            $Type: 'UI.DataField',
            Value: outputContextPath,
            Label: '{i18n>OutputContextpath}',
        },
        {
            $Type: 'UI.DataField',
            Value: contextType_code,
            Label: '{i18n>ContextType}',
        },
        {
            $Type: 'UI.DataField',
            Value: isRAGEnabled,
            Label: '{i18n>IsRagenabled}',
        },
        {
            $Type: 'UI.DataField',
            Value: ragClass,
            Label: '{i18n>RagClass}',
        },
        {
            $Type: 'UI.DataField',
            Value: ragSource,
            Label: '{i18n>RagSource}',
        },
        {
            $Type: 'UI.DataField',
            Value: ragTopK,
            Label: '{i18n>RagTopk}',
        },
        {
            $Type: 'UI.DataField',
            Value: implementationClass,
            Label: '{i18n>ImplementationClass}',
        },
    ],
    UI.PresentationVariant #BotTypes : {
        $Type : 'UI.PresentationVariantType',
        Visualizations : [
            '@UI.LineItem#BotTypes'
        ],
        SortOrder : [
            {
                $Type : 'Common.SortOrderType',
                Property : sequence,
                Descending : false
            }
        ]
    },
    UI.HeaderInfo         : {
        Title         : {
            $Type: 'UI.DataField',
            Value: name,
        },
        TypeName      : '',
        TypeNamePlural: '',
        Description   : {
            $Type: 'UI.DataField',
            Value: description,
        },
    },
    UI.Facets             : [
        {
            $Type : 'UI.CollectionFacet',
            Label : 'Bot Type',
            ID    : 'BotTypeCollection',
            Facets: [
                {
                    $Type : 'UI.ReferenceFacet',
                    Label : 'General',
                    ID    : 'BotTypeGeneralFacet',
                    Target: '@UI.FieldGroup#BotTypeGeneral',
                },
                {
                    $Type : 'UI.ReferenceFacet',
                    Label : 'RAG Configuration',
                    ID    : 'BotTypeRAGFacet',
                    Target: '@UI.FieldGroup#BotTypeRAG',
                },
            ]
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : 'Prompts',
            ID    : 'Prompts',
            Target: 'prompts/@UI.LineItem#Prompts',
        }
    ],
    UI.FieldGroup #BotTypeGeneral: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Value: name,
                Label: '{i18n>Name}',
            },
            {
                $Type: 'UI.DataField',
                Value: description,
                Label: '{i18n>Description}',
            },
            {
                $Type: 'UI.DataField',
                Value: sequence,
                Label: '{i18n>Sequence}',
            },
            {
                $Type: 'UI.DataField',
                Value: autoRun,
                Label: '{i18n>AutoRun}',
            },
            {
                $Type: 'UI.DataField',
                Value: functionType_code,
                Label: '{i18n>FunctionType}',
            },
            {
                $Type: 'UI.DataField',
                Value: model_ID,
                Label: '{i18n>Model}',
            },
            {
                $Type: 'UI.DataField',
                Value: implementationClass,
                Label: '{i18n>ImplementationClass}',
            },
            {
                $Type: 'UI.DataField',
                Value: contextType_code,
                Label: '{i18n>ContextType}',
            },
            {
                $Type: 'UI.DataField',
                Value: outputContextPath,
                Label: '{i18n>OutputContextpath}',
            },
            {
                $Type: 'UI.DataField',
                Value: executionCondition,
                Label: '{i18n>ExecutionCondition}',
            },
        ],
    },
    UI.FieldGroup #BotTypeRAG   : {
        Data: [
            {
                $Type: 'UI.DataField',
                Value: isRAGEnabled,
                Label: '{i18n>IsRagenabled}',
            },
            {
                $Type: 'UI.DataField',
                Value: ragClass,
                Label: '{i18n>RagClass}',
            },
            {
                $Type: 'UI.DataField',
                Value: ragSource,
                Label: '{i18n>RagSource}',
            },
            {
                $Type: 'UI.DataField',
                Value: ragTopK,
                Label: '{i18n>RagTopk}',
            },
            {
                $Type: 'UI.DataField',
                Value: ragThreshold,
                Label: '{i18n>RagThreshold}',
            },
            {
                $Type: 'UI.DataField',
                Value: ragParameter,
                Label: '{i18n>RagParameter}',
            },
            {
                $Type: 'UI.DataField',
                Value: ragOutputContextPath,
                Label: '{i18n>RagOutputContextPath}',
            },
        ]
    }
);

annotate service.BotTypes with {
    implementationClass @(
        Common.Text                    : implementationClass,
        Common.ValueList               : {
            $Type         : 'Common.ValueListType',
            CollectionPath: 'BotExecutionClass',
            Parameters    : [
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: implementationClass,
                    ValueListProperty: 'Name',
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'Description',
                },
            ],
        },
        Common.ValueListWithFixedValues: false,
    )
};

annotate service.BotTypes with {
    ragClass @(
        Common.Text                    : ragClass,
        Common.ValueList               : {
            $Type         : 'Common.ValueListType',
            CollectionPath: 'RAGExtractorClass',
            Parameters    : [
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: ragClass,
                    ValueListProperty: 'Name',
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'Description',
                },
            ],
        },
        Common.ValueListWithFixedValues: false,
    )
};


annotate service.BotTypes with {
    functionType @(
        Common.Text                    : functionType.descr,
        Common.ValueList               : {
            $Type         : 'Common.ValueListType',
            CollectionPath: 'BotFunctionTypes',
            Parameters    : [
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: functionType_code,
                    ValueListProperty: 'code',
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'name',
                },
            ],
        },
        Common.ValueListWithFixedValues: false,
    )
};

annotate service.BotFunctionTypes with {
    code @Common.Text: descr
};

annotate service.BotTypes with {
    model @(
        Common.Text                    : model.modelName,
        Common.ValueList               : {
            $Type         : 'Common.ValueListType',
            CollectionPath: 'ModelConfigs',
            Parameters    : [
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: model_ID,
                    ValueListProperty: 'ID',
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'name',
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'provider',
                },
            ],
        },
        Common.ValueListWithFixedValues: false,
    )
};

annotate service.ModelConfigs with {
    ID @Common.Text: modelName
};

annotate service.BotTypes with {
    contextType @(
        Common.Text                    : contextType.descr,
        Common.ValueList               : {
            $Type         : 'Common.ValueListType',
            CollectionPath: 'ContextTypes',
            Parameters    : [
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: contextType_code,
                    ValueListProperty: 'code',
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'name',
                },
            ],
        },
        Common.ValueListWithFixedValues: false,
    )
};

annotate service.ContextTypes with {
    code @Common.Text: descr
};

annotate service.PromptTexts with {
    lang @(
        Common.Text                    : lang.descr,
        Common.ValueList               : {
            $Type         : 'Common.ValueListType',
            CollectionPath: 'Languages',
            Parameters    : [
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: lang_code,
                    ValueListProperty: 'code',
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'name',
                },
            ],
        },
        Common.ValueListWithFixedValues: false,
    )
};


annotate service.PromptTexts with @(
    UI.LineItem #Prompts     : [
        {
            $Type: 'UI.DataField',
            Value: lang_code,
            Label: '{i18n>Language}',
        },
        {
            $Type: 'UI.DataField',
            Value: name,
            Label: '{i18n>Name}',
        }
    // {
    //     $Type : 'UI.DataField',
    //     Value : content,
    //     Label : '{i18n>Content}',
    // },
    ],
    UI.HeaderInfo            : {
        Title         : {
            $Type: 'UI.DataField',
            Value: botType.description,
        },
        TypeName      : '',
        TypeNamePlural: '',
    },
    UI.Facets                : [{
        $Type : 'UI.ReferenceFacet',
        Label : 'Prompt Info',
        ID    : 'PromptInfo',
        Target: '@UI.FieldGroup#PromptInfo',
    }, ],
    UI.FieldGroup #PromptInfo: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                $Type: 'UI.DataField',
                Value: lang_code,
                Label: '{i18n>Language}',
            },
            {
                $Type: 'UI.DataField',
                Value: name,
                Label: '{i18n>Name}',

            }
        // {
        //     $Type : 'UI.DataField',
        //     Value : content,
        //     Label : '{i18n>Content}',
        // },
        ],
    },
);


annotate service.PromptTexts with {
    content @UI.MultiLineText: true
};


using ConfigService as service from '../../srv/orchestration-config-service';

annotate service.ModelConfigs with @odata.draft.enabled;

annotate service.ModelConfigs with @(
    UI.FieldGroup #GeneratedGroup : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Label : 'Name',
                Value : name,
            },
            {
                $Type : 'UI.DataField',
                Label : 'Model Provider',
                Value : provider,
            },
            {
                $Type : 'UI.DataField',
                Label : 'Model Name',
                Value : modelName,
            }
        ],
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'GeneratedFacet1',
            Label : 'Model Information',
            Target : '@UI.FieldGroup#GeneratedGroup',
        },
    ],
    UI.LineItem : [
        {
            $Type : 'UI.DataField',
            Label : '{i18n>ID}',
            Value : ID,
        },
        {
            $Type : 'UI.DataField',
            Label : '{i18n>TaskTypeName}',
            Value : name,
        },
        {
            $Type : 'UI.DataField',
            Label : '{i18n>ModelProvider}',
            Value : provider,
        },
        {
            $Type : 'UI.DataField',
            Label : '{i18n>ModelName}',
            Value : modelName,
        },
        {
            $Type : 'UI.DataField',
            Label : '{i18n>Parameters}',
            Value : parameters,
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
            Value : modelName,
        },
    },
);

annotate service.ModelConfigs with {
    parameters @UI.MultiLineText : true;
    ID @(
        Core.Computed : true,
        Common.Label : '{i18n>ID}',
        UI.HiddenFilter : true
    )
};


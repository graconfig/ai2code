using MainService as service from '../../srv/orchestration-service';

annotate service.CDSViews with @odata.draft.enabled;

annotate service.CDSViews with @(
    UI.HeaderInfo:{
        TypeName:'Views',
        TypeNamePlural:'Views'
    },

    UI.FieldGroup #GeneratedGroup : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Value : viewCategory,
            },
            {
                $Type : 'UI.DataField',
                Value : viewName,
            },
            {
                $Type : 'UI.DataField',
                Value : viewDesc,
            },
            {
                $Type : 'UI.DataField',
                Value : isActive,
            },
        ],
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'GeneratedFacet1',
            Label : 'General Information',
            Target : '@UI.FieldGroup#GeneratedGroup',
        },
    ],
    UI.LineItem : [
        {
            $Type : 'UI.DataField',
            Value : viewName,
        },
        {
            $Type : 'UI.DataField',
            Value : viewCategory,
        },
        {
            $Type : 'UI.DataField',
            Value : viewDesc,
        },
        {
            $Type : 'UI.DataField',
            Value : isActive,
        },
    ],
);


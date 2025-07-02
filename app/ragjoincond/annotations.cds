using MainService as service from '../../srv/orchestration-service';

annotate service.RagJoinCond with @odata.draft.enabled;
annotate service.RagJoinCond with @(
    UI.FieldGroup #GeneratedGroup : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Label : 'tableFirst',
                Value : tableFirst,
            },
            {
                $Type : 'UI.DataField',
                Label : 'tableSecond',
                Value : tableSecond,
            }
        ],
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'GeneratedFacet1',
            Label : 'Tables',
            Target : '@UI.FieldGroup#GeneratedGroup',
        },
    ],
    UI.LineItem : [
        {
            $Type : 'UI.DataField',
            Label : 'tableFirst',
            Value : tableFirst,
        },
        {
            $Type : 'UI.DataField',
            Label : 'tableSecond',
            Value : tableSecond,
        },
        {
            $Type : 'UI.DataField',
            Label : 'tableJoin',
            Value : tableJoin,
        },
    ],
);


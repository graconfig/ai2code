using MainService as service from '../../srv/orchestration-service';

annotate service.CDSViewFiles with @(
    UI.FieldGroup #GeneratedGroup : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Label : 'category',
                Value : category,
            },
            {
                $Type : 'UI.DataField',
                Label : 'fileName',
                Value : fileName,
            },
            {
                $Type : 'UI.DataField',
                Label : 'size',
                Value : size,
            },
            {
                $Type : 'UI.DataField',
                Label : 'mediaType',
                Value : mediaType,
            },
            {
                $Type : 'UI.DataField',
                Label : 'isGenerated',
                Value : isGenerated,
            },
            {
                $Type : 'UI.DataField',
                Label : 'fileContent',
                Value : fileContent,
            },
        ],
    },
    UI.Facets : [
        {
            $Type : 'UI.CollectionFacet',
            Label : 'File Overview',
            ID : 'FileOverview',
            Facets : [
                {
                    $Type : 'UI.ReferenceFacet',
                    Label : 'File Details',
                    ID : 'FileDetails',
                    Target : '@UI.FieldGroup#FileDetails',
                },
            ],
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : 'View Fields',
            ID : 'ViewFields',
            Target : 'viewFields/@UI.LineItem#ViewFields',
        },
    ],
    UI.LineItem : [
        {
            $Type : 'UI.DataField',
            Label : 'File Name',
            Value : fileName,
        },
        {
            $Type : 'UI.DataField',
            Label : 'File Size',
            Value : size,
        },
        {
            $Type : 'UI.DataField',
            Label : 'Media Type',
            Value : mediaType,
        }
    ],
    UI.FieldGroup #FileDetails : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Value : fileName,
                Label : 'file Name',
            },
            {
                $Type : 'UI.DataField',
                Value : size,
                Label : 'File Size',
            },
            {
                $Type : 'UI.DataField',
                Value : mediaType,
                Label : 'Media Type',
            },
            {
                $Type : 'UI.DataField',
                Value : fileContent,
                Label : 'File Content',
            }
        ],
    },
);

annotate service.Viewfields with @(
    UI.LineItem #ViewFields : [
        {
            $Type : 'UI.DataField',
            Value : langu,
            Label : 'Language',
        },
        {
            $Type : 'UI.DataField',
            Value : tableName,
            Label : 'View Name',
        },
        {
            $Type : 'UI.DataField',
            Value : tableDesc,
            Label : 'View Desc',
        },
        {
            $Type : 'UI.DataField',
            Value : content,
            Label : 'Fields',
        }
    ]
);




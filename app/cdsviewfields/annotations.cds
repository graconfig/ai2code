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
        },
        {
            $Type : 'UI.DataField',
            Label : 'isGenerateEmbedding',
            Value : isGenerated,
        },
                {
            $Type : 'UI.DataFieldForAction',
            Action: 'MainService.generateEmbeddings',
            Label : 'Generate Embeddings'
        },
        {
            $Type : 'UI.DataFieldForAction',
            Action: 'MainService.deleteEmbeddings',
            Label : 'Delete Embeddings'
        },
    ],
    UI.FieldGroup #FileDetails : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Value : fileName,
                Label : '{i18n>FileName}',
            },
            {
                $Type : 'UI.DataField',
                Value : size,
                Label : '{i18n>FileSize}',
            },
            {
                $Type : 'UI.DataField',
                Value : mediaType,
                Label : '{i18n>MediaType}',
            },
            {
                $Type : 'UI.DataField',
                Value : fileContent,
                Label : '{i18n>FileContent}',
            },
            {
                $Type : 'UI.DataField',
                Value : isGenerated,
                Label : '{i18n>isGenerateEmbedding}',
            },
        ],
    },
)actions {
    @Common.SideEffects: {TargetProperties: ['in/isGenerated'], }
    generateEmbeddings;
    @Common.SideEffects: {TargetProperties: ['in/isGenerated'], }
    deleteEmbeddings;
};

annotate service.Viewfields with @(
    UI.LineItem #ViewFields : [
        {
            $Type : 'UI.DataField',
            Value : langu,
            Label : '{i18n>Language}',
        },
        {
            $Type : 'UI.DataField',
            Value : tableName,
            Label : '{i18n>ViewName}',
        },
        {
            $Type : 'UI.DataField',
            Value : tableDesc,
            Label : '{i18n>ViewDesc}',
        },
        {
            $Type : 'UI.DataField',
            Value : content,
            Label : '{i18n>Fields}',
        },
        {
            $Type : 'UI.DataField',
            Value : isGeneratedEmbedding,
            Label : '{i18n>Isgenerateembedding}',
        },
    ]
);




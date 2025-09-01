using {batch} from '../db/springbatch-model.cds';


service BatchService {
    entity JobInstance      as projection on batch.job_instance;
    entity JobExecution     as projection on batch.job_execution;
    entity JobExecutionContext as projection on batch.job_execution_context;
    entity JobExecutionParams  as projection on batch.job_execution_params;
    entity StepExecution        as projection on batch.step_execution;
    entity StepExecutionContext as projection on batch.step_execution_context;
}


annotate BatchService.JobInstance with @UI: {
    Facets        : [
        {
            $Type : 'UI.ReferenceFacet',
            ID    : 'idIdentification',
            Label : 'Basic',
            Target: '@UI.Identification'
        },

        {
            ID    : 'JobExecution',
            Target: 'to_Executions/@UI.LineItem',
            $Type : 'UI.ReferenceFacet',
            Label : 'Job Executions'
        // position       : 20
        }
    ],
    Identification: [
        {Value: JOB_INSTANCE_ID},
        {Value: JOB_NAME}
    ],

    LineItem      : [
        {Value: JOB_INSTANCE_ID},
        {Value: JOB_NAME}
    ]
};


annotate BatchService.JobExecution with @UI: {
    Facets               : [
        {
            $Type : 'UI.ReferenceFacet',
            ID    : 'idIdentification',
            Label : 'Basic',
            Target: '@UI.Identification'
        },
        {
            ID    : 'Function',
            Target: '@UI.FieldGroup#Execution',
            $Type : 'UI.ReferenceFacet',
            Label : 'Execution'
        },
        {
            ID    : 'Status',
            Target: '@UI.FieldGroup#Status',
            $Type : 'UI.ReferenceFacet',
            Label : 'Status'
        },
        {
            ID    : 'Context',
            Target: 'to_Context/@UI.Identification',
            $Type : 'UI.ReferenceFacet',
            Label : 'Context'
        },
        {
            ID    : 'Parameters',
            Target: 'to_Params/@UI.LineItem',
            $Type : 'UI.ReferenceFacet',
            Label : 'Parameters'
        },
        {
            ID    : 'JobExecution',
            Target: 'to_StepExecutions/@UI.LineItem',
            $Type : 'UI.ReferenceFacet',
            Label : 'Steps'
        // position       : 20
        }
    ],
    Identification       : [
        // {Value: JOB_INSTANCE_ID},
        {Value: JOB_EXECUTION_ID},
        {Value: CREATE_TIME}
    ],
    FieldGroup #Execution: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: START_TIME},
            {Value: END_TIME},
        ]
    },
    FieldGroup #Status   : {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: STATUS},
            {Value: EXIT_CODE},
            {Value: EXIT_MESSAGE}
        ]
    },

    LineItem             : [
        // {Value: JOB_INSTANCE_ID},
        {Value: JOB_EXECUTION_ID},
        {Value: CREATE_TIME},
        {Value: START_TIME},
        {Value: END_TIME},
        {Value: STATUS},
        {Value: EXIT_CODE},
        {Value: EXIT_MESSAGE}
    ]
};


annotate BatchService.JobExecutionContext with @UI: {
    LineItem      : [
        {Value: SHORT_CONTEXT},
        {Value: SERIALIZED_CONTEXT}
    ],
    Identification: [
        {Value: SHORT_CONTEXT},
        {Value: SERIALIZED_CONTEXT}
    ]
};

annotate BatchService.JobExecutionParams with @UI: {LineItem: [
    {Value: PARAMETER_NAME},
    {Value: PARAMETER_TYPE},
    {Value: PARAMETER_VALUE}
]};


annotate BatchService.StepExecution with @UI: {
    Facets               : [
        {
            $Type : 'UI.ReferenceFacet',
            ID    : 'idIdentification',
            Label : 'Basic',
            Target: '@UI.Identification'
        },
        {
            ID    : 'Function',
            Target: '@UI.FieldGroup#Execution',
            $Type : 'UI.ReferenceFacet',
            Label : 'Execution'
        },
        {
            ID    : 'Status',
            Target: '@UI.FieldGroup#Status',
            $Type : 'UI.ReferenceFacet',
            Label : 'Status'
        },
        {
            ID    : 'DataCount',
            Target: '@UI.FieldGroup#DataCount',
            $Type : 'UI.ReferenceFacet',
            Label : 'Data Count'
        },
        {
            ID    : 'Parameters',
            Target: 'to_Context/@UI.Identification',
            $Type : 'UI.ReferenceFacet',
            Label : 'Parameters'
        }

    // {
    //     ID    : 'JobExecution',
    //     Target: 'to_Executions/@UI.LineItem',
    //     $Type : 'UI.ReferenceFacet',
    //     Label : 'Job Executions'
    // // position       : 20
    // }
    ],
    Identification       : [
        // {Value: },
        // {Value: JOB_EXECUTION_ID},
        {Value: STEP_EXECUTION_ID},
        {Value: STEP_NAME},
        {Value: CREATE_TIME}
    ],
    FieldGroup #Execution: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: START_TIME},
            {Value: END_TIME},
        ]
    },
    FieldGroup #Status   : {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: STATUS},
            {Value: EXIT_CODE},
            {Value: EXIT_MESSAGE}
        ]
    },
    FieldGroup #DataCount: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: COMMIT_COUNT},
            {Value: READ_COUNT},
            {Value: FILTER_COUNT},
            {Value: WRITE_COUNT},
            {Value: READ_SKIP_COUNT},
            {Value: WRITE_SKIP_COUNT},
            {Value: PROCESS_SKIP_COUNT},
            {Value: ROLLBACK_COUNT}
        ]
    },

    LineItem             : [
        // {Value: JOB_EXECUTION_ID},
        {Value: STEP_EXECUTION_ID},
        {Value: STEP_NAME},
        {Value: CREATE_TIME},
        {Value: START_TIME},
        {Value: END_TIME},
        {Value: STATUS},
        {Value: EXIT_CODE},
        {Value: EXIT_MESSAGE},
        {Value: READ_COUNT},
        {Value: WRITE_COUNT}
    // {Value: to_Context.SHORT_CONTEXT}
    ]
};


annotate BatchService.StepExecutionContext with @UI: {
    LineItem      : [
        {Value: SHORT_CONTEXT},
        {Value: SERIALIZED_CONTEXT}
    ],
    Identification: [
        {Value: SHORT_CONTEXT},
        {Value: SERIALIZED_CONTEXT}
    ]
};

package customer.ai2code.service.impl.execution;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.ObjectUtils.Null;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataResponseException;

// import cds.gen.zsrvd_genddls.ZcGenddlsPAutoActiveCDSContext;
// import cds.gen.zsrvd_genddls.ZcGenddlsP_;
// import cds.gen.zsrvd_genddls.ZsgenDdlsSourceList;
// import cds.gen.zsrvd_genddls.ZtgenddlsL;
// import cds.gen.zsrvd_gensrvd.*;
// import cds.gen.zsrvd_gensrvd.V0001;
import cds.gen.com.sap.gateway.srvd.zsrvd_gentabl.v0001.V0001;
import cds.gen.com.sap.gateway.srvd.zsrvd_gentabl.v0001.ZaGenResponseLog;
import cds.gen.com.sap.gateway.srvd.zsrvd_gentabl.v0001.ZcGentablT;
import cds.gen.com.sap.gateway.srvd.zsrvd_gentabl.v0001.ZcGentablTActiveTableContext;
import cds.gen.com.sap.gateway.srvd.zsrvd_gentabl.v0001.ZcGentablTAutoActiveTableContext;
import cds.gen.com.sap.gateway.srvd.zsrvd_gentabl.v0001.ZcGentablT_;
import cds.gen.com.sap.gateway.srvd.zsrvd_gentabl.v0001.ZsgenTablFieldList;
import cds.gen.mainservice.ContextNodes;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.impl.TaskBotCacheManager;

@BotExecutor(name = "Call Gen Table Remote Odata", description = "Implementation for Call Gen Table Odata", version = "1.0", enabled = true)
public class CreateGenTableOdataBotExecution implements BotExecution {
    // private final GenericCqnService genericCqnService;
    private final ContextService contextService;
    private final V0001 zsrvdGentabl;
    private final ObjectMapper objectMapper;
    private final TaskBotCacheManager taskBotCacheManager;

    public CreateGenTableOdataBotExecution(ContextService contextService,
            V0001 zsrvdGentabl, ObjectMapper objectMapper, TaskBotCacheManager taskBotCacheManager) {
        // 默认构造函数
        // this.genericCqnService = genericCqnService;
        this.contextService = contextService;
        this.zsrvdGentabl = zsrvdGentabl;
        this.objectMapper = objectMapper;
        this.taskBotCacheManager = taskBotCacheManager;
    }

    // String DestinationName,String Request,String Response,，List<ContextNodes>
    @ExecuteMethod
    public String execute(
            @ExecuteParameter(name = "botInstanceId", description = "Bot Instance") String botInstanceId,
            @ExecuteParameter(description = "zcGentablTAutoActiveTableContext", name = "Auto Active Table Context") ZcGentablTAutoActiveTableContext AutoActiveContext) {
    
        ZcGentablTAutoActiveTableContext zcGentablTAutoActiveTableContext = ZcGentablTAutoActiveTableContext.create();
        CqnSelect select = (CqnSelect) Select.from(ZcGentablT_.CDS_NAME);
        zcGentablTAutoActiveTableContext.setCqn(select);
        zcGentablTAutoActiveTableContext.setTabname(AutoActiveContext.getTabname());
        zcGentablTAutoActiveTableContext.setTabdesc(AutoActiveContext.getTabdesc());
        zcGentablTAutoActiveTableContext.setContflag(AutoActiveContext.getContflag());
        zcGentablTAutoActiveTableContext.setMainflag(AutoActiveContext.getMainflag());
        zcGentablTAutoActiveTableContext.setDevclass(AutoActiveContext.getDevclass());
        zcGentablTAutoActiveTableContext.setTrkorr(AutoActiveContext.getTrkorr());
        zcGentablTAutoActiveTableContext.setStatus(AutoActiveContext.getStatus());
        zcGentablTAutoActiveTableContext.setReference(AutoActiveContext.getReference());


        Collection<ZsgenTablFieldList> Sources = new ArrayList<>();
        zcGentablTAutoActiveTableContext.getFields().forEach(
                source -> {
                    ZsgenTablFieldList sourceNew = ZsgenTablFieldList.create();
                    sourceNew.setFieldname(source.getFieldname());
                    sourceNew.setFieldpos(source.getFieldpos());
                    sourceNew.setKeyflag(source.getKeyflag());
                    sourceNew.setDataelement(source.getDataelement());
                    sourceNew.setFieldname(source.getFieldname());
                    sourceNew.setDatatype(source.getDatatype());
                    sourceNew.setDecimals(source.getDecimals());
                    sourceNew.setDescription(source.getDescription());
                    sourceNew.setReftab(source.getReftab());
                    sourceNew.setReffield(source.getReffield());
                    sourceNew.setGroupname(source.getGroupname());
                    Sources.add(sourceNew);
                });

        zcGentablTAutoActiveTableContext.setFields(Sources);
        String Result = "";// 返回结果
        // 打印CDS
        // System.out.println("CdsSource=" +
        // AutoActiveCDSContextNew.getSource().toString());
        try {
            System.out.println("TableFields=" + objectMapper.writeValueAsString(zcGentablTAutoActiveTableContext.getFields()));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // 调用action
        try {
            zsrvdGentabl.emit(zcGentablTAutoActiveTableContext);
        } catch (ServiceException e) {
            ODataResponseException odataexce = (ODataResponseException) e.getCause().getCause();
            int statusCode = odataexce.getHttpCode();
            String errorMessage = (String) odataexce.getHttpBody().getOrElse("");
            System.err.println("statusCode=" + statusCode);
            System.err.println("errorMessage=" + errorMessage);
            Result = errorMessage;
            return Result;
        }

        Collection<ZaGenResponseLog> Results = new ArrayList<>();// 返回结果
        Results = zcGentablTAutoActiveTableContext.getResult();
        if (Results == null) {
            Result = "Results is null";
            return Result;
        }
        try {
            Result = objectMapper.writeValueAsString(Results);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // 打印返回消息
        for (ZaGenResponseLog item : Results) {
            String type = item.getType();
            String message = item.getMessage();
            String REFERENCE = item.getReference();

            System.out.println("Result=" + item.toString());
            System.out.println("type=" + type);
            System.out.println("message=" + message);
            System.out.println("REFERENCE=" + REFERENCE);

            // contextService.upsertContext(botInstanceId, REFERENCE, , type)
            try {
                contextService.updateAdditionInfo(taskBotCacheManager.getCachedBot(botInstanceId), REFERENCE, objectMapper.writeValueAsString(item));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                contextService.updateAdditionInfo(taskBotCacheManager.getCachedBot(botInstanceId), REFERENCE, e.getMessage());
            }

        }
        return Result;

    }

    // private Object safeGetStringFromProxy(Object valueFromGetter, ZcGenddlsPAutoActiveCDSContext context,
    //         String fieldName) {
    //     // 1. 首先尝试 getter 方法

    //     // 2. 如果 getter 返回 null，尝试从 toString() 解析
    //     if (valueFromGetter == null) {
    //         System.out.println(fieldName + " is null from getter, trying to parse from toString()");
    //         // String toStringValue = contextNode.toString();
    //         return context.get(fieldName);
    //     }
    //     return valueFromGetter;

    //     // return safeGetString(value, fieldName);
    // }

}

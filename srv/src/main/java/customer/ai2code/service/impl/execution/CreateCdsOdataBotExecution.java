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

import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.V0001;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.ZcGenddlsPAutoActiveCDSContext;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.ZcGenddlsP_;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.ZsgenDdlsSourceList;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.ZtgenddlsL;
import cds.gen.mainservice.ContextNodes;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.execution.BotExecution;

@BotExecutor(name = "Call Remote Odata", description = "Implementation for Call S4/HANA OP Odata", version = "1.0", enabled = true)
public class CreateCdsOdataBotExecution implements BotExecution {
    // private final GenericCqnService genericCqnService;
    private final ContextService contextService;
    private final V0001 zsrvdGenddls;
    private final ObjectMapper objectMapper;

    // public CreateCdsOdataBotExecution(GenericCqnService genericCqnService,
    // ZsrvdGenddls zsrvdGenddls) {
    // public CreateCdsOdataBotExecution(V0001 zsrvdGenddls) {
    public CreateCdsOdataBotExecution(ContextService contextService,
            V0001 zsrvdGenddls, ObjectMapper objectMapper) {
        // 默认构造函数
        // this.genericCqnService = genericCqnService;
        this.contextService = contextService;
        this.zsrvdGenddls = zsrvdGenddls;
        this.objectMapper = objectMapper;
    }

    // String DestinationName,String Request,String Response,，List<ContextNodes>
    @ExecuteMethod
    public String execute(
            @ExecuteParameter(name = "botInstanceId", description = "Bot Instance") String botInstanceId,
            @ExecuteParameter(description = "ZcGenddlsPAutoActiveCDSContext", name = "ZcGenddlsPAutoActiveCDSContext") ZcGenddlsPAutoActiveCDSContext AutoActiveCDSContext) {
        // zsrvdGenddls.run();
        ZcGenddlsPAutoActiveCDSContext AutoActiveCDSContextNew = ZcGenddlsPAutoActiveCDSContext.create();
        CqnSelect select = (CqnSelect) Select.from(ZcGenddlsP_.CDS_NAME);
        AutoActiveCDSContextNew.setCqn(select);
        AutoActiveCDSContextNew.setProjname(AutoActiveCDSContext.getProjname());
        AutoActiveCDSContextNew.setProjdesc(AutoActiveCDSContext.getProjdesc());
        AutoActiveCDSContextNew.setWithadditionalsave(AutoActiveCDSContext.getWithadditionalsave());
        AutoActiveCDSContextNew.setWithdraft(AutoActiveCDSContext.getWithdraft());
        // AutoActiveCDSContextNew.setSource(AutoActiveCDSContext.getSource());
        Collection<ZsgenDdlsSourceList> Sources = new ArrayList<>();
        AutoActiveCDSContext.getSource().forEach(
                source -> {
                    ZsgenDdlsSourceList sourceNew = ZsgenDdlsSourceList.create();
                    // sourceNew.setViewname(String.valueOf(source.get("viewname")));
                    sourceNew.setViewname(source.getViewname());
                    sourceNew.setViewdesc(source.getViewdesc());
                    sourceNew.setReference(source.getReference());
                    sourceNew.setSourcecode(source.getSourcecode());
                    // sourceNew.setReference(String.valueOf(source.get("reference")));
                    // sourceNew.setSourcecode(String.valueOf(source.get("sourcecode")));

                    Sources.add(sourceNew);
                });
        // .setSourcecode(source.getSourcecode()));
        // ZtgenddlsL sourceItem = ZtgenddlsL.create();
        // sourceItem.setViewname(source.getViewname());
        // sourceItem.setViewdesc(source.getViewdesc());
        // sourceItem.setReference(source.getReference());
        // sourceItem.setSourcecode(source.getSourcecode());
        // AutoActiveCDSContextNew.addSource(sourceItem);
        // AutoActiveCDSContextNew.set
        // }
        // );
        AutoActiveCDSContextNew.setSource(Sources);

        AutoActiveCDSContextNew.setTrkorr(AutoActiveCDSContext.getTrkorr());
        AutoActiveCDSContextNew.setDevclass(AutoActiveCDSContext.getDevclass());

        // AutoActiveCDSContext.setCqn(select);
        // AutoActiveCDSContext.setProjname("TESTO703_01");
        // AutoActiveCDSContext.setProjdesc("test report222");
        // AutoActiveCDSContext.setWithadditionalsave(false);
        // AutoActiveCDSContext.setWithdraft(false);
        // AutoActiveCDSContext.setTrkorr("DM2K900068");
        // AutoActiveCDSContext.setDevclass("ZAIREPORT");
        String Result = "";// 返回结果
        // 打印CDS
        // System.out.println("CdsSource=" +
        // AutoActiveCDSContextNew.getSource().toString());
        try {
            System.out.println("CdsSource=" + objectMapper.writeValueAsString(AutoActiveCDSContextNew.getSource()));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // 调用action
        try {
            zsrvdGenddls.emit(AutoActiveCDSContext);
        } catch (ServiceException e) {
            ODataResponseException odataexce = (ODataResponseException) e.getCause().getCause();
            int statusCode = odataexce.getHttpCode();
            String errorMessage = (String) odataexce.getHttpBody().getOrElse("");
            System.err.println("statusCode=" + statusCode);
            System.err.println("errorMessage=" + errorMessage);
            Result = errorMessage;
            return Result;
        }

        Collection<ZtgenddlsL> Results = new ArrayList<>();// 返回结果
        Results = AutoActiveCDSContextNew.getResult();
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
        for (ZtgenddlsL item : Results) {
            String type = item.getType();
            String message = item.getMessage();
            String REFERENCE = item.getReference();
            System.out.println("Result=" + item.toString());

            System.out.println("type=" + type);
            System.out.println("message=" + message);
            System.out.println("REFERENCE=" + REFERENCE);

            // contextService.upsertContext(botInstanceId, REFERENCE, , type)
            try {
                contextService.updateAdditionInfo(botInstanceId, REFERENCE, objectMapper.writeValueAsString(item));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                contextService.updateAdditionInfo(botInstanceId, REFERENCE, e.getMessage());
            }

        }
        return Result;

    }

    private Object safeGetStringFromProxy(Object valueFromGetter, ZcGenddlsPAutoActiveCDSContext context,
            String fieldName) {
        // 1. 首先尝试 getter 方法

        // 2. 如果 getter 返回 null，尝试从 toString() 解析
        if (valueFromGetter == null) {
            System.out.println(fieldName + " is null from getter, trying to parse from toString()");
            // String toStringValue = contextNode.toString();
            return context.get(fieldName);
        }
        return valueFromGetter;

        // return safeGetString(value, fieldName);
    }

}

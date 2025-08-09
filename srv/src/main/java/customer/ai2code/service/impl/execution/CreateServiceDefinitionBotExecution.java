package customer.ai2code.service.impl.execution;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.request.ParameterInfo;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataResponseException;

import cds.gen.com.sap.gateway.srvd.zsrvd_gensrvd.v0001.V0001;
import cds.gen.com.sap.gateway.srvd.zsrvd_gensrvd.v0001.ZcGensrvdT;
import cds.gen.com.sap.gateway.srvd.zsrvd_gensrvd.v0001.ZcGensrvdTAutoActiveSRVDSRVBContext;
import cds.gen.com.sap.gateway.srvd.zsrvd_gensrvd.v0001.ZcGensrvdT_;
import cds.gen.com.sap.gateway.srvd.zsrvd_gensrvd.v0001.ZsgenSrvdExposeList;
import cds.gen.com.sap.gateway.srvd.zsrvd_gensrvd.v0001.ZtgensrvdL;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.impl.TaskBotCacheManager;

@BotExecutor(name = "Call Remote OData", description = "Implementation for Call S4/HANA OP OData", version = "1.0", enabled = true)
public class CreateServiceDefinitionBotExecution implements BotExecution {

    private final ContextService contextService;
    private final V0001 zsrvdGensrvd;
    private final ObjectMapper objectMapper;
    private final TaskBotCacheManager taskBotCacheManager;

    // 默认构造函数
    public CreateServiceDefinitionBotExecution(ContextService contextService,
            V0001 zsrvdGensrvd2, ObjectMapper objectMapper, TaskBotCacheManager taskBotCacheManager) {
        this.contextService = contextService;
        this.zsrvdGensrvd = zsrvdGensrvd2;
        this.objectMapper = objectMapper;
        this.taskBotCacheManager = taskBotCacheManager;
    }

    @ExecuteMethod
    public String execute(
            @ExecuteParameter(name = "botInstanceId", description = "Bot Instance") String botInstanceId,
            @ExecuteParameter(name = "autoActiveSRVDSRVBContext", description = "Auto Activate Context") ZcGensrvdTAutoActiveSRVDSRVBContext autoActiveSRVDSRVBContext) {
        String returnMessage = "";// 返回消息
        Collection<ZtgensrvdL> results;// OData返回结果

        // 填充报文
        ZcGensrvdTAutoActiveSRVDSRVBContext newContext = ZcGensrvdTAutoActiveSRVDSRVBContext.create();
        CqnSelect select = (CqnSelect)Select.from(ZcGensrvdT_.CDS_NAME);
        newContext.setCqn(select);

        newContext.setSrvdname(autoActiveSRVDSRVBContext.getSrvdname());
        newContext.setSrvddesc(autoActiveSRVDSRVBContext.getSrvddesc());
        newContext.setLabeltext(autoActiveSRVDSRVBContext.getLabeltext());
        newContext.setLeadingEntity(autoActiveSRVDSRVBContext.getLeadingEntity());
        newContext.setDevclass(autoActiveSRVDSRVBContext.getDevclass());
        newContext.setTrkorr(autoActiveSRVDSRVBContext.getTrkorr());
        newContext.setReference(autoActiveSRVDSRVBContext.getReference());
        newContext.setBindingType(autoActiveSRVDSRVBContext.getBindingType());
        newContext.setSrvbname(autoActiveSRVDSRVBContext.getSrvbname());
        newContext.setSrvbdesc(autoActiveSRVDSRVBContext.getSrvbdesc());

        
        Collection<ZsgenSrvdExposeList> exposeList = new ArrayList<>();
        // ZsgenSrvdExposeList expose = ZsgenSrvdExposeList.create();
        autoActiveSRVDSRVBContext.getExpose().forEach(
                expose -> {
                    ZsgenSrvdExposeList exposeNew = ZsgenSrvdExposeList.create();
                    exposeNew.setDdlsname(expose.getDdlsname());
                    // exposeNew.setDdlstext(expose.getDdlstext());
                    // exposeNew.setExposed(expose.getExposed());
                    // exposeNew.setReference(expose.getReference());
                    exposeList.add(exposeNew);
                });
        newContext.setExpose(exposeList);
        // expose.setDdlsname("ZC_RAP100_ATRA2390");
        // exposeList.add(expose);
        // autoActiveSRVDSRVBContext.setExpose(exposeList);




        // 调用action
        try {
            zsrvdGensrvd.emit(newContext);
        } catch (ServiceException e) {
            ODataResponseException odataResponseException = (ODataResponseException) e.getCause().getCause();
            int statusCode = odataResponseException.getHttpCode();
            String errorMessage = (String) odataResponseException.getHttpBody().getOrElse("");
            System.err.println("statusCode=" + statusCode);
            System.err.println("errorMessage=" + errorMessage);
            returnMessage = errorMessage;
            return returnMessage;
        }

        // 结果返回
        try {
            results = newContext.getResult();
            if (results == null) {
                returnMessage = "OData Response is null";
            } else {
                returnMessage = objectMapper.writeValueAsString(results);
                for (ZtgensrvdL result : results) {
                    System.out.println("OData Result: " + result.toJson());
                    // String type = result.getType();
                    // String message = result.getMessage();
                    String REFERENCE = result.getReference();
                    try {
                        contextService.updateAdditionInfo(taskBotCacheManager.getCachedBot(botInstanceId), REFERENCE,
                                objectMapper.writeValueAsString(result));
                    } catch (Exception e) {
                        // TODO: handle exception
                        contextService.updateAdditionInfo(taskBotCacheManager.getCachedBot(botInstanceId), REFERENCE,
                                e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnMessage;
    }

}

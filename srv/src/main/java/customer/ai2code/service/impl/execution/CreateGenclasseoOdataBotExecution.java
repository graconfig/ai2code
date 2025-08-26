package customer.ai2code.service.impl.execution;

import java.util.Collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataResponseException;
import cds.gen.com.sap.gateway.srvd.zsrvd_genclas_seo.v0001.V0001;
import cds.gen.com.sap.gateway.srvd.zsrvd_genclas_seo.v0001.ZtgenclasL;
import cds.gen.com.sap.gateway.srvd.zsrvd_genclas_seo.v0001.ZcGenclasGlobal_;
import cds.gen.com.sap.gateway.srvd.zsrvd_genclas_seo.v0001.ZcGenclasGlobalAutoActiveCLASContext;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.impl.TaskBotCacheManager;

@BotExecutor(name = "Call Genclasseo Odata", description = "Implementation for Call SRVD_GENCLAS Odata", version = "1.0", enabled = true)
public class CreateGenclasseoOdataBotExecution implements BotExecution {

    private final ContextService contextService;
    private final V0001 zsrvdGenclasseo;
    private final ObjectMapper objectMapper;
    private final TaskBotCacheManager taskBotCacheManager;

    public CreateGenclasseoOdataBotExecution(ContextService contextService,
            V0001 zsrvdGenclasseo,
            ObjectMapper objectMapper,
            TaskBotCacheManager taskBotCacheManager) {
        this.contextService = contextService;
        this.zsrvdGenclasseo = zsrvdGenclasseo;
        this.objectMapper = objectMapper;
        this.taskBotCacheManager = taskBotCacheManager;
    }

    @ExecuteMethod
    public String execute(
            @ExecuteParameter(name = "botInstanceId", description = "Bot Instance ID") String botInstanceId,
            @ExecuteParameter(name = "ZcGenclasGlobalAutoActiveCLASContext", description = "ZcGenclasGlobalAutoActiveCLASContext") ZcGenclasGlobalAutoActiveCLASContext autoActiveCLASContext) {

        // 1. 创建新的上下文并复制参数
        ZcGenclasGlobalAutoActiveCLASContext newContext = ZcGenclasGlobalAutoActiveCLASContext.create();
        CqnSelect select = (CqnSelect) Select.from(ZcGenclasGlobal_.CDS_NAME);
        newContext.setCqn(select);

        // 复制基础参数
        newContext.setClasname(autoActiveCLASContext.getClasname());
        newContext.setClasdesc(autoActiveCLASContext.getClasdesc());
        newContext.setBdefname(autoActiveCLASContext.getBdefname());
        newContext.setDevclass(autoActiveCLASContext.getDevclass());
        newContext.setTrkorr(autoActiveCLASContext.getTrkorr());
        newContext.setReference(autoActiveCLASContext.getReference());
        newContext.setSourcecode(autoActiveCLASContext.getSourcecode());

        // 2. 调用OData服务
        String result = "";
        try {
            zsrvdGenclasseo.emit(newContext);
        } catch (ServiceException e) {
            // 处理OData异常
<<<<<<< HEAD
            // ODataResponseException odataEx = (ODataResponseException)
            // e.getCause().getCause();
            // result = String.format("Error (HTTP %d): %s", odataEx.getHttpCode(),
            // odataEx.getHttpBody());
=======
            // ODataResponseException odataEx = (ODataResponseException) e.getCause().getCause();
            // result = String.format("Error (HTTP %d): %s", odataEx.getHttpCode(), odataEx.getHttpBody());
>>>>>>> origin/develop
            // return result;

            ODataResponseException odataexce = (ODataResponseException) e.getCause().getCause();
            int statusCode = odataexce.getHttpCode();
            String errorMessage = (String) odataexce.getHttpBody().getOrElse("");
            System.err.println("statusCode=" + statusCode);
            System.err.println("errorMessage=" + errorMessage);
            result = errorMessage;
            return result;
        }

        // 3. 处理返回结果
        Collection<ZtgenclasL> results = newContext.getResult();
        if (results == null) {
            return "No results returned from OData service";
        }

        try {
            result = objectMapper.writeValueAsString(results);
            // 更新上下文信息
            for (ZtgenclasL item : results) {
                contextService.updateAdditionInfo(
                        taskBotCacheManager.getCachedBot(botInstanceId),
                        item.getReference(),
                        objectMapper.writeValueAsString(item));
            }
        } catch (JsonProcessingException e) {
            result = "Failed to process results: " + e.getMessage();
        }

        return result;
    }
}
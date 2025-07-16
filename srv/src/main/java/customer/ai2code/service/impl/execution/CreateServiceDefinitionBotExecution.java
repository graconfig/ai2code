package customer.ai2code.service.impl.execution;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.request.ParameterInfo;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataResponseException;

import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.V0001;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.ZcGenddlsPAutoActiveCDSContext;
import cds.gen.zsrvd_gensrvd.ZcGensrvdTAutoActiveSRVDSRVBContext;
import cds.gen.zsrvd_gensrvd.ZsrvdGensrvd;
import cds.gen.zsrvd_gensrvd.ZtgensrvdL;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.execution.BotExecution;

@BotExecutor(name = "Call Remote OData", description = "Implementation for Call S4/HANA OP OData", version = "1.0", enabled = true)
public class CreateServiceDefinitionBotExecution implements BotExecution {

    private final ContextService contextService;
    private final ZsrvdGensrvd zsrvdGensrvd;
    private final ObjectMapper objectMapper;
    @Autowired
    private ParameterInfo parameterInfo;

    // 默认构造函数
    public CreateServiceDefinitionBotExecution(ContextService contextService,
            ZsrvdGensrvd zsrvdGensrvd, ObjectMapper objectMapper) {
        this.contextService = contextService;
        this.zsrvdGensrvd = zsrvdGensrvd;
        this.objectMapper = objectMapper;
    }

    @ExecuteMethod
    public String execute(
            @ExecuteParameter(name = "botInstanceId", description = "Bot Instance") String botInstanceId,
            @ExecuteParameter(name = "autoActiveSRVDSRVBContext", description = "Auto Activate Context") ZcGensrvdTAutoActiveSRVDSRVBContext autoActiveSRVDSRVBContext) {
        String returnMessage = "";// 返回消息
        Collection<ZtgensrvdL> results;// OData返回结果

        // 填充报文

        // 调用action
        try {
            zsrvdGensrvd.emit(autoActiveSRVDSRVBContext);
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
            results = autoActiveSRVDSRVBContext.getResult();
            if (results == null) {
                returnMessage = "OData Response is null";
            } else {
                returnMessage = objectMapper.writeValueAsString(results);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnMessage;
    }

}
